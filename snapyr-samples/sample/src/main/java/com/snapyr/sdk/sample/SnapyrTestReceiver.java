package com.snapyr.sdk.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.MessageFormat;

public class SnapyrTestReceiver extends BroadcastReceiver {

    public SnapyrTestReceiver() {
        Log.e("YYY", "SnapyrTestReceiver (BROADCAST): CONSTRUCTOR");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.e("YYY", "BROADCAST RECEIVER onReceive::: (awaiting debugger connection...");
//        android.os.Debug.waitForDebugger();
        Log.e("YYY", MessageFormat.format(
                "ONRECEIVE!!! Intent: {0}\nAction: {1}\nCategories: {2}\nData: {3}\nExtras: {4}\n",
                intent, intent.getAction(), intent.getCategories(), intent.getData(), intent.getExtras()));

    }
}