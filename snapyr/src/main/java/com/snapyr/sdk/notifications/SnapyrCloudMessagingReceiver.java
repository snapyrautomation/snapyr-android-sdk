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
        String snapyr = extras.getString("snapyr");
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
