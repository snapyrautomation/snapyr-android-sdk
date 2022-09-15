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

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import com.snapyr.sdk.Properties;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.core.R;
import com.snapyr.sdk.services.ServiceFacade;

public class WebviewModalView extends FrameLayout {
    static PopupWindow popupWindow = null;
    private final View contents;
    private boolean wasInteractedWith = false;
    private final String actionToken;
    private final Snapyr snapyr;

    public static void showPopup(Activity activity, String rawHtml, String actionToken) {
        if (WebviewModalView.popupWindow != null) {
            return;
        }
        activity.runOnUiThread(() -> new WebviewModalView(activity, rawHtml, actionToken));
    }

    public static void closePopups() {
        if (WebviewModalView.popupWindow != null) {
            WebviewModalView.popupWindow.dismiss();
            return;
        }
    }

    public WebviewModalView(Context context, String html, String token) {
        super(context);
        this.actionToken = token;
        this.snapyr = Snapyr.with(context);
        contents = inflate(getContext(), R.layout.webview_modal, this);

        WebView view = configureWebview(context, token);

        ImageButton dismissButton = this.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(
                view1 -> {
                    popupWindow.dismiss();
                });

        // I don't know why this needs to be base64 but w/e
        String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
        view.loadData(encodedHtml, "text/html", "base64");

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        popupWindow = new PopupWindow(this, width, height, true);
        popupWindow.setOnDismissListener(() -> WebviewModalView.popupWindow = null);
        popupWindow.showAtLocation(contents, Gravity.CENTER, 0, 0);
        view.setVisibility(INVISIBLE);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private InAppWebviewClient createClient() {
        return new InAppWebviewClient() {
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                view.setVisibility(VISIBLE);
            }
        };
    }

    private void dismissPopup(){
        if (!wasInteractedWith){
            // user didn't click a button, so send the dismissed
            Properties props =
                    new Properties()
                            .putValue("actionToken", actionToken)
                            .putValue("platform", "android")
                            .putValue("interactionType", "dismiss");
            snapyr.track("snapyr.observation.event.Behavior", props);
        }

        ServiceFacade.getCurrentActivity().runOnUiThread(() -> {
            if (WebviewModalView.popupWindow != null) {
                WebviewModalView.popupWindow.dismiss();
            }
        });
    }

    private WebView configureWebview(Context context, String actionToken) {
        WebView wv = this.findViewById(R.id.webview);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ServiceFacade.getCurrentActivity()
                .getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);

        wv.setWebViewClient(createClient());
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(
                new WebviewJavascriptAPI(
                        context,
                        new WebviewJavascriptAPI.SnapyrWebviewInterfaceCallback() {
                            @Override
                            public void onClose() {
                                dismissPopup();
                            }

                            @Override
                            public void onClick(String id, ValueMap parameters) {
                                if (id.equals("resize")){
                                    return; // TODO: remove this if we change resizes cb
                                }
                                wasInteractedWith = true;
                                Properties props =
                                        new Properties(parameters)
                                                .putValue("actionToken", actionToken)
                                                .putValue("platform", "android")
                                                .putValue("interactionType", "click");
                                snapyr.track("snapyr.observation.event.Behavior", props);
                                // TODO: should we dismiss on click?
                                dismissPopup();
                            }

                            @Override
                            public void onLoad(float clientHeight) {
                                Properties props =
                                        new Properties()
                                                .putValue("actionToken", actionToken)
                                                .putValue("platform", "android");
                                snapyr.track("snapyr.observation.event.Impression", props);
                            }
                        }),
                "snapyrMessageHandler");

        return wv;
    }
}
