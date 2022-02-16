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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationManagerCompat;

import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.internal.Utils;


/**
 * SnapyrNotificationListener
 * Listens for broadcast events for the action `@package_name.TRACK_BROADCAST` and fires off
 * a track event with the payload from the intent. This is currently used by action buttons to
 * trigger track notifications instead of creating a new activity. If the action button specifies
 * a deeplink then the listener will attempt to transition to the URI provided.
 */
public class SnapyrNotificationListener extends BroadcastReceiver {
    private static final String TAG = "SnapyrNotificationListener";



//    @Override
//    public void onCreate(Bundle bundle){
//        super.onCreate(bundle);
//
//        Intent startIntent = new Intent();
//        startIntent.setAction("android.intent.action.MAIN");
//        startIntent.addCategory("android.intent.category.LAUNCHER");
//        startIntent.setData(Uri.parse("snapyrsample://test"));
//        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        this.startActivity(startIntent);
//    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionId =  intent.getStringExtra(SnapyrNotificationHandler.ACTION_ID_KEY);
        String deepLink =   intent.getStringExtra(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY);
        int notificationId =  intent.getIntExtra(SnapyrNotificationHandler.NOTIFICATION_ID, -1);
        SnapyrNotificationHandler.INTERACTION_TYPE interaction =
                (SnapyrNotificationHandler.INTERACTION_TYPE)intent
                        .getExtras().get(SnapyrNotificationHandler.INTERACTION_KEY);

        Snapyr snapyr = Snapyr.with(context);
        snapyr.trackNotificationInteraction(intent);

        // Dismiss source notification
        NotificationManagerCompat.from(context.getApplicationContext()).cancel(notificationId);
        // Close notification drawer (so newly opened activity isn't behind anything)
        context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        if (!Utils.isNullOrEmpty(deepLink)){
            Intent deepLinkIntent = new Intent();
            deepLinkIntent.setAction("android.intent.action.MAIN");
            deepLinkIntent.addCategory("android.intent.category.LAUNCHER");
            deepLinkIntent.setData(Uri.parse(deepLink));
            deepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(deepLinkIntent);
        }

        // TODO: if deeplink is valid then do something
    }
}
