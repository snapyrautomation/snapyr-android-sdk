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
package com.snapyr.sdk.inapp.webview;

import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.snapyr.sdk.notifications.SnapyrNotificationHandler;
import com.snapyr.sdk.notifications.SnapyrNotificationListener;

class InAppWebviewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // TODO: fix this to allow non-deeplink links to work, right now we're only allowing
        // deep links
        // We're hijacking the normal web url loading process here and checking if the
        // target link is of a host/scheme that is valid to deeplink with.

        // close any active dialogs we have before we transition or they'll be behind the new
        // activity, which is less than ideal
        WebviewModal.CloseDialogs();

        // nav forward using a new intent/activity
        Intent trackIntent = new Intent(view.getContext(), SnapyrNotificationListener.class);
        trackIntent.setAction(SnapyrNotificationHandler.NOTIFICATION_ACTION);
        trackIntent.putExtra(SnapyrNotificationHandler.ACTION_ID_KEY, url);
        trackIntent.putExtra(SnapyrNotificationHandler.ACTION_DEEP_LINK_KEY, url);
        trackIntent.putExtra(SnapyrNotificationHandler.NOTIFICATION_ID, url);
        trackIntent.putExtra(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, url);
        // Intent intent;
        view.getContext().startActivity(trackIntent);
        return true;
    }
}
