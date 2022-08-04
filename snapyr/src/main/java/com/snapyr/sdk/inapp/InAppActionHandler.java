package com.snapyr.sdk.inapp;

import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.SnapyrAction;
import com.snapyr.sdk.SnapyrActionHandler;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.integrations.AliasPayload;
import com.snapyr.sdk.integrations.GroupPayload;
import com.snapyr.sdk.integrations.IdentifyPayload;
import com.snapyr.sdk.integrations.ScreenPayload;
import com.snapyr.sdk.integrations.TrackPayload;

public class InAppActionHandler implements SnapyrActionHandler {
    @Override
    public void handleAction(SnapyrAction action) {
        if (action.containsKey("content") && action.getValueMap("content") != null){
            ValueMap content = action.getValueMap("content");
            if (content.containsKey("bodyHtml") && content.getString("bodyHtml") != null){
                String rawHtml = content.getString("bodyHtml");
                InAppModal.ShowPopup(Snapyr.with(action.context).GetActivity(), rawHtml);
            }
        }
    }

    @Override
    public void onIdentify(IdentifyPayload payload) {

    }

    @Override
    public void onTrack(TrackPayload payload) {

    }

    @Override
    public void onAlias(AliasPayload payload) {

    }

    @Override
    public void onGroup(GroupPayload payload) {

    }

    @Override
    public void onScreen(ScreenPayload payload) {

    }
}
