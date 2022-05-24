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
package com.snapyr.sdk

/**
 * A class that wraps an existing [Map] to expose value type functionality.
 * All [ ] methods will simply be forwarded to a delegate map. This class is meant to
 * subclassed and provide methods to access values in keys.
 *
 *
 * Library users won't need to create instances of this class, they can use plain old [Map]
 * instead, and our library will handle serializing them.
 *
 *
 * Although it lets you use custom objects for values, note that type information is lost during
 * serialization. You should use one of the coercion methods instead to get objects of a concrete
 * type.
 */
interface ValueMap : MutableMap<String, Any?> {
    /** Helper method to be able to chain put methods.  */
    open fun <T : ValueMap> putValue(key: String, value: Any): T {
        this[key] = value
        return this as T
    }
}

open class LegacyValueMap(var map: Map<String, Any?> = emptyMap()) : ValueMap,
    MutableMap<String, Any?> by map.toMutableMap()

// Utils
private fun Map<*, *>.filterStringKeys(): Map<String, Any?> {
    val result = LinkedHashMap<String, Any?>()
    for (entry in this) {
        val key = entry.key
        if (key is String) {
            result[key] = entry.value
        }
    }
    return result
}

fun valueMapOf(map: Map<*, *> = emptyMap<String, Any?>()): ValueMap = LegacyValueMap(map.filterStringKeys())

fun <V> valueMapOf(vararg pairs: Pair<String, V?>): Map<String, V?> =
    if (pairs.isNotEmpty()) pairs.toMap(LinkedHashMap(pairs.size)) else emptyMap()

fun emptyValueMap(): ValueMap = valueMapOf(emptyMap<String, Any?>())
