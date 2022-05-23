@file:JvmName("ValueMapUtils")

package com.snapyr.sdk

import com.snapyr.sdk.Properties.Product
import com.snapyr.sdk.internal.Utils
import org.json.JSONObject

/**
 * Returns the value mapped by `key` if it exists and is a integer or can be coerced to a
 * integer. Returns `defaultValue` otherwise.
 */
fun ValueMap.getInt(key: String, defaultValue: Int): Int {
    val value = get(key)
    if (value is Int) {
        return value
    }
    if (value is Number) {
        return value.toInt()
    } else if (value is String) {
        try {
            return Integer.valueOf(value)
        } catch (ignored: java.lang.NumberFormatException) {
        }
    }
    return defaultValue
}

/**
 * Returns the value mapped by `key` if it exists and is a long or can be coerced to a
 * long. Returns `defaultValue` otherwise.
 */
fun ValueMap.getLong(key: String, defaultValue: Long): Long {
    val value = get(key)
    if (value is Long) {
        return value
    }
    if (value is Number) {
        return value.toLong()
    } else if (value is String) {
        try {
            return java.lang.Long.valueOf(value)
        } catch (ignored: NumberFormatException) {
        }
    }
    return defaultValue
}

/**
 * Returns the value mapped by `key` if it exists and is a float or can be coerced to a
 * float. Returns `defaultValue` otherwise.
 */
fun ValueMap.getFloat(key: String, defaultValue: Float): Float {
    val value = get(key)
    return Utils.coerceToFloat(value, defaultValue)
}

/**
 * Returns the value mapped by `key` if it exists and is a double or can be coerced to a
 * double. Returns `defaultValue` otherwise.
 */
fun ValueMap.getDouble(key: String?, defaultValue: Double): Double {
    val value = get(key)
    if (value is Double) {
        return value
    }
    if (value is Number) {
        return value.toDouble()
    } else if (value is String) {
        try {
            return java.lang.Double.valueOf(value)
        } catch (ignored: NumberFormatException) {
        }
    }
    return defaultValue
}

/**
 * Returns the value mapped by `key` if it exists and is a char or can be coerced to a
 * char. Returns `defaultValue` otherwise.
 */
fun ValueMap.getChar(key: String, defaultValue: Char): Char {
    val value = get(key)
    if (value is Char) {
        return value
    }
    if (value != null && value is String) {
        if (value.length == 1) {
            return value[0]
        }
    }
    return defaultValue
}

/**
 * Returns the value mapped by `key` if it exists and is a string or can be coerced to a
 * string. Returns null otherwise.
 *
 *
 * This will return null only if the value does not exist, since all types can have a String
 * representation.
 */
fun ValueMap.getString(key: String): String? {
    val value = get(key)
    if (value is String) {
        return value
    } else if (value != null) {
        return value.toString()
    }
    return null
}

/**
 * Returns the value mapped by `key` if it exists and is a boolean or can be coerced to a
 * boolean. Returns `defaultValue` otherwise.
 */
fun ValueMap.getBoolean(key: String, defaultValue: Boolean): Boolean {
    val value = get(key)
    if (value is Boolean) {
        return value
    } else if (value is String) {
        return java.lang.Boolean.valueOf(value)
    }
    return defaultValue
}

/**
 * Returns the value mapped by `key` if it exists and is a enum or can be coerced to an
 * enum. Returns null otherwise.
 */
fun <T : Enum<T>?> ValueMap.getEnum(enumType: Class<T>?, key: String): T? {
    requireNotNull(enumType) { "enumType may not be null" }
    val value = get(key)
    if (enumType.isInstance(value)) {
        return value as T
    } else if (value is String) {
        return java.lang.Enum.valueOf(enumType, value)
    }
    return null
}

/**
 * Returns the value mapped by `key` if it exists and if it can be coerced to the given
 * type. The expected subclass MUST have a constructor that accepts a [Map].
 */
fun ValueMap.getValueMap(key: String): ValueMap {
    val value = get(key)
    return checkNotNull(coerceToValueMap(value))
}

/**
 * Coerce an object to a JsonMap. It will first check if the object is already of the expected
 * type. If not, it checks if the object a [Map] type, and feeds it to the constructor by
 * reflection.
 */
private fun coerceToValueMap(value: Any?): ValueMap? =
    if (value is Map<*, *>) valueMapOf(value) else null

/**
 * Returns the value mapped by `key` if it exists and is a List of `T`. Returns null
 * otherwise.
 */
fun ValueMap.getList(key: Any): List<ValueMap>? {
    val value = get(key)
    if (value is List<*>) {
        try {
            val real = ArrayList<ValueMap>()
            for (item in value) {
                val typedValue = coerceToValueMap(item)
                if (typedValue != null) {
                    real.add(typedValue)
                }
            }
            return real
        } catch (ignored: Exception) {
        }
    }
    return null
}

/** Return a copy of the contents of this map as a [JSONObject].  */
fun ValueMap.toJsonObject(): JSONObject {
    return Utils.toJsonObject(this)
}

/** Return a copy of the contents of this map as a `Map<String, String>`.  */
fun ValueMap.toStringMap(): Map<String, String> =
    mapValues { (_, value) ->
        value.toString()
    }

fun List<ValueMap>.toProducts(): List<Product> = map { Product(it) }
