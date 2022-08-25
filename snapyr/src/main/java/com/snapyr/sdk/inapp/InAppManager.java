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

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.snapyr.sdk.SnapyrAction;
import com.snapyr.sdk.integrations.Logger;
import java.util.LinkedList;
import java.util.List;

public class InAppManager implements InAppIFace {
    private final int mInterval = 5000; // 5 seconds by default, can be changed later
    private Runnable backgroundThread = null;
    private Handler handler = null;
    private Logger logger = null;
    private List<InAppMessage> pendingActions;

    public InAppManager(@NonNull InAppConfig config) {
        this.logger = config.Logger;
        this.startBackgroundThread(config.PollingDelayMs);
        this.pendingActions = new LinkedList<>();
    }

    @Override
    public void ProcessTrackResponse(Context context, SnapyrAction action) {
        try {
            this.pendingActions.add(new InAppMessage(action));
        } catch (InAppMessage.MalformedMessageException e) {
            logger.error(e, "failed to convert action to in-app message", action);
        }
    }

    private void startBackgroundThread(int pollingDelayMs) {
        this.handler = new Handler();
        backgroundThread =
                () -> {
                    try {
                        process();
                    } finally {
                        handler.postDelayed(backgroundThread, pollingDelayMs);
                    }
                };
        backgroundThread.run();
    }

    private void process() {
        logger.info("polling for in-app content");
        for (InAppMessage action : this.pendingActions) {
            // nothing yet!
            this.pendingActions.remove(action);
        }
    }
}
