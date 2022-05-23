package com.snapyr.sdk

import android.content.Context
import android.content.SharedPreferences
import com.snapyr.sdk.internal.Utils
import java.io.IOException

class ValueMapCache(
    context: Context,
    private val cartographer: Cartographer,
    private val key: String,
    private val tag: String
) {
    private val preferences: SharedPreferences = Utils.getSnapyrSharedPreferences(context, tag)
    private var value: ValueMap? = null

    fun get(): ValueMap? {
        if (value == null) {
            val json = preferences.getString(key, null)
            if (Utils.isNullOrEmpty(json)) return null
            value = try {
                val map = cartographer.fromJson(json)
                valueMapOf(map)
            } catch (ignored: IOException) {
                return null
            }
        }
        return value
    }

    fun isSet(): Boolean {
        return preferences.contains(key)
    }

    fun set(map: Map<*, *>) {
        this.value = valueMapOf(map)
        val json = cartographer.toJson(value)
        preferences.edit().putString(key, json).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}
