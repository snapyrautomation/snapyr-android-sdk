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
package com.snapyr.sdk.internal;

/* Example payload:
    "pushTemplates": [
           {
               "id": "d3cd39ac-6917-4bce-a16f-448d579be9b4",
               "modified": "2022-02-11T15:03:41.658Z",
               "actions": [
                   {
                       "id": "127974f8-893e-4cdf-bf78-c2d86d4426e9",
                       "actionId": "asdlkj",
                       "title": "Button1",
                       "deepLinkUrl": "snapyrtest://dltest?msg=Hello%20World!&type=success"
                   },
                   {
                       "id": "16b78326-8cec-40a0-a54e-41efc570c051",
                       "actionId": "asdlfkji",
                       "title": "Button2",
                       "deepLinkUrl": "snapyrtest://dltest?msg=Hello%20World!&type=failure"
                   }
               ]
           }
       ]
*/

import com.snapyr.sdk.ValueMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PushTemplate Defines a push template from the control plane. Handles parsing of the template from
 * the json blob recieved during the sdk configuration update.
 */
public class PushTemplate {
    String id;
    Date modified;
    ArrayList<ActionButton> buttons;

    public PushTemplate(Map<String, Object> src) {
        super();
        this.id = (String) src.get("id");
        String modifiedStr = (String) src.get("modified");

        this.modified = Utils.parseISO8601Date(modifiedStr);
        this.buttons = new ArrayList<>();

        ArrayList<Map<String, Object>> buttonsRaw = (ArrayList) src.get("actions");
        for (Map<String, Object> v : buttonsRaw) {
            this.buttons.add(new ActionButton(v));
        }
    }

    public static Map<String, PushTemplate> ParseTemplate(ValueMap metadata) {
        ArrayList<Map<String, Object>> templates =
                (ArrayList<Map<String, Object>>) metadata.get("pushTemplates");
        HashMap<String, PushTemplate> parsed = new HashMap<String, PushTemplate>();

        for (Map<String, Object> v : templates) {
            PushTemplate t = new PushTemplate(v);
            parsed.put(t.id, t);
        }

        return parsed;
    }

    public String getId() {
        return id;
    }

    public Date getModified() {
        return modified;
    }

    public List<ActionButton> getButtons() {
        return Collections.unmodifiableList(this.buttons);
    }
}
