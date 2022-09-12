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
import android.os.Build;
import android.os.StrictMode;
import android.os.strictmode.Violation;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.inapp.InAppConfig;
import com.snapyr.sdk.inapp.InAppMessage;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import java.util.concurrent.Executors;

public class SampleApp extends Application {

    // https://segment.com/segment-engineering/sources/android-test/settings/keys
    //    private static final String ANALYTICS_WRITE_KEY = "HO63Z36e0Ufa8AAgbjDomDuKxFuUICqI";
    private static final String ANALYTICS_WRITE_KEY = "cTcjOQYhhxOTXF6eHFflOCyYPO6pfAOV";

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder()
                        .detectLeakedClosableObjects()
                        .detectLeakedRegistrationObjects()
                        // .penaltyDeath()
                        .penaltyListener(
                                Executors.newSingleThreadExecutor(),
                                (Violation var1) -> {
                                    // This catches leaks in our code that would have propagated to
                                    // client code in the wild. If you want to be really careful
                                    // uncomment the penaltyDeath line above --BS
                                    Log.e(
                                            var1.getLocalizedMessage(),
                                            String.valueOf(var1.getCause().getStackTrace()));
                                })
                        .build());

        ViewPump.init(
                ViewPump.builder()
                        .addInterceptor(
                                new CalligraphyInterceptor(
                                        new CalligraphyConfig.Builder()
                                                .setDefaultFontPath("fonts/CircularStd-Book.otf")
                                                .setFontAttrId(R.attr.fontPath)
                                                .build()))
                        .build());

        // Initialize a new instance of the Analytics client.
        Snapyr.Builder builder =
                new Snapyr.Builder(this, ANALYTICS_WRITE_KEY)
                        .enableDevEnvironment()
                        .experimentalNanosecondTimestamps()
                        .trackApplicationLifecycleEvents()
                        .trackDeepLinks()
                        .logLevel(Snapyr.LogLevel.DEBUG)
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
                        .flushQueueSize(1)
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
        Snapyr snapyr = Snapyr.with(this);
    }

    private void userInAppCallback(InAppMessage message) {
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
                        + message.Content
                        + "\n\t");
    }
}
