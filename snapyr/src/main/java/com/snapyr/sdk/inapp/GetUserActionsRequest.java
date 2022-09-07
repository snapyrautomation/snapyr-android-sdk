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
package com.snapyr.sdk.inapp;

import com.snapyr.sdk.http.HTTPException;
import com.snapyr.sdk.http.ReadConnection;
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.services.ServiceFacade;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GetUserActionsRequest {
    public static final String AckInAppActionUrl = "v1/actions/";

    static String getUrl(String user, String token) {
        return AckInAppActionUrl + user;
    }

    static List<InAppMessage> execute(String user, String token) throws IOException {
        String builtUrl = getUrl(user, token);
        List<Object> results;
        HttpURLConnection conn;
        ReadConnection rc = null;
        List<InAppMessage> created = new LinkedList<>();
        try {
            conn = ServiceFacade.getConnectionFactory().engineRequest(builtUrl, "GET");
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new HTTPException(responseCode, "failed to fetch in-app messages", "");
            }
            rc = new ReadConnection(conn);
            results =
                    ServiceFacade.getCartographer()
                            .fromJsonArray(Utils.buffer(rc.getInputStream()));
            for (Object actionMap : results) {
                try {
                    if (actionMap instanceof Map) {
                        final SnapyrAction action =
                                SnapyrAction.create((Map<String, Object>) actionMap);
                        created.add(new InAppMessage(action));
                    }
                } catch (InAppMessage.MalformedMessageException e) {
                    ServiceFacade.getLogger().error(e, "failed parsing in-app message");
                }
            }
        } finally {
            if (rc != null) {
                rc.close();
            }
        }
        return created;
    }
}
