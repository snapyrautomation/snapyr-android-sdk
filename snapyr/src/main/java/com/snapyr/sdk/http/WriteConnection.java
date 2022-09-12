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

import android.text.TextUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

/**
 * Wraps an HTTP connection. Callers can either read from the connection via the {@link InputStream}
 * or write to the connection via {@link OutputStream}.
 */
public class WriteConnection extends ReadConnection {
    final HttpURLConnection connection;
    final Boolean gzipped;
    GZIPOutputStream gzipStream = null;

    public WriteConnection(HttpURLConnection connection) {
        super(connection);
        String contentEncoding = connection.getRequestProperty("Content-Encoding");
        this.gzipped = TextUtils.equals("gzip", contentEncoding);
        this.connection = connection;
    }

    public OutputStream getOutputStream() throws IOException {
        if (gzipped && gzipStream == null) {
            gzipStream = new GZIPOutputStream(connection.getOutputStream());
            return gzipStream;
        } else {
            return connection.getOutputStream();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (gzipStream != null) {
            gzipStream.finish();
            gzipStream.close();
        }
    }
}
