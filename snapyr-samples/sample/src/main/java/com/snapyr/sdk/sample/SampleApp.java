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

import com.snapyr.sdk.LegacyValueMap;
import com.snapyr.sdk.ProjectSettings;
import com.snapyr.sdk.Snapyr;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;

public class SampleApp extends Application {

    // https://segment.com/segment-engineering/sources/android-test/settings/keys
    //    private static final String ANALYTICS_WRITE_KEY = "HO63Z36e0Ufa8AAgbjDomDuKxFuUICqI";
    private static final String ANALYTICS_WRITE_KEY = "JLIfT7Q8gAkXDqkMbENkF1tKoaHRF552";

    @Override
    public void onCreate() {
        super.onCreate();

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
                        .defaultProjectSettings(
                                new ProjectSettings()
                                        .putValue("integrations",
                                                new LegacyValueMap()
                                                        .putValue("Snapyr", new LegacyValueMap()
                                                                .putValue("apiKey", ANALYTICS_WRITE_KEY)
                                                                .putValue("trackAttributionData", true)
                                                        )
                                        )
                        )
                        .flushQueueSize(1)
                        .enableSnapyrPushHandling()
                        .recordScreenViews();

        // Set the initialized instance as a globally accessible instance.
        Snapyr.setSingletonInstance(builder.build());

        // Now anytime you call Snapyr.with, the custom instance will be returned.
        Snapyr snapyr = Snapyr.with(this);
    }
}
