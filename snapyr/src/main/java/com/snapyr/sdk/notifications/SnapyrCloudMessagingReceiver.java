package com.snapyr.sdk.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.cloudmessaging.CloudMessage;
import com.google.android.gms.cloudmessaging.CloudMessagingReceiver;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class SnapyrCloudMessagingReceiver extends CloudMessagingReceiver {

    @Override
    protected int onMessageReceive(@NonNull Context context, @NonNull CloudMessage cloudMessage) {
        boolean isMainThread = Looper.myLooper() == Looper.getMainLooper(); // false
        boolean initialStickyBroadcast = isInitialStickyBroadcast(); // false
        boolean orderedBroadcast = isOrderedBroadcast(); // false

        String messageId = cloudMessage.getMessageId(); // 0:1671656937970145%805b8d1cf9fd7ecd
        String collapseKey = cloudMessage.getCollapseKey(); // null
        String messageType = cloudMessage.getMessageType(); // null
        Map<String, String> data = cloudMessage.getData(); //
        String from = cloudMessage.getFrom(); // 983718517902
        int originalPriority = cloudMessage.getOriginalPriority(); // 2
        String originalPriorityString = getPriorityString(originalPriority);
        //        CloudMessage.MessagePriority
        int priority = cloudMessage.getPriority(); // 2
        String priorityString = getPriorityString(priority);
        String senderId = cloudMessage.getSenderId(); // 983718517902
        long sentTime = cloudMessage.getSentTime(); // 1671656937939
        Date sentDate = new Date(sentTime);
        String to = cloudMessage.getTo(); // null (hmm...?)
        int ttl = cloudMessage.getTtl(); // 2419200

        Parcel parcel = Parcel.obtain();
        cloudMessage.writeToParcel(parcel, 0);

        byte[] rawData = cloudMessage.getRawData(); // null
        if (rawData != null) {
            String s = rawData.toString();
            Class<? extends byte[]> aClass = rawData.getClass();
            Log.i("Snapyr.CloudMsg", "rawData:\n" + s);
        }

        Intent intent = cloudMessage.getIntent();
        Bundle extras = intent.getExtras();
        Set<String> keySet = extras.keySet();

        String google_delivered_priority =
                extras.getString("google.delivered_priority"); // "normal"
        Long google_sent_time = extras.getLong("google.sent_time"); // 1671656180939
        Date sentDate2 = new Date(google_sent_time);
        // default seems to be 2419200, which (in seconds) equals 28 days
        Integer google_ttl = extras.getInt("google.ttl"); // 2419200
        String google_original_priority = extras.getString("google.original_priority"); // "normal"
        // the original "snapyr" key from the push payload, as a String (needs to be JSON parsed)
        String snapyr =
                extras.getString(
                        "snapyr"); // {"xxdeepLinkUrl":"https:\/\/cricheroes.app\/home-screen\/my-cricket\/my-matches","deepLinkUrl":"https:\/\/cricheroes.app\/sasdfhome-screen\/my-cricket\/my-matches","subtitle":"Yes it is!","imageUrl":"https:\/\/images-na.ssl-images-amazon.com\/images\/S\/pv-target-images\/fb1fd46fbac48892ef9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg","contentText":"Bring your teammates on CricHeroes and start scoring. 💪","pushTemplate":{"modified":"2022-11-10T21:20:15.409Z","id":"b29f9018-ea25-4bf5-8a9b-d8d6a8bc29a1"},"actionToken":"Zjk1OTkxZGEtZWE5Yy00ZTQ0LTk5OGQtNWZmNWY0Y2EwNGQzOmIyOWY5MDE4LWVhMjUtNGJmNS04YTliLWQ4ZDZhOGJjMjlhMTpmYmYxMTljMS1lMjNlLTQxZDEtODkyMi05MjE5M2RlNGRkODk6NGZjOGE5NGQtNGU0Zi00NTQ4LWE3NjYtMzM1NDY2YmViMzBjOnB1Ymxpc2g6NGY5YTA0NzMtYWY4OS00M2QyLWEyZTMtZmFiN2FkNmU0MzUyOnBhdWwxMTExYTp0YmQ6MTY2ODY0ODQzOTphY3Rpb25Ub2tlbi00ZjlhMDQ3My1hZjg5LTQzZDItYTJlMy1mYWI3YWQ2ZTQzNTIucGF1bDExMTFhLjE2Njg2NDg0Mzk=","title":"4Alice, it's high time!","xdeepLinkUrl":"https:\/\/cricheroes.app\/ch-leader-board"}
        // Matches Firebase Project ID
        String from2 = extras.getString("from"); // 983718517902
        // Unique message id, which comes back as `results.message_id` in response body when sending
        // the FCM push
        String google_message_id =
                extras.getString("google.message_id"); // 0:1671656180975670%805b8d1cf9fd7ecd
        // Matches Firebase Project ID (same as "from")
        String google_c_sender_id = extras.getString("google.c.sender.id"); // 983718517902

        //        stopBroadcastPropagation(true);

        return 0;
    }

    public String getPriorityString(int priority) {
        switch (priority) {
            case CloudMessage.PRIORITY_HIGH:
                return "PRIORITY_HIGH";
            case CloudMessage.PRIORITY_NORMAL:
                return "PRIORITY_NORMAL";
            case CloudMessage.PRIORITY_UNKNOWN:
                return "PRIORITY_UNKNOWN";
        }
        throw new IllegalArgumentException("Unknown priority value: " + String.valueOf(priority));
    }

    private void stopBroadcastPropagation(Boolean success) {
        if (success) {
            setResultCode(Activity.RESULT_OK);
        } else {
            setResultCode(Activity.RESULT_CANCELED);
        }
        abortBroadcast();
    }

    //    @Override
    //    public void onReceive(Context context, Intent intent) {
    //        // TODO: This method is called when the BroadcastReceiver is receiving
    //        // an Intent broadcast.
    //        throw new UnsupportedOperationException("Not yet implemented");
    //    }
}
