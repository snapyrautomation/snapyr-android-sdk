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
import androidx.annotation.NonNull;
import com.snapyr.sdk.inapp.requests.AckUserActionRequest;
import com.snapyr.sdk.inapp.requests.GetUserActionsRequest;
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.services.ServiceFacade;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InAppManager implements InAppIFace {
    private final int pollingInterval;
    private final InAppActionProcessor actionProcessor;
    private final ScheduledExecutorService pollExecutor;
    private Context context;

    public InAppManager(@NonNull InAppConfig config, @NonNull Context context) {
        this.pollingInterval = config.PollingDelayMs;
        this.actionProcessor = new InAppActionProcessor(config.UserCallback);
        this.context = context;
        this.pollExecutor =
                Executors.newScheduledThreadPool(
                        1, new Utils.AnalyticsThreadFactory("Snapyr-InAppPoller"));
        this.startBackgroundThread();
    }

    private void processAndAck(InAppMessage message) {
        actionProcessor.process(message);
        ServiceFacade.getNetworkExecutor()
                .submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    AckUserActionRequest.execute(
                                            message.UserId, message.ActionToken);
                                } catch (Exception e) {
                                    ServiceFacade.getLogger()
                                            .error(e, "failed to ack in-app action");
                                }
                            }
                        });
    }

    @Override
    public void processTrackResponse(SnapyrAction action) {
        try {
            processAndAck(new InAppMessage(action));
        } catch (InAppMessage.MalformedMessageException e) {
            ServiceFacade.getLogger()
                    .error(e, "failed to convert action to in-app message", action);
        }
    }

    @Override
    public void dispatchPending(Context context) {
        ServiceFacade.getLogger().info("polling for in-app content");
        ServiceFacade.getNetworkExecutor()
                .submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<InAppMessage> polledActions =
                                        GetUserActionsRequest.execute(
                                                ServiceFacade.getSnapyrContext().traits().userId());

                                ServiceFacade.getLogger()
                                        .info("pulled " + polledActions.size() + " actions");
                                for (InAppMessage action : polledActions) {
                                    processAndAck(action);
                                }
                            }
                        });
    }

    private void startBackgroundThread() {
        this.pollExecutor.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            dispatchPending(context);
                        } finally {
                        }
                    }
                },
                0,
                pollingInterval,
                TimeUnit.MILLISECONDS);
    }
}
