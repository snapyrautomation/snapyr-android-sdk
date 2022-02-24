package com.snapyr.sdk.internal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationManagerCompat;

import com.snapyr.sdk.Properties;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.notifications.SnapyrNotificationHandler;

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

    public static void trackNotificationInteraction(Context context, Intent intent) {
        Snapyr snapyr = Snapyr.with(context);
        Context applicationContext = snapyr.getApplication().getApplicationContext();

        String deepLinkUrl = intent.getStringExtra(SnapyrNotificationHandler.NOTIF_DEEP_LINK_KEY);
        String actionId = intent.getStringExtra(SnapyrNotificationHandler.ACTION_ID_KEY);
        int notificationId = intent.getIntExtra("notificationId", 0);

        String token;

        Properties props =
                new Properties()
                        .putValue("deepLinkUrl", deepLinkUrl)
                        .putValue("actionId", actionId);

        token = intent.getStringExtra(SnapyrNotificationHandler.NOTIF_TOKEN_KEY);
        props.putValue(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, token)
                .putValue("interactionType", "notificationPressed");

        // if autocancel = true....
        // Dismiss source notification
        if (applicationContext != null){
            NotificationManagerCompat.from(applicationContext).cancel(notificationId);
        }
        // Close notification drawer (so newly opened activity isn't behind anything)
        // NOTE (BS): I don't think we need this anymore & it was causing permission errors b/c it
        // can be called from other activities. I'll leave it commented out for now
        //applicationContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        snapyr.pushNotificationClicked(props);
    }
}
