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

import android.util.Base64;
import com.snapyr.sdk.core.BuildConfig;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Abstraction to customize how connections are created. This is can be used to point our SDK at
 * your proxy server for instance.
 */
public class ConnectionFactory {
    static final String USER_AGENT = "snapyr-android/" + BuildConfig.VERSION_NAME;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    private static final String PROD_ENGINE_ENDPOINT = "https://engine.snapyr.com/";
    private static final String PROD_CONFIG_ENDPOINT = "https://api.snapyr.com/sdk/";
    private static final String STAGE_CONFIG_ENDPOINT = "https://stage-api.snapyrdev.net/sdk/";
    private static final String STAGE_ENGINE_ENDPOINT =
            "https://stage-engine.snapyrdev.net/v1/batch";
    private static final String DEV_CONFIG_ENDPOINT = "https://dev-api.snapyrdev.net/sdk/";
    private static final String DEV_ENGINE_ENDPOINT = "https://dev-engine.snapyrdev.net/";
    private final String engineURL;
    private final String configURL;
    private final String writeKey;

    public ConnectionFactory(String writeKey, ConnectionFactory.Environment environment) {
        this.writeKey = writeKey;
        switch (environment) {
            case DEV:
                engineURL = DEV_ENGINE_ENDPOINT;
                configURL = DEV_CONFIG_ENDPOINT;
                break;
            case STAGE:
                engineURL = STAGE_ENGINE_ENDPOINT;
                configURL = STAGE_CONFIG_ENDPOINT;
                break;
            case PROD:
            default:
                engineURL = PROD_ENGINE_ENDPOINT;
                configURL = PROD_CONFIG_ENDPOINT;
        }
    }

    private static String authorizationHeader(String writeKey) {
        return "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP);
    }

    /** Return a {@link HttpURLConnection} that reads JSON formatted project settings. */
    public HttpURLConnection getSettings() throws IOException {
        return openConnection(configURL + writeKey, "GET");
    }

    /**
     * Return a {@link HttpURLConnection} that writes batched payloads to {@code
     * https://engine.snapyr.com/v1/import}.
     */
    public WriteConnection postBatch() throws IOException {
        return new WriteConnection(engineRequest("v1/batch", "POST"));
    }

    public HttpURLConnection engineRequest(String path, String method) throws IOException {
        HttpURLConnection connection = openConnection(engineURL + path, method);
        connection.setRequestProperty("Authorization", authorizationHeader(writeKey));
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        connection.setRequestMethod(method);
        if (method == "POST") {
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
        } else if (method == "GET") {
            connection.setDoInput(false);
        }
        return connection;
    }

    public HttpURLConnection openConnection(String url, String method) throws IOException {
        URL requestedURL;

        try {
            requestedURL = new URL(url);
        } catch (MalformedURLException e) {
            throw new IOException("Attempted to use malformed url: " + url, e);
        }

        HttpURLConnection connection = (HttpURLConnection) requestedURL.openConnection();
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        return connection;
    }

    public enum Environment {
        PROD,
        STAGE,
        DEV
    }
}
