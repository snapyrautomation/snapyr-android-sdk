package com.snapyr.sdk.inapp;

import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.snapyr.sdk.notifications.SnapyrNotificationHandler;
import com.snapyr.sdk.notifications.SnapyrNotificationListener;

class InAppWebviewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // TODO: fix this to allow non-deeplink links to work, right now we're only allowing deeplinks
        // We're hijacking the normal web url loading process here and checking if the
        // target link is of a host/scheme that is valid to deeplink with.

        // close any active dialogs we have before we transition or they'll be behind the new
        // activity, which is less than ideal
        InAppModal.CloseDialogs();

        // nav forward using a new intent/activity
        Intent trackIntent = new Intent(view.getContext(), SnapyrNotificationListener.class);
        trackIntent.setAction(SnapyrNotificationHandler.NOTIFICATION_ACTION);
        trackIntent.putExtra(SnapyrNotificationHandler.ACTION_ID_KEY, url);
        trackIntent.putExtra(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY, url);
        trackIntent.putExtra(SnapyrNotificationHandler.NOTIFICATION_ID, url);
        trackIntent.putExtra(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, url);
        //Intent intent;
        view.getContext().startActivity(trackIntent);
      return true;
    }
}
