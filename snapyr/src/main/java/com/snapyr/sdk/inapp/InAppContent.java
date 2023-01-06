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

import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.services.Cartographer;
import java.io.IOException;

public class InAppContent {
    private ValueMap jsonPayload;
    private InAppPayloadType payloadType;
    private String rawPayload;
    private String htmlPayload; // TODO: is there a better type for this? The HTML type
    // looks useless

    public InAppContent(ValueMap raw) throws InAppMessage.MalformedMessageException {
        if (raw == null) {
            throw new InAppMessage.MalformedMessageException("content object is null");
        }
        String payloadType = raw.getString("payloadType");
        if (payloadType == null) {
            throw new InAppMessage.MalformedMessageException("no payloadType key");
        }

        Object payload = raw.get("payload");
        if (payload == null) {
            throw new InAppMessage.MalformedMessageException("no payload");
        }

        this.rawPayload = (String) payload;

        switch (payloadType) {
            case "json":
                this.payloadType = InAppPayloadType.PAYLOAD_TYPE_JSON;
                try {
                    this.jsonPayload =
                            new ValueMap(Cartographer.INSTANCE.fromJson((String) payload));
                } catch (IOException e) {
                    throw new InAppMessage.MalformedMessageException(
                            "failed to parse json content: " + e.getMessage());
                }
                if (this.jsonPayload == null) {
                    throw new InAppMessage.MalformedMessageException(
                            "unknown format for custom-json");
                }
                break;
            case "html":
                this.payloadType = InAppPayloadType.PAYLOAD_TYPE_HTML;
                this.htmlPayload = (String) payload;
                break;
            default:
                if (this.jsonPayload == null) {
                    throw new InAppMessage.MalformedMessageException("unknown format for content");
                }
                break;
        }
    }

    public InAppPayloadType getType() {
        return this.payloadType;
    }

    public ValueMap getJsonPayload() throws IncorrectContentAccessException {
        if (this.payloadType != InAppPayloadType.PAYLOAD_TYPE_JSON) {
            throw new IncorrectContentAccessException("payload is not json");
        }
        return this.jsonPayload;
    }

    public String getHtmlPayload() throws IncorrectContentAccessException {
        if (this.payloadType != InAppPayloadType.PAYLOAD_TYPE_HTML) {
            throw new IncorrectContentAccessException("payload is not html");
        }
        return this.htmlPayload;
    }

    public ValueMap asValueMap() {
        ValueMap map =
                new ValueMap()
                        .putValue(
                                "payloadType",
                                (this.payloadType == InAppPayloadType.PAYLOAD_TYPE_JSON)
                                        ? "json"
                                        : "html")
                        .putValue(
                                "payload",
                                (this.payloadType == InAppPayloadType.PAYLOAD_TYPE_JSON)
                                        ? this.jsonPayload
                                        : this.htmlPayload);
        return map;
    }

    public static class IncorrectContentAccessException extends Exception {
        public IncorrectContentAccessException(String error) {
            super(error);
        }
    }
}
