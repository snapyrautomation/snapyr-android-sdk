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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.http.ConnectionFactory;
import com.snapyr.sdk.internal.Utils;

class SnapyrNotificationUtils {
    static Snapyr getSnapyrInstance(Context context) {
        if (Snapyr.Valid()) {
            // there is an existing shared instance from the main application - use that
            return Snapyr.with(context);
        }
        SharedPreferences servicePreferences =
                Utils.getSnapyrSharedPreferences(context, Snapyr.SERVICE_PREFS_TAG);
        String writeKey = servicePreferences.getString(Snapyr.WRITE_KEY_RESOURCE_IDENTIFIER, null);
        ConnectionFactory.Environment environment;
        try {
            environment =
                    ConnectionFactory.Environment.valueOf(
                            servicePreferences.getString(Snapyr.SERVICE_PREFS_KEY_ENV, null));
        } catch (Exception e) {
            Log.d("Snapyr", "Notification service: couldn't read environment; defaulting to prod.");
            environment = ConnectionFactory.Environment.PROD;
        }
        if (writeKey == null) {
            return null;
        }
        return new Snapyr.Builder(context, writeKey)
                .snapyrEnvironment(environment)
                .enableHelperInstance(context)
                .build();
    }
}
