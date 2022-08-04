package com.snapyr.sdk.inapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.webkit.WebView;

public class InAppModal {
    static boolean showingModal = false;

    public static void ShowPopup(Activity activity, String rawHtml) {
        if (InAppModal.showingModal == true){
            return;
        }
       InAppModal.showingModal = true;
       // DisplayMetrics displayMetrics = new DisplayMetrics();
        //activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int height = displayMetrics.heightPixels;
        //int width = displayMetrics.widthPixels;

        WebView v = new WebView(activity);
        //v.canGoBackOrForward(0);
       // v.layout(0, 0, height, width);


        String encodedHtml = Base64.encodeToString(rawHtml.getBytes(), Base64.NO_PADDING);
        v.loadData(encodedHtml, "text/html", "base64");
        //v.setOn

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
