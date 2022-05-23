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
        this.value = valueMapOf(map.filterStringKeys())
        val json = cartographer.toJson(value)
        preferences.edit().putString(key, json).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}

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

fun valueMapOf(map: Map<*, *>): ValueMap = ValueMap(map.filterStringKeys())


// Traits
private const val TRAITS_CACHE_PREFIX = "traits-"
fun createTraitsCache(
    context: Context,
    cartographer: Cartographer,
    tag: String
) = ValueMapCache(context, cartographer, TRAITS_CACHE_PREFIX + tag, tag)

fun ValueMapCache.getTraits(): Traits = Traits(get())

// ProjectSettings
private const val PROJECT_SETTINGS_CACHE_KEY_PREFIX = "project-settings-plan-"
fun createProjectSettingsCache(
    context: Context,
    cartographer: Cartographer,
    tag: String
) = ValueMapCache(context, cartographer, PROJECT_SETTINGS_CACHE_KEY_PREFIX + tag, tag)

fun ValueMapCache.getProjectSettings(): ProjectSettings = ProjectSettings(get())
