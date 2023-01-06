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
package com.snapyr.sdk.internal;

import android.net.Uri;
import java.util.Map;

public class ActionButton {
    public String id;
    public String actionId;
    public String title;
    public Uri deeplinkURL;

    public ActionButton(Map<String, Object> src) {
        this.id = (String) src.get("id");
        this.title = (String) src.get("title");
        String link = (String) src.get("deepLinkUrl");
        if (link != null) {
            this.deeplinkURL = Uri.parse(link);
        } // error?
    }

    public ActionButton(String id, String actionId, String title, String deeplink) {
        this.id = id;
        this.title = title;
        if (deeplink != null) {
            this.deeplinkURL = Uri.parse(deeplink);
        }
    }
}
