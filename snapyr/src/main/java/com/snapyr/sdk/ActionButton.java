package com.snapyr.sdk;

import android.net.Uri;

import java.util.Map;

public class ActionButton {
    public String id;
    public String actionId;
    public String title;
    public Uri deeplinkURL;

    public ActionButton(Map<String, Object> src) {
        this.id = (String) src.get("id");
        this.actionId = (String) src.get("actionId");
        this.title = (String) src.get("title");
        String link = (String) src.get("deepLinkUrl");
        if (link != null) {
            this.deeplinkURL = Uri.parse(link);
        } // error?
    }

    public ActionButton(String id, String actionId, String title, String deeplink) {
        this.id = id;
        this.actionId = actionId;
        this.title = title;
        if (deeplink != null) {
            this.deeplinkURL = Uri.parse(deeplink);
        }
    }
}
