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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.internal.TrackerUtil;
import com.snapyr.sdk.services.ServiceFacade;

/**
 * SnapyrNotificationListener Activity that triggers and fires off a track event with the payload
 * from the intent. This is currently used by action buttons and notifications to trigger track
 * notifications. If the action button or notification specifies a deeplink then the listener will
 * attempt to transition to the URI provided.
 */
public class SnapyrNotificationListener extends Activity {
    private static final String TAG = "SnapyrNotificationListener";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();

        SnapyrNotification snapyrNotification = intent.getParcelableExtra("snapyrNotification");

        int notificationId = intent.getIntExtra(SnapyrNotificationHandler.NOTIFICATION_ID, -1);
        // Dismiss source notification
        NotificationManagerCompat.from(this.getApplicationContext()).cancel(notificationId);

        try {
            Snapyr snapyrInst = SnapyrNotificationUtils.getSnapyrInstance(this);
            TrackerUtil.trackNotificationInteraction(snapyrInst, snapyrNotification);
            snapyrInst.flush();
        } catch (Exception e) {
            Log.e(
                    "Snapyr",
                    "Notification interaction listener couldn't initialize Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.",
                    e);
        }

        sendNotificationTappedBroadcast(snapyrNotification);
        launchActualActivity(snapyrNotification);

        this.finish(); // Nothing to do, go back in the stack
    }

    private void sendNotificationTappedBroadcast(SnapyrNotification snapyrNotification) {
        Intent deepLinkIntent = new Intent(SnapyrNotificationHandler.NOTIFICATION_TAPPED_ACTION);
        // makes intent "explicit", allowing it to reach manifest-defined receivers in current app
        deepLinkIntent.setPackage(this.getPackageName());
        deepLinkIntent.putExtra("snapyrNotification", snapyrNotification);
        this.sendBroadcast(deepLinkIntent);
        ServiceFacade.getLogger()
                .info("SnapyrNotificationListener: notification-tapped broadcast sent");
    }

    private void launchActualActivity(SnapyrNotification snapyrNotification) {
        Intent launchActivityIntent = getLaunchIntent(snapyrNotification.deepLinkUrl);
        launchActivityIntent.putExtra("snapyrNotification", snapyrNotification);

        try {
            this.startActivity(launchActivityIntent);
            return;
        } catch (ActivityNotFoundException e) {
            String detailMessage;
            if (snapyrNotification.deepLinkUrl != null) {
                detailMessage =
                        String.format(
                                "Deep link url: %s - attempting fallback launcher activity...",
                                snapyrNotification.deepLinkUrl.toString());
            } else {
                detailMessage = "Main launcher activity (no deep link specified)";
            }
            ServiceFacade.getLogger()
                    .error(
                            e,
                            "SnapyrNotificationListener: failed to launch activity from notification: %s",
                            detailMessage);
        }

        if (snapyrNotification.deepLinkUrl != null) {
            // Tried launching deep link intent but failed. Fall back to standard launch intent for
            // this app
            Intent fallbackIntent = getLaunchIntent(null);
            try {
                startActivity(fallbackIntent);
            } catch (ActivityNotFoundException e) {
                // This should never happen but log in case it does...
                ServiceFacade.getLogger()
                        .error(
                                e,
                                "SnapyrNotificationListener: failed to start fallback launcher activity");
            }
        }
    }

    private Intent getLaunchIntent(Uri deepLinkUri) {
        Intent launchIntent;
        PackageManager pm = this.getPackageManager();

        if (deepLinkUri != null) {
            launchIntent = new Intent(Intent.ACTION_VIEW, deepLinkUri);
            launchIntent.setPackage(this.getPackageName());
        } else {
            try {
                launchIntent = pm.getLaunchIntentForPackage(this.getPackageName());
                if (launchIntent == null) {
                    // No launch intent specified / found for this app. Default to ACTION_MAIN
                    launchIntent = new Intent(Intent.ACTION_MAIN);
                    launchIntent.setPackage(this.getPackageName());
                }

                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } catch (Exception e) {
                Log.e("Snapyr", "Could not get launch intent", e);
                launchIntent = new Intent(Intent.ACTION_MAIN);
            }
        }

        if (!isTaskRoot()) {
            // If app/activity is already open, and is standard launch mode, this prevents a
            // duplicate activity from being launched. i.e. behave more like launcher mode - bring
            // activity back to front.
            // Only add this flag if already open; otherwise, activity silently fails to open
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }

        try {
            ResolveInfo resolveInfo = pm.resolveActivity(launchIntent, 0);
            int launchMode = resolveInfo.activityInfo.launchMode;
            if (launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
                // Ensures onNewIntent is called for existing, singleTop activity
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            }
        } catch (Exception e) {
            ServiceFacade.getLogger()
                    .info("SnapyrNotificationListener: Exception checking launchMode", e);
        }

        return launchIntent;
    }
}
