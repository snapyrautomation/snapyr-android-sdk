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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.services.Logger;
import com.snapyr.sdk.internal.TrackerUtil;

public class SnapyrNotificationLifecycleCallbacks
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private final Snapyr snapyr;
    private final Boolean enabled;
    private final Logger logger;

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

        TrackerUtil.trackDeepLink(activity, activity.getIntent());
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
