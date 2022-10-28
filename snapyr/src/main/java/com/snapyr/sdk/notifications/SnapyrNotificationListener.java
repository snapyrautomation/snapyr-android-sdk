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

import static com.snapyr.sdk.notifications.SnapyrNotificationHandler.DEEPLINK_ACTION;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.internal.TrackerUtil;
import com.snapyr.sdk.internal.Utils;
import java.text.MessageFormat;
import java.util.List;

/**
 * SnapyrNotificationListener Activity that triggers and fires off a track event with the payload
 * from the intent. This is currently used by action buttons and notifications to trigger track
 * notifications. If the action button or notification specifies a deeplink then the listener will
 * attempt to transition to the URI provided.
 */
public class SnapyrNotificationListener extends Activity {
    private static final String TAG = "SnapyrNotificationListener";

    public SnapyrNotificationListener() {
        super();
        Log.e("XXX", "SnapyrNotificationListener: CONSTRUCTOR");
    }

    @Override
    protected void finalize() throws Throwable {
        Log.e("XXX", "SnapyrNotificationListener: FINALIZE");
        super.finalize();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("YYY", "SnapyrNotificationListener: onCreate - ATTACH DEBUGGER!!!");
        //        android.os.Debug.waitForDebugger();
        Intent intent = this.getIntent();
        String deepLink = intent.getStringExtra(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY);
        int notificationId = intent.getIntExtra(SnapyrNotificationHandler.NOTIFICATION_ID, -1);

        try {
            Snapyr snapyrInst = SnapyrNotificationUtils.getSnapyrInstance(this);
            TrackerUtil.trackNotificationInteraction(snapyrInst, intent);
            snapyrInst.flush();
        } catch (Exception e) {
            Log.e(
                    "Snapyr",
                    "Notification interaction listener couldn't initialize Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.");
        }

        PackageManager packageManager = this.getApplicationContext().getPackageManager();

        // Dismiss source notification
        Log.e("XXX", "SnapyrNotificationListener: NOT cancelling notification...");
//        NotificationManagerCompat.from(this.getApplicationContext()).cancel(notificationId);

        Intent deepLinkIntent = new Intent(DEEPLINK_ACTION);
        //        deepLinkIntent.setAction(DEEPLINK_ACTION);
        deepLinkIntent.setPackage(
                this.getPackageName()); // makes this intent "explicit" which allows it to reach
        // a manifest-defined receiver
        deepLinkIntent.putExtras(intent); // forward all extras, i.e. Snapyr-defined data

//                if (!Utils.isNullOrEmpty(deepLink)) {
//                    deepLinkIntent.setData(Uri.parse(deepLink));
//                }

        List<ResolveInfo> receivers = packageManager.queryBroadcastReceivers(deepLinkIntent, 0);

        for (ResolveInfo receiver : receivers) {
            Log.e(
                    "YYY",
                    MessageFormat.format(
                            "Receiver: {0} Packagename: {1} ServiceInfo: {2} ActivityInfo: {3} ProviderInfo: {4} isDefault: {5}",
                            receiver,
                            receiver.resolvePackageName,
                            receiver.serviceInfo,
                            receiver.activityInfo,
                            receiver.providerInfo,
                            receiver.isDefault));
        }

        this.sendBroadcast(deepLinkIntent);

        Intent nextActivityIntent = this.getLaunchIntent();

        if (!Utils.isNullOrEmpty(deepLink)) {
            nextActivityIntent.setData(Uri.parse(deepLink));
            nextActivityIntent.putExtras(intent);
        }

        this.startActivity(nextActivityIntent);

        if (!Utils.isNullOrEmpty(deepLink)) { // deeplink provided, respect it and advance

            //            getPackageManager().resolveActivity(deepLinkIntent)

            //            ComponentName componentName =
            // deepLinkIntent.resolveActivity(getPackageManager());
            //
            //            Log.e(
            //                    "YYY",
            //                    MessageFormat.format(
            //                            "SnapyrNotificationListener: intent component: {0}",
            // componentName));

            try {
                //                this.startActivity(deepLinkIntent);
                //
                // LocalBroadcastManager.getInstance(this).sendBroadcast(deepLinkIntent);
                //                sendBroadcast(deepLinkIntent);

                //                sendBroadcast(deepLinkIntent, "owner.custom.permission");
                //                Log.e("YYY", MessageFormat.format("BROADCAST SENT FROM
                // SnapyrNotificationListener: {0}", deepLinkIntent));
                //                Log.e("YYY", "Broadcast sent without error!");
                //                Intent intent2 = new Intent(DEEPLINK_ACTION); // notifications
                // scope
                //                Intent intent2 = new
                // Intent("com.snapyr.sdk.sample.ACTION_DEEPLINK");  // sample scope
                //                sendBroadcast(intent2);
                //                sendBroadcast(intent2, "owner.custom.permission");
                //                Log.e("YYY", MessageFormat.format("BROADCAST SENT FROM
                // SnapyrNotificationListener: {0}", intent2));
            } catch (ActivityNotFoundException e) {
                Log.e("YYY", "RUH ROH! ACTIVITY NOT FOUND!");
            } catch (Exception e) {
                Log.e("YYY", MessageFormat.format("OTHER ERROR: {0}", e));
            }
        }

//        this.startActivityIfNeeded()

        this.finish(); // Nothing to do, go back in the stack
    }

    public Intent getLaunchIntent() {
        Context applicationContext = this.getApplicationContext();
        try {
            PackageManager pm = applicationContext.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(applicationContext.getPackageName());
            if (launchIntent == null) {
                // No launch intent specified / found for this app. Default to ACTION_MAIN
                launchIntent = new Intent(Intent.ACTION_MAIN);


                launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                launchIntent.setPackage(applicationContext.getPackageName());
                Log.i("XXX", "Listener: getLaunchIntent: defaulting to ACTION_MAIN...");
            } else {
//                launchIntent.setFlags(0);
                launchIntent.setPackage(null); // helps make existing activity resume (if any)
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
//                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

//                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.i("XXX", "Listener: getLaunchIntent: `getLaunchIntentForPackage`");
            }
            String x = launchIntent.getDataString();
            Log.i(
                    "XXX",
                    "Listener: getLaunchIntent: "
                            + x
                            + " - "
                            + launchIntent.toString());
            return launchIntent;
        } catch (Exception e) {
            Log.e("Snapyr", "Could not get launch intent", e);
            return new Intent(Intent.ACTION_MAIN);
        }
    }

    @Override
    protected void onStop() {
        Log.e("XXX", "SnapyrNotificationListener: onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.e("XXX", "SnapyrNotificationListener: onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("XXX", "SnapyrNotificationListener: onDestroy");
        super.onDestroy();
    }
}
