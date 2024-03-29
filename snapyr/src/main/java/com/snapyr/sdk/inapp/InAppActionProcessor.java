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

import com.snapyr.sdk.inapp.webview.WebviewModalView;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.services.ServiceFacade;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InAppActionProcessor {
    private final InAppCallback userCallback;
    private final ExecutorService userCallbackExecutor;

    InAppActionProcessor(InAppCallback userCallback) {
        this.userCallback = userCallback;
        // Used to fire-and-forget user callback. Pool size of 1 means callbacks will fire
        // sequentially (additional executions will be queued and processed in order by the executor
        // automatically)
        // This allows us to loop through our own list without the potential for user code to affect
        // timing or interrupt processing due to errors
        this.userCallbackExecutor =
                Executors.newFixedThreadPool(
                        1, new Utils.AnalyticsThreadFactory("Snapyr-InAppUserCallback"));
    }

    public void process(InAppMessage message) {
        switch (message.ActionType) {
            case ACTION_TYPE_CUSTOM:
                ServiceFacade.getLogger().info("dispatching user in-app action");
                userCallbackExecutor.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                userCallback.onAction(message);
                            }
                        });
                break;
            case ACTION_TYPE_OVERLAY:
                try {
                    handleOverlay(message);
                } catch (InAppContent.IncorrectContentAccessException e) {
                    ServiceFacade.getLogger().error(e, "action type does not match content");
                }
                break;
        }
    }

    private void handleOverlay(InAppMessage message)
            throws InAppContent.IncorrectContentAccessException {
        switch (message.Content.getType()) {
            case PAYLOAD_TYPE_JSON:
                ServiceFacade.getLogger().info("no action for json overlays currently");
                break;
            case PAYLOAD_TYPE_HTML:
                WebviewModalView.showPopup(
                        ServiceFacade.getCurrentActivity(),
                        message.Content.getHtmlPayload(),
                        message.ActionToken);
                break;
        }
    }
}
