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
package com.snapyr.sdk.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.snapyr.sdk.PushTemplate;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.core.R;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SnapyrNotificationHandler {
    public static final String NOTIF_TITLE_KEY = "title";
    public static final String NOTIF_SUBTITLE_KEY = "subtitle";
    public static final String NOTIF_CONTENT_KEY = "contentText";
    public static final String NOTIF_DEEP_LINK_KEY = "deepLinkUrl";
    public static final String NOTIF_IMAGE_URL_KEY = "imageUrl";
    public static final String NOTIF_TOKEN_KEY = "actionToken";
    public static final String NOTIF_CHANNEL_ID_KEY = "categoryId";
    public static final String NOTIF_CHANNEL_NAME_KEY =
            "categoryName"; // TODO (@paulwsmith): get from config?
    public static final String NOTIF_CHANNEL_DESCRIPTION_KEY =
            "categoryDescription"; // TODO (@paulwsmith): get from config?

    public static final String ACTION_BUTTONS_KEY = "actionButtons";
    public static final String ACTION_ID_KEY = "actionId";
    public static final String ACTION_TITLE_KEY = "title";
    public static final String ACTION_DEEP_LINK_KEY = "deepLinkUrl";
    public static final String ACTION_TOKEN_KEY = "behaviorToken";

    public static final String INTERACTION_KEY = "interactionType";

    public enum INTERACTION_TYPE {
        NOTIFICATION_PRESS
    }

    private final Context context;
    private final Context applicationContext;
    private final NotificationManagerCompat notificationMgr;
    private int nextMessageId = 0;
    public String defaultChannelId = "channel1";
    public String defaultChannelName = "General Notifications";
    public String defaultChannelDescription =
            "Displays all Snapyr-managed notifications by default";
    public int defaultChannelImportance = NotificationManagerCompat.IMPORTANCE_HIGH;

    public SnapyrNotificationHandler(Context ctx) {
        context = ctx;
        applicationContext = context.getApplicationContext();
        notificationMgr = NotificationManagerCompat.from(applicationContext);
        registerChannel(
                defaultChannelId,
                defaultChannelName,
                defaultChannelDescription,
                defaultChannelImportance);
    }

    public void registerChannel(String channelId, String name, String description, int importance) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            notificationMgr.createNotificationChannel(channel);
        }
    }

    public String getOrDefault(Map<String, Object> data, String key, String defaultVal) {
        String val = (String) data.get(key);
        if (val == null) {
            return defaultVal;
        }
        return val;
    }

    public void showRemoteNotification(Map<String, Object> data) {
        String channelId = getOrDefault(data, NOTIF_CHANNEL_ID_KEY, defaultChannelId);
        String channelName = getOrDefault(data, NOTIF_CHANNEL_NAME_KEY, defaultChannelName);
        String channelDescription =
                getOrDefault(data, NOTIF_CHANNEL_DESCRIPTION_KEY, defaultChannelDescription);
        registerChannel(channelId, channelName, channelDescription, defaultChannelImportance);

        int notificationId = ++nextMessageId;
        Random r = new Random();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this.context, channelId);
        builder.setSmallIcon(R.drawable.ic_snapyr_logo_only)
                .setContentTitle((String)data.get(NOTIF_TITLE_KEY))
                .setContentText((String)data.get(NOTIF_CONTENT_KEY))
                .setSubText((String)data.get(NOTIF_SUBTITLE_KEY))
                .setColor(Color.BLUE) // TODO (@paulwsmith): make configurable
                .setAutoCancel(true); // true means notification auto dismissed after tapping. TODO
        // (@paulwsmith): make configurable?

        String deepLinkUrl = (String)data.get(NOTIF_DEEP_LINK_KEY);
        if (deepLinkUrl != null) {
            Intent baseIntent = getLaunchIntent();

            try {
                Uri uri = Uri.parse(deepLinkUrl);
                baseIntent.setData(uri);
                baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } catch (Exception e) {
                Log.e("Snapyr", "showRemoteNotification: exception setting URI", e);
            }

            baseIntent.putExtra((String)ACTION_ID_KEY, (String)data.get(ACTION_ID_KEY));
            baseIntent.putExtra(NOTIF_DEEP_LINK_KEY, deepLinkUrl);
            baseIntent.putExtra((String)NOTIF_TOKEN_KEY, (String)data.get(NOTIF_TOKEN_KEY));
            baseIntent.putExtra(INTERACTION_KEY, INTERACTION_TYPE.NOTIFICATION_PRESS);
            baseIntent.putExtra("notificationId", notificationId);

            builder.setContentIntent(
                    PendingIntent.getActivity(applicationContext, r.nextInt(), baseIntent, 0));
        }

        PushTemplate pushTemplate = (PushTemplate) data.get(ACTION_BUTTONS_KEY);
        if (pushTemplate != null) {
            List<PushTemplate.ActionButton> buttons = pushTemplate.getButtons();
            for (int i = 0; i < buttons.size(); i++) {
                PushTemplate.ActionButton button = buttons.get(i);
                // Create intent to open service, which tracks interaction and then triggers
                // original intent
                Intent buttonIntent = getLaunchIntent();
                buttonIntent.setAction(button.actionId);
                buttonIntent.putExtra(ACTION_ID_KEY, button.actionId);
                buttonIntent.putExtra(NOTIF_DEEP_LINK_KEY, button.deeplinkURL);
                buttonIntent.putExtra(INTERACTION_KEY, INTERACTION_TYPE.NOTIFICATION_PRESS);
                buttonIntent.putExtra("notificationId", notificationId);

                buttonIntent.setData(button.deeplinkURL);
                buttonIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent buttonAction =
                        PendingIntent.getActivity(
                                applicationContext, 0, buttonIntent, 0);
                builder.addAction(R.drawable.ic_snapyr_logo_only, button.title, buttonAction);
            }
        }

        // Image handling - fetch from URL
        // TODO (@paulwsmith): move off-thread? (maybe not necessary; not part of main thread
        // anyway)
        String imageUrl = (String)data.get(NOTIF_IMAGE_URL_KEY);
        if (imageUrl != null) {
            InputStream inputStream = null;
            Bitmap image = null;
            try {
                inputStream = new URL(imageUrl).openStream();
                image = BitmapFactory.decodeStream(inputStream);
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Notification notification = builder.build();
        notificationMgr.notify(notificationId, notification);
    }

    private Intent getLaunchIntent() {
        try {
            String packageName = applicationContext.getPackageName();
            PackageManager packageManager = applicationContext.getPackageManager();
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

            if (launchIntent == null) {
                // No launch intent specified / found for this app. Default to ACTION_MAIN
                launchIntent = new Intent(Intent.ACTION_MAIN);
            }

            return launchIntent;
        } catch (Exception e) {
            Log.e("Snapyr", "Could not get launch intent", e);
            return null;
        }
    }

    public void showSampleNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this.context, this.defaultChannelId);
        builder.setSmallIcon(R.drawable.ic_snapyr_logo_only)
                .setContentTitle("Snapyr: Title")
                .setSubText("Snapyr: Subtext")
                .setContentText("Snapyr: Content text")
                .setAutoCancel(true);

        Random r = new Random();
        Intent baseIntent = getLaunchIntent();

        if (baseIntent == null) {
            Log.e("SnapyrPaul", "Base intent was null, can't continue.");
            return;
        }

        //        Intent buttonIntent = new Intent(applicationContext, applicationContext.activ);
        //        buttonIntent.putExtra(ACTION_ID_KEY, actionButton.getString(ACTION_ID_KEY));
        //        buttonIntent.putExtra(NOTIF_DEEP_LINK_KEY, buttonDeepLinkUrl);
        //        buttonIntent.putExtra(ACTION_TOKEN_KEY, buttonToken);
        //        buttonIntent.putExtra(INTERACTION_KEY, INTERACTION_TYPE.ACTION_BUTTON_PRESS);
        //        buttonIntent.putExtra("notificationId", 123);

        // Clone base intent retrieved from package, above.
        Intent buttonIntent = new Intent(baseIntent);

        PendingIntent buttonAction =
                PendingIntent.getActivity(applicationContext, r.nextInt(), buttonIntent, 0);
        builder.addAction(R.drawable.ic_snapyr_logo_only, "1", buttonAction);

        buttonIntent = new Intent(baseIntent);
        buttonAction = PendingIntent.getActivity(applicationContext, r.nextInt(), buttonIntent, 0);
        builder.addAction(
                R.drawable.ic_snapyr_logo_only, "2Supercalifragilisticexpalidocious", buttonAction);

        buttonIntent = new Intent(baseIntent);
        buttonAction = PendingIntent.getActivity(applicationContext, r.nextInt(), buttonIntent, 0);
        builder.addAction(R.drawable.ic_snapyr_logo_only, "a", buttonAction);

        builder.setContentIntent(this.getDeepLinkIntent("test%20builtin"));
        Notification notification = builder.build();
        notificationMgr.notify(nextMessageId++, notification);
    }

    public PendingIntent getDeepLinkIntent(String extra) {
        Intent deepLinkIntent =
                new Intent(Intent.ACTION_MAIN, Uri.parse("snapyrsample://test/Alice/" + extra));
        return PendingIntent.getActivity(applicationContext, 0, deepLinkIntent, 0);
    }

    public void autoRegisterFirebaseToken(Snapyr snapyrInstance) {
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(
                        new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                if (!task.isSuccessful()) {
                                    return;
                                }

                                // Get new Instance ID token
                                String token = task.getResult();
                                Log.e(
                                        "Snapyr",
                                        "SnapyrFirebaseMessagingService: applying FB token: "
                                                + token);
                                snapyrInstance.setPushNotificationToken(token);
                            }
                        });
    }
}
