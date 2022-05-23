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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A class that wraps an existing {@link Map} to expose value type functionality. All {@link
 * java.util.Map} methods will simply be forwarded to a delegate map. This class is meant to
 * subclassed and provide methods to access values in keys.
 *
 * <p>Library users won't need to create instances of this class, they can use plain old {@link Map}
 * instead, and our library will handle serializing them.
 *
 * <p>Although it lets you use custom objects for values, note that type information is lost during
 * serialization. You should use one of the coercion methods instead to get objects of a concrete
 * type.
 */
public class ValueMap implements Map<String, Object> {

    private final Map<String, Object> delegate;

    public ValueMap() {
        delegate = new LinkedHashMap<>();
    }

    public ValueMap(int initialCapacity) {
        delegate = new LinkedHashMap<>(initialCapacity);
    }

    public ValueMap(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map must not be null.");
        }
        this.delegate = map;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public Object get(Object key) {
        return delegate.get(key);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Object put(String key, Object value) {
        return delegate.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        delegate.putAll(map);
    }

    @Override
    public Object remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Collection<Object> values() {
        return delegate.values();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass") //
    @Override
    public boolean equals(Object object) {
        return object == this || delegate.equals(object);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    /** Helper method to be able to chain put methods. */
    public ValueMap putValue(String key, Object value) {
        delegate.put(key, value);
        return this;
    }

}
