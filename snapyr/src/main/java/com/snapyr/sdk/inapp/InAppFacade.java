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
import com.snapyr.sdk.SnapyrAction;

public class InAppFacade {
    enum InAppState {
        IN_APP_STATE_SUPPRESSED,
        IN_APP_STATE_ALLOWED,
    }

    private static InAppState inappState = InAppState.IN_APP_STATE_ALLOWED;
    private static InAppIFace impl = new NoopInApp();

    public static InAppIFace createInApp(InAppConfig config, Context context) {
        if (InAppFacade.impl instanceof InAppManager) {
            config.Logger.info("inApp already initialized");
            return impl;
        }

        InAppFacade.impl = new InAppManager(config, context);
        return InAppFacade.impl;
    }

    /** Suppresses all snapyr-rendered in-app creatives from rendering */
    public static void suppressInApp() {
        InAppFacade.inappState = InAppState.IN_APP_STATE_SUPPRESSED;
    }

    /** Allows snapyr-rendered in-app creatives to render */
    public static void allowInApp() {
        InAppFacade.inappState = InAppState.IN_APP_STATE_ALLOWED;
    }

    public static void processTrackResponse(Context context, SnapyrAction action) {
        if (InAppFacade.inappState != InAppState.IN_APP_STATE_ALLOWED) {
            return;
        }
        impl.processTrackResponse(context, action);
    }

    /**
     * processes any pending data immediately
     *
     * @param context
     */
    public static void processPending(Context context) {
        if (InAppFacade.inappState != InAppState.IN_APP_STATE_ALLOWED) {
            return;
        }
        impl.dispatchPending(context);
    }
}
