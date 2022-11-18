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

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.services.ServiceFacade;
import java.io.IOException;

public class WebviewJavascriptAPI {
    Context mContext;
    private final SnapyrWebviewInterfaceCallback callbackHandler;

    public enum MessageType {
        loaded,
        log,
        click,
        close,
        resize,
    }

    public enum LogLevel {
        log,
        error,
    }

    // probably don't need/want this to be public - only did it for testing directly from sample app
    public WebviewJavascriptAPI(Context c, SnapyrWebviewInterfaceCallback callbackHandler) {
        mContext = c;
        this.callbackHandler = callbackHandler;
    }

    @JavascriptInterface
    public void handleMessage(String jsonData) {
        ServiceFacade.getLogger()
                .verbose("WebviewJavascriptAPI: message received from JS", jsonData);

        ValueMap data = null;
        try {
            data = new ValueMap(ServiceFacade.getCartographer().fromJson(jsonData));
        } catch (IOException e) {
            e.printStackTrace();
            ServiceFacade.getLogger()
                    .error(
                            e,
                            "Received message from in-app webview, but unable to parse JSON: %s",
                            jsonData);
            return;
        }

        MessageType messageType;

        try {
            messageType = MessageType.valueOf(data.getString("type"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            ServiceFacade.getLogger()
                    .error(
                            e,
                            "Received message from in-app webview, but no valid message type found: %s",
                            jsonData);
            return;
        }

        switch (messageType) {
            case loaded:
                // May want to wait until now to actually display the dialog/webview, to prevent
                // flickering
                // Fires on window load event in the browser, which indicates that all resources
                // (including CSS/images)
                // have finished being fetched
                float reportedHeight = Float.parseFloat(data.getString("height"));
                callbackHandler.onLoad(reportedHeight);
                Log.d("Snapy", "In-App message content loaded!");
                break;
            case log:
                // Browser `console.log` and `console.error` forward here... ignore except for
                // debugging
                LogLevel level = LogLevel.valueOf(data.getString("level"));
                switch (level) {
                    case log:
                        Log.i("Snapyr", "WebView Log: " + data.getString("message"));
                        break;
                    case error:
                        Log.e("Snapyr", "WebView Error: " + data.getString("message"));
                }
                break;
            case click:
                // Do tracking and maybe other stuff? We could possibly trigger a user-configured
                // callback
                String clickId = data.getString("id");
                String url = data.getString("url");
                ValueMap parameters = null;
                try {
                    // All `data-x` attributes on the element that was clicked (may be empty object)
                    parameters = data.getValueMap("parameters");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e(
                        "Snapyr",
                        String.format(
                                "In-App click received! \nid: %s \nparameters: %s",
                                clickId, parameters));
                callbackHandler.onClick(clickId, url, parameters);
                break;
            case close:
                // Do tracking and maybe other stuff? We could possibly trigger a user-configured
                // callback
                // But also, close the webview
                Log.e("Snapyr", "In-App close received!");
                callbackHandler.onClose();
                break;
            case resize:
                // not handling yet
                break;
        }
    }

    public interface SnapyrWebviewInterfaceCallback {
        void onClose();

        void onClick(String id, String url, ValueMap parameters);

        void onLoad(float clientHeight);
    }
}
