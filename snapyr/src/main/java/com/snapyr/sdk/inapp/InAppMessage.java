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

import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.internal.Utils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

enum InAppActionType {
    ACTION_TYPE_CUSTOM,
    ACTION_TYPE_OVERLAY,
}

enum InAppContentType {
    CONTENT_TYPE_JSON,
    CONTENT_TYPE_HTML,
}

public class InAppMessage {
    public static final SimpleDateFormat Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final Date Timestamp;
    public final InAppActionType ActionType;
    public final String ActionToken;
    public final InAppContent Content;
    public final String UserId;

    public InAppMessage(SnapyrAction action) throws MalformedMessageException {
        String rawTs = action.getString("timestamp");
        if (rawTs == null) {
            throw new MalformedMessageException("no timestamp in action");
        }
        try {
            this.Timestamp = Formatter.parse(rawTs);
        } catch (ParseException e) {
            throw new MalformedMessageException("malformed timestamp");
        }

        String contentType = action.getString("actionType");
        if (Utils.isNullOrEmpty(contentType)) {
            throw new MalformedMessageException("missing actionType in action");
        }
        this.UserId = action.getString("userId");

        switch (contentType) {
            case "custom":
                this.ActionType = InAppActionType.ACTION_TYPE_CUSTOM;
                break;
            case "overlay":
                this.ActionType = InAppActionType.ACTION_TYPE_OVERLAY;
                break;
            default:
                throw new MalformedMessageException("unknown content type: " + contentType);
        }

        this.ActionToken = action.getString("actionToken");
        if (Utils.isNullOrEmpty(this.ActionToken)) {
            throw new MalformedMessageException("missing actionToken in action");
        }
        this.Content = new InAppContent(action.getValueMap("content"));
    }

    public ValueMap asValueMap() {
        ValueMap map =
                new ValueMap()
                        .putValue("timestamp", Formatter.format(this.Timestamp))
                        .putValue("userId", this.UserId)
                        .putValue("actionToken", this.ActionToken)
                        .putValue(
                                "actionType",
                                (this.ActionType == InAppActionType.ACTION_TYPE_CUSTOM
                                        ? "custom"
                                        : "overlay"))
                        .putValue("content", this.Content.asValueMap());
        return map;
    }

    public static class MalformedMessageException extends Exception {
        public MalformedMessageException(String error) {
            super(error);
        }
    }
}
