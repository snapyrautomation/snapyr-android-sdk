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

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import com.snapyr.sdk.Properties;
import com.snapyr.sdk.Snapyr;

public class SnapyrActionService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deepLinkUrl = intent.getStringExtra(SnapyrNotificationHandler.NOTIF_DEEP_LINK_KEY);
        String actionId = intent.getStringExtra(SnapyrNotificationHandler.ACTION_ID_KEY);
        int notificationId = intent.getIntExtra("notificationId", 0);

        SnapyrNotificationHandler.INTERACTION_TYPE interactionType =
                (SnapyrNotificationHandler.INTERACTION_TYPE)
                        intent.getSerializableExtra(SnapyrNotificationHandler.INTERACTION_KEY);
        String token;

        Properties props =
                new Properties()
                        .putValue("deepLinkUrl", deepLinkUrl)
                        .putValue("actionId", actionId)
                        .putValue("testVersion", 6);

        switch (interactionType) {
            case NOTIFICATION_PRESS:
                token = intent.getStringExtra(SnapyrNotificationHandler.NOTIF_TOKEN_KEY);
                props.putValue(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, token)
                        .putValue("interactionType", "notificationPressed");
                break;
            case ACTION_BUTTON_PRESS:
                token = intent.getStringExtra(SnapyrNotificationHandler.ACTION_TOKEN_KEY);
                props.putValue(SnapyrNotificationHandler.ACTION_TOKEN_KEY, token)
                        .putValue("interactionType", "actionButtonPressed");
                break;
        }

        // if autocancel = true....
        // Dismiss source notification
        NotificationManagerCompat.from(this.getApplicationContext()).cancel(notificationId);
        // Close notification drawer (so newly opened activity isn't behind anything)
        this.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        Snapyr.with(this).pushNotificationClicked(props);

        Uri uri = Uri.parse(deepLinkUrl);
        Intent openDeepLinkIntent = new Intent(Intent.ACTION_VIEW, uri);
        openDeepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Snapyr.with(this).getApplication().startActivity(openDeepLinkIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @return an intent that launches the default activity for the app, i.e. "open the app" when
     *     the notification is clicked.
     */
    public Intent getDefaultIntent() {
        String packageName = this.getPackageName();
        Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        return launchIntent;
        //        String x = launchIntent.getAction();
        //        return PendingIntent.getActivity(this.getApplicationContext(), 0, launchIntent,
        // 0);
    }
}
