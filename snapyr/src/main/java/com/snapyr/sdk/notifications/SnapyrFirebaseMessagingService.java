package com.snapyr.sdk.notifications;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.Snapyr;

import java.util.Map;

public class SnapyrFirebaseMessagingService extends FirebaseMessagingService {

    public SnapyrFirebaseMessagingService() {
        super();
        Log.e("Paul", "SNAPYRFIREBASE");
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.e("Paul", "NEW TOKEN");
        Snapyr.with(this).setPushNotificationToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        Log.e("Paul", "MESSAGE RECEIVED");

        com.snapyr.sdk.Properties properties = new com.snapyr.sdk.Properties();
        properties.putAll(data);
        Snapyr.with(this).pushNotificationReceived(properties);

        Snapyr.with(this).getNotificationHandler().showRemoteNotification(data);
    }

//    @Override
//    public int onStartCommand() {
//
//    }
}
