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
package com.snapyr.sdk.inapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.snapyr.sdk.ValueMap;

public class InAppModal {
    static boolean showingModal = false; // super lazy modal logic
    static AlertDialog activeDialog = null;

    public static void CloseDialogs() {
        if (activeDialog != null) {
            activeDialog.cancel();
        }
    }

    public static void ShowPopup(Activity activity, String rawHtml) {
        if (InAppModal.showingModal == true) {
            return;
        }
        InAppModal.showingModal = true;
        WebView v = new WebView(activity);
        v.setWebViewClient(new InAppWebviewClient());

        WebSettings webViewSettings = v.getSettings();
        webViewSettings.setJavaScriptEnabled(true);

        v.addJavascriptInterface(
                new SnapyrWebviewInterface(
                        activity,
                        new SnapyrWebviewInterfaceCallback() {
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
                            public void onLoad() {
                                Log.d("Snapyr", "InAppModal got onLoad");
                            }
                        }),
                "snapyrMessageHandler");

        // doing this seems to make the the webpage super jittery, so ignore unless we need later
        // DisplayMetrics displayMetrics = new DisplayMetrics();
        // activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // int height = displayMetrics.heightPixels;
        // int width = displayMetrics.widthPixels;
        // v.layout(0, 0, height, width);

        // v.canGoBackOrForward(0);

        // I don't know why this needs to be base64 but w/e
        String encodedHtml = Base64.encodeToString(rawHtml.getBytes(), Base64.NO_PADDING);
        v.loadData(encodedHtml, "text/html", "base64");

        activeDialog = new AlertDialog.Builder(activity).setView(v).create();
        activeDialog.setMessage("Brandon is cool");
        activeDialog.show();
        activeDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    public void onDismiss(final DialogInterface dialog) {
                        InAppModal.showingModal = false;
                    }
                });
    }
}
