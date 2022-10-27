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

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.Pair;
import com.snapyr.sdk.services.Logger;
import java.util.concurrent.CountDownLatch;

/**
 * An {@link AsyncTask} that fetches the advertising info and attaches it to the given {@link
 * SnapyrContext} instance.
 */
class GetAdvertisingIdTask extends AsyncTask<Context, Void, Pair<String, Boolean>> {

    private final SnapyrContext snapyrContext;
    private final CountDownLatch latch;
    private final Logger logger;

    GetAdvertisingIdTask(SnapyrContext snapyrContext, CountDownLatch latch, Logger logger) {
        Log.e("Snapyr", "GetAdvertisingIdTask...");
        this.snapyrContext = snapyrContext;
        this.latch = latch;
        this.logger = logger;
    }

    private Pair<String, Boolean> getGooglePlayServicesAdvertisingID(Context context)
            throws Exception {
        Log.e("Snapyr", "getGooglePlayServicesAdvertisingID...");
        Object advertisingInfo =
                Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                        .getMethod("getAdvertisingIdInfo", Context.class)
                        .invoke(null, context);
        Boolean isLimitAdTrackingEnabled =
                (Boolean)
                        advertisingInfo
                                .getClass()
                                .getMethod("isLimitAdTrackingEnabled")
                                .invoke(advertisingInfo);

        if (isLimitAdTrackingEnabled) {
            logger.debug(
                    "Not collecting advertising ID because isLimitAdTrackingEnabled (Google Play Services) is true.");
            return Pair.create(null, false);
        }

        String advertisingId =
                (String) advertisingInfo.getClass().getMethod("getId").invoke(advertisingInfo);
        return Pair.create(advertisingId, true);
    }

    private Pair<String, Boolean> getAmazonFireAdvertisingID(Context context) throws Exception {
        Log.e("Snapyr", "getAmazonFireAdvertisingID...");
        ContentResolver contentResolver = context.getContentResolver();

        // Ref: http://prateeks.link/2uGs6bf
        // limit_ad_tracking != 0 indicates user wants to limit ad tracking.
        boolean limitAdTracking = Secure.getInt(contentResolver, "limit_ad_tracking") != 0;

        if (limitAdTracking) {
            logger.debug(
                    "Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true.");
            return Pair.create(null, false);
        }

        String advertisingId = Secure.getString(contentResolver, "advertising_id");
        return Pair.create(advertisingId, true);
    }

    @Override
    protected Pair<String, Boolean> doInBackground(Context... contexts) {
        Log.e("Snapyr", "doInBackground...");
        final Context context = contexts[0];
        try {
            return getGooglePlayServicesAdvertisingID(context);
        } catch (Exception e) {
            logger.error(e, "Unable to collect advertising ID from Google Play Services.");
        }
        try {
            return getAmazonFireAdvertisingID(context);
        } catch (Exception e) {
            logger.error(e, "Unable to collect advertising ID from Amazon Fire OS.");
        }
        logger.debug(
                "Unable to collect advertising ID from Amazon Fire OS and Google Play Services.");
        return null;
    }

    @Override
    protected void onPostExecute(Pair<String, Boolean> info) {
        Log.e("Snapyr", "onPostExecute...");
        super.onPostExecute(info);

        try {
            if (info == null) {
                return;
            }
            SnapyrContext.Device device = snapyrContext.device();
            if (device == null) {
                logger.debug("Not collecting advertising ID because context.device is null.");
                return;
            }
            device.putAdvertisingInfo(info.first, info.second);
        } finally {
            latch.countDown();
        }
    }
}
