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
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.internal.PushTemplate;

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

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            super.onMessageReceived(remoteMessage);
            handleMessageReceived(remoteMessage);
        } catch (Exception e) {
            Log.e(
                    "Snapyr",
                    "Notification service encountered an unexpected error while attempting to process and display an incoming push notification",
                    e);
        }
    }

    private void handleMessageReceived(RemoteMessage remoteMessage) {
        SnapyrNotification snapyrNotification;
        Snapyr snapyrInstance;

        try {
            snapyrNotification = new SnapyrNotification(remoteMessage);
        } catch (SnapyrNotification.NonSnapyrMessageException e) {
            // Non-Snapyr notification - probably not really an error, but nothing for us to do
            Log.i("Snapyr", e.getMessage());
            return;
        } catch (Exception e) {
            Log.e("Snapyr", "Error parsing Snapyr notification; returning.", e);
            return;
        }

        try {
            snapyrInstance = SnapyrNotificationUtils.getSnapyrInstance(this);
            if (snapyrInstance == null) {
                Log.e(
                        "Snapyr",
                        "Notification service couldn't initialize Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.");
                return;
            }
        } catch (Exception e) {
            Log.e(
                    "Snapyr",
                    "Notification service encountered exception while initializing Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.");
            return;
        }

        PushTemplate template = processPushTemplate(snapyrNotification, snapyrInstance);
        if (template != null) {
            // rich push, inject the template data into the context data we're passing down
            snapyrNotification.setPushTemplate(template);
        }

        com.snapyr.sdk.Properties properties =
                new com.snapyr.sdk.Properties()
                        .putValue(
                                SnapyrNotificationHandler.NOTIF_TOKEN_KEY,
                                snapyrNotification.actionToken)
                        .putValue(
                                SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY,
                                (snapyrNotification.deepLinkUrl != null)
                                        ? snapyrNotification.deepLinkUrl.toString()
                                        : null);

        snapyrInstance.pushNotificationReceived(properties);

        snapyrInstance.getNotificationHandler().showRemoteNotification(snapyrNotification);
        sendPushReceivedBroadcast(snapyrNotification);
        snapyrInstance.flush();
    }

    private void sendPushReceivedBroadcast(SnapyrNotification snapyrNotification) {
        Intent pushReceivedIntent =
                new Intent(SnapyrNotificationHandler.NOTIFICATION_RECEIVED_ACTION);
        pushReceivedIntent.putExtra("snapyr.notification", snapyrNotification);
        // make this intent "explicit" which allows it to reach a manifest-defined receiver
        pushReceivedIntent.setPackage(this.getPackageName());
        this.sendBroadcast(pushReceivedIntent);
    }

    private PushTemplate processPushTemplate(
            @NonNull SnapyrNotification snapyrNotification, Snapyr sdkInstance) {
        if (snapyrNotification.templateId == null || snapyrNotification.templateModified == null) {
            return null;
        }

        PushTemplate template = sdkInstance.getPushTemplates().get(snapyrNotification.templateId);
        if ((template != null)
                && (!template.getModified().before(snapyrNotification.templateModified))) {
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
