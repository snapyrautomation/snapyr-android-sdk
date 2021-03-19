package com.snapyr.analytics.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import com.snapyr.analytics.Snapyr;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Properties;

public class SnapyrFirebaseMessagingService extends FirebaseMessagingService {

    public SnapyrFirebaseMessagingService() {
        super();
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FBMessaging","Token FB " + token);
        Snapyr.with(this).setPushNotificationToken(token);
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        com.snapyr.analytics.Properties properties = new com.snapyr.analytics.Properties();
        properties.putAll(remoteMessage.getData());
        Snapyr.with(this).pushNotificationReceived(properties);
    }
}