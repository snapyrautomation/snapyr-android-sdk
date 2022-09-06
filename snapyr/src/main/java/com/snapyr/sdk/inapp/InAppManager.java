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
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.services.Logger;
import com.snapyr.sdk.services.ServiceFacade;
import java.util.LinkedList;
import java.util.Queue;

public class InAppManager implements InAppIFace {
    private final int processInterval;
    private final Logger logger;
    private final InAppCallback UserCallback;
    private Queue<InAppMessage> pendingActions;
    private Context context;

    public InAppManager(@NonNull InAppConfig config, @NonNull Context context) {
        this.logger = config.Logger;
        this.processInterval = config.PollingDelayMs;
        this.UserCallback = config.Handler;
        this.pendingActions = new LinkedList<>();
        this.context = context;
        this.startBackgroundThread(config.PollingDelayMs);
    }

    @Override
    public void processTrackResponse(SnapyrAction action) {
        try {
            InAppMessage message = new InAppMessage(action);
            this.pendingActions.add(message);
            try {
                AckActionRequest.execute(message.UserId, message.ActionToken);
            } catch (Exception e) {
                ServiceFacade.getLogger().error(e, "failed to ack in-app action");
            }
        } catch (InAppMessage.MalformedMessageException e) {
            logger.error(e, "failed to convert action to in-app message", action);
        }
    }

    @Override
    public void dispatchPending(Context context) {
        logger.info("polling for in-app content");
        while (this.pendingActions.peek() != null) {
            InAppMessage action = this.pendingActions.remove();
            if (action.ActionType == InAppActionType.ACTION_TYPE_CUSTOM) {
                logger.info("dispatching user in-app action");
                this.UserCallback.onAction(action);
            } else {
                // TODO: handle internally
            }
        }
    }

    Handler handler = new Handler();
    private Runnable backgroundThread =
            new Runnable() {
                @Override
                public void run() {
                    try {
                        dispatchPending(context);
                    } finally {
                        handler.postDelayed(backgroundThread, 500);
                    }
                }
            };

    private void startBackgroundThread(int pollingDelayMs) {
        this.handler.post(this.backgroundThread);
    }
}
