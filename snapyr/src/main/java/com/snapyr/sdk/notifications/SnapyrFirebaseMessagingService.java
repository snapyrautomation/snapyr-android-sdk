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
package com.snapyr.sdk.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.Snapyr;
import java.util.Map;

public class SnapyrFirebaseMessagingService extends FirebaseMessagingService {

    public SnapyrFirebaseMessagingService() {
        super();
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        try {
            Snapyr.with(this).setPushNotificationToken(token);
        } catch (Exception e) {
            // do nothing if Snapyr is not yet initialized
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        // Log.e("Snapyr", "MESSAGE RECEIVED:");
        // Log.e("Snapyr", String.valueOf(remoteMessage));

        com.snapyr.sdk.Properties properties = new com.snapyr.sdk.Properties();
        properties.putAll(data);
        Snapyr.with(this).pushNotificationReceived(properties);

        Snapyr.with(this).getNotificationHandler().showRemoteNotification(data);
    }
}
