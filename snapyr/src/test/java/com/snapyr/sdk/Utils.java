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

public final class Utils {
    private Utils() {
        throw new AssertionError("No instances.");
    }

    /** Create a {@link Traits} with only a randomly generated anonymous ID. */
    public static Traits createTraits() {
        return new Traits();
    }

    /** Create a {@link Traits} object with the given {@code userId}. */
    public static Traits createTraits(String userId) {
        Traits traits = createTraits();
        traits.setUserId(userId);
        return traits;
    }

    /** Create an {@link SnapyrContext} with the given {@code traits}. */
    public static SnapyrContext createContext(Traits traits) {
        SnapyrContext context = new SnapyrContext(new LinkedHashMap<String, Object>());
        context.setTraits(traits);
        return context;
    }
}
