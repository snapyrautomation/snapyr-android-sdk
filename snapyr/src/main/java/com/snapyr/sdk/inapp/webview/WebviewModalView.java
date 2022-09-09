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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.fragment.app.DialogFragment;

import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.core.R;
import com.snapyr.sdk.services.ServiceFacade;

public class WebviewModalView extends FrameLayout {
    static PopupWindow activeDialog = null;
    private final View contents;

    public static void showPopup(Activity activity, String rawHtml) {
        if (WebviewModal.showingModal == true) {
            return;
        }

        activity.runOnUiThread(() ->
                new WebviewModalView(activity, rawHtml));
    }

    public WebviewModalView(Context context, String html) {
        super(context);

        contents = inflate(getContext(), R.layout.webview_modal, this);

        WebView view = configureWebview(context);

        ImageButton dismissButton = this.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(view1 -> {
            activeDialog.dismiss();
        });

        // I don't know why this needs to be base64 but w/e
        String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
        view.loadData(encodedHtml, "text/html", "base64");

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;

        activeDialog = new PopupWindow(this, width, height, true);
        activeDialog.setOnDismissListener(
                () -> WebviewModal.showingModal = false);
        activeDialog.setBackgroundDrawable(new ColorDrawable(
                android.graphics.Color.TRANSPARENT));
    }

    private InAppWebviewClient createClient(){
        return new InAppWebviewClient() {
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                // Page finished loading, `onLoad` set height; now we can
                // actually display
                // the whole thing without a ton of flicker
                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window tolken
                activeDialog.showAtLocation(contents, Gravity.CENTER, 0, 0);
                // activeDialog.getWindow().setAttributes(...) MUST be called
                // after
                // activeDialog.show() in order to set values successfully.
                // Set dialog width to MATCH_PARENT, in this case meaning
                // maximum possible
                // width for the display
                //WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
                //attributes.copyFrom(activeDialog.getWindow().getAttributes());
                //attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
                //activeDialog.getWindow().setAttributes(attributes);
            }
        };
    }
    private WebView configureWebview(Context context){
        WebView wv = this.findViewById(R.id.webview);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        int defaultMargin = 44; // we should get this from somewhere real but it's the PERFECT
        ServiceFacade.getCurrentActivity()
                .getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels - (defaultMargin * 2);

        wv.setWebViewClient(createClient());
        wv.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        wv.addJavascriptInterface(
                new WebviewJavascriptAPI(
                        context,
                        new WebviewJavascriptAPI.SnapyrWebviewInterfaceCallback() {
                            @Override
                            public void onClose() {
                                // todo: post to engine
                                if (activeDialog != null) {
                                    activeDialog.dismiss();
                                }
                            }

                            @Override
                            public void onClick(String id, ValueMap parameters) {
                                // todo: post to engine
                            }

                            @Override
                            public void onLoad(float clientHeight) {
                                // Now that we've received "real" height reported by the
                                // web page,
                                // set the height on the web view
                                // so it's the right size to fit within the modal
                                wv.layout(0, 0, width, (int) clientHeight);
                                wv.requestLayout();
                            }
                        }),
                "snapyrMessageHandler");

        return wv;
    }
}
