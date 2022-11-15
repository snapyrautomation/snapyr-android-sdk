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

import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.PushTemplate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class SnapyrFirebaseMessagingService extends FirebaseMessagingService {
    private SnapyrNotificationListener activityHandler;

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        SnapyrNotification snapyrNotification;
        try {
            snapyrNotification = new SnapyrNotification(remoteMessage);
        } catch (Exception e) {
            Log.e("Snapyr", "Error parsing Snapyr notification; returning.", e);
            return;
        }

//        Map<String, String> rawData = remoteMessage.getData();
//
//        String snapyrDataJson = rawData.get("snapyr");
//        if (snapyrDataJson == null) {
//            Log.i(
//                    "Snapyr",
//                    "onMessageReceived: No 'snapyr' data found on notification payload (not a Snapyr notification); skipping.");
//            return;
//        }
//
//        JSONObject jsonData = null;
//        try {
//            jsonData = new JSONObject(snapyrDataJson);
//        } catch (Exception e) {
//            Log.e(
//                    "Snapyr",
//                    "onMessageReceived: Invalid message - encountered JSON error trying to parse payload JSON; returning.",
//                    e);
//            return;
//        }
//
//        ValueMap data = new ValueMap();
//
//        for (Iterator<String> it = jsonData.keys(); it.hasNext(); ) {
//            String key = it.next();
//            String value = null;
//            try {
//                value = jsonData.getString(key);
//            } catch (JSONException ignored) {
//            }
//            data.put(key, value);
//        }

        Snapyr snapyrInstance = SnapyrNotificationUtils.getSnapyrInstance(this);
        if (snapyrInstance == null) {
            Log.e(
                    "Snapyr",
                    "Notification service couldn't initialize Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.");
            return;
        }

        PushTemplate template = processPushTemplate(snapyrNotification, snapyrInstance);
        if (template != null) {
            // rich push, inject the template data into the context data we're passing down
            snapyrNotification.setPushTemplate(template);
//            data.put(SnapyrNotificationHandler.ACTION_BUTTONS_KEY, template);
        }

        com.snapyr.sdk.Properties properties = new com.snapyr.sdk.Properties().putValue(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, snapyrNotification.actionToken).putValue(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY, snapyrNotification.deepLinkUrl.toString());
//        properties.putAll(data);
        snapyrInstance.pushNotificationReceived(properties);

        snapyrInstance.getNotificationHandler().showRemoteNotification(snapyrNotification);
        sendPushReceivedBroadcast(snapyrNotification, remoteMessage);
        snapyrInstance.flush();
    }

    private void sendPushReceivedBroadcast(SnapyrNotification snapyrNotification, RemoteMessage remoteMessage) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: sendPushReceivedBroadcast");
//        Intent listenerIntent = getIntent();
//        Uri inputData = listenerIntent.getData();

        Intent pushReceivedIntent = remoteMessage.toIntent();
        pushReceivedIntent.setAction(SnapyrNotificationHandler.NOTIFICATION_RECEIVED_ACTION);
//        deepLinkIntent.setData(listenerIntent.getData());
        pushReceivedIntent.setPackage(
                this.getPackageName()); // makes this intent "explicit" which allows it to reach
        // a manifest-defined receiver
//        deepLinkIntent.putExtras(listenerIntent); // forward all extras, i.e. Snapyr-defined data
        this.sendBroadcast(pushReceivedIntent);
        Log.e("XXX", "SnapyrFirebaseMessagingService: BROADCAST SENT");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private PushTemplate processPushTemplate(@NonNull SnapyrNotification snapyrNotification, Snapyr sdkInstance) {
        if (snapyrNotification.templateId == null || snapyrNotification.templateModified == null) {
            return null;
        }

        PushTemplate template = sdkInstance.getPushTemplates().get(snapyrNotification.templateId);
        if ((template != null) && (!template.getModified().before(snapyrNotification.templateModified))) {
            // if the modified date in the push payload is equal to or older than the cached
            // templates we're good to go and can just used the cached template value
            return template;
        }

        // either missing template or it's older than the timestamp in the push notification
        // re-fetch the sdk config and retry
        sdkInstance.RefreshConfiguration(true);
        return sdkInstance.getPushTemplates().get(snapyrNotification.templateId);
    }
}
