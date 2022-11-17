package com.snapyr.sdk.notifications;

import static com.snapyr.sdk.internal.Utils.isNullOrEmpty;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.internal.PushTemplate;

import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class SnapyrNotification implements Parcelable {
    private static final Random random = new Random(); // seeded with current System.nanotime() by default

    public final int notificationId = random.nextInt(Integer.MAX_VALUE); // MAX_VALUE ensures non-negative
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

    public SnapyrNotification(RemoteMessage remoteMessage) throws RuntimeException {
        Map<String, String> rawData = remoteMessage.getData();
        String snapyrDataJson = rawData.get("snapyr");
        if (snapyrDataJson == null) {
            Log.i(
                    "Snapyr",
                    "onMessageReceived: No 'snapyr' data found on notification payload (not a Snapyr notification); skipping.");
            throw new RuntimeException("No 'snapyr' data found on notification payload (not a Snapyr notification)");
        }

        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(snapyrDataJson);
        } catch (Exception e) {
            Log.e(
                    "Snapyr",
                    "onMessageReceived: Invalid message - encountered JSON error trying to parse payload JSON; returning.",
                    e);
            throw new RuntimeException("Invalid message - encountered JSON error trying to parse payload JSON");
        }

        this.titleText = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_TITLE_KEY, null);
        this.contentText = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_CONTENT_KEY, null);
        if (titleText == null || contentText == null) {
            throw new RuntimeException("Invalid message - missing required data");
        }
        this.subtitleText = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_SUBTITLE_KEY, null);

        String templateId;
        Date templateModified;
        try {
            JSONObject template = jsonData.getJSONObject(SnapyrNotificationHandler.NOTIF_TEMPLATE_KEY);
            templateId = template.getString("id");
            String modifiedStr = jsonData.getString("modified");
            templateModified = Utils.parseISO8601Date(modifiedStr);
        } catch (Exception e) {
            Log.e("Snapyr", "Could not parse push template data. Continuing without template/action buttons.");
            templateId = null;
            templateModified = null;
        }
        this.templateId = templateId;
        this.templateModified = templateModified;

        this.channelId = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_CHANNEL_ID_KEY, SnapyrNotificationHandler.CHANNEL_DEFAULT_ID);
        this.channelName = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_CHANNEL_NAME_KEY, SnapyrNotificationHandler.CHANNEL_DEFAULT_NAME);
        this.channelDescription = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_CHANNEL_DESCRIPTION_KEY, SnapyrNotificationHandler.CHANNEL_DEFAULT_DESCRIPTION);

        this.actionId = getOrDefault(jsonData, SnapyrNotificationHandler.ACTION_ID_KEY, null); // unused?
        this.actionToken = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_TOKEN_KEY, null);

        String deepLinkUrl = getOrDefault(jsonData, SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY, null);
        this.deepLinkUrl = (deepLinkUrl != null) ? Uri.parse(deepLinkUrl) : null;

//        String imageUrl = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_IMAGE_URL_KEY, null);
//        this.imageUrl = (imageUrl != null) ? Uri.parse(imageUrl) : null;
        this.imageUrl = getOrDefault(jsonData, SnapyrNotificationHandler.NOTIF_IMAGE_URL_KEY, null);
    }

    protected SnapyrNotification(Parcel in) {
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

    public static final Creator<SnapyrNotification> CREATOR = new Creator<SnapyrNotification>() {
        @Override
        public SnapyrNotification createFromParcel(Parcel in) {
            return new SnapyrNotification(in);
        }

        @Override
        public SnapyrNotification[] newArray(int size) {
            return new SnapyrNotification[size];
        }
    };

    private String getOrDefault(JSONObject data, String key, String defaultVal) {
        String val = null;
        try {
            val = data.getString(key);
        } catch (Exception ignore) {}

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(titleText);
        dest.writeString(contentText);
        dest.writeString(subtitleText);
        dest.writeString(templateId);
//        long modifiedDateLong = (templateModified != null) ? templateModified.getTime() : -1;
        dest.writeLong((templateModified != null) ? templateModified.getTime() : -1);
        dest.writeParcelable(deepLinkUrl, flags);
        dest.writeString(imageUrl);
        dest.writeString(actionId);
        dest.writeString(actionToken);
        dest.writeString(channelId);
        dest.writeString(channelName);
        dest.writeString(channelDescription);
    }

    public ValueMap asValueMap() {
        return new ValueMap()
            .putValue("notificationId", notificationId)
            .putValue("titleText", titleText)
            .putValue("contentText", contentText)
            .putValue("subtitleText", subtitleText)
//            .putValue("templateId", templateId)
//            .putValue("(", (templateModified != null) ? templateModified.getTime() : -1)
            .putValue("deepLinkUrl", deepLinkUrl.toString())
            .putValue("imageUrl", imageUrl)
//            .putValue("actionId", actionId)
//            .putValue("actionToken", actionToken)
//            .putValue("channelId", channelId)
//            .putValue("channelName", channelName)
//            .putValue("channelDescription", channelDescription)
        ;
    }
}
