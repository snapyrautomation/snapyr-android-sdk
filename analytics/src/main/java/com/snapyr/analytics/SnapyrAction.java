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
