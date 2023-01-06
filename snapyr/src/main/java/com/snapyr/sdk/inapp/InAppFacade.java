/**
 * The MIT License (MIT)
 *
 * <p>Copyright (c) 2014 Segment.io, Inc.
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.snapyr.sdk.inapp;

import android.content.Context;
import androidx.annotation.NonNull;
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.services.ServiceFacade;

public class InAppFacade {
    enum InAppState {
        IN_APP_STATE_SUPPRESSED,
        IN_APP_STATE_ALLOWED,
    }

    private static InAppState inappState = InAppState.IN_APP_STATE_ALLOWED;
    private static InAppIFace impl = new NoOpManager();

    public static InAppIFace createInApp(@NonNull InAppConfig config, @NonNull Context context) {
        if (InAppFacade.impl instanceof InAppManager) {
            ServiceFacade.getLogger().info("inApp already initialized");
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

    /**
     * Processes any actions from a track/identify/etc response
     *
     * @param action
     */
    public static void processTrackResponse(SnapyrAction action) {
        if (InAppFacade.inappState != InAppState.IN_APP_STATE_ALLOWED) {
            return;
        }
        impl.processTrackResponse(action);
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

    /** Dummy implementation of the InAppIFace that does nothing */
    public static class NoOpManager implements InAppIFace {
        public NoOpManager() {}

        @Override
        public void processTrackResponse(SnapyrAction action) {}

        @Override
        public void dispatchPending(Context context) {}
    }

    /** Dummy implementation of the InAppCallback that does nothing */
    public static class NoOpHandler implements InAppCallback {
        public NoOpHandler() {}

        @Override
        public void onAction(InAppMessage message) {}
    }
}
