package com.snapyr.sdk.inapp;

import com.snapyr.sdk.inapp.webview.WebviewModal;
import com.snapyr.sdk.services.ServiceFacade;

public class InAppActionProcessor {
    private final InAppCallback userCallback;
    InAppActionProcessor(InAppCallback userCallback){
        this.userCallback = userCallback;
    }

    public void process(InAppMessage message){
        switch (message.ActionType) {
            case ACTION_TYPE_CUSTOM:
                ServiceFacade.getLogger().info("dispatching user in-app action");
                userCallback.onAction(message);
                break;
            case ACTION_TYPE_OVERLAY:
                try {
                    handleOverlay(message);
                } catch (InAppContent.IncorrectContentAccessException e) {
                    ServiceFacade.getLogger().error(e,"action type does not match content");
                }
                break;
        }
    }

    private void handleOverlay(InAppMessage message) throws InAppContent.IncorrectContentAccessException {
        switch (message.Content.getType()){
            case CONTENT_TYPE_JSON:
                ServiceFacade.getLogger().info("no action for json overlays currently");
                break;
            case CONTENT_TYPE_HTML:
                WebviewModal.ShowPopup(ServiceFacade.getCurrentActivity(), message.Content.getHtmlContent());
                break;
        }
    }
}
