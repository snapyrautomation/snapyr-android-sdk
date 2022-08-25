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

import com.snapyr.sdk.SnapyrAction;
import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.Utils;
import java.util.Date;

/*
"message": {
    "content": "{}",
    "type": 'custom-json',
    "config": "{}",
    "actionToken": "actionToken-0817645f-0953-4c49-9743-d3ae887f530c.paul0817.1660936988",
    "timestamp": 1660936988
}
 */

enum InAppContentType {
    CONTENT_TYPE_OVERLAY_HTTP,
    CONTENT_TYPE_CUSTOM_JSON,
    CONTENT_TYPE_CUSTOM_HTML,
}

public class InAppMessage {
    public final Date Timestamp;
    public final InAppContentType ContentType;
    public final ValueMap Config;
    public final String ActionToken;
    public final InAppContent Content;

    public InAppMessage(SnapyrAction action) throws MalformedMessageException {
        this.Timestamp = (Date) (action.get("timestamp"));
        if (this.Timestamp == null) {
            throw new MalformedMessageException("no timestamp in action");
        }

        String contentType = action.getString("type");
        if (Utils.isNullOrEmpty(contentType)) {
            throw new MalformedMessageException("missing content type in action");
        }

        switch (contentType) {
            case "overlay-html":
                this.ContentType = InAppContentType.CONTENT_TYPE_OVERLAY_HTTP;
                break;
            case "custom-json":
                this.ContentType = InAppContentType.CONTENT_TYPE_CUSTOM_JSON;
                break;
            case "custom-html":
                this.ContentType = InAppContentType.CONTENT_TYPE_CUSTOM_HTML;
                break;
            default:
                throw new MalformedMessageException("unknown content type: " + contentType);
        }

        this.Config = action.getValueMap("config");
        this.ActionToken = action.getString("actionToken");
        if (Utils.isNullOrEmpty(this.ActionToken)) {
            throw new MalformedMessageException("missing actionToken in action");
        }
        this.Content = new InAppContent(this.ContentType, action.get("content"));
    }

    public static class MalformedMessageException extends Exception {
        public MalformedMessageException(String error) {
            super(error);
        }
    }
}
