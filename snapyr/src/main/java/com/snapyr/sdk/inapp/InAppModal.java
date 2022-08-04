package com.snapyr.sdk.inapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.webkit.WebView;

public class InAppModal {
    static boolean showingModal = false; // super lazy modal logic

    public static void ShowPopup(Activity activity, String rawHtml) {
        if (InAppModal.showingModal == true){
            return;
        }
        InAppModal.showingModal = true;
        WebView v = new WebView(activity);

        // doing this seems to make the the webpage super jittery, so ignore unless we need later
        // DisplayMetrics displayMetrics = new DisplayMetrics();
        //activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int height = displayMetrics.heightPixels;
        //int width = displayMetrics.widthPixels;
        // v.layout(0, 0, height, width);

        //v.canGoBackOrForward(0);

        // I don't know why this needs to be base64 but w/e
        String encodedHtml = Base64.encodeToString(rawHtml.getBytes(), Base64.NO_PADDING);
        v.loadData(encodedHtml, "text/html", "base64");

        AlertDialog dialog = new AlertDialog.Builder(activity).setView(v).create();
        dialog.setMessage("Brandon is cool");
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(final DialogInterface dialog) {
                InAppModal.showingModal = false;
            }
        });
    }
}
