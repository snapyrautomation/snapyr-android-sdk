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
import android.webkit.WebView;

public class WebviewModal {
    static boolean showingModal = false; // super lazy modal logic
    static AlertDialog activeDialog = null;

    public static void CloseDialogs() {
        if (activeDialog != null) {
            activeDialog.cancel();
        }
    }

    public static void ShowPopup(Activity activity, String rawHtml) {
        if (WebviewModal.showingModal == true) {
            return;
        }

        activity.runOnUiThread(() -> buildAndShowModal(activity, rawHtml));
    }

    static void buildAndShowModal(Activity activity, String rawHtml) {

        WebviewModal.showingModal = true;
        WebView v = new WebView(activity);
        /*
        View.inflate(activity,R.layout.webview_modal, activity);
        v.setWebViewClient(
                new InAppWebviewClient() {
                    @Override
                    public void onPageCommitVisible(WebView view, String url) {
                        super.onPageCommitVisible(view, url);
                        // Page finished loading, `onLoad` set height; now we can
                        // actually display
                        // the whole thing without a ton of flicker
                        activeDialog.show();
                        // activeDialog.getWindow().setAttributes(...) MUST be called
                        // after
                        // activeDialog.show() in order to set values successfully.
                        // Set dialog width to MATCH_PARENT, in this case meaning
                        // maximum possible
                        // width for the display
                        WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
                        attributes.copyFrom(activeDialog.getWindow().getAttributes());
                        attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
                        activeDialog.getWindow().setAttributes(attributes);
                    }
                });

        WebSettings webViewSettings = v.getSettings();
        webViewSettings.setJavaScriptEnabled(true);

        // doing this seems to make the the webpage super jittery, so ignore unless we
        // need later
        int defaultMargin = 44; // we should get this from somewhere real but it's the PERFECT
        // amount for my
        // Pixel 3a emulator
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels - (defaultMargin * 2);
        int width = displayMetrics.widthPixels - (defaultMargin * 2);
        // CRUCIAL CALL for flicker avoidance: `layout()` actually sets dimensions on
        // the webview
        // (even though it's not attached to the view tree yet)
        // Having an appropriately-sized viewport lets the HTML render correctly before
        // webview is
        // displayed, so that the HTML doc can determine
        // its own real height and report it back in the `onLoad` javascript interface
        // callback
        // above
        v.layout(0, 0, width, height);

        v.addJavascriptInterface(
                new WebviewJavascriptAPI(
                        activity,
                        new WebviewJavascriptAPI.SnapyrWebviewInterfaceCallback() {
                            @Override
                            public void onClose() {
                                if (activeDialog != null) {
                                    activeDialog.cancel();
                                }
                            }

                            @Override
                            public void onClick(String id, ValueMap parameters) {
                                Log.d("Snapyr", "InAppModal got a click yay");
                            }

                            @Override
                            public void onLoad(float clientHeight) {
                                Log.e(
                                        "Snapyr",
                                        "PROGRESS::: onLoad; reported height: "
                                                + String.valueOf(clientHeight));
                                // Now that we've received "real" height reported by the
                                // web page,
                                // set the height on the web view
                                // so it's the right size to fit within the modal
                                v.layout(0, 0, width, (int) clientHeight);
                                v.requestLayout();
                            }
                        }),
                "snapyrMessageHandler");

        // Max out width, but shrink height to fit content
        // NB the current behavior on iOS is to size the overlay window to be "full
        // screen" (minus
        // some margins), rather than
        // dynamic height... may want to do the same here, and save all this auto-height
        // stuff for
        // future options like "center"
        // or "top banner"
        v.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // v.canGoBackOrForward(0);

         */

        // I don't know why this needs to be base64 but w/e
        new WebviewModalView(activity, rawHtml);
    }
}
