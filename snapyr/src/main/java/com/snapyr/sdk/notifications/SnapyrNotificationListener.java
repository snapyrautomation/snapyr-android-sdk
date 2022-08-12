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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.NotificationManagerCompat;
import com.snapyr.sdk.internal.TrackerUtil;
import com.snapyr.sdk.internal.Utils;

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
        String deepLink = intent.getStringExtra(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY);
        int notificationId = intent.getIntExtra(SnapyrNotificationHandler.NOTIFICATION_ID, -1);

        TrackerUtil.trackNotificationInteraction(this, intent);

        // Dismiss source notification
        NotificationManagerCompat.from(this.getApplicationContext()).cancel(notificationId);

        if (!Utils.isNullOrEmpty(deepLink)) { // deeplink provided, respect it and advance
            Intent deepLinkIntent = new Intent();
            deepLinkIntent.setAction("com.snapyr.sdk.notifications.ACTION_DEEPLINK");
            deepLinkIntent.setData(Uri.parse(deepLink));
            deepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(deepLinkIntent);
        }

        this.finish(); // Nothing to do, go back in the stack
    }
}
