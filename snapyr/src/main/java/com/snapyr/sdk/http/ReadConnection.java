package com.snapyr.sdk.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

/**
 * Wraps an HTTP connection. Callers can either read from the connection via the {@link
 * InputStream} or write to the connection via {@link OutputStream}.
 */
public class ReadConnection implements Closeable {
    final HttpURLConnection connection;

    ReadConnection(HttpURLConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection == null");
        }
        this.connection = connection;
    }

    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
        return connection.getResponseMessage();
    }

    public InputStream getInputStream() throws IOException {
        try {
            return connection.getInputStream();
        } catch (IOException ignored) {
            return connection.getErrorStream();
        }
    }

    @Override
    public void close() throws IOException {
        connection.disconnect();
        if (connection.getInputStream() != null){
            connection.getInputStream().close();
        }
    }
}