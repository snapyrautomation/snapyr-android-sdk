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

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.LegacyValueMap;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.PushTemplate;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class SnapyrFirebaseMessagingService extends FirebaseMessagingService {
    private SnapyrNotificationListener activityHandler;

    public SnapyrFirebaseMessagingService() {
        super();
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        try {
            Snapyr.with(this).setPushNotificationToken(token);
        } catch (Exception e) {
            // do nothing if Snapyr is not yet initialized
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> rawData = remoteMessage.getData();

        String snapyrDataJson = rawData.get("snapyr");
        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(snapyrDataJson);
        } catch (JSONException e) {
            Log.e(
                    "Snapyr",
                    "onMessageReceived: No 'snapyr' data found on notification payload (not a Snapyr notification?); returning.");
            return;
        }

        ValueMap data = new LegacyValueMap();

        for (Iterator<String> it = jsonData.keys(); it.hasNext(); ) {
            String key = it.next();
            String value = null;
            try {
                value = jsonData.getString(key);
            } catch (JSONException ignored) {
            }
            data.put(key, value);
        }

        Snapyr snapyr = Snapyr.with(this);

        PushTemplate template = processPushTemplate(data);
        if (template != null) {
            // rich push, inject the template data into the context data we're passing down
            data.put(SnapyrNotificationHandler.ACTION_BUTTONS_KEY, template);
        }

        com.snapyr.sdk.Properties properties = new com.snapyr.sdk.Properties();
        properties.putAll(data);
        Snapyr.with(this).pushNotificationReceived(properties);
        Snapyr.with(this).getNotificationHandler().showRemoteNotification(data);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private PushTemplate processPushTemplate(@NonNull ValueMap templateObject) {
        String templateRaw = templateObject.get("pushTemplate").toString();
        String templateId = null;
        Date modified = null;
        try {
            JSONObject jsonData = new JSONObject(templateRaw);
            templateId = jsonData.getString("id");
            String modifiedStr = jsonData.getString("modified");
            TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(modifiedStr);
            modified = Date.from(Instant.from(ta));
            // modified =  LocalDateTime.parse(modifiedDate);
        } catch (JSONException e) {
            Log.e(
                    "Snapyr",
                    "processPushTemplate: invalid template data found on object; returning.");
            return null;
        }
        if (templateId == null) {
            return null;
        }

        Snapyr sdkInstance = Snapyr.with(this);
        PushTemplate template = sdkInstance.getPushTemplates().get(templateId);
        if ((template != null) && (template.getModified().after(modified))) {
            // if the modified date in the push payload is older than the cached templates we're
            // good to go and can just used the cached template value
            return template;
        }

        // either missing template or it's older than the timestamp in the push notification
        // re-fetch the sdk config and retry
        sdkInstance.RefreshConfiguration(true);
        return sdkInstance.getPushTemplates().get(templateId);
    }
}
