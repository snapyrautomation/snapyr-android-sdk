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
package com.snapyr.sdk.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.text.MessageFormat;

public class SnapyrNotifBroadcastReceiver extends BroadcastReceiver {

    public SnapyrNotifBroadcastReceiver() {
        Log.e("YYY", "SnapyrNotifBroadcastReceiver (BROADCAST): CONSTRUCTOR");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.e("YYY", "SNAPYRNOTIF onReceive::: (awaiting debugger connection...");
        //        android.os.Debug.waitForDebugger();
        Log.e(
                "YYY",
                MessageFormat.format(
                        "SNAPYRNOTIF ONRECEIVE!!! Intent: {0}\nAction: {1}\nCategories: {2}\nData: {3}\nExtras: {4}\n",
                        intent,
                        intent.getAction(),
                        intent.getCategories(),
                        intent.getData(),
                        intent.getExtras()));
    }
}
