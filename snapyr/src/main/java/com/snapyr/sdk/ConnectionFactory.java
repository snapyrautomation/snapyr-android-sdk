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

    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    static final String USER_AGENT = "snapyr-android/" + BuildConfig.VERSION_NAME;

    private final String configEndpoint;
    private final String engineEndpoint;

    private static final String PROD_CONFIG_ENDPOINT = "https://api.snapyr.com/sdk/";
    private static final String PROD_ENGINE_ENDPOINT = "https://engine.snapyr.com/v1/batch";

    private static final String STAGE_CONFIG_ENDPOINT = "https://stage-api.snapyr.com/sdk/";
    private static final String STAGE_ENGINE_ENDPOINT = "https://stage-engine.snapyr.com/v1/batch";

    private static final String DEV_CONFIG_ENDPOINT = "https://dev-api.snapyr.com/sdk/";
    private static final String DEV_ENGINE_ENDPOINT = "https://dev-engine.snapyr.com/v1/batch";

    public enum Environment {
        PROD,
        STAGE,
        DEV
    }

    public ConnectionFactory() {
        this(Environment.PROD);
    }

    public ConnectionFactory(Environment environment) {
        switch (environment) {
            case DEV:
                configEndpoint = DEV_CONFIG_ENDPOINT;
                engineEndpoint = DEV_ENGINE_ENDPOINT;
                break;
            case STAGE:
                configEndpoint = STAGE_CONFIG_ENDPOINT;
                engineEndpoint = STAGE_ENGINE_ENDPOINT;
                break;
            case PROD:
            default:
                configEndpoint = PROD_CONFIG_ENDPOINT;
                engineEndpoint = PROD_ENGINE_ENDPOINT;
        }
    }

    private String authorizationHeader(String writeKey) {
        return "Basic " + Base64.encodeToString((writeKey + ":").getBytes(), Base64.NO_WRAP);
    }

    /** Return a {@link HttpURLConnection} that reads JSON formatted project settings. */
    public HttpURLConnection projectSettings(String writeKey) throws IOException {
        return openConnection(configEndpoint + writeKey);
    }

    /**
     * Return a {@link HttpURLConnection} that writes batched payloads to {@code
     * https://engine.snapyr.com/v1/import}.
     */
    public HttpURLConnection upload(String writeKey) throws IOException {
        HttpURLConnection connection = openConnection(engineEndpoint);
        connection.setRequestProperty("Authorization", authorizationHeader(writeKey));
        // connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);
        return connection;
    }

    /**
     * Configures defaults for connections opened with {@link #upload(String)}, and {@link
     * #projectSettings(String)}.
     */
    protected HttpURLConnection openConnection(String url) throws IOException {
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
        connection.setDoInput(true);
        return connection;
    }
}
