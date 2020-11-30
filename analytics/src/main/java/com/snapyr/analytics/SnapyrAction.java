package com.snapyr.analytics;

import static java.util.Collections.unmodifiableMap;

import com.snapyr.analytics.internal.Private;
import java.util.Map;

public class SnapyrAction extends ValueMap {

    private static final String ACTION_KEY = "action";
    private static final String PROPERTIES_KEY = "properties";

    @Private
    SnapyrAction(Map<String, Object> map) {
        super(unmodifiableMap(map));
    }

    public String getAction() {
        return getString(ACTION_KEY);
    }

    public ValueMap getProperties() {
        return getValueMap(PROPERTIES_KEY);
    }

    public static SnapyrAction create(Map<String, Object> map) {
        return new SnapyrAction(map);
    }
}
