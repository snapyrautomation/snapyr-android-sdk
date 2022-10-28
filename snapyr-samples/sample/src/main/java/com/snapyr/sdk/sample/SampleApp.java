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
package com.snapyr.sdk.sample;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.StrictMode;
import android.os.strictmode.Violation;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.inapp.InAppConfig;
import com.snapyr.sdk.inapp.InAppMessage;
import com.snapyr.sdk.inapp.InAppPayloadType;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import java.util.concurrent.Executors;

public class SampleApp extends Application {

    // https://segment.com/segment-engineering/sources/android-test/settings/keys
    //    private static final String ANALYTICS_WRITE_KEY = "HO63Z36e0Ufa8AAgbjDomDuKxFuUICqI";
    private static final String ANALYTICS_WRITE_KEY = "MsZEepxHLRM9d7CU0ClTC84T0E9w9H8w"; //
    // Paul's workspace - DEV
    //    private static final String ANALYTICS_WRITE_KEY =
    //            "Kl34fEmzG753oODf9UhGz76wYMXW6Gia"; // Paul's push/in-app testing WS - PROD

    //    public SampleApp()
    public SampleApp() {
        super();
        Log.e("XXX", "SampleApp: constructor - ATTACH DEBUGGER!!!");
        //        android.os.Debug.waitForDebugger();
        Log.e("XXX", "SampleApp: constructor - resuming.");
    }

    @Override
    protected void finalize() throws Throwable {
        Log.e("XXX", "SampleApp: FINALIZE");
        super.finalize();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e("XXX", "SampleApp: onTerminate");
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("XXX", "SampleApp: onCreate - ATTACH DEBUGGER!!!");
        //        android.os.Debug.waitForDebugger();
        Log.e("XXX", "SampleApp: onCreate - resuming.");

        // Register contextual broadcast receiver
        //        IntentFilter filter = new
        // IntentFilter("com.snapyr.sdk.notifications.ACTION_DEEPLINK"); // notifications scope

        IntentFilter filter =
                new IntentFilter("com.snapyr.sdk.sample.ACTION_DEEPLINK"); // sample scope
        NonImplicitReceiver nonImplicitReceiver = new NonImplicitReceiver();
        this.registerReceiver(nonImplicitReceiver, filter);

        IntentFilter filter2 = new IntentFilter("com.snapyr.sdk.ACTION_DEEPLINK"); // sdk scope
        NonImplicitReceiver nonImplicitReceiver2 = new NonImplicitReceiver();
        this.registerReceiver(nonImplicitReceiver2, filter2);

        IntentFilter filter3 =
                new IntentFilter(
                        "com.snapyr.sdk.notifications.ACTION_DEEPLINK"); // notifications scope
        NonImplicitReceiver nonImplicitReceiver3 = new NonImplicitReceiver();
        this.registerReceiver(nonImplicitReceiver3, filter3);

        Log.e("YYY", "SampleApp: registered receivers!");

        StrictMode.VmPolicy.Builder policyBuilder =
                new StrictMode.VmPolicy.Builder()
                        .detectLeakedClosableObjects()
                        .detectLeakedRegistrationObjects()
                        .detectActivityLeaks()
                        //                        .detectAll()
                        //                        .penaltyLog()
                        //                                         .penaltyDeath()
                        .penaltyListener(
                                Executors.newSingleThreadExecutor(),
                                new StrictMode.OnVmViolationListener() {
                                    @Override
                                    public void onVmViolation(Violation v) {
                                        // This catches leaks in our code that would have propagated
                                        // to
                                        // client code in the wild. If you want to be really careful
                                        // uncomment the penaltyDeath line above --BS
                                        Log.e("Snapyr", "onVmViolation:" + v.toString(), v);
                                        android.os.Debug.waitForDebugger();
                                        v.printStackTrace();
                                        Log.e("Snapyr", "getCause():", v.getCause());
                                        Log.e(
                                                "Snapyr",
                                                "fillInStackTrace():",
                                                v.fillInStackTrace());
                                        Throwable[] suppressed = v.getSuppressed();
                                        String stackTraceString =
                                                Log.getStackTraceString(v.fillInStackTrace());
                                    }
                                });
        //                        .penaltyListener(
        //                                Executors.newSingleThreadExecutor(),
        //                                (Violation var1) -> {
        //                                    // This catches leaks in our code that
        // would have propagated to
        //                                    // client code in the wild. If you
        // want to be really careful
        //                                    // uncomment the penaltyDeath line
        // above --BS
        //                                    Log.e(
        //                                            var1.getLocalizedMessage(),
        //
        // String.valueOf(var1.getCause().getStackTrace()));
        //                                })

        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        //            policyBuilder.detectIncorrectContextUse();
        //        }
        StrictMode.setVmPolicy(policyBuilder.build());

        //                        .build());

        Typeface typeface =
                Typeface.createFromAsset(this.getAssets(), "fonts/CircularStd-Book.otf");

        ViewPump.init(
                ViewPump.builder()
                        .addInterceptor(
                                new CalligraphyInterceptor(
                                        new CalligraphyConfig.Builder()
                                                .setDefaultFontPath("fonts/CircularStd-Book.otf")
                                                //
                                                // .setFontAttrId(R.attr.fontPath)
                                                .build()))
                        .build());

        // Initialize a new instance of the Analytics client.
        Snapyr.Builder builder =
                new Snapyr.Builder(this, ANALYTICS_WRITE_KEY)
                        .enableDevEnvironment()
                        .experimentalNanosecondTimestamps()
                        .trackApplicationLifecycleEvents()
                        .trackDeepLinks()
                        .logLevel(Snapyr.LogLevel.VERBOSE)
                        .defaultProjectSettings(
                                new ValueMap()
                                        .putValue(
                                                "integrations",
                                                new ValueMap()
                                                        .putValue(
                                                                "Snapyr",
                                                                new ValueMap()
                                                                        .putValue(
                                                                                "apiKey",
                                                                                ANALYTICS_WRITE_KEY)
                                                                        .putValue(
                                                                                "trackAttributionData",
                                                                                true))))
                        .flushQueueSize(20)
                        .enableSnapyrPushHandling()
                        .configureInAppHandling(
                                new InAppConfig()
                                        .setPollingRate(30000)
                                        .setActionCallback(
                                                (inAppMessage) -> {
                                                    userInAppCallback(inAppMessage);
                                                }))
                        .recordScreenViews();

        // Set the initialized instance as a globally accessible instance.
        Snapyr.setSingletonInstance(builder.build());

        // Now anytime you call Snapyr.with, the custom instance will be returned.
        //        Snapyr snapyr = Snapyr.with(this);

        Log.e("XXX", "SampleApp: onCreate - done, and Snapyr initialized.");
    }

    private void userInAppCallback(InAppMessage message) {
        String contentPayload = "";
        try {
            contentPayload =
                    (message.Content.getType() == InAppPayloadType.PAYLOAD_TYPE_HTML)
                            ? message.Content.getHtmlPayload()
                            : message.Content.getJsonPayload().toString();
        } catch (Exception e) {
            Log.e("Snapyr", "Exception trying to parse in-app content:", e);
        }
        Log.println(
                Log.INFO,
                "SnapyrInApp",
                "inapp cb triggered: \n\t"
                        + message.Timestamp
                        + "\n\t"
                        + message.ActionType
                        + "\n\t"
                        + message.UserId
                        + "\n\t"
                        + message.ActionToken
                        + "\n\t"
                        + contentPayload
                        + "\n\t");

        Log.e("YYY", "Broadcasting message");
        Intent intent = new Intent("sample-intent-passer");
        // You can also include some extra data.

        intent.putExtra("inAppMessage", message);
        //        ValueMap valueMap = message.asValueMap();
        //        intent.putExtra("inAppMessage", valueMap.toJsonObject());
        intent.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
