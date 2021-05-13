package com.snapyr.sdk.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

    public void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, this.defaultChannelId);
        builder.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setContentTitle("Hello world 2")
            .setContentText("Now is the time for all good men to come to the aid of their country")
            .setAutoCancel(true);
        builder.setContentIntent(this.getDefaultIntent());
        Notification notification = builder.build();
        notificationMgr.notify(nextMessageId++, notification);
    }

    /**
     * @return an intent that launches the default activity for the app, i.e. "open the app"
     * when the notification is clicked.
     */
    public PendingIntent getDefaultIntent() {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        return PendingIntent.getActivity(applicationContext, 0 ,launchIntent, 0);
    }
}
