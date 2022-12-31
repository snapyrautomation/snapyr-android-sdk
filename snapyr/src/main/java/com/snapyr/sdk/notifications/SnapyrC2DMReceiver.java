package com.snapyr.sdk.notifications;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.internal.PushTemplate;
import com.snapyr.sdk.internal.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class SnapyrC2DMReceiver extends BroadcastReceiver {

    /**
     * TODO:
     *  - onReceive is called on main thread. Need to move to another thread/executor
     *      - See BroadcastReceiver `goAsync()`
     *      - `CloudMessagingReceiver` (from GCM code) does this too, maybe look there
     *  - Review device token handling
     *      - Not done through C2DM; this will still be done through Firebase. That means we / the client still need to include Firebase packages through Gradle the same as before (as well as google-services.json). 
     *      - Looks like we CAN fully get rid of SnapyrFirebaseMessagingService
     *      - autoRegisterFirebaseToken called at Snapyr init should handle getting + storing the token
     *      - onNewToken was duplicating calls and should be unnecessary
     *      - There does not seem to be any situation where a token would change DURING app runtime, unless there was explicit code to delete the old token. So checking the token on every app startup should be sufficient for keeping things up-to-date...
     *          - https://groups.google.com/g/firebase-talk/c/Ka6bTmQeNwg - notes about when tokens change
     *          - See if we can listen for changes some other way, just to be safe?
     *  - Watch out for duplicate sends of the same notification? (use message ID to dedupe?)
     *  - Test w/ React Native - make sure everything works the same as before
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isMainThread = Looper.myLooper() == Looper.getMainLooper(); // true
        boolean initialStickyBroadcast = isInitialStickyBroadcast(); // false
        boolean orderedBroadcast = isOrderedBroadcast(); // true

        String action = intent.getAction(); // com.google.android.c2dm.intent.RECEIVE
        int flags = intent.getFlags(); // 16777232
        printFlagsForIntent(intent);
        Uri data = intent.getData(); // null
        Set<String> categories = intent.getCategories(); // null
        ClipData clipData = intent.getClipData(); // null
        ComponentName component = intent.getComponent(); // ComponentInfo{com.snapyr.sdk.sample/com.snapyr.sdk.notifications.SnapyrC2DMReceiver}
        String identifier = "XXXXX"; // null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            identifier = intent.getIdentifier(); // null
        }
        String aPackage = intent.getPackage(); // null
        String scheme = intent.getScheme(); // null
        Intent selector = intent.getSelector(); // null
        Rect sourceBounds = intent.getSourceBounds(); // null
        String type = intent.getType(); // null
        Class<? extends Intent> aClass = intent.getClass(); // class android.content.Intent

        Bundle extras = intent.getExtras(); // all the data, I think

        Set<String> keySet = extras.keySet();


        String google_delivered_priority = extras.getString("google.delivered_priority"); // "normal"
        Long google_sent_time = extras.getLong("google.sent_time"); // 1671656180939
        Date sentDate = new Date(google_sent_time);
        // default seems to be 2419200, which (in seconds) equals 28 days
        Integer google_ttl = extras.getInt("google.ttl"); // 2419200
        String google_original_priority = extras.getString("google.original_priority"); // "normal"
        // the original "snapyr" key from the push payload, as a String (needs to be JSON parsed)
        String snapyr = extras.getString("snapyr"); // {"xxdeepLinkUrl":"https:\/\/cricheroes.app\/home-screen\/my-cricket\/my-matches","deepLinkUrl":"https:\/\/cricheroes.app\/sasdfhome-screen\/my-cricket\/my-matches","subtitle":"Yes it is!","imageUrl":"https:\/\/images-na.ssl-images-amazon.com\/images\/S\/pv-target-images\/fb1fd46fbac48892ef9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg","contentText":"Bring your teammates on CricHeroes and start scoring. 💪","pushTemplate":{"modified":"2022-11-10T21:20:15.409Z","id":"b29f9018-ea25-4bf5-8a9b-d8d6a8bc29a1"},"actionToken":"Zjk1OTkxZGEtZWE5Yy00ZTQ0LTk5OGQtNWZmNWY0Y2EwNGQzOmIyOWY5MDE4LWVhMjUtNGJmNS04YTliLWQ4ZDZhOGJjMjlhMTpmYmYxMTljMS1lMjNlLTQxZDEtODkyMi05MjE5M2RlNGRkODk6NGZjOGE5NGQtNGU0Zi00NTQ4LWE3NjYtMzM1NDY2YmViMzBjOnB1Ymxpc2g6NGY5YTA0NzMtYWY4OS00M2QyLWEyZTMtZmFiN2FkNmU0MzUyOnBhdWwxMTExYTp0YmQ6MTY2ODY0ODQzOTphY3Rpb25Ub2tlbi00ZjlhMDQ3My1hZjg5LTQzZDItYTJlMy1mYWI3YWQ2ZTQzNTIucGF1bDExMTFhLjE2Njg2NDg0Mzk=","title":"4Alice, it's high time!","xdeepLinkUrl":"https:\/\/cricheroes.app\/ch-leader-board"}
        String definitelyEmptyExtra = extras.getString("ivwepibyuwaepuiawepvbui");
        // Matches Firebase Project ID
        String from = extras.getString("from"); // 983718517902
        // Unique message id, which comes back as `results.message_id` in response body when sending the FCM push
        String google_message_id = extras.getString("google.message_id"); // 0:1671656180975670%805b8d1cf9fd7ecd
        // Matches Firebase Project ID (same as "from")
        String google_c_sender_id = extras.getString("google.c.sender.id"); // 983718517902


        try {
            processSnapyrNotification(snapyr, context);
        } catch (Exception e) {
            Log.e("Snapyr", "Notification service encountered an unexpected error while attempting to process and display an incoming push notification");
        }

        Log.i("Snapyr.C2DM", String.format("onReceive called!\n\tmessageId: [%s]\n\tsent: [%s]\n\tsnapyr: [%s]",
                google_message_id, Utils.toISO8601String(sentDate), snapyr != null ? snapyr.substring(0, 128) + "..." : "MISSING"));
    }

    // Mostly copied from SnapyrFirebaseMessagingService `onMessageReceived()`
    private void processSnapyrNotification(String snapyrData, Context context) {
        SnapyrNotification snapyrNotification = null;
        Boolean isSnapyrNotif = false;
        try {
            snapyrNotification = new SnapyrNotification(snapyrData);
            isSnapyrNotif = true;
        } catch (SnapyrNotification.NonSnapyrMessageException e) {
            // Non-Snapyr notification - probably not really an error, but nothing for us to do
            Log.i("Snapyr", e.getMessage());
        } catch (Exception e) {
            Log.e("Snapyr", "Error parsing Snapyr notification; returning.", e);
        }

        if (!isSnapyrNotif || snapyrNotification == null) {
            // nothing to do; return silently and let the intent pass through to any other receivers (including FirebaseMessagingService)
            return;
        }

        Snapyr snapyrInstance = SnapyrNotificationUtils.getSnapyrInstance(context);
        if (snapyrInstance == null) {
            Log.e(
                    "Snapyr",
                    "Notification service couldn't initialize Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.");
            stopBroadcastPropagation(false);
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
        showNotification(snapyrNotification, snapyrInstance);
        sendPushReceivedBroadcast(snapyrNotification, context);
        snapyrInstance.flush();

//        stopBroadcastPropagation(true);
    }

    private void stopBroadcastPropagation(Boolean success) {
        if (success) {
            setResultCode(Activity.RESULT_OK);
        } else {
            setResultCode(Activity.RESULT_CANCELED);
        }
        abortBroadcast();
    }

    private void showNotification(SnapyrNotification snapyrNotification, Snapyr snapyrInstance) {
        // Execute in one-off thread to ensure image fetch / notify doesn't run in UI thread
        new Thread() {
            @Override
            public void run() {
                snapyrInstance.getNotificationHandler().showRemoteNotification(snapyrNotification);
            }
        }.start();
    }

    // Copied from SnapyrFirebaseMessagingService
    private void sendPushReceivedBroadcast(SnapyrNotification snapyrNotification, Context context) {
        Intent pushReceivedIntent =
                new Intent(SnapyrNotificationHandler.NOTIFICATION_RECEIVED_ACTION);
        pushReceivedIntent.putExtra("snapyr.notification", snapyrNotification);
        // make this intent "explicit" which allows it to reach a manifest-defined receiver
        pushReceivedIntent.setPackage(context.getPackageName());
        context.sendBroadcast(pushReceivedIntent);
    }

    // Copied from SnapyrFirebaseMessagingService
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

    // DEBUG USE ONLY

    public static ArrayList<String> getFlagsForIntent(Intent intent) {
        ArrayList<String> allFlags = new ArrayList<>();
        Field[] declaredFields = Intent.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getName().startsWith("FLAG_")) {

                try {
                    int flag = field.getInt(null);

                    if ((intent.getFlags() & flag) != 0) {
                        allFlags.add(field.getName());
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
        return allFlags;
    }

    public static void printFlagsForIntent(Intent intent) {
        ArrayList<String> flagsForIntent = getFlagsForIntent(intent);
        String flagsString = String.join("\n\t", flagsForIntent);
        Log.d("Snapyr.C2DM", String.format("Flags for intent: [\n\t%s\n]", flagsString));
    }
}