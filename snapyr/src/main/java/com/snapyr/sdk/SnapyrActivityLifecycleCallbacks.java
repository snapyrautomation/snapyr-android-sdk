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
package com.snapyr.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.snapyr.sdk.internal.TrackerUtil;
import com.snapyr.sdk.notifications.SnapyrNotificationListener;
import com.snapyr.sdk.services.ServiceFacade;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class SnapyrActivityLifecycleCallbacks
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    enum MonitoredCallbacks {
        ON_ACTIVITY_CREATED,
        ON_ACTIVITY_STARTED,
        ON_ACTIVITY_RESUMED,
    }

    private class ActivityCallbackTracker {
        public Boolean hasManualCall = false;
        public Boolean hasAutoCall = false;

        public Boolean callShouldExecute(Boolean isManual) {
            Boolean shouldExecute = true;
            if (isManual) {
                // manual call always skips if any other call has been made
                if (this.hasManualCall || this.hasAutoCall) {
                    shouldExecute = false;
                }
                this.hasManualCall = true;
            } else {
                // if there's been a manual call, the first auto call should skip.
                // Subsequent auto calls should process, e.g. to allow the same activity to resume
                // from the background
                if (this.hasManualCall && !this.hasAutoCall) {
                    shouldExecute = false;
                }
                this.hasAutoCall = true;
            }
            return shouldExecute;
        }
    }

    // This is just a stub LifecycleOwner which is used when we need to call some lifecycle
    // methods without going through the actual lifecycle callbacks
    private static final LifecycleOwner stubOwner =
            new LifecycleOwner() {
                final Lifecycle stubLifecycle =
                        new Lifecycle() {
                            @Override
                            public void addObserver(@NonNull LifecycleObserver observer) {
                                // NO-OP
                            }

                            @Override
                            public void removeObserver(@NonNull LifecycleObserver observer) {
                                // NO-OP
                            }

                            @NonNull
                            @Override
                            public Lifecycle.State getCurrentState() {
                                return State.DESTROYED;
                            }
                        };

                @NonNull
                @Override
                public Lifecycle getLifecycle() {
                    return stubLifecycle;
                }
            };
    private final Snapyr snapyr;
    private final ExecutorService analyticsExecutor;
    private final Boolean shouldTrackApplicationLifecycleEvents;
    private final Boolean trackDeepLinks;
    private final Boolean shouldRecordScreenViews;
    private final PackageInfo packageInfo;
    private final AtomicBoolean trackedApplicationLifecycleEvents;
    private final AtomicInteger numberOfActivities;
    private final AtomicBoolean firstLaunch;
    private final AtomicBoolean isChangingActivityConfigurations;
    HashMap<String, ActivityCallbackTracker> activityCbs = new HashMap<>();
    private final Boolean useNewLifecycleMethods;
    private long backgroundStart;

    private SnapyrActivityLifecycleCallbacks(
            Snapyr snapyr,
            ExecutorService analyticsExecutor,
            Boolean shouldTrackApplicationLifecycleEvents,
            Boolean trackDeepLinks,
            Boolean shouldRecordScreenViews,
            PackageInfo packageInfo,
            Boolean useNewLifecycleMethods) {
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: CONSTRUCTOR");
        this.trackedApplicationLifecycleEvents = new AtomicBoolean(false);
        this.numberOfActivities = new AtomicInteger(1);
        this.firstLaunch = new AtomicBoolean(false);
        this.snapyr = snapyr;
        this.analyticsExecutor = analyticsExecutor;
        this.shouldTrackApplicationLifecycleEvents = shouldTrackApplicationLifecycleEvents;
        this.trackDeepLinks = trackDeepLinks;
        this.shouldRecordScreenViews = shouldRecordScreenViews;
        this.packageInfo = packageInfo;
        this.useNewLifecycleMethods = useNewLifecycleMethods;
        this.isChangingActivityConfigurations = new AtomicBoolean(false);
    }

    private void registerDeeplinkReceiver() {
        IntentFilter filter =
                new IntentFilter("com.snapyr.sdk.notifications.ACTION_DEEPLINK"); // sample scope
        //        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        NonImplicitReceiver nonImplicitReceiver = new NonImplicitReceiver();

        ServiceFacade.getApplication().registerReceiver(nonImplicitReceiver, filter);
        Log.e("YYY", "LIFECYCLE: registered receiver!");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onStop");
        backgroundStart = System.currentTimeMillis();
        // App in background
        if (shouldTrackApplicationLifecycleEvents
                && numberOfActivities.decrementAndGet() == 0
                && !isChangingActivityConfigurations.get()) {
            snapyr.track("Application Backgrounded");
        }
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onStop: done");
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onStart");

        registerDeeplinkReceiver();

        long elapsed = System.currentTimeMillis() - backgroundStart;
        if (elapsed > 30000) {
            // end the last session whenever the background first occurred
            snapyr.sessionEnded(backgroundStart);
            snapyr.sessionStarted(); // backgrounded too long, create a new session
        }

        // App in foreground
        if (shouldTrackApplicationLifecycleEvents
                && numberOfActivities.incrementAndGet() == 1
                && !isChangingActivityConfigurations.get()) {
            Properties properties = new Properties();
            if (firstLaunch.get()) {
                properties
                        .putValue("version", packageInfo.versionName)
                        .putValue("build", String.valueOf(packageInfo.versionCode));
            }
            properties.putValue("from_background", !firstLaunch.getAndSet(false));
            snapyr.track("Application Opened", properties);
        }
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onStart: done");
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        // App created
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onCreate");

        registerDeeplinkReceiver();

        if (!trackedApplicationLifecycleEvents.getAndSet(true)
                && shouldTrackApplicationLifecycleEvents) {
            numberOfActivities.set(0);
            firstLaunch.set(true);
            snapyr.trackApplicationLifecycleEvents();
        }
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onCreate: done");
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        Log.e("XXX", "SnapyrActivityLifecycleCallbacks: onResume");

        registerDeeplinkReceiver();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        Log.e("XXX", "SnapyrActivityLifecycleCallbacks: onPause");
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.e("XXX", "SnapyrActivityLifecycleCallbacks: onDestroy");
    }

    /**
     * Tracks repeated triggers for the same activity lifecycle event, to account for manual
     * "replay" methods that allow client to explicitly call these, and ensure that we don't trigger
     * the same callback twice (i.e. once from automatic lifecycle trigger, again from manual client
     * replay)
     *
     * @param callback Which lifecycle callback to check
     * @param activity The activity undergoing a lifecycle event
     * @param manualCall Whether the callback was triggered manually by a client call
     * @return true if this lifecycle has already been processed and should be skipped; otherwise
     *     false
     */
    private Boolean checkAndUpdateCallbackTracked(
            MonitoredCallbacks callback, Activity activity, Boolean manualCall) {
        String key = String.valueOf(activity.hashCode()) + callback;
        ActivityCallbackTracker tracker = activityCbs.get(key);
        if (tracker == null) {
            tracker = new ActivityCallbackTracker();
            tracker.callShouldExecute(manualCall);
            activityCbs.put(key, tracker);
            return false;
        }
        Boolean result = tracker.callShouldExecute(manualCall);
        if (!result) {
            Log.d(
                    "Snapyr",
                    String.format(
                            "Activity lifecycle: %s: already processed; skipping",
                            callback.name()));
            return true;
        }
        return false;
    }

    private Boolean shouldTrackForActivity(Activity activity) {
        // don't act on internal Snapyr SDK activities
        if (activity instanceof SnapyrNotificationListener) {
            return false;
        }
        return true;
    }

    private void resetTrackedCallbacks(Activity activity) {
        for (MonitoredCallbacks cb : MonitoredCallbacks.values()) {
            String key = String.valueOf(activity.hashCode()) + cb;
            activityCbs.remove(key);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle activitySavedInstanceState) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityCreated");
        this.onActivityCreated(activity, activitySavedInstanceState, false);
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityCreated: done");
    }

    public void onActivityCreated(
            Activity activity, Bundle activitySavedInstanceState, Boolean manualCall) {
        if (this.checkAndUpdateCallbackTracked(
                MonitoredCallbacks.ON_ACTIVITY_CREATED, activity, manualCall)) {
            return;
        }

        if (!useNewLifecycleMethods) {
            onCreate(stubOwner);
        }

        if (trackDeepLinks) {
            TrackerUtil.trackDeepLink(activity, activity.getIntent());
        }
        this.trackNotificationIntent(activity);
        ServiceFacade.getInstance().setCurrentActivity(activity);
    }

    private void trackNotificationIntent(Activity activity) {
        Log.i("XXX", "SnapyrActivityLifecycleCallbacks: trackNotificationIntent");
        if (!trackDeepLinks) {
            Log.i("XXX", "trackDeepLinks off; returning");
            return;
        }

        Intent launchIntent = activity.getIntent();
        if (launchIntent == null) {
            Log.i(
                    "Snapyr",
                    "SnapyrActivityLifecycleCallbacks: trackNotificationIntent: launchIntent is null. Returning.");
            return;
        }

        Uri intentData = launchIntent.getData();
        if (intentData == null) {
            // No deep link URL - this was a "normal" activity launch, by opening the app or
            // otherwise.
            // No notification-related behavior, so nothing to track
            Log.i(
                    "Snapyr",
                    "SnapyrActivityLifecycleCallbacks: trackNotificationIntent: intentData is null. Returning.");
            return;
        }

        TrackerUtil.trackDeepLink(activity, activity.getIntent());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityStarted");
        this.onActivityStarted(activity, false);
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityStarted: done");
    }

    public void onActivityStarted(Activity activity, Boolean manualCall) {
        if (this.checkAndUpdateCallbackTracked(
                MonitoredCallbacks.ON_ACTIVITY_STARTED, activity, manualCall)) {
            return;
        }

        if (shouldRecordScreenViews) {
            snapyr.recordScreenViews(activity);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityResumed");
        this.onActivityResumed(activity, false);
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityResumed: done");
    }

    public void onActivityResumed(Activity activity, Boolean manualCall) {
        if (this.checkAndUpdateCallbackTracked(
                MonitoredCallbacks.ON_ACTIVITY_RESUMED, activity, manualCall)) {
            return;
        }

        if (!useNewLifecycleMethods) {
            onStart(stubOwner);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityPaused");
        this.resetTrackedCallbacks(activity);
        if (!useNewLifecycleMethods) {
            onPause(stubOwner);
        }
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityPaused: done");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityStopped");
        this.resetTrackedCallbacks(activity);
        if (!useNewLifecycleMethods) {
            onStop(stubOwner);
        }
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityStopped: done");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivitySaveInstanceState");
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivitySaveInstanceState: done");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (!shouldTrackForActivity(activity)) return;
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityDestroyed");
        this.resetTrackedCallbacks(activity);
        if (!useNewLifecycleMethods) {
            onDestroy(stubOwner);
        }
        Log.d("XXX", "SnapyrActivityLifecycleCallbacks: onActivityDestroyed: done");
    }

    public static class Builder {
        private Snapyr snapyr;
        private ExecutorService analyticsExecutor;
        private Boolean shouldTrackApplicationLifecycleEvents;
        private Boolean trackDeepLinks;
        private Boolean shouldRecordScreenViews;
        private PackageInfo packageInfo;
        private Boolean useNewLifecycleMethods;

        public Builder() {}

        public Builder snapyr(Snapyr snapyr) {
            this.snapyr = snapyr;
            return this;
        }

        Builder analyticsExecutor(ExecutorService analyticsExecutor) {
            this.analyticsExecutor = analyticsExecutor;
            return this;
        }

        Builder shouldTrackApplicationLifecycleEvents(
                Boolean shouldTrackApplicationLifecycleEvents) {
            this.shouldTrackApplicationLifecycleEvents = shouldTrackApplicationLifecycleEvents;
            return this;
        }

        Builder trackDeepLinks(Boolean trackDeepLinks) {
            this.trackDeepLinks = trackDeepLinks;
            return this;
        }

        Builder shouldRecordScreenViews(Boolean shouldRecordScreenViews) {
            this.shouldRecordScreenViews = shouldRecordScreenViews;
            return this;
        }

        Builder packageInfo(PackageInfo packageInfo) {
            this.packageInfo = packageInfo;
            return this;
        }

        Builder useNewLifecycleMethods(boolean useNewLifecycleMethods) {
            this.useNewLifecycleMethods = useNewLifecycleMethods;
            return this;
        }

        public SnapyrActivityLifecycleCallbacks build() {
            return new SnapyrActivityLifecycleCallbacks(
                    snapyr,
                    analyticsExecutor,
                    shouldTrackApplicationLifecycleEvents,
                    trackDeepLinks,
                    shouldRecordScreenViews,
                    packageInfo,
                    useNewLifecycleMethods);
        }
    }
}
