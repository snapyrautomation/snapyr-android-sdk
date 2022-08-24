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

import static java.net.HttpURLConnection.HTTP_OK;

import com.snapyr.sdk.internal.Cartographer;
import com.snapyr.sdk.internal.Utils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/** HTTP client which can upload payloads and fetch project settings from the Snapyr public API. */
public class Client {

    final ConnectionFactory connectionFactory;
    final String writeKey;

    public Client(String writeKey, ConnectionFactory connectionFactory) {
        this.writeKey = writeKey;
        this.connectionFactory = connectionFactory;
    }

    private static ReadConnection createGetConnection(HttpURLConnection connection)
            throws IOException {
        return new ReadConnection(connection);
    }

    public WriteConnection upload() throws IOException {
        return new WriteConnection(connectionFactory.upload(writeKey));
    }

    public Map<String, Object> fetchSettings() throws IOException {
        Map<String, Object> results = null;
        HttpURLConnection connection = null;
        try {
            connection = connectionFactory.projectSettings(writeKey);
            int responseCode = connection.getResponseCode();
            if (responseCode != HTTP_OK) {
                connection.disconnect();
                throw new IOException(
                        "HTTP " + responseCode + ": " + connection.getResponseMessage());
            }
            results = Cartographer.INSTANCE.fromJson(Utils.buffer(connection.getInputStream()));
        } catch (IOException e) {
            throw e; // rethrow, just catching to close the connection
        } finally {
            connection.disconnect();
        }
        return results;
    }

    /** Represents an HTTP exception thrown for unexpected/non 2xx response codes. */
    public static class HTTPException extends IOException {
        public final int responseCode;
        public final String responseMessage;
        public final String responseBody;

        public HTTPException(int responseCode, String responseMessage, String responseBody) {
            super("HTTP " + responseCode + ": " + responseMessage + ". Response: " + responseBody);
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
            this.responseBody = responseBody;
        }

        public boolean is4xx() {
            return responseCode >= 400 && responseCode < 500;
        }
    }
}
