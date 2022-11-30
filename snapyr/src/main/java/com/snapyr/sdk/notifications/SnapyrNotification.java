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

import static com.snapyr.sdk.internal.Utils.isNullOrEmpty;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.PushTemplate;
import com.snapyr.sdk.internal.Utils;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import org.json.JSONObject;

public class SnapyrNotification implements Parcelable {
    private static final Random random =
            new Random(); // seeded with current System.nanotime() by default

    public final int notificationId;
    public final String titleText;
    public final String contentText;
    public final String subtitleText;

    public final String templateId;
    public final Date templateModified;
    private PushTemplate pushTemplate;

    public final Uri deepLinkUrl;
    public final String imageUrl;

    public final String actionId;
    public final String actionToken;

    public final String channelId;
    public final String channelName;
    public final String channelDescription;

    public SnapyrNotification(RemoteMessage remoteMessage)
            throws IllegalArgumentException, NonSnapyrMessageException {
        this.notificationId = random.nextInt(Integer.MAX_VALUE); // MAX_VALUE ensures non-negative
        Map<String, String> rawData = remoteMessage.getData();
        String snapyrDataJson = rawData.get("snapyr");
        if (snapyrDataJson == null) {
            throw new NonSnapyrMessageException(
                    "No 'snapyr' data found on notification payload (not a Snapyr notification)");
        }

        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(snapyrDataJson);
        } catch (Exception e) {
            Log.e(
                    "Snapyr",
                    "onMessageReceived: Invalid message - encountered JSON error trying to parse payload JSON; returning.",
                    e);
            throw new IllegalArgumentException(
                    "Invalid message - encountered JSON error trying to parse payload JSON");
        }

        this.titleText = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_TITLE_KEY, null);
        this.contentText =
                getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_CONTENT_KEY, null);
        if (titleText == null || contentText == null) {
            throw new IllegalArgumentException("Invalid message - missing required data");
        }
        this.subtitleText =
                getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_SUBTITLE_KEY, null);

        String templateId;
        Date templateModified;
        try {
            JSONObject template =
                    jsonData.getJSONObject(SnapyrNotificationHandler.NOTIF_TEMPLATE_KEY);
            templateId = template.getString("id");
            String modifiedStr = template.getString("modified");
            templateModified = Utils.parseISO8601Date(modifiedStr);
        } catch (Exception e) {
            Log.w(
                    "Snapyr",
                    "Could not parse push template data. Continuing without template/action buttons.");
            templateId = null;
            templateModified = null;
        }
        this.templateId = templateId;
        this.templateModified = templateModified;

        this.channelId =
                getOrDefault(
                        jsonData,
                        SnapyrNotificationHandler.NOTIF_CHANNEL_ID_KEY,
                        SnapyrNotificationHandler.CHANNEL_DEFAULT_ID);
        this.channelName =
                getOrDefault(
                        jsonData,
                        SnapyrNotificationHandler.NOTIF_CHANNEL_NAME_KEY,
                        SnapyrNotificationHandler.CHANNEL_DEFAULT_NAME);
        this.channelDescription =
                getOrDefault(
                        jsonData,
                        SnapyrNotificationHandler.NOTIF_CHANNEL_DESCRIPTION_KEY,
                        SnapyrNotificationHandler.CHANNEL_DEFAULT_DESCRIPTION);

        this.actionId =
                getOrDefault(jsonData, SnapyrNotificationHandler.ACTION_ID_KEY, null); // unused?
        this.actionToken = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_TOKEN_KEY, null);

        String deepLinkUrl =
                getOrDefault(jsonData, SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY, null);
        this.deepLinkUrl = (deepLinkUrl != null) ? Uri.parse(deepLinkUrl) : null;

        this.imageUrl = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_IMAGE_URL_KEY, null);
    }

    private String getOrDefault(JSONObject data, String key, String defaultVal) {
        String val = null;
        try {
            val = data.getString(key);
        } catch (Exception ignore) {
        }

        if (isNullOrEmpty(val)) {
            return defaultVal;
        }
        return val;
    }

    public void setPushTemplate(PushTemplate pushTemplate) {
        this.pushTemplate = pushTemplate;
    }

    public PushTemplate getPushTemplate() {
        return this.pushTemplate;
    }

    public ValueMap asValueMap() {
        return new ValueMap()
                .putValue("notificationId", notificationId)
                .putValue("titleText", titleText)
                .putValue("contentText", contentText)
                .putValue("subtitleText", subtitleText)
                .putValue("deepLinkUrl", (deepLinkUrl != null) ? deepLinkUrl.toString() : null)
                .putValue("imageUrl", imageUrl)
                .putValue("actionToken", actionToken);
    }

    // Constructor for use by Parcelable CREATOR below. NB order is significant - order and type of
    // read operations must exactly match order and type of write operations in `writeToParcel`
    // method
    protected SnapyrNotification(Parcel in) {
        notificationId = in.readInt();
        titleText = in.readString();
        contentText = in.readString();
        subtitleText = in.readString();
        templateId = in.readString();
        long templateModifiedRaw = in.readLong();
        templateModified = (templateModifiedRaw != -1) ? new Date(templateModifiedRaw) : null;
        deepLinkUrl = in.readParcelable(Uri.class.getClassLoader());
        imageUrl = in.readString();
        actionId = in.readString();
        actionToken = in.readString();
        channelId = in.readString();
        channelName = in.readString();
        channelDescription = in.readString();
    }

    public static final Creator<SnapyrNotification> CREATOR =
            new Creator<SnapyrNotification>() {
                @Override
                public SnapyrNotification createFromParcel(Parcel in) {
                    return new SnapyrNotification(in);
                }

                @Override
                public SnapyrNotification[] newArray(int size) {
                    return new SnapyrNotification[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    // NB order is significant - order and type of write operations here must exactly match order
    // and type of read operations in `SnapyrNotification(Parcel in)` method, which is used by
    // `createFromParcel`
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(notificationId);
        dest.writeString(titleText);
        dest.writeString(contentText);
        dest.writeString(subtitleText);
        dest.writeString(templateId);
        dest.writeLong((templateModified != null) ? templateModified.getTime() : -1);
        dest.writeParcelable(deepLinkUrl, flags);
        dest.writeString(imageUrl);
        dest.writeString(actionId);
        dest.writeString(actionToken);
        dest.writeString(channelId);
        dest.writeString(channelName);
        dest.writeString(channelDescription);
    }

    public static class NonSnapyrMessageException extends Exception {
        public NonSnapyrMessageException(String error) {
            super(error);
        }
    }
}
