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

import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.SnapyrAction;
import com.snapyr.sdk.SnapyrActionHandler;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.integrations.AliasPayload;
import com.snapyr.sdk.integrations.GroupPayload;
import com.snapyr.sdk.integrations.IdentifyPayload;
import com.snapyr.sdk.integrations.ScreenPayload;
import com.snapyr.sdk.integrations.TrackPayload;

public class InAppActionHandler implements SnapyrActionHandler {
    @Override
    public void handleAction(SnapyrAction action) {
        if (action.containsKey("content") && action.getValueMap("content") != null) {
            ValueMap content = action.getValueMap("content");
            if (content.containsKey("bodyHtml") && content.getString("bodyHtml") != null) {
                String rawHtml = content.getString("bodyHtml");
                InAppModal.ShowPopup(Snapyr.with(action.context).GetActivity(), rawHtml);
            }
        }
    }

    @Override
    public void onIdentify(IdentifyPayload payload) {}

    @Override
    public void onTrack(TrackPayload payload) {}

    @Override
    public void onAlias(AliasPayload payload) {}

    @Override
    public void onGroup(GroupPayload payload) {}

    @Override
    public void onScreen(ScreenPayload payload) {}
}
