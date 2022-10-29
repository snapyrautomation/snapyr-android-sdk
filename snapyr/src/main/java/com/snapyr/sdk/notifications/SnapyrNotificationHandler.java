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

import static com.snapyr.sdk.internal.Utils.isNullOrEmpty;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.core.R;
import com.snapyr.sdk.internal.ActionButton;
import com.snapyr.sdk.internal.PushTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SnapyrNotificationHandler {
    public static final String NOTIF_ICON_SNAPYR_DEFAULT = "ic_snapyr_notification_default";
    public static final String NOTIF_ICON_FALLBACK = "ic_notification";
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
    public static final String ACTION_DEEP_LINK_KEY = "deepLinkUrl";

    public static final String NOTIFICATION_ID = "notification_id";
    // 'DEEPLINK_ACTION' is used by manifest, don't delete
    public static final String DEEPLINK_ACTION = "com.snapyr.sdk.notifications.ACTION_DEEPLINK";
    public static final String NOTIFICATION_ACTION = "com.snapyr.sdk.notifications.TRACK_BROADCAST";

    private final Context context;
    private final Context applicationContext;
    private final NotificationManagerCompat notificationMgr;
    public String defaultChannelId = "channel1";
    public String defaultChannelName = "General Notifications";
    public String defaultChannelDescription =
            "Displays all Snapyr-managed notifications by default";
    public int defaultChannelImportance = NotificationManagerCompat.IMPORTANCE_HIGH;
    private int nextMessageId = 0;
    private int nextActionButtonCode = 0;

    public SnapyrNotificationHandler(Context ctx) {
        context = ctx;
        applicationContext = context.getApplicationContext();
        notificationMgr = NotificationManagerCompat.from(applicationContext);
        registerChannel(
                defaultChannelId,
                defaultChannelName,
                defaultChannelDescription,
                defaultChannelImportance);
        getLaunchIntent();
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

    @RequiresApi(api = Build.VERSION_CODES.N)
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
        builder.setSmallIcon(getNotificationIcon())
                .setContentTitle((String) data.get(NOTIF_TITLE_KEY))
                .setContentText((String) data.get(NOTIF_CONTENT_KEY))
                .setSubText((String) data.get(NOTIF_SUBTITLE_KEY))
                .setColor(Color.BLUE) // TODO (@paulwsmith): make configurable
                .setAutoCancel(true); // true means notification auto dismissed after tapping. TODO

        TaskStackBuilder ts = TaskStackBuilder.create(this.context);

        Intent trackIntent = new Intent(this.context, SnapyrNotificationListener.class);
        trackIntent.setAction(NOTIFICATION_ACTION);
        trackIntent.putExtra(ACTION_ID_KEY, (String) data.get(ACTION_ID_KEY));
        trackIntent.putExtra(ACTION_DEEP_LINK_KEY, (String) data.get(NOTIF_DEEP_LINK_KEY));
        trackIntent.putExtra(NOTIFICATION_ID, notificationId);
        trackIntent.putExtra(NOTIF_TOKEN_KEY, (String) data.get(NOTIF_TOKEN_KEY));

        ts.addNextIntent(getLaunchIntent());
        ts.addNextIntent(trackIntent);

        int flags = getDefaultIntentFlags();
        builder.setContentIntent(ts.getPendingIntent(0, flags));

        PushTemplate pushTemplate = (PushTemplate) data.get(ACTION_BUTTONS_KEY);
        if (pushTemplate != null) {
            String token = (String) data.get(NOTIF_TOKEN_KEY);
            List<ActionButton> actionButtons = pushTemplate.getButtons();
            for (ActionButton button : actionButtons) {
                createActionButton(builder, notificationId, button, token);
            }
        }

        // Image handling - fetch from URL
        // TODO (@paulwsmith): move off-thread? (maybe not necessary; not part of main thread
        // anyway)
        String imageUrl = (String) data.get(NOTIF_IMAGE_URL_KEY);
        if (!isNullOrEmpty(imageUrl)) {
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

    private int getNotificationIcon() {
        // Resource value of 0 means it was not found. We try to find the best icon in order of
        // preference, looking for each in both `drawable` and `mipmap`.
        Resources resources = applicationContext.getResources();
        int result =
                resources.getIdentifier(
                        NOTIF_ICON_SNAPYR_DEFAULT, "drawable", applicationContext.getPackageName());
        if (result == 0) {
            result =
                    resources.getIdentifier(
                            NOTIF_ICON_SNAPYR_DEFAULT,
                            "mipmap",
                            applicationContext.getPackageName());
        }
        if (result == 0) {
            result =
                    resources.getIdentifier(
                            NOTIF_ICON_FALLBACK, "drawable", applicationContext.getPackageName());
        }
        if (result == 0) {
            result =
                    resources.getIdentifier(
                            NOTIF_ICON_FALLBACK, "mipmap", applicationContext.getPackageName());
        }
        if (result == 0) {
            // Nothing found yet; use the app's own launcher icon. Should always be set...
            Log.d(
                    "Snapyr",
                    "SnapyrNotificationHandler: couldn't find notification icon; falling back to your app's launcher icon");
            result = applicationContext.getApplicationInfo().icon;
        }
        if (result == 0) {
            // ... if not, use an Android built-in icon guaranteed to be present (a bell)
            Log.d(
                    "Snapyr",
                    "SnapyrNotificationHandler: couldn't find app's launcher icon; falling back to system icon");
            result = android.R.drawable.ic_popup_reminder;
        }

        return result;
    }

    private Intent getLaunchIntent() {
        try {
            PackageManager pm = applicationContext.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(applicationContext.getPackageName());
            if (launchIntent == null) {
                // No launch intent specified / found for this app. Default to ACTION_MAIN
                launchIntent = new Intent(Intent.ACTION_MAIN);
                launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                launchIntent.setPackage(applicationContext.getPackageName());
            }
            return launchIntent;
        } catch (Exception e) {
            Log.e("Snapyr", "Could not get launch intent", e);
            return new Intent(Intent.ACTION_MAIN);
        }
    }

    private void createActionButton(
            NotificationCompat.Builder builder,
            int notificationId,
            ActionButton template,
            String actionToken) {
        Intent trackIntent = new Intent(this.context, SnapyrNotificationListener.class);
        trackIntent.setAction(NOTIFICATION_ACTION);
        trackIntent.putExtra(ACTION_ID_KEY, template.id);
        trackIntent.putExtra(ACTION_DEEP_LINK_KEY, template.deeplinkURL.toString());
        trackIntent.putExtra(NOTIFICATION_ID, notificationId);
        trackIntent.putExtra(NOTIF_TOKEN_KEY, actionToken);

        TaskStackBuilder ts = TaskStackBuilder.create(this.context);
        ts.addNextIntent(getLaunchIntent());
        ts.addNextIntent(trackIntent);

        int flags = getDefaultIntentFlags();

        builder.addAction(
                R.drawable.ic_snapyr_logo_only,
                template.title,
                ts.getPendingIntent(++nextActionButtonCode, flags));
    }

    private int getDefaultIntentFlags() {
        // Newer versions of Android require one of FLAG_MUTABLE or FLAG_IMMUTABLE to
        // be included. FLAG_IMMUTABLE is the default as we don't currently support
        // notifications with mutable content, such as inline-reply notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    public void showSampleNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this.context, this.defaultChannelId);
        builder.setSmallIcon(getNotificationIcon())
                .setContentTitle("Snapyr: Title")
                .setSubText("Snapyr: Subtext")
                .setContentText("Snapyr: Content text")
                .setAutoCancel(true);
        int notificationId = ++nextMessageId;

        createActionButton(
                builder,
                notificationId,
                new ActionButton(
                        "button_one", "button_one", "button_one", "snapyrsample://firsttest"),
                "");

        createActionButton(
                builder,
                notificationId,
                new ActionButton(
                        "button_two", "button_two", "button_two", "snapyrsample://secondtest"),
                "");

        String url = "snapyrsample://test/hello+world/more+stuff";
        Intent trackIntent = new Intent(applicationContext, SnapyrNotificationListener.class);
        trackIntent.setAction(SnapyrNotificationHandler.NOTIFICATION_ACTION);
        trackIntent.putExtra(SnapyrNotificationHandler.ACTION_ID_KEY, url);
        trackIntent.putExtra(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY, url);
        trackIntent.putExtra(SnapyrNotificationHandler.NOTIFICATION_ID, url);
        trackIntent.putExtra(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, url);

//        trackIntent.addFlags(
//                Intent.FLAG_ACTIVITY_SINGLE_TOP |
//                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
//                        Intent.FLAG_ACTIVITY_NO_ANIMATION
//        );

        builder.setContentIntent(
                PendingIntent.getActivity(applicationContext, (int) Math.random() * 100000,
                        trackIntent, getDefaultIntentFlags()));
        Notification notification = builder.build();
        notificationMgr.notify(nextMessageId++, notification);
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
