package com.snapyr.sdk

import com.snapyr.sdk.internal.Utils
import java.text.ParseException
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun valueMap(key: String) =
    object : ReadWriteProperty<ValueMap, ValueMap?> {
        override fun getValue(thisRef: ValueMap, property: KProperty<*>): ValueMap? =
            thisRef.getValueMapOrNull(key)

        override fun setValue(thisRef: ValueMap, property: KProperty<*>, value: ValueMap?) {
            thisRef[key] = value
        }
    }

fun intValue(key: String, defaultValue: Int? = null) =
    object : ReadWriteProperty<ValueMap, Int?> {
        override fun getValue(thisRef: ValueMap, property: KProperty<*>): Int? =
            thisRef.getInt(key, defaultValue)

        override fun setValue(thisRef: ValueMap, property: KProperty<*>, value: Int?) {
            thisRef[key] = value
        }
    }

fun longValue(key: String, defaultValue: Long? = null) =
    object : ReadWriteProperty<ValueMap, Long?> {
        override fun getValue(thisRef: ValueMap, property: KProperty<*>): Long? =
            thisRef.getLong(key, defaultValue)

        override fun setValue(thisRef: ValueMap, property: KProperty<*>, value: Long?) {
            thisRef[key] = value
        }
    }

fun stringValue(key: String, defaultValue: String? = null) =
    object : ReadWriteProperty<ValueMap, String?> {
        override fun getValue(thisRef: ValueMap, property: KProperty<*>): String? =
            thisRef.getString(key) ?: defaultValue

        override fun setValue(thisRef: ValueMap, property: KProperty<*>, value: String?) {
            thisRef[key] = value
        }
    }

fun dateValue(key: String, defaultValue: Date? = null) =
    object : ReadWriteProperty<ValueMap, Date?> {
        override fun getValue(thisRef: ValueMap, property: KProperty<*>): Date? =
            thisRef.getString(key)?.toDate() ?: defaultValue

        override fun setValue(thisRef: ValueMap, property: KProperty<*>, value: Date?) {
            thisRef[key] = Utils.toISO8601Date(value)
        }
    }

private fun String.toDate(): Date? {
    return try {
        Utils.toISO8601Date(this)
    } catch (e: ParseException) {
        null
    }
}
