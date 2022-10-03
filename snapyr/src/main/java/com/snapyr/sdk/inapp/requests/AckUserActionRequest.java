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
package com.snapyr.sdk.inapp.requests;

import com.snapyr.sdk.http.HTTPException;
import com.snapyr.sdk.services.ServiceFacade;
import java.io.IOException;
import java.net.HttpURLConnection;

public class AckUserActionRequest {
    public static final String AckInAppActionUrl = "v1/actions/";

    public static String getUrl(String user, String token) {
        return AckInAppActionUrl + user + "?actionToken=" + token + "&status=delivered";
    }

    public static void execute(String user, String token) throws IOException {
        String builtUrl = getUrl(user, token);
        HttpURLConnection conn = null;
        try {
            conn = ServiceFacade.getConnectionFactory().engineRequest(builtUrl, "POST");
            // NB "output" stream on URLConnection is for client sending request payload/body.
            // "input" stream is for reading the response body back from the server.
            // These ack requests have no payload despite being POST
            conn.setDoOutput(false);
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new HTTPException(responseCode, "failed to ack inapp message", "");
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
