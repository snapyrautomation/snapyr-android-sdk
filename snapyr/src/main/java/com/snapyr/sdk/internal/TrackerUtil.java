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
package com.snapyr.sdk.internal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationManagerCompat;
import com.snapyr.sdk.Properties;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.notifications.SnapyrNotification;
import com.snapyr.sdk.notifications.SnapyrNotificationHandler;
import com.snapyr.sdk.services.ServiceFacade;

public class TrackerUtil {
    public static void trackDeepLink(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        Properties properties = new Properties();
        Uri uri = intent.getData();
        for (String parameter : uri.getQueryParameterNames()) {
            String value = uri.getQueryParameter(parameter);
            if (value != null && !value.trim().isEmpty()) {
                properties.put(parameter, value);
            }
        }

        properties.put("url", uri.toString());
        Snapyr.with(context).track("Deep Link Opened", properties);
    }

    public static void trackNotificationInteraction(
            Snapyr snapyrInst, SnapyrNotification snapyrNotification) {
        Context applicationContext = ServiceFacade.getApplication().getApplicationContext();
        String deepLinkUrl =
                snapyrNotification.deepLinkUrl != null
                        ? snapyrNotification.deepLinkUrl.toString()
                        : null;

        Properties props =
                new Properties()
                        .putValue("deepLinkUrl", deepLinkUrl)
                        .putValue("actionId", snapyrNotification.actionId);

        props.putValue(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, snapyrNotification.actionToken)
                .putValue("interactionType", "notificationPressed");

        // if autocancel = true....
        // Dismiss source notification
        if (applicationContext != null) {
            NotificationManagerCompat.from(applicationContext)
                    .cancel(snapyrNotification.notificationId);
        }
        // Close notification drawer (so newly opened activity isn't behind anything)
        // NOTE (BS): I don't think we need this anymore & it was causing permission errors b/c it
        // can be called from other activities. I'll leave it commented out for now
        // applicationContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        snapyrInst.pushNotificationClicked(props);
    }
}
