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
import com.snapyr.sdk.services.Cartographer;
import java.io.IOException;

public class InAppContent {
    private ValueMap jsonContent;
    private InAppContentType contentType;
    private String rawPayload;
    private String htmlContent; // TODO: is there a better type for this? The HTML type
    // looks useless

    public InAppContent(ValueMap raw) throws InAppMessage.MalformedMessageException {
        if (raw == null) {
            throw new InAppMessage.MalformedMessageException("content object is nul");
        }
        String contentType = raw.getString("payloadType");
        if (contentType == null) {
            throw new InAppMessage.MalformedMessageException("no payloadType key");
        }

        Object content = raw.get("payload");
        if (content == null) {
            throw new InAppMessage.MalformedMessageException("no content");
        }

        this.rawPayload = (String) content;

        switch (contentType) {
            case "json":
                this.contentType = InAppContentType.CONTENT_TYPE_JSON;
                try {
                    this.jsonContent =
                            new ValueMap(Cartographer.INSTANCE.fromJson((String) content));
                } catch (IOException e) {
                    throw new InAppMessage.MalformedMessageException(
                            "failed to parse json content: " + e.getMessage());
                }
                if (this.jsonContent == null) {
                    throw new InAppMessage.MalformedMessageException(
                            "unknown format for custom-json");
                }
                break;
            case "html":
                this.contentType = InAppContentType.CONTENT_TYPE_HTML;
                this.htmlContent = (String) content;
                break;
            default:
                if (this.jsonContent == null) {
                    throw new InAppMessage.MalformedMessageException("unknown format for content");
                }
                break;
        }
    }

    public InAppContentType getType() {
        return this.contentType;
    }

    public ValueMap getJsonContent() throws IncorrectContentAccessException {
        if (this.contentType != InAppContentType.CONTENT_TYPE_JSON) {
            throw new IncorrectContentAccessException("content is not json");
        }
        return this.jsonContent;
    }

    public String getHtmlContent() throws IncorrectContentAccessException {
        if (this.contentType != InAppContentType.CONTENT_TYPE_HTML) {
            throw new IncorrectContentAccessException("content is not html");
        }
        return this.htmlContent;
    }

    public ValueMap asValueMap() {
        ValueMap map =
                new ValueMap()
                        .putValue(
                                "payloadType",
                                (this.contentType == InAppContentType.CONTENT_TYPE_JSON)
                                        ? "json"
                                        : "html")
                        .putValue(
                                "payload",
                                (this.contentType == InAppContentType.CONTENT_TYPE_JSON)
                                        ? this.jsonContent
                                        : this.htmlContent);
        return map;
    }

    public static class IncorrectContentAccessException extends Exception {
        public IncorrectContentAccessException(String error) {
            super(error);
        }
    }
}
