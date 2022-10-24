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

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.PushTemplate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

// import com.google.gson.Gson;

public class SnapyrFirebaseMessagingService extends FirebaseMessagingService {
    private SnapyrNotificationListener activityHandler;

    public SnapyrFirebaseMessagingService() {
        super();
        Log.e("XXX", "SnapyrFirebaseMessagingService: constructor 2 - ATTACH DEBUGGER!!!");
        //        android.os.Debug.waitForDebugger();
        Log.e("XXX", "SnapyrFirebaseMessagingService: constructor - resuming.");
        Application application = this.getApplication();
    }

    @Override
    protected void finalize() throws Throwable {
        Log.e("XXX", "SnapyrFirebaseMessagingService: FINALIZE");
        super.finalize();
    }

    @Override
    public void handleIntent(Intent intent) {
        Log.d("XXX", "SnapyrFirebaseMessagingService: handleIntent");
        super.handleIntent(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("XXX", "SnapyrFirebaseMessagingService: onCreate");
    }

    @Override
    public void onDestroy() {
        Log.e("XXX", "SnapyrFirebaseMessagingService: onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: onRebind");
        super.onRebind(intent);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: onStart");
        super.onStart(intent, startId);
    }

    @Override
    public ComponentName startService(Intent service) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: startService");
        return super.startService(service);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: unbindService");
        super.unbindService(conn);
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        Log.e("XXX", "SnapyrFirebaseMessagingService: startForegroundService");
        return super.startForegroundService(service);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e("XXX", "SnapyrFirebaseMessagingService: onTaskRemoved...");
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.e("XXX", "SnapyrFirebaseMessagingService: onNewToken: " + token);
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
        Log.e(
                "XXX",
                "SnapyrFirebaseMessagingService: onMessageReceived. Next: remoteMessage; snapyrDataJson");
        Log.d("XXX", remoteMessage.toString());

        Map<String, String> rawData = remoteMessage.getData();
        //        Gson gson = new Gson();
        //        String json = gson.toJson(rawData);

        String snapyrDataJson = rawData.get("snapyr");
        Log.d("XXX", snapyrDataJson);
        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(snapyrDataJson);
        } catch (JSONException e) {
            Log.e(
                    "XXX",
                    "onMessageReceived: No 'snapyr' data found on notification payload (not a Snapyr notification?); returning.");
            return;
        }

        ValueMap data = new ValueMap();

        for (Iterator<String> it = jsonData.keys(); it.hasNext(); ) {
            String key = it.next();
            String value = null;
            try {
                value = jsonData.getString(key);
            } catch (JSONException ignored) {
            }
            data.put(key, value);
        }

        Snapyr snapyrInstance = SnapyrNotificationUtils.getSnapyrInstance(this);
        if (snapyrInstance == null) {
            Log.e(
                    "Snapyr",
                    "Notification service couldn't initialize Snapyr. Make sure you've initialized Snapyr from within your main application prior to receiving notifications.");
            return;
        }

        PushTemplate template = processPushTemplate(data, snapyrInstance);
        if (template != null) {
            // rich push, inject the template data into the context data we're passing down
            data.put(SnapyrNotificationHandler.ACTION_BUTTONS_KEY, template);
        }

        com.snapyr.sdk.Properties properties = new com.snapyr.sdk.Properties();
        properties.putAll(data);
        snapyrInstance.pushNotificationReceived(properties);
        snapyrInstance.getNotificationHandler().showRemoteNotification(data);
        snapyrInstance.flush();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private PushTemplate processPushTemplate(@NonNull ValueMap templateObject, Snapyr sdkInstance) {
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

        PushTemplate template = sdkInstance.getPushTemplates().get(templateId);
        if ((template != null) && (!template.getModified().before(modified))) {
            // if the modified date in the push payload is equal to or older than the cached
            // templates we're good to go and can just used the cached template value
            return template;
        }

        // either missing template or it's older than the timestamp in the push notification
        // re-fetch the sdk config and retry
        sdkInstance.RefreshConfiguration(true);
        return sdkInstance.getPushTemplates().get(templateId);
    }
}
