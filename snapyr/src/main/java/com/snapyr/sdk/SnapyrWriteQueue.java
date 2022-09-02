/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.snapyr.sdk;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;
import androidx.annotation.Nullable;
import com.snapyr.sdk.http.Client;
import com.snapyr.sdk.http.WriteConnection;
import com.snapyr.sdk.inapp.InAppFacade;
import com.snapyr.sdk.integrations.BasePayload;
import com.snapyr.sdk.integrations.Logger;
import com.snapyr.sdk.internal.Cartographer;
import com.snapyr.sdk.internal.Private;
import com.snapyr.sdk.internal.Utils;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Entity that queues payloads on disks and uploads them periodically. */
class SnapyrWriteQueue {

    /**
     * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
     * 32KB, this bounds the queue size to ~32MB (ignoring headers), which also leaves room for
     * QueueFile's 2GB limit.
     */
    static final int MAX_QUEUE_SIZE = 1000;
    /** Our servers only accept payloads < 32KB. */
    static final int MAX_PAYLOAD_SIZE = 32000; // 32KB.
    /**
     * Our servers only accept batches < 500KB. This limit is 475KB to account for extra data that
     * is not present in payloads themselves, but is added later, such as {@code sentAt}, {@code
     * integrations} and other json tokens.
     */
    @Private static final int MAX_BATCH_SIZE = 475000; // 475KB.

    @Private static final Charset UTF_8 = Charset.forName("UTF-8");
    static final String SNAPYR_KEY = "Snapyr";

    private static final String SNAPYR_THREAD_NAME = Utils.THREAD_PREFIX + "SnapyrDispatcher";
    /**
     * We don't want to stop adding payloads to our disk queue when we're uploading payloads. So we
     * upload payloads on a network executor instead.
     *
     * <p>Given: 1. Peek returns the oldest elements 2. Writes append to the tail of the queue 3.
     * Methods on QueueFile are synchronized (so only thread can access it at a time)
     *
     * <p>We offload flushes to the network executor, read the QueueFile and remove entries on it,
     * while we continue to add payloads to the QueueFile on the default Dispatcher thread.
     *
     * <p>We could end up in a case where (assuming MAX_QUEUE_SIZE is 10): 1. Executor reads 10
     * payloads from the QueueFile 2. Dispatcher is told to add an payloads (the 11th) to the queue.
     * 3. Dispatcher sees that the queue size is at it's limit (10). 4. Dispatcher removes an
     * payloads. 5. Dispatcher adds a payload. 6. Executor finishes uploading 10 payloads and
     * proceeds to remove 10 elements from the file. Since the dispatcher already removed the 10th
     * element and added a 11th, this would actually delete the 11th payload that will never get
     * uploaded.
     *
     * <p>This lock is used ensure that the Dispatcher thread doesn't remove payloads when we're
     * uploading.
     */
    @Private final Object flushLock = new Object();

    private final Context context;
    private final PayloadQueue payloadQueue;
    private final Client client;
    private final int flushQueueSize;
    private final Stats stats;
    private final Handler handler;
    private final HandlerThread snapyrThread;
    private final Logger logger;
    private final Cartographer cartographer;
    private final ExecutorService networkExecutor;
    private final ScheduledExecutorService flushScheduler;
    private final SnapyrActionHandler actionHandler;
    private final Crypto crypto;

    SnapyrWriteQueue(
            Context context,
            Client client,
            Cartographer cartographer,
            ExecutorService networkExecutor,
            Stats stats,
            long flushIntervalInMillis,
            int flushQueueSize,
            Logger logger,
            Crypto crypto,
            @Nullable PayloadQueue queueOverride,
            SnapyrActionHandler actionHandler) {
        this.context = context;
        this.client = client;
        this.networkExecutor = networkExecutor;
        this.stats = stats;
        this.logger = logger;
        this.cartographer = cartographer;
        this.flushQueueSize = flushQueueSize;
        this.flushScheduler =
                Executors.newScheduledThreadPool(1, new Utils.AnalyticsThreadFactory());
        this.actionHandler = actionHandler;
        this.crypto = crypto;

        PayloadQueue payloadQueue = queueOverride;
        if (payloadQueue == null) {
            try {
                File folder = context.getDir("snapyr-disk-queue", Context.MODE_PRIVATE);
                QueueFile queueFile = createQueueFile(folder, "payload_queue");
                payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
            } catch (IOException e) {
                logger.error(e, "Could not create disk queue. Falling back to memory queue.");
                payloadQueue = new PayloadQueue.MemoryQueue();
            }
        }
        this.payloadQueue = payloadQueue;

        snapyrThread = new HandlerThread(SNAPYR_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        snapyrThread.start();
        handler = new SnapyrDispatcherHandler(snapyrThread.getLooper(), this);

        long initialDelay = payloadQueue.size() >= flushQueueSize ? 0L : flushIntervalInMillis;
        flushScheduler.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        flush();
                    }
                },
                initialDelay,
                flushIntervalInMillis,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Create a {@link QueueFile} in the given folder with the given name. If the underlying file is
     * somehow corrupted, we'll delete it, and try to recreate the file. This method will throw an
     * {@link IOException} if the directory doesn't exist and could not be created.
     */
    static QueueFile createQueueFile(File folder, String name) throws IOException {
        Utils.createDirectory(folder);
        File file = new File(folder, name);
        try {
            return new QueueFile(file);
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            if (file.delete()) {
                return new QueueFile(file);
            } else {
                throw new IOException(
                        "Could not create queue file (" + name + ") in " + folder + ".");
            }
        }
    }

    void performEnqueue(BasePayload original) {
        // Override any user provided values with anything that was bundled.
        // e.g. If user did Mixpanel: true and it was bundled, this would correctly override it with
        // false so that the server doesn't send that event as well.
        ValueMap providedIntegrations = original.integrations();
        // Make a copy of the payload so we don't mutate the original.
        ValueMap payload = new ValueMap();
        payload.putAll(original);
        if (payloadQueue.size() >= MAX_QUEUE_SIZE) {
            synchronized (flushLock) {
                // Double checked locking, the network executor could have removed payload from the
                // queue
                // to bring it below our capacity while we were waiting.
                if (payloadQueue.size() >= MAX_QUEUE_SIZE) {
                    logger.info(
                            "Queue is at max capacity (%s), removing oldest payload.",
                            payloadQueue.size());
                    try {
                        payloadQueue.remove(1);
                    } catch (IOException e) {
                        logger.error(e, "Unable to remove oldest payload from queue.");
                        return;
                    }
                }
            }
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream cos = crypto.encrypt(bos);
            cartographer.toJson(payload, new OutputStreamWriter(cos));
            byte[] bytes = bos.toByteArray();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_PAYLOAD_SIZE) {
                throw new IOException("Could not serialize payload " + payload);
            }
            payloadQueue.add(bytes);
        } catch (IOException e) {
            logger.error(e, "Could not add payload %s to queue: %s.", payload, payloadQueue);
            return;
        }

        logger.verbose(
                "Enqueued %s payload. %s elements in the queue.", original, payloadQueue.size());
        if (payloadQueue.size() >= flushQueueSize) {
            submitFlush();
        }
    }

    /** Enqueues a flush message to the handler. */
    public void flush() {
        handler.sendMessage(handler.obtainMessage(SnapyrDispatcherHandler.REQUEST_FLUSH));
    }

    /** Submits a flush message to the network executor. */
    void submitFlush() {
        if (!shouldFlush()) {
            return;
        }

        if (networkExecutor.isShutdown()) {
            logger.info(
                    "A call to flush() was made after shutdown() has been called.  In-flight events may not be uploaded right away.");
            return;
        }

        networkExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        synchronized (flushLock) {
                            performFlush();
                        }
                    }
                });
    }

    private boolean shouldFlush() {
        return payloadQueue.size() > 0 && Utils.isConnected(context);
    }

    /** Upload payloads to our servers and remove them from the queue file. */
    void performFlush() {
        // Conditions could have changed between enqueuing the task and when it is run.
        if (!shouldFlush()) {
            return;
        }

        logger.verbose("Uploading payloads in queue to Snapyr.");
        int payloadsUploaded = 0;
        WriteConnection connection = null;
        try {
            // Open a connection.
            connection = client.upload();

            // Write the payloads into the OutputStream.
            BatchPayloadWriter writer =
                    new BatchPayloadWriter(connection.getOutputStream())
                            .beginObject()
                            .beginBatchArray();
            PayloadWriter payloadWriter = new PayloadWriter(writer, crypto);
            payloadQueue.forEach(payloadWriter);
            writer.endBatchArray().endObject().close();
            // Don't use the result of QueueFiles#forEach, since we may not upload the last element.
            payloadsUploaded = payloadWriter.payloadCount;

            // Upload the payloads.
            int responseCode = connection.getResponseCode();
            InputStream inputStream = connection.getInputStream();
            String responseBody = null;
            // Log.e("Snapyr", "flush code: " + responseCode);
            if (responseCode >= 300) {
                if (inputStream != null) {
                    try {
                        responseBody = Utils.readFully(inputStream);
                    } catch (IOException e) {
                        responseBody = "Could not read response body for rejected message: " + e;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                }
                throw new Client.HTTPException(
                        responseCode, connection.getResponseMessage(), responseBody);
            } else if (inputStream != null) {
                responseBody = Utils.readFully(inputStream);
                logger.info("flush response: " + responseBody);
                handleActionsIfAny(responseBody);
            }

            Utils.closeQuietly(inputStream);
            connection.close();
        } catch (Client.HTTPException e) {
            if (e.is4xx() && e.responseCode != 429) {
                // Simply log and proceed to remove the rejected payloads from the queue.
                logger.error(e, "Payloads were rejected by server. Marked for removal.");
                try {
                    payloadQueue.remove(payloadsUploaded);
                } catch (IOException e1) {
                    logger.error(
                            e, "Unable to remove " + payloadsUploaded + " payload(s) from queue.");
                }
                return;
            } else {
                logger.error(e, "Error while uploading payloads");
                return;
            }
        } catch (IOException e) {
            logger.error(e, "Error while uploading payloads");
            return;
        } catch (Exception e) {
            logger.error(e, "Error while uploading payloads");

        } finally {
            Utils.closeQuietly(connection);
        }

        try {
            payloadQueue.remove(payloadsUploaded);
        } catch (IOException e) {
            logger.error(e, "Unable to remove " + payloadsUploaded + " payload(s) from queue.");
            return;
        }

        logger.verbose(
                "Uploaded %s payloads. %s remain in the queue.",
                payloadsUploaded, payloadQueue.size());
        stats.dispatchFlush(payloadsUploaded);
        if (payloadQueue.size() > 0) {
            performFlush(); // Flush any remaining items.
        }
    }

    void handleActionsIfAny(String uploadResponse) {
        try {
            Object response = cartographer.parseJson(uploadResponse);
            if (response instanceof List) {
                for (Object eventResponse : (List) response) {
                    handleEventActions((Map<String, Object>) eventResponse);
                }
            } else if (response instanceof Map) {
                handleEventActions((Map<String, Object>) response);
            }
        } catch (IOException e) {
            logger.error(e, "Error parsing upload response");
        }
    }

    void handleEventActions(Map<String, Object> eventResponse) {
        if (eventResponse.containsKey("actions") && eventResponse.get("actions") != null) {
            List<Map<String, Object>> actionMapList =
                    (List<Map<String, Object>>) eventResponse.get("actions");

            for (Map<String, Object> actionMap : actionMapList) {
                final SnapyrAction action = SnapyrAction.create(actionMap);
                InAppFacade.processTrackResponse(action);

                if (actionHandler != null) {
                    Snapyr.HANDLER.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        actionHandler.handleAction(action);
                                    } catch (Exception e) {
                                        logger.error(
                                                e,
                                                "error handling action: "
                                                        + action.getString("action"));
                                    }
                                }
                            });
                }
            }
        }
    }

    void dispatchAction(SnapyrAction action) {}

    void shutdown() {
        flushScheduler.shutdownNow();
        snapyrThread.quit();
        Utils.closeQuietly(payloadQueue);
    }

    static class PayloadWriter implements PayloadQueue.ElementVisitor {
        final BatchPayloadWriter writer;
        final Crypto crypto;
        int size;
        int payloadCount;

        PayloadWriter(BatchPayloadWriter writer, Crypto crypto) {
            this.writer = writer;
            this.crypto = crypto;
        }

        @Override
        public boolean read(InputStream in, int length) throws IOException {
            InputStream is = crypto.decrypt(in);
            final int newSize = size + length;
            if (newSize > MAX_BATCH_SIZE) {
                return false;
            }
            size = newSize;
            byte[] data = new byte[length];
            //noinspection ResultOfMethodCallIgnored
            is.read(data, 0, length);
            // Remove trailing whitespace.
            writer.emitPayloadObject(new String(data, UTF_8).trim());
            payloadCount++;
            return true;
        }
    }

    /** A wrapper that emits a JSON formatted batch payload to the underlying writer. */
    static class BatchPayloadWriter implements Closeable {
        public static final boolean DEBUG_MODE = false;
        private final JsonWriter jsonWriter;
        /** Keep around for writing payloads as Strings. */
        private final BufferedWriter bufferedWriter;

        StringBuilder debugString = new StringBuilder();
        private boolean needsComma = false;

        BatchPayloadWriter(OutputStream stream) {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(stream));
            jsonWriter = new JsonWriter(bufferedWriter);
        }

        public static void largeLog(String tag, String content) {
            if (content.length() > 4000) {
                Log.e(tag, content.substring(0, 4000));
                largeLog(tag, content.substring(4000));
            } else {
                Log.e(tag, content);
            }
        }

        BatchPayloadWriter beginObject() throws IOException {
            jsonWriter.beginObject();
            if (DEBUG_MODE) {
                debugString.append("{");
            }
            return this;
        }

        BatchPayloadWriter beginBatchArray() throws IOException {
            jsonWriter.name("batch").beginArray();
            needsComma = false;
            if (DEBUG_MODE) {
                debugString.append("\"batch\":[");
            }
            return this;
        }

        BatchPayloadWriter emitPayloadObject(String payload) throws IOException {
            // Payloads already serialized into json when storing on disk. No need to waste cycles
            // deserializing them.
            if (needsComma) {
                bufferedWriter.write(',');
                if (DEBUG_MODE) {
                    debugString.append(",");
                }
            } else {
                needsComma = true;
            }
            bufferedWriter.write(payload);
            if (DEBUG_MODE) {
                debugString.append(payload);
            }
            return this;
        }

        BatchPayloadWriter endBatchArray() throws IOException {
            if (!needsComma) {
                throw new IOException("At least one payload must be provided.");
            }
            jsonWriter.endArray();
            if (DEBUG_MODE) {
                debugString.append("]");
            }
            return this;
        }

        BatchPayloadWriter endObject() throws IOException {
            /**
             * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can
             * be used to correct the original timestamp in situations where the local clock cannot
             * be trusted, for example in our mobile libraries. The sentAt and receivedAt timestamps
             * will be assumed to have occurred at the same time, and therefore the difference is
             * the local clock skew.
             */
            jsonWriter.name("sentAt").value(Utils.toISO8601Date(new Date())).endObject();
            if (DEBUG_MODE) {
                debugString.append(",\"sentAt\":\"" + Utils.toISO8601Date(new Date()) + "\"}");
            }
            return this;
        }

        @Override
        public void close() throws IOException {
            if (DEBUG_MODE) {
                Log.e("Snapyr", "Payload sent to Snapyr engine:");
                largeLog("Snapyr", debugString.toString());
            }
            jsonWriter.close();
        }
    }

    static class SnapyrDispatcherHandler extends Handler {
        static final int REQUEST_FLUSH = 1;
        @Private static final int REQUEST_ENQUEUE = 0;
        private final SnapyrWriteQueue snapyrIntegration;

        SnapyrDispatcherHandler(Looper looper, SnapyrWriteQueue snapyrIntegration) {
            super(looper);
            this.snapyrIntegration = snapyrIntegration;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REQUEST_ENQUEUE:
                    BasePayload payload = (BasePayload) msg.obj;
                    snapyrIntegration.performEnqueue(payload);
                    break;
                case REQUEST_FLUSH:
                    snapyrIntegration.submitFlush();
                    break;
                default:
                    throw new AssertionError("Unknown dispatcher message: " + msg.what);
            }
        }
    }
}
