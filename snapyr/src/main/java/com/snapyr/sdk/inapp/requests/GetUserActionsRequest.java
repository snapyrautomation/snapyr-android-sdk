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

import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.http.HTTPException;
import com.snapyr.sdk.http.ReadConnection;
import com.snapyr.sdk.inapp.InAppMessage;
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

    public static List<InAppMessage> execute(String user) {
        String builtUrl = AckInAppActionUrl + user;
        HttpURLConnection conn;
        ReadConnection rc = null;
        List<InAppMessage> created = new LinkedList<>();

        if (Utils.isNullOrEmpty(user)) {
            return created; // no point in querying if there's no user
        }

        try {
            conn = ServiceFacade.getConnectionFactory().engineRequest(builtUrl, "GET");
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new HTTPException(responseCode, "failed to fetch in-app messages", "");
            }
            rc = new ReadConnection(conn);
            ValueMap results =
                    new ValueMap(
                            ServiceFacade.getCartographer()
                                    .fromJson(Utils.buffer(rc.getInputStream())));

            for (Object actionMap : results.getList("actions", ValueMap.class)) {
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

        } catch (IOException e) {
            ServiceFacade.getLogger().error(e, "failed polling for in-app messages");
        } finally {
            if (rc != null) {
                try {
                    rc.close();
                } catch (IOException e) {
                }
            }
        }
        return created;
    }
}
