package com.snapyr.sdk.http;

import static java.net.HttpURLConnection.HTTP_OK;

import com.snapyr.sdk.internal.Cartographer;
import com.snapyr.sdk.internal.Utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

public class SettingsRequest {
    public static Map<String, Object> execute() throws IOException {
        Map<String, Object> results;
        HttpURLConnection connection = null;
        try {
            connection = ConnectionFactory.getInstance().getSettings();
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
}
