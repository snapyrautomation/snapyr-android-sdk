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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.snapyr.sdk.Properties;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.integrations.Logger;

public class SnapyrNotificationLifecycleCallbacks
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private Snapyr snapyr;
    private Boolean enabled;
    private Logger logger;

    public SnapyrNotificationLifecycleCallbacks(Snapyr snapyr, Logger logger, Boolean enabled) {
        this.snapyr = snapyr;
        this.logger = logger;
        this.enabled = enabled;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (!enabled) {
            return;
        }

        Intent launchIntent = activity.getIntent();
        if (launchIntent == null) {
            logger.info("NotifCB: launchIntent is null. Returning.");
            return;
        }

        Uri intentData = launchIntent.getData();
        if (intentData == null) {
            // No deep link URL - this was a "normal" activity launch, by opening the app or
            // otherwise.
            // No notification-related behavior, so nothing to track
            logger.info("NotifCB: intentData is null. Returning.");
            return;
        }

        trackNotificationInteraction(activity);
    }

    private void trackNotificationInteraction(Activity activity) {
        Intent intent = activity.getIntent();
        Context applicationContext = activity.getApplicationContext();

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
                        .putValue("actionId", actionId);

        if (interactionType != null) {
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
        }

        // if autocancel = true....
        // Dismiss source notification
        NotificationManagerCompat.from(applicationContext).cancel(notificationId);
        // Close notification drawer (so newly opened activity isn't behind anything)
        applicationContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        Snapyr.with(applicationContext).pushNotificationClicked(props);
    }

    // Method stubs required for valid interface implementation
    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {}

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {}

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {}

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {}

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {}
}
