package com.snapyr.sdk.http;

import java.io.IOException;

/** Represents an HTTP exception thrown for unexpected/non 2xx response codes. */
public class HTTPException extends IOException {
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