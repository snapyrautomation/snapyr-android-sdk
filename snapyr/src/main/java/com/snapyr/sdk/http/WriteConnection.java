package com.snapyr.sdk.http;

import android.text.TextUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

/**
 * Wraps an HTTP connection. Callers can either read from the connection via the {@link
 * InputStream} or write to the connection via {@link OutputStream}.
 */
public class WriteConnection extends ReadConnection {
    final HttpURLConnection connection;
    final Boolean gzipped;

    public WriteConnection(HttpURLConnection connection) {
        super(connection);
        String contentEncoding = connection.getRequestProperty("Content-Encoding");
        this.gzipped = TextUtils.equals("gzip", contentEncoding);
        this.connection = connection;
    }

    public OutputStream getOutputStream() throws IOException {
        if (gzipped) {
            return new GZIPOutputStream(connection.getOutputStream());
        } else {
            return connection.getOutputStream();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (connection.getOutputStream() != null){
            connection.getOutputStream().close();
        }
    }
}