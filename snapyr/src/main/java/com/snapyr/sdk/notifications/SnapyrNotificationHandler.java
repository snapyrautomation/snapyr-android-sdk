package com.snapyr.sdk.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

//import androidx.navigation.NavDeepLinkBuilder;

public class SnapyrNotificationHandler {
    private final Context context;
    private final Context applicationContext;
    private final NotificationManagerCompat notificationMgr;
    private int nextMessageId = 0;
    public String defaultChannelId = "channel1";
    public String defaultChannelName = "General Notifications";
    public String defaultChannelDescription = "Displays all Snapyr-managed notifications by default";
    public int defaultChannelImportance = NotificationManager.IMPORTANCE_DEFAULT;

    public SnapyrNotificationHandler(Context ctx) {
        Log.d("SnapyrSample", "Notification handler constructor");
        context = ctx;
        applicationContext = context.getApplicationContext();
        notificationMgr = NotificationManagerCompat.from(applicationContext);
        registerChannel(defaultChannelId, defaultChannelName, defaultChannelDescription, defaultChannelImportance);
    }

    public void registerChannel(String channelId, String name, String description, int importance) {
        Log.d("SnapyrSample", "Notification registerChannel");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d("SnapyrSample", "Notification registerChannel: registering...");
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
        String channelId = getOrDefault(data, "channelId", defaultChannelId);
        String channelName = getOrDefault(data, "channelName", defaultChannelName);
        String channelDescription = getOrDefault(data, "channelDescription", defaultChannelDescription);
        registerChannel(channelId, channelName, channelDescription, NotificationManager.IMPORTANCE_DEFAULT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, channelId);
        builder.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
                .setContentTitle(getOrDefault(data, "title", "Default Title"))
                .setContentText(getOrDefault(data, "contentText", "Default content text"))
                .setColor(Color.BLUE)
                .setAutoCancel(true);
        String deepLinkUrl = data.get("deepLinkUrl");
        // previously working good version below....
//        if (deepLinkUrl != null) {
//            Intent deepLinkIntent = new Intent(Intent.ACTION_MAIN, Uri.parse(deepLinkUrl));
//            builder.setContentIntent(PendingIntent.getActivity(applicationContext, 0, deepLinkIntent, 0));
//        }
        // new version we're experimenting with...
        if (deepLinkUrl != null) {
            Intent serviceIntent = new Intent(applicationContext, SnapyrActionService.class);
            serviceIntent.putExtra("snapyrDeepLink", deepLinkUrl);
            serviceIntent.putExtra("actionId", "action1");
            builder.setContentIntent(PendingIntent.getService(applicationContext, 0, serviceIntent, 0));
        }


        String actionButtonsJson = data.get("actionButtons");
        if (actionButtonsJson != null) {
            JSONArray actionButtonsList;
            try {
                actionButtonsList = new JSONArray(actionButtonsJson);
                for (int i = 0; i < actionButtonsList.length(); i++) {
                    String title = actionButtonsList.getJSONObject(i).getString("title");
                    String uri = actionButtonsList.getJSONObject(i).getString("deepLinkUrl");
                    Intent buttonDeepLinkIntent = new Intent(Intent.ACTION_MAIN, Uri.parse(uri));
                    PendingIntent buttonAction = PendingIntent.getActivity(applicationContext, 0, buttonDeepLinkIntent, 0);
                    builder.addAction(androidx.core.R.drawable.notification_icon_background,
                            title, buttonAction);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }

        Intent testIntent = new Intent(applicationContext, this.getClass());
//        testIntent.setAction()
//        PendingIntent.getService()
        Intent serviceIntent = new Intent(applicationContext, SnapyrActionService.class);
        serviceIntent.putExtra("snapyrDeepLink", "snapyrsample://test/Alice/option%20number%20one");
        serviceIntent.putExtra("actionId", "action1");

        ComponentName componentName = applicationContext.startService(serviceIntent);


        // Image handling - fetch from URL
        String imageUrl = data.get("imageUrl");
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
        notificationMgr.notify(nextMessageId++, notification);
    }

    public void showSampleNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, this.defaultChannelId);
        builder.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
                .setContentTitle("Hello world 2")
                .setSubText("THIS IS THE SUBTEXT")
                .setContentText("Now is the time for all good men to come to the aid of their country")
                .setAutoCancel(true);
//        builder.setContentIntent(this.getDefaultIntent());
        builder.setContentIntent(this.getDeepLinkIntent("test%20builtin"));
        Notification notification = builder.build();
        notificationMgr.notify(nextMessageId++, notification);
    }

    public PendingIntent getDeepLinkIntent(String extra) {
        Intent deepLinkIntent = new Intent(Intent.ACTION_MAIN, Uri.parse("snapyrsample://test/Alice/" + extra));
        return PendingIntent.getActivity(applicationContext, 0, deepLinkIntent, 0);
    }

    /**
     * @return an intent that launches the default activity for the app, i.e. "open the app"
     * when the notification is clicked.
     */
    public PendingIntent getDefaultIntent() {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        return PendingIntent.getActivity(applicationContext, 0, launchIntent, 0);
    }
}
