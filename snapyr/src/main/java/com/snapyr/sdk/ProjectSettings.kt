package com.snapyr.sdk

import android.content.Context

class ProjectSettings(map: ValueMap = emptyValueMap()) : ValueMap by map {
    var timestamp by longValue("timestamp", System.currentTimeMillis())
    var plan by valueMap("plan")
    var integrations by valueMap("integrations")
    var trackingPlan by valueMap("track")
    var edgeFunctions by valueMap("edgeFunction")
}

fun ValueMap.asProjectSettings(): ProjectSettings = ProjectSettings(this)

private const val PROJECT_SETTINGS_CACHE_KEY_PREFIX = "project-settings-plan-"
fun createProjectSettingsCache(
    context: Context,
    cartographer: Cartographer,
    tag: String
) = ValueMapCache(context, cartographer, PROJECT_SETTINGS_CACHE_KEY_PREFIX + tag, tag)
