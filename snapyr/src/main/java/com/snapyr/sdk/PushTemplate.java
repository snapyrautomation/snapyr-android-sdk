package com.snapyr.sdk;

/*
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

import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushTemplate {
    String id;
    Date modified;
    ArrayList<ActionButton> buttons;

    public String getId(){ return id; }
    public Date getModified() { return modified; }
    public List<ActionButton> getButtons() { return Collections.unmodifiableList(this.buttons); }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Map<String, PushTemplate> ParseTemplate(ValueMap metadata){
        ArrayList<Map<String, Object>> templates = (ArrayList<Map<String, Object>>) metadata.get("pushTemplates");
        HashMap<String, PushTemplate> parsed = new HashMap<String, PushTemplate>();

        templates.forEach((v)->{
            PushTemplate t = new PushTemplate(v);
            parsed.put(t.id, t);
        });


        return parsed;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public PushTemplate(Map<String, Object> src){
        super();
        this.id = (String)src.get("id");
        String modifiedStr = (String)src.get("modified");

        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(modifiedStr);
        this.modified = Date.from(Instant.from(ta));
        this.buttons = new ArrayList<>();

        ArrayList<Map<String, Object>> buttonsRaw = (ArrayList) src.get("actions");
        buttonsRaw.forEach((v)->{
            this.buttons.add(new ActionButton(v));
        });
    }
}
