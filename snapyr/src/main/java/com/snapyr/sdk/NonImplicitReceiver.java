package com.snapyr.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.MessageFormat;

public class NonImplicitReceiver extends BroadcastReceiver {

    public NonImplicitReceiver() {
        Log.e("YYY", "SDK NonImplicitReceiver (BROADCAST): CONSTRUCTOR");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.e("YYY", "SDK BROADCAST RECEIVER NonImplicitReceiver onReceive::: (awaiting debugger connection...");
//        android.os.Debug.waitForDebugger();
        Log.e("YYY", MessageFormat.format(
                "SDK NonImplicitReceiver ONRECEIVE!!! Intent: {0}\nAction: {1}\nCategories: {2}\nData: {3}\nExtras: {4}\n",
                intent, intent.getAction(), intent.getCategories(), intent.getData(), intent.getExtras()));

    }
}