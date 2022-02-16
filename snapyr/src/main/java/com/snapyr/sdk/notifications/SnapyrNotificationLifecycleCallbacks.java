package com.snapyr.sdk.notifications;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

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

        snapyr.trackNotificationInteraction(activity.getIntent());
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
