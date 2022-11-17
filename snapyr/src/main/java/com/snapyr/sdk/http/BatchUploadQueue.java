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
package com.snapyr.sdk.http;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.inapp.InAppFacade;
import com.snapyr.sdk.internal.BasePayload;
import com.snapyr.sdk.internal.Private;
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.services.ServiceFacade;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Entity that queues payloads on disks and uploads them periodically. */
public class BatchUploadQueue {

    /**
     * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
     * 32KB, this bounds the queue size to ~32MB (ignoring headers), which also leaves room for
     * QueueFile's 2GB limit.
     */
    public static final int MAX_QUEUE_SIZE = 1000;
    /** Our servers only accept payloads up to 32KB. */
    public static final int MAX_PAYLOAD_SIZE = 32000; // 32KB.

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
    private final BatchQueue batchQueue;
    private final int flushQueueSize;
    private final Handler handler;
    private final HandlerThread snapyrThread;
    private final ScheduledExecutorService flushScheduler;
    private int flushesPerformed;

    public BatchUploadQueue(
            Context context,
            long flushIntervalInMillis,
            int flushQueueSize,
            @Nullable BatchQueue queueOverride) {
        this.context = context;
        this.flushQueueSize = flushQueueSize;
        this.flushScheduler =
                Executors.newScheduledThreadPool(1, new Utils.AnalyticsThreadFactory());

        BatchQueue BatchQueue = queueOverride;
        if (BatchQueue == null) {
            try {
                File folder = context.getDir("snapyr-disk-queue", Context.MODE_PRIVATE);
                QueueFile queueFile = createQueueFile(folder, "payload_queue");
                BatchQueue = new BatchQueue.PersistentQueue(queueFile);
            } catch (IOException e) {
                ServiceFacade.getLogger()
                        .error(e, "Could not create disk queue. Falling back to memory queue.");
                BatchQueue = new BatchQueue.MemoryQueue();
            }
        }
        this.batchQueue = BatchQueue;

        snapyrThread = new HandlerThread(SNAPYR_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        snapyrThread.start();
        handler = new SnapyrDispatcherHandler(snapyrThread.getLooper(), this);

        long initialDelay = BatchQueue.size() >= flushQueueSize ? 0L : flushIntervalInMillis;
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

    public int getFlushesPerformed() {
        return this.flushesPerformed;
    }

    public void performEnqueue(BasePayload original) {
        // Make a copy of the payload so we don't mutate the original.
        ValueMap payload = new ValueMap();
        payload.putAll(original);
        if (batchQueue.size() >= MAX_QUEUE_SIZE) {
            synchronized (flushLock) {
                // Double checked locking, the network executor could have removed payload from the
                // queue
                // to bring it below our capacity while we were waiting.
                if (batchQueue.size() >= MAX_QUEUE_SIZE) {
                    ServiceFacade.getLogger()
                            .info(
                                    "Queue is at max capacity (%s), removing oldest payload.",
                                    batchQueue.size());
                    try {
                        batchQueue.remove(1);
                    } catch (IOException e) {
                        ServiceFacade.getLogger()
                                .error(e, "Unable to remove oldest payload from queue.");
                        return;
                    }
                }
            }
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream cos = ServiceFacade.getCrypto().encrypt(bos);
            ServiceFacade.getCartographer().toJson(payload, new OutputStreamWriter(cos));
            cos.close();
            byte[] bytes = bos.toByteArray();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_PAYLOAD_SIZE) {
                throw new IOException("Could not serialize payload " + payload);
            }
            batchQueue.add(bytes);
        } catch (IOException e) {
            ServiceFacade.getLogger()
                    .error(e, "Could not add payload %s to queue: %s.", payload, batchQueue);
            return;
        }

        ServiceFacade.getLogger()
                .verbose(
                        "Enqueued %s payload. %s elements in the queue.",
                        original, batchQueue.size());
        if (batchQueue.size() >= flushQueueSize) {
            submitFlush();
        }
    }

    /** Enqueues a flush message to the handler. */
    public void flush() {
        handler.sendMessage(handler.obtainMessage(SnapyrDispatcherHandler.REQUEST_FLUSH));
    }

    /** Submits a flush message to the network executor. */
    public void submitFlush() {
        if (!shouldFlush()) {
            return;
        }

        ExecutorService networkExecutor = ServiceFacade.getNetworkExecutor();
        if (networkExecutor.isShutdown()) {
            ServiceFacade.getLogger()
                    .info(
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
        return batchQueue.size() > 0 && Utils.isConnected(context);
    }

    /** Upload payloads to our servers and remove them from the queue file. */
    public void performFlush() {
        // Conditions could have changed between enqueuing the task and when it is run.
        if (!shouldFlush()) {
            return;
        }

        this.flushesPerformed++;

        ServiceFacade.getLogger().verbose("Uploading payloads in queue to Snapyr.");
        int payloadsUploaded = 0;
        WriteConnection connection = null;
        try {
            // Open a connection.
            connection = ServiceFacade.getConnectionFactory().postBatch();

            // Write the payloads into the OutputStream.
            payloadsUploaded =
                    BatchUploadRequest.execute(
                            this.batchQueue,
                            connection.getOutputStream(),
                            ServiceFacade.getCrypto());

            // Process the response.
            int responseCode = connection.getResponseCode();
            InputStream inputStream = connection.getInputStream();
            String responseBody = null;
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
                throw new HTTPException(
                        responseCode, connection.getResponseMessage(), responseBody);
            } else if (inputStream != null) {
                responseBody = Utils.readFully(inputStream);
                handleActionsIfAny(responseBody);
            }

            if (BatchUploadRequest.DEBUG_MODE) {
                Log.e("Snapyr", "Response from engine:");
                BatchUploadRequest.largeLog("Snapyr", responseBody);
            }

            Utils.closeQuietly(inputStream);
            connection.close();
        } catch (HTTPException e) {
            if (e.is4xx() && e.responseCode != 429) {
                // Simply log and proceed to remove the rejected payloads from the queue.
                ServiceFacade.getLogger()
                        .error(e, "Payloads were rejected by server. Marked for removal.");
                try {
                    batchQueue.remove(payloadsUploaded);
                } catch (IOException e1) {
                    ServiceFacade.getLogger()
                            .error(
                                    e,
                                    "Unable to remove "
                                            + payloadsUploaded
                                            + " payload(s) from queue.");
                }
                return;
            } else {
                ServiceFacade.getLogger().error(e, "Error while uploading payloads");
                return;
            }
        } catch (IOException e) {
            ServiceFacade.getLogger().error(e, "Error while uploading payloads");
            return;
        } catch (Exception e) {
            ServiceFacade.getLogger().error(e, "Error while uploading payloads");

        } finally {
            Utils.closeQuietly(connection);
        }

        try {
            batchQueue.remove(payloadsUploaded);
        } catch (IOException e) {
            ServiceFacade.getLogger()
                    .error(e, "Unable to remove " + payloadsUploaded + " payload(s) from queue.");
            return;
        }

        ServiceFacade.getLogger()
                .verbose(
                        "Uploaded %s payloads. %s remain in the queue.",
                        payloadsUploaded, batchQueue.size());
        if (batchQueue.size() > 0) {
            performFlush(); // Flush any remaining items.
        }
    }

    void handleActionsIfAny(String uploadResponse) {
        try {
            Object response = ServiceFacade.getCartographer().parseJson(uploadResponse);
            if (response instanceof List) {
                for (Object eventResponse : (List) response) {
                    handleEventActions((Map<String, Object>) eventResponse);
                }
            } else if (response instanceof Map) {
                handleEventActions((Map<String, Object>) response);
            }
        } catch (IOException e) {
            ServiceFacade.getLogger().error(e, "Error parsing upload response");
        }
    }

    void handleEventActions(Map<String, Object> eventResponse) {
        if (eventResponse.containsKey("actions") && eventResponse.get("actions") != null) {
            List<Map<String, Object>> actionMapList =
                    (List<Map<String, Object>>) eventResponse.get("actions");

            for (Map<String, Object> actionMap : actionMapList) {
                final SnapyrAction action = SnapyrAction.create(actionMap);
                InAppFacade.processTrackResponse(action);
            }
        }
    }

    public void shutdown() {
        flushScheduler.shutdownNow();
        snapyrThread.quit();
        Utils.closeQuietly(batchQueue);
    }

    static class SnapyrDispatcherHandler extends Handler {
        static final int REQUEST_FLUSH = 1;
        @Private static final int REQUEST_ENQUEUE = 0;
        private final BatchUploadQueue snapyrIntegration;

        SnapyrDispatcherHandler(Looper looper, BatchUploadQueue snapyrIntegration) {
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
