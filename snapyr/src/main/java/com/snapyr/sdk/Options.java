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
package com.snapyr.sdk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Options let you control behaviour for a specific analytics action, including setting a custom
 * timestamp and disabling integrations on demand.
 */
public class Options {
    private final Map<String, Object> context;

    public Options() {
        context = new ConcurrentHashMap<>();
    }

    public Options(Map<String, Object> context) {
        this.context = context;
    }

    /**
     * Attach some additional context information. Unlike with {@link Snapyr#getSnapyrContext()},
     * this only has effect for this call.
     *
     * @param key The key of the extra context data
     * @param value The value of the extra context data
     * @return This options object for chaining
     */
    public Options putContext(String key, Object value) {
        context.put(key, value);
        return this;
    }

    /** Returns a copy of the context. */
    public Map<String, Object> context() {
        return new LinkedHashMap<>(context);
    }
}
