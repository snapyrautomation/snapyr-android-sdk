package com.snapyr.sdk.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Random;

public class SnapyrNotificationHandler {
    public static final String NOTIF_TITLE_KEY = "title";
    public static final String NOTIF_SUBTITLE_KEY = "subtitle";
    public static final String NOTIF_CONTENT_KEY = "contentText";
    public static final String NOTIF_DEEP_LINK_KEY = "deepLinkUrl";
    public static final String NOTIF_IMAGE_URL_KEY = "imageUrl";
    public static final String NOTIF_TOKEN_KEY = "actionToken";
    public static final String NOTIF_CHANNEL_ID_KEY = "categoryId";
    public static final String NOTIF_CHANNEL_NAME_KEY = "categoryName"; // TODO (@paulwsmith): get from config?
    public static final String NOTIF_CHANNEL_DESCRIPTION_KEY = "categoryDescription"; // TODO (@paulwsmith): get from config?

    public static final String ACTION_BUTTONS_KEY = "actionButtons";
    public static final String ACTION_ID_KEY = "actionId";
    public static final String ACTION_TITLE_KEY = "title";
    public static final String ACTION_DEEP_LINK_KEY = "deepLinkUrl";
    public static final String ACTION_TOKEN_KEY = "behaviorToken";

    public static final String INTERACTION_KEY = "interactionType";

    public enum INTERACTION_TYPE {
        NOTIFICATION_PRESS,
        ACTION_BUTTON_PRESS
    }

    private final Context context;
    private final Context applicationContext;
    private final NotificationManagerCompat notificationMgr;
    private int nextMessageId = 0;
    public String defaultChannelId = "channel1";
    public String defaultChannelName = "General Notifications";
    public String defaultChannelDescription = "Displays all Snapyr-managed notifications by default";
    public int defaultChannelImportance = NotificationManagerCompat.IMPORTANCE_DEFAULT;

    public SnapyrNotificationHandler(Context ctx) {
        Log.d("Snapyr", "Notification handler constructor");
        context = ctx;
        applicationContext = context.getApplicationContext();
        notificationMgr = NotificationManagerCompat.from(applicationContext);
        registerChannel(defaultChannelId, defaultChannelName, defaultChannelDescription, defaultChannelImportance);
    }

    public void registerChannel(String channelId, String name, String description, int importance) {
        Log.d("Snapyr", "Notification registerChannel");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d("Snapyr", "Notification registerChannel: registering...");
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            notificationMgr.createNotificationChannel(channel);
        }
    }

    public String getOrDefault(Map<String, String> data, String key, String defaultVal) {
        String val = data.get(key);
        if (val == null) {
            return defaultVal;
        }
        return val;
    }

    public void showRemoteNotification(Map<String, String> data) {
        // TODO (@paulwsmith): remove following lines! :)
        Log.e("Snapyr", "showRemoteNotification payload:");
        Log.e("Snapyr", String.valueOf(data));

        String channelId = getOrDefault(data, NOTIF_CHANNEL_ID_KEY, defaultChannelId);
        String channelName = getOrDefault(data, NOTIF_CHANNEL_NAME_KEY, defaultChannelName);
        String channelDescription = getOrDefault(data, NOTIF_CHANNEL_DESCRIPTION_KEY, defaultChannelDescription);
        registerChannel(channelId, channelName, channelDescription, NotificationManagerCompat.IMPORTANCE_DEFAULT);

        int notificationId = ++nextMessageId;
        Random r = new Random();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, channelId);
        builder.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
                // .setContentTitle(getOrDefault(data, NOTIF_TITLE_KEY, "Default Title"))
                // .setContentText(getOrDefault(data, NOTIF_CONTENT_KEY, "Default content text"))
                .setContentTitle(data.get(NOTIF_TITLE_KEY))
                .setContentText(data.get(NOTIF_CONTENT_KEY))
                .setSubText(data.get(NOTIF_SUBTITLE_KEY))
                .setColor(Color.BLUE) // TODO (@paulwsmith): make configurable
                .setAutoCancel(true); // true means notification auto dismissed after tapping. TODO (@paulwsmith): make configurable?

        String deepLinkUrl = data.get(NOTIF_DEEP_LINK_KEY);
        if (deepLinkUrl != null) {
            Intent serviceIntent = new Intent(applicationContext, SnapyrActionService.class);

            serviceIntent.putExtra(ACTION_ID_KEY, data.get(ACTION_ID_KEY));
            serviceIntent.putExtra(NOTIF_DEEP_LINK_KEY, deepLinkUrl);
            serviceIntent.putExtra(NOTIF_TOKEN_KEY, data.get(NOTIF_TOKEN_KEY));
            serviceIntent.putExtra(INTERACTION_KEY, INTERACTION_TYPE.NOTIFICATION_PRESS);
            serviceIntent.putExtra("notificationId", notificationId);

            builder.setContentIntent(PendingIntent.getService(applicationContext, r.nextInt(), serviceIntent, 0));
        }


        String actionButtonsJson = data.get(ACTION_BUTTONS_KEY);
        if (actionButtonsJson != null) {
            JSONArray actionButtonsList;
            try {
                actionButtonsList = new JSONArray(actionButtonsJson);
                for (int i = 0; i < actionButtonsList.length(); i++) {
                    JSONObject actionButton = actionButtonsList.getJSONObject(i);
                    String title = actionButton.getString(ACTION_TITLE_KEY);
                    String buttonDeepLinkUrl = actionButton.getString(ACTION_DEEP_LINK_KEY);
                    String buttonToken = actionButton.getString(ACTION_TOKEN_KEY);
                    // Create intent to open service, which tracks interaction and then triggers original intent
                    Intent buttonIntent = new Intent(applicationContext, SnapyrActionService.class);

                    buttonIntent.putExtra(ACTION_ID_KEY, actionButton.getString(ACTION_ID_KEY));
                    buttonIntent.putExtra(NOTIF_DEEP_LINK_KEY, buttonDeepLinkUrl);
                    buttonIntent.putExtra(ACTION_TOKEN_KEY, buttonToken);
                    buttonIntent.putExtra(INTERACTION_KEY, INTERACTION_TYPE.ACTION_BUTTON_PRESS);
                    buttonIntent.putExtra("notificationId", notificationId);

                    PendingIntent buttonAction = PendingIntent.getService(applicationContext, r.nextInt(), buttonIntent, 0);
                    builder.addAction(androidx.core.R.drawable.notification_icon_background,
                            title, buttonAction);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }

        // Image handling - fetch from URL
        // TODO (@paulwsmith): move off-thread?
        String imageUrl = data.get(NOTIF_IMAGE_URL_KEY);
        if (imageUrl != null) {
            InputStream inputStream = null;
            Bitmap image = null;
            try {
                inputStream = new URL(imageUrl).openStream();
                image = BitmapFactory.decodeStream(inputStream);
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(image));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Notification notification = builder.build();
        notificationMgr.notify(notificationId, notification);
    }

    public void showSampleNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, this.defaultChannelId);
        builder.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
                .setContentTitle("Snapyr: Title")
                .setSubText("Snapyr: Subtext")
                .setContentText("Snapyr: Content text")
                .setAutoCancel(true);

        builder.setContentIntent(this.getDeepLinkIntent("test%20builtin"));
        Notification notification = builder.build();
        notificationMgr.notify(nextMessageId++, notification);
    }

    public PendingIntent getDeepLinkIntent(String extra) {
        Intent deepLinkIntent = new Intent(Intent.ACTION_MAIN, Uri.parse("snapyrsample://test/Alice/" + extra));
        return PendingIntent.getActivity(applicationContext, 0, deepLinkIntent, 0);
    }
}
