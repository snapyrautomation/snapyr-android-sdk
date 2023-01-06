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

import android.util.JsonWriter;
import android.util.Log;
import com.snapyr.sdk.internal.Private;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.services.Crypto;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;

/** A wrapper that emits a JSON formatted batch payload to the underlying writer. */
public class BatchUploadRequest implements Closeable, BatchQueue.ElementVisitor {
    /**
     * Our servers only accept batches up to 500KB. This limit is 475KB to account for extra data
     * that is not present in payloads themselves, but is added later, such as {@code sentAt},
     * {@code integrations} and other json tokens.
     */
    @Private static final int MAX_BATCH_SIZE = 475000; // 475KB.

    @Private static final Charset UTF_8 = Charset.forName("UTF-8");
    static final String SNAPYR_KEY = "Snapyr";

    public static final boolean DEBUG_MODE = false;
    StringBuilder debugString = new StringBuilder();
    private boolean needsComma = false;
    private JsonWriter jsonWriter;
    private BufferedWriter bufferedWriter;
    private Crypto crypto;
    int size;
    int payloadCount;

    public static int execute(BatchQueue queue, OutputStream stream, Crypto crypto)
            throws IOException {
        BatchUploadRequest uploader = new BatchUploadRequest(stream, crypto);
        try {
            uploader.beginObject();
            uploader.beginBatchArray();
            queue.forEach(uploader);
            uploader.endBatchArray();
            uploader.endObject();
            uploader.close();
            if (DEBUG_MODE) {
                Log.e("Snapyr", "Payload sent to Snapyr engine:");
                largeLog("Snapyr", uploader.debugString.toString());
            }
        } finally {
            uploader.close();
            stream.close();
        }
        return uploader.payloadCount;
    }

    private BatchUploadRequest(OutputStream stream, Crypto crypto) {
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(stream));
        this.jsonWriter = new JsonWriter(bufferedWriter);
        this.crypto = crypto;
    }

    @Override
    public boolean read(InputStream in, int length) throws IOException {
        InputStream is = this.crypto.decrypt(in);
        final int newSize = size + length;
        if (newSize > MAX_BATCH_SIZE) {
            return false;
        }
        size = newSize;
        byte[] data = new byte[length];
        //noinspection ResultOfMethodCallIgnored
        is.read(data, 0, length);
        // Remove trailing whitespace.
        emitPayloadObject(new String(data, UTF_8).trim());
        payloadCount++;
        return true;
    }

    public static void largeLog(String tag, String content) {
        if (content.length() > 4000) {
            Log.e(tag, content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.e(tag, content);
        }
    }

    BatchUploadRequest beginObject() throws IOException {
        jsonWriter.beginObject();
        if (DEBUG_MODE) {
            debugString.append("{");
        }
        return this;
    }

    BatchUploadRequest beginBatchArray() throws IOException {
        jsonWriter.name("batch").beginArray();
        needsComma = false;
        if (DEBUG_MODE) {
            debugString.append("\"batch\":[");
        }
        return this;
    }

    BatchUploadRequest emitPayloadObject(String payload) throws IOException {
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

    BatchUploadRequest endBatchArray() throws IOException {
        if (!needsComma) {
            throw new IOException("At least one payload must be provided.");
        }
        jsonWriter.endArray();
        if (DEBUG_MODE) {
            debugString.append("]");
        }
        return this;
    }

    BatchUploadRequest endObject() throws IOException {
        /**
         * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can be
         * used to correct the original timestamp in situations where the local clock cannot be
         * trusted, for example in our mobile libraries. The sentAt and receivedAt timestamps will
         * be assumed to have occurred at the same time, and therefore the difference is the local
         * clock skew.
         */
        jsonWriter.name("sentAt").value(Utils.toISO8601Date(new Date())).endObject();
        if (DEBUG_MODE) {
            debugString.append(",\"sentAt\":\"" + Utils.toISO8601Date(new Date()) + "\"}");
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        jsonWriter.close();
        bufferedWriter.close();
    }
}
