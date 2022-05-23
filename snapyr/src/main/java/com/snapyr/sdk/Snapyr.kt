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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.snapyr.sdk.internal.Private
import com.snapyr.sdk.notifications.SnapyrNotificationLifecycleCallbacks
import com.snapyr.sdk.notifications.SnapyrNotificationHandler
import com.snapyr.sdk.internal.PushTemplate
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import com.snapyr.sdk.Snapyr
import com.snapyr.sdk.integrations.IdentifyPayload
import com.snapyr.sdk.internal.NanoDate
import com.snapyr.sdk.integrations.GroupPayload
import com.snapyr.sdk.integrations.TrackPayload
import com.snapyr.sdk.integrations.ScreenPayload
import com.snapyr.sdk.integrations.AliasPayload
import com.snapyr.sdk.integrations.BasePayload
import com.snapyr.sdk.internal.Utils.AnalyticsNetworkExecutorService
import androidx.lifecycle.ProcessLifecycleOwner
import com.snapyr.sdk.integrations.Logger
import com.snapyr.sdk.internal.Utils
import java.lang.AssertionError
import java.lang.Exception
import java.lang.UnsupportedOperationException
import java.util.LinkedHashMap
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The entry point into the Snapyr for Android SDK.
 *
 *
 * The idea is simple: one pipeline for all your data. Snapyr is the single hub to collect,
 * translate and route your data with the flip of a switch.
 *
 *
 * Analytics for Android will automatically batch events, queue them to disk, and upload it
 * periodically to Snapyr for you. It will also look up your project's settings (that you've
 * configured in the web interface), specifically looking up settings for bundled integrations, and
 * then initialize them for you on the user's phone, and mapping our standardized events to formats
 * they can all understand. You only need to instrument Snapyr once, then flip a switch to install
 * new tools.
 *
 *
 * This class is the main entry point into the client API. Use [ ][.with] for the global singleton instance or construct your own instance
 * with [Builder].
 *
 * @see [Snapyr](https://snapyr.com/)
 */
class Snapyr internal constructor(
    /** Return the [Application] used to create this instance.  */
    val application: Application?,
    val networkExecutor: ExecutorService,
    val stats: Stats,
    // TODO (major version change) hide internals (don't give out working copy), expose a better
    @field:Private val traitsCache: Traits.Cache,
    /** Get the [SnapyrContext] used by this instance.  */
    @field:Private val snapyrContext: SnapyrContext,
    @field:Private val defaultOptions: Options,
    /**
     * Return the [Logger] instance used by this client.
     *
     */
    @get:Deprecated("This will be removed in a future release.") val logger: Logger,
    // API
    //  for modifying the global context
    val tag: String?,
    val client: Client,
    val cartographer: Cartographer,
    private val projectSettingsCache: ProjectSettings.Cache,
    @field:Private val writeKey: String?,
    val flushQueueSize: Int,
    val flushIntervalInMillis: Long,
    private val analyticsExecutor: ExecutorService?,
    shouldTrackApplicationLifecycleEvents: Boolean,
    @field:Private val actionHandler: SnapyrActionHandler?,
    // Retrieving the advertising ID is asynchronous. This latch helps us wait to ensure the
    // advertising ID is ready.
    private val advertisingIdLatch: CountDownLatch,
    shouldRecordScreenViews: Boolean,
    trackDeepLinks: Boolean,
    private val optOut: BooleanPreference,
    val crypto: Crypto?,
    defaultProjectSettings: ValueMap,
    @field:Private val lifecycle: Lifecycle,
    @field:Private val nanosecondTimestamps: Boolean,
    @field:Private val useNewLifecycleMethods: Boolean,
    enableSnapyrPushHandling: Boolean
) {
    @Private
    val activityLifecycleCallback: SnapyrActivityLifecycleCallbacks

    @Private
    val notificationLifecycleCallbacks: SnapyrNotificationLifecycleCallbacks

    var projectSettings = ProjectSettings(emptyMap())

    @Volatile
    var shutdown = false
    var notificationHandler: SnapyrNotificationHandler? = null
    private var pushToken: String? = null
    var pushTemplates: Map<String, PushTemplate>?
        private set
    private val sendQueue: SnapyrWriteQueue

    init {
        pushTemplates = null
        sendQueue = SnapyrWriteQueue(
            application,
            client,
            cartographer,
            networkExecutor,
            stats,
            flushIntervalInMillis,
            flushQueueSize,
            logger,
            crypto,
            null,
            actionHandler
        )
        namespaceSharedPreferences()
        analyticsExecutor!!.submit {
            RefreshConfiguration(false)
            if (Utils.isNullOrEmpty(projectSettings)) {
                // Backup mode - Enable the Snapyr integration and load the provided
                // defaultProjectSettings
                if (!defaultProjectSettings.containsKey("integrations")) {
                    defaultProjectSettings["integrations"] = ValueMap()
                }
                if (!defaultProjectSettings
                        .getValueMap("integrations")
                        .containsKey("Snapyr")
                ) {
                    defaultProjectSettings
                        .getValueMap("integrations")["Snapyr"] = ValueMap()
                }
                if (!defaultProjectSettings
                        .getValueMap("integrations")
                        .getValueMap("Snapyr")
                        .containsKey("apiKey")
                ) {
                    defaultProjectSettings
                        .getValueMap("integrations")
                        .getValueMap("Snapyr")
                        .putValue("apiKey", writeKey)
                }
                if (!defaultProjectSettings.containsKey("metadata")) {
                    defaultProjectSettings["metadata"] = ValueMap()
                }
                if (!defaultProjectSettings
                        .getValueMap("metadata")
                        .containsKey("platform")
                ) {
                    defaultProjectSettings
                        .getValueMap("metadata")["platform"] = "Android"
                }
                projectSettings = ProjectSettings.create(defaultProjectSettings)
            }
        }
        logger.debug("Created analytics client for project with tag:%s.", tag)
        activityLifecycleCallback = SnapyrActivityLifecycleCallbacks.Builder()
            .snapyr(this)
            .analyticsExecutor(analyticsExecutor)
            .shouldTrackApplicationLifecycleEvents(
                shouldTrackApplicationLifecycleEvents
            )
            .trackDeepLinks(trackDeepLinks)
            .shouldRecordScreenViews(shouldRecordScreenViews)
            .packageInfo(getPackageInfo(application))
            .useNewLifecycleMethods(useNewLifecycleMethods)
            .build()
        application!!.registerActivityLifecycleCallbacks(activityLifecycleCallback)
        if (useNewLifecycleMethods) {
            analyticsExecutor.submit { HANDLER.post { lifecycle.addObserver(activityLifecycleCallback) } }
        }
        notificationLifecycleCallbacks = SnapyrNotificationLifecycleCallbacks(this, logger, trackDeepLinks)
        application.registerActivityLifecycleCallbacks(notificationLifecycleCallbacks)
        if (enableSnapyrPushHandling) {
            notificationHandler = SnapyrNotificationHandler(application)
            notificationHandler?.autoRegisterFirebaseToken(this)

            // Add lifecycle callback observer so we can track user behavior on notifications
            // (i.e. tapping a notification or tapping an action button on notification)
            analyticsExecutor.submit { HANDLER.post { lifecycle.addObserver(notificationLifecycleCallbacks) } }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun RefreshConfiguration(force: Boolean) {
        val newSettings = getSettings(force)
        if (newSettings != null && !Utils.isNullOrEmpty(newSettings)) {
            projectSettings = newSettings
            val metadata = projectSettings.getValueMap("metadata")
            snapyrContext.putSdkMeta(metadata)
            pushTemplates = PushTemplate.ParseTemplate(metadata)
        }
    }

    @Private
    fun trackApplicationLifecycleEvents() {
        // Get the current version.
        val packageInfo = getPackageInfo(application)
        val currentVersion = packageInfo.versionName
        val currentBuild = packageInfo.versionCode

        // Get the previous recorded version.
        val sharedPreferences = Utils.getSnapyrSharedPreferences(
            application, tag
        )
        val previousVersion = sharedPreferences.getString(VERSION_KEY, null)
        val previousBuild = sharedPreferences.getInt(BUILD_KEY, -1)

        // Check and track Application Installed or Application Updated.
        if (previousBuild == -1) {
            track(
                "Application Installed",
                Properties() //
                    .putValue(VERSION_KEY, currentVersion)
                    .putValue(BUILD_KEY, currentBuild.toString())
            )
        } else if (currentBuild != previousBuild) {
            track(
                "Application Updated",
                Properties() //
                    .putValue(VERSION_KEY, currentVersion)
                    .putValue(BUILD_KEY, currentBuild.toString())
                    .putValue("previous_" + VERSION_KEY, previousVersion)
                    .putValue("previous_" + BUILD_KEY, previousBuild.toString())
            )
        }

        // Update the recorded version.
        val editor = sharedPreferences.edit()
        editor.putString(VERSION_KEY, currentVersion)
        editor.putInt(BUILD_KEY, currentBuild)
        editor.apply()
    }

    // Analytics API
    @Private
    fun recordScreenViews(activity: Activity) {
        val packageManager = activity.packageManager
        try {
            val info = packageManager.getActivityInfo(
                activity.componentName, PackageManager.GET_META_DATA
            )
            val activityLabel = info.loadLabel(packageManager)
            screen(activityLabel.toString())
        } catch (e: PackageManager.NameNotFoundException) {
            throw AssertionError("Activity Not Found: $e")
        } catch (e: Exception) {
            logger.error(e, "Unable to track screen view for %s", activity.toString())
        }
    }

    /** @see .identify
     */
    fun identify(payload: IdentifyPayload) {
        identify(payload.userId(), payload.traits(), null)
    }

    /** @see .identify
     */
    fun identify(userId: String) {
        identify(userId, null, null)
    }

    /** @see .identify
     */
    fun identify(traits: Traits) {
        identify(null, traits, null)
    }

    /**
     * Identify lets you tie one of your users and their actions to a recognizable `userId`.
     * It also lets you record `traits` about the user, like their email, name, account type,
     * etc.
     *
     *
     * Traits and userId will be automatically cached and available on future sessions for the
     * same user. To update a trait on the server, call identify with the same user id (or null).
     * You can also use [.identify] for this purpose.
     *
     * @param userId Unique identifier which you recognize a user by in your own database. If this
     * is null or empty, any previous id we have (could be the anonymous id) will be used.
     * @param newTraits Traits about the user.
     * @param options To configure the call, these override the defaultOptions, to extend use
     * #getDefaultOptions()
     * @throws IllegalArgumentException if both `userId` and `newTraits` are not
     * provided
     * @see [Identify Documentation](https://segment.com/docs/spec/identify/)
     */
    fun identify(
        userId: String?,
        newTraits: Traits?,
        options: Options?
    ) {
        assertNotShutdown()
        require(
            !(Utils.isNullOrEmpty(userId) && Utils.isNullOrEmpty(
                newTraits
            ))
        ) { "Either userId or some traits must be provided." }
        val timestamp = NanoDate()
        analyticsExecutor!!.submit {
            val traits = traitsCache.get()
            if (!Utils.isNullOrEmpty(userId)) {
                traits.putUserId(userId)
            }
            if (!Utils.isNullOrEmpty(newTraits)) {
                traits.putAll(newTraits!!)
            }
            traitsCache.set(traits) // Save the new traits
            snapyrContext.setTraits(traits) // Update the references
            val builder = IdentifyPayload.Builder()
                .timestamp(timestamp)
                .traits(traitsCache.get())
            fillAndEnqueue(builder, options)
        }
        if (pushToken != null) {
            track("snapyr.hidden.fcmTokenSet", Properties().putValue("token", pushToken))
        }
    }

    /** @see .group
     */
    fun group(payload: GroupPayload) {
        group(payload.groupId(), payload.traits(), null)
    }

    /**
     * The group method lets you associate a user with a group. It also lets you record custom
     * traits about the group, like industry or number of employees.
     *
     *
     * If you've called [.identify] before, this will
     * automatically remember the userId. If not, it will fall back to use the anonymousId instead.
     *
     * @param groupId Unique identifier which you recognize a group by in your own database. Must
     * not be null or empty.
     * @param options To configure the call, these override the defaultOptions, to extend use
     * #getDefaultOptions()
     * @throws IllegalArgumentException if groupId is null or an empty string.
     * @see [Group Documentation](https://segment.com/docs/spec/group/)
     */
    /** @see .group
     */
    @JvmOverloads
    fun group(
        groupId: String,
        groupTraits: Traits? = null,
        options: Options? = null
    ) {
        assertNotShutdown()
        require(!Utils.isNullOrEmpty(groupId)) { "groupId must not be null or empty." }
        val timestamp = NanoDate()
        analyticsExecutor!!.submit {
            val finalGroupTraits: Traits
            finalGroupTraits = groupTraits ?: Traits()
            val builder = GroupPayload.Builder()
                .timestamp(timestamp)
                .groupId(groupId)
                .traits(finalGroupTraits)
            fillAndEnqueue(builder, options)
        }
    }

    /** @see .track
     */
    fun track(payload: TrackPayload) {
        track(payload.userId()!!, payload.properties(), null)
    }

    /**
     * @see .screen
     */
    @Deprecated("Use {@link #screen(String)} instead.")
    fun screen(payload: ScreenPayload) {
        screen(payload.category(), payload.name(), payload.properties(), null)
    }

    /**
     * @see .screen
     */
    @Deprecated("Use {@link #screen(String)} instead.")
    fun screen(category: String?, name: String?) {
        screen(category, name, null, null)
    }

    /**
     * @see .screen
     */
    @Deprecated("Use {@link #screen(String, Properties)} instead.")
    fun screen(
        category: String?, name: String?, properties: Properties?
    ) {
        screen(category, name, properties, null)
    }

    /** @see .screen
     */
    fun screen(name: String?) {
        screen(null, name, null, null)
    }

    /** @see .screen
     */
    fun screen(name: String?, properties: Properties?) {
        screen(null, name, properties, null)
    }

    /**
     * The screen methods let your record whenever a user sees a screen of your mobile app, and
     * attach a name, category or properties to the screen. Either category or name must be
     * provided.
     *
     * @param category A category to describe the screen. Deprecated.
     * @param name A name for the screen.
     * @param properties [Properties] to add extra information to this call.
     * @param options To configure the call, these override the defaultOptions, to extend use
     * #getDefaultOptions()
     * @see [Screen Documentation](https://segment.com/docs/spec/screen/)
     */
    fun screen(
        category: String?,
        name: String?,
        properties: Properties?,
        options: Options?
    ) {
        assertNotShutdown()
        require(
            !(Utils.isNullOrEmpty(category) && Utils.isNullOrEmpty(
                name
            ))
        ) { "either category or name must be provided." }
        val timestamp = NanoDate()
        analyticsExecutor!!.submit {
            val finalProperties: Properties
            finalProperties = properties ?: EMPTY_PROPERTIES
            val builder = ScreenPayload.Builder()
                .timestamp(timestamp)
                .name(name)
                .category(category)
                .properties(finalProperties)
            fillAndEnqueue(builder, options)
        }
    }

    fun setPushNotificationToken(token: String) {
        assertNotShutdown()
        require(!Utils.isNullOrEmpty(token)) { "token must not be null or empty." }
        pushToken = token
        track("snapyr.hidden.fcmTokenSet", Properties().putValue("token", token))
    }

    fun pushNotificationReceived(properties: Properties?) {
        assertNotShutdown()
        track("snapyr.observation.event.Impression", properties)
    }

    fun pushNotificationClicked(properties: Properties?) {
        assertNotShutdown()
        track("snapyr.observation.event.Behavior", properties)
    }
    /**
     * The track method is how you record any actions your users perform. Each action is known by a
     * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
     * For example a 'Purchased a Shirt' event might have properties like revenue or size.
     *
     * @param event Name of the event. Must not be null or empty.
     * @param properties [Properties] to add extra information to this call.
     * @param options To configure the call, these override the defaultOptions, to extend use
     * #getDefaultOptions()
     * @throws IllegalArgumentException if event name is null or an empty string.
     * @see [Track Documentation](https://segment.com/docs/spec/track/)
     */
    /** @see .track
     */
    /** @see .track
     */
    @JvmOverloads
    fun track(
        event: String,
        properties: Properties? = null,
        options: Options? = null
    ) {
        assertNotShutdown()
        require(!Utils.isNullOrEmpty(event)) { "event must not be null or empty." }
        val timestamp = NanoDate()
        analyticsExecutor!!.submit {
            val finalProperties: Properties
            finalProperties = properties ?: EMPTY_PROPERTIES
            val builder = TrackPayload.Builder()
                .timestamp(timestamp)
                .event(event)
                .properties(finalProperties)
            fillAndEnqueue(builder, options)
        }
    }

    /** @see .alias
     */
    fun alias(payload: AliasPayload) {
        alias(payload.userId()!!, null)
    }
    /**
     * The alias method is used to merge two user identities, effectively connecting two sets of
     * user data as one. This is an advanced method, but it is required to manage user identities
     * successfully in some of our integrations.
     *
     *
     * Usage:
     *
     * <pre> `
     * analytics.track("user did something");
     * analytics.alias(newId);
     * analytics.identify(newId);
    ` *  </pre>
     *
     * @param newId The new ID you want to alias the existing ID to. The existing ID will be either
     * the previousId if you have called identify, or the anonymous ID.
     * @param options To configure the call, these override the defaultOptions, to extend use
     * #getDefaultOptions()
     * @throws IllegalArgumentException if newId is null or empty
     * @see [Alias Documentation](https://segment.com/docs/tracking-api/alias/)
     */
    /** @see .alias
     */
    @JvmOverloads
    fun alias(newId: String, options: Options? = null) {
        assertNotShutdown()
        require(!Utils.isNullOrEmpty(newId)) { "newId must not be null or empty." }
        val timestamp = NanoDate()
        analyticsExecutor!!.submit {
            val builder = AliasPayload.Builder()
                .timestamp(timestamp)
                .userId(newId)
                .previousId(snapyrContext.traits().currentId())
            fillAndEnqueue(builder, options)
        }
    }

    private fun waitForAdvertisingId() {
        try {
            advertisingIdLatch.await(15, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.error(e, "Thread interrupted while waiting for advertising ID.")
        }
        if (advertisingIdLatch.count == 1L) {
            logger.debug(
                "Advertising ID may not be collected because the API did not respond within 15 seconds."
            )
        }
    }

    @Private
    fun fillAndEnqueue(builder: BasePayload.Builder<*, *>, options: Options?) {
        waitForAdvertisingId()

        // TODO (major version change) -> do not override, merge it with defaultOptions
        val finalOptions: Options
        finalOptions = options ?: defaultOptions

        // Create a new working copy
        var contextCopy = SnapyrContext(
            LinkedHashMap(
                snapyrContext.size
            )
        )
        contextCopy.putAll(snapyrContext)
        contextCopy.putAll(finalOptions.context())
        contextCopy = contextCopy.unmodifiableCopy()
        builder.context(contextCopy)
        builder.anonymousId(contextCopy.traits().anonymousId())
        builder.nanosecondTimestamps(nanosecondTimestamps)
        val cachedUserId = contextCopy.traits().userId()
        if (!builder.isUserIdSet && !Utils.isNullOrEmpty(cachedUserId)) {
            // userId is not set, retrieve from cached traits and set for payload
            builder.userId(cachedUserId)
        }
        val payload = builder.build()
        if (optOut.get()) {
            return
        }
        dispatchToHandler(payload)
        logger.verbose("Created payload %s.", payload)
        sendQueue.performEnqueue(payload)
    }

    private fun dispatchToHandler(payload: BasePayload) {
        if (actionHandler == null) {
            return
        }
        when (payload.type()) {
            BasePayload.Type.alias -> actionHandler.onAlias(payload as AliasPayload)
            BasePayload.Type.track -> actionHandler.onTrack(payload as TrackPayload)
            BasePayload.Type.identify -> actionHandler.onIdentify(payload as IdentifyPayload)
            BasePayload.Type.screen -> actionHandler.onScreen(payload as ScreenPayload)
            BasePayload.Type.group -> actionHandler.onGroup(payload as GroupPayload)
            else -> {}
        }
    }

    /**
     * Asynchronously flushes all messages in the queue to the server, and tells bundled
     * integrations to do the same.
     */
    fun flush() {
        check(!shutdown) { "Cannot enqueue messages after client is shutdown." }
        sendQueue.flush()
    }

    /** Creates a [StatsSnapshot] of the current stats for this instance.  */
    val snapshot: StatsSnapshot
        get() = stats.createSnapshot()

    /**
     * Return the [LogLevel] for this instance.
     *
     */
    @get:Deprecated("This will be removed in a future release.")
    val logLevel: LogLevel
        get() = logger.logLevel

    /** Return a new [Logger] with the given sub-tag.  */
    fun logger(tag: String?): Logger {
        return logger.subLog(tag)
    }

    /**
     * Logs out the current user by clearing any information, including traits and user id.
     *
     */
    @Deprecated("Use {@link #reset()} instead")
    fun logout() {
        reset()
    }

    /**
     * Resets the analytics client by clearing any stored information about the user. Events queued
     * on disk are not cleared, and will be uploaded at a later time. Preserves BUILD and VERSION
     * values.
     */
    fun reset() {
        val sharedPreferences = Utils.getSnapyrSharedPreferences(
            application, tag
        )
        // LIB-1578: only remove traits, preserve BUILD and VERSION keys in order to to fix
        // over-sending
        // of 'Application Installed' events and under-sending of 'Application Updated' events
        val editor = sharedPreferences.edit()
        editor.remove(TRAITS_KEY + "-" + tag)
        editor.apply()
        traitsCache.delete()
        traitsCache.set(Traits.create())
        snapyrContext.setTraits(traitsCache.get())
    }

    /**
     * Set the opt-out status for the current device and analytics client combination. This flag is
     * persisted across device reboots, so you can simply call this once during your application
     * (such as in a screen where a user can opt out of analytics tracking).
     */
    fun optOut(optOut: Boolean) {
        this.optOut.set(optOut)
    }

    /**
     * Stops this instance from accepting further requests. In-flight events may not be uploaded
     * right away.
     */
    fun shutdown() {
        if (this === singleton) {
            throw UnsupportedOperationException(
                "Default singleton instance cannot be shutdown."
            )
        }
        if (shutdown) {
            return
        }
        flush()
        sendQueue.shutdown()
        application!!.unregisterActivityLifecycleCallbacks(activityLifecycleCallback)
        application.unregisterActivityLifecycleCallbacks(notificationLifecycleCallbacks)
        if (useNewLifecycleMethods) {
            // only unregister if feature is enabled
            lifecycle.removeObserver(activityLifecycleCallback)
        }
        // Only supplied by us for testing, so it's ok to shut it down. If we were to make this
        // public,
        // we'll have to add a check similar to that of AnalyticsNetworkExecutorService below.
        analyticsExecutor!!.shutdown()
        (networkExecutor as? AnalyticsNetworkExecutorService)?.shutdown()
        stats.shutdown()
        shutdown = true
    }

    private fun assertNotShutdown() {
        check(!shutdown) { "Cannot enqueue messages after client is shutdown." }
    }

    private fun downloadSettings(): ProjectSettings? {
        try {
            val projectSettings = networkExecutor
                .submit(
                    Callable {
                        var connection: Client.Connection? = null
                        try {
                            connection = client.fetchSettings()
                            val map = cartographer.fromJson(
                                Utils.buffer(connection.`is`)
                            )
                            if (!map.containsKey("integrations")) {
                                map["integrations"] = ValueMap()
                                    .putValue(
                                        "Snapyr",
                                        ValueMap()
                                            .putValue(
                                                "apiKey",
                                                writeKey
                                            )
                                    )
                            }
                            if (!map.containsKey("metadata")) {
                                map["metadata"] = ValueMap()
                                    .putValue(
                                        "platform", "Android"
                                    )
                            }
                            return@Callable ProjectSettings.create(map)
                        } finally {
                            Utils.closeQuietly(connection)
                        }
                    })
                .get()
            projectSettingsCache.set(projectSettings)
            return projectSettings
        } catch (e: InterruptedException) {
            logger.error(e, "Thread interrupted while fetching settings.")
        } catch (e: ExecutionException) {
            logger.error(
                e, "Unable to fetch settings. Retrying in %s ms.", SETTINGS_RETRY_INTERVAL
            )
        }
        return null
    }

    /**
     * Retrieve settings from the cache or the network: 1. If the cache is empty, fetch new
     * settings. 2. If the cache is not stale, use it. 2. If the cache is stale, try to get new
     * settings.
     */
    @Private
    fun getSettings(force: Boolean): ProjectSettings? {
        val cachedSettings = projectSettingsCache.get()
        if (Utils.isNullOrEmpty(cachedSettings) || force) {
            return downloadSettings()
        }
        val expirationTime = cachedSettings.timestamp() + settingsRefreshInterval
        if (expirationTime > System.currentTimeMillis()) {
            return cachedSettings
        }
        val downloadedSettings = downloadSettings()
        return if (Utils.isNullOrEmpty(downloadedSettings)) {
            cachedSettings
        } else downloadedSettings
    }

    // 1 minute
    private val settingsRefreshInterval: Long
        private get() {
            var returnInterval = SETTINGS_REFRESH_INTERVAL
            if (logger.logLevel == LogLevel.DEBUG) {
                returnInterval = (60 * 1000).toLong() // 1 minute
            }
            return returnInterval
        }

    /**
     * Previously (until version 4.1.7) shared preferences were not namespaced by a tag. This meant
     * that all analytics instances shared the same shared preferences. This migration checks if the
     * namespaced shared preferences instance contains `namespaceSharedPreferences: true`. If
     * it does, the migration is already run and does not need to be run again. If it doesn't, it
     * copies the legacy shared preferences mapping into the namespaced shared preferences, and sets
     * namespaceSharedPreferences to false.
     */
    private fun namespaceSharedPreferences() {
        val newSharedPreferences = Utils.getSnapyrSharedPreferences(
            application, tag
        )
        val namespaceSharedPreferences = BooleanPreference(newSharedPreferences, "namespaceSharedPreferences", true)
        if (namespaceSharedPreferences.get()) {
            val legacySharedPreferences = application!!.getSharedPreferences("analytics-android", Context.MODE_PRIVATE)
            Utils.copySharedPreferences(legacySharedPreferences, newSharedPreferences)
            namespaceSharedPreferences.set(false)
        }
    }

    /** Controls the level of logging.  */
    enum class LogLevel {
        /** No logging.  */
        NONE,

        /** Log exceptions only.  */
        INFO,

        /** Log exceptions and print debug output.  */
        DEBUG,

        /**
         * Log exceptions and print debug output.
         *
         */
        @Deprecated("Use {@link LogLevel#DEBUG} instead.")
        BASIC,

        /** Same as [LogLevel.DEBUG], and log transformations in bundled integrations.  */
        VERBOSE;

        fun log(): Boolean {
            return this != NONE
        }
    }

    /** Fluent API for creating [Snapyr] instances.  */
    class Builder(context: Context?, writeKey: String?) {
        private val application: Application?
        private val writeKey: String?
        private var collectDeviceID = Utils.DEFAULT_COLLECT_DEVICE_ID
        private var flushQueueSize = Utils.DEFAULT_FLUSH_QUEUE_SIZE
        private var flushIntervalInMillis = Utils.DEFAULT_FLUSH_INTERVAL.toLong()
        private var defaultOptions: Options? = null
        private var tag: String? = null
        private var logLevel: LogLevel? = null
        private var networkExecutor: ExecutorService? = null
        private var executor: ExecutorService? = null
        private var connectionFactory: ConnectionFactory? = null
        private var actionHandler: SnapyrActionHandler? = null
        private var trackApplicationLifecycleEvents = false
        private var recordScreenViews = false
        private var trackDeepLinks = false
        private var snapyrPush = false
        private var nanosecondTimestamps = false
        private var crypto: Crypto? = null
        private var defaultProjectSettings = ValueMap()
        private var useNewLifecycleMethods = true // opt-out feature
        private var snapyrEnvironment = ConnectionFactory.Environment.PROD

        /** Start building a new [Snapyr] instance.  */
        init {
            requireNotNull(context) { "Context must not be null." }
            require(Utils.hasPermission(context, Manifest.permission.INTERNET)) { "INTERNET permission is required." }
            application = context.applicationContext as? Application
            requireNotNull(application) { "Application context must not be null." }
            require(!Utils.isNullOrEmpty(writeKey)) { "writeKey must not be null or empty." }
            this.writeKey = writeKey
        }

        /**
         * Set the queue size at which the client should flush events. The client will automatically
         * flush events to Snapyr when the queue reaches `flushQueueSize`.
         *
         * @throws IllegalArgumentException if the flushQueueSize is less than or equal to zero.
         */
        fun flushQueueSize(flushQueueSize: Int): Builder {
            require(flushQueueSize > 0) { "flushQueueSize must be greater than or equal to zero." }
            // 250 is a reasonably high number to trigger queue size flushes.
            // The queue may go over this size (as much as 1000), but you should flush much before
            // then.
            require(flushQueueSize <= 250) { "flushQueueSize must be less than or equal to 250." }
            this.flushQueueSize = flushQueueSize
            return this
        }

        /**
         * Set the interval at which the client should flush events. The client will automatically
         * flush events to Snapyr every `flushInterval` duration, regardless of `flushQueueSize`.
         *
         * @throws IllegalArgumentException if the flushInterval is less than or equal to zero.
         */
        fun flushInterval(flushInterval: Long, timeUnit: TimeUnit?): Builder {
            requireNotNull(timeUnit) { "timeUnit must not be null." }
            require(flushInterval > 0) { "flushInterval must be greater than zero." }
            flushIntervalInMillis = timeUnit.toMillis(flushInterval)
            return this
        }

        /**
         * Enable or disable collection of [android.provider.Settings.Secure.ANDROID_ID],
         * [android.os.Build.SERIAL] or the Telephony Identifier retrieved via
         * TelephonyManager as available. Collection of the device identifier is enabled by default.
         */
        fun collectDeviceId(collect: Boolean): Builder {
            collectDeviceID = collect
            return this
        }

        /**
         * Set some default options for all calls. This will only be used to figure out which
         * integrations should be enabled or not for actions by default.
         *
         * @see Options
         */
        fun defaultOptions(defaultOptions: Options?): Builder {
            requireNotNull(defaultOptions) { "defaultOptions must not be null." }
            // Make a defensive copy
            this.defaultOptions = Options()
            return this
        }

        /**
         * Set a tag for this instance. The tag is used to generate keys for caching. By default the
         * writeKey is used. You may want to specify an alternative one, if you want the instances
         * with the same writeKey to share different caches (you probably do).
         *
         * @throws IllegalArgumentException if the tag is null or empty.
         */
        fun tag(tag: String?): Builder {
            require(!Utils.isNullOrEmpty(tag)) { "tag must not be null or empty." }
            this.tag = tag
            return this
        }

        /** Set a [LogLevel] for this instance.  */
        fun logLevel(logLevel: LogLevel?): Builder {
            requireNotNull(logLevel) { "LogLevel must not be null." }
            this.logLevel = logLevel
            return this
        }

        /**
         * Specify the executor service for making network calls in the background.
         *
         *
         * Note: Calling [Snapyr.shutdown] will not shutdown supplied executors.
         *
         *
         * Use it with care! http://bit.ly/1JVlA2e
         */
        fun networkExecutor(networkExecutor: ExecutorService?): Builder {
            requireNotNull(networkExecutor) { "Executor service must not be null." }
            this.networkExecutor = networkExecutor
            return this
        }

        /**
         * Specify the connection factory for customizing how connections are created.
         *
         *
         * This is a beta API, and might be changed in the future. Use it with care!
         * http://bit.ly/1JVlA2e
         */
        fun connectionFactory(connectionFactory: ConnectionFactory?): Builder {
            requireNotNull(connectionFactory) { "ConnectionFactory must not be null." }
            this.connectionFactory = connectionFactory
            return this
        }

        /** Configure Snapyr to use the Snapyr dev environment - internal use only  */
        fun enableDevEnvironment(): Builder {
            snapyrEnvironment = ConnectionFactory.Environment.DEV
            return this
        }

        /**
         * Enable Snapyr's automatic push handling. This relies on Firebase Messaging and will
         * attempt to automatically register the device's Firebase token.
         */
        fun enableSnapyrPushHandling(): Builder {
            snapyrPush = true
            return this
        }

        /** Specify the crypto interface for customizing how data is stored at rest.  */
        fun crypto(crypto: Crypto?): Builder {
            requireNotNull(crypto) { "Crypto must not be null." }
            this.crypto = crypto
            return this
        }

        /**
         * Automatically track application lifecycle events, including "Application Installed",
         * "Application Updated" and "Application Opened".
         */
        fun trackApplicationLifecycleEvents(): Builder {
            trackApplicationLifecycleEvents = true
            return this
        }

        /** Automatically record screen calls when activities are created.  */
        fun recordScreenViews(): Builder {
            recordScreenViews = true
            return this
        }

        /**
         * Automatically track attribution information from enabled providers. This build option has
         * been removed. TODO (major version change)
         */
        @Deprecated("")
        fun trackAttributionInformation(): Builder {
            return this
        }

        /** Automatically track deep links as part of the screen call.  */
        fun trackDeepLinks(): Builder {
            trackDeepLinks = true
            return this
        }

        /**
         * Enable the use of nanoseconds timestamps for all payloads. Timestamps will be formatted
         * as yyyy-MM-ddThh:mm:ss.nnnnnnnnnZ Note: This is an experimental feature (and strictly
         * opt-in)
         */
        fun experimentalNanosecondTimestamps(): Builder {
            nanosecondTimestamps = true
            return this
        }

        /** Enable/Disable the use of the new Lifecycle Observer methods. Enabled by default.  */
        fun experimentalUseNewLifecycleMethods(useNewLifecycleMethods: Boolean): Builder {
            this.useNewLifecycleMethods = useNewLifecycleMethods
            return this
        }

        /**
         * Set the default project settings to use, if Snapyr.com cannot be reached. An example
         * configuration can be found here, using your write key: [
          * https://api.snapyr.com/sdk/write-key-goes-here ](https://api.snapyr.com/sdk/write-key-goes-here)
         */
        fun defaultProjectSettings(defaultProjectSettings: ValueMap?): Builder {
            this.defaultProjectSettings = requireNotNull(defaultProjectSettings) { "defaultProjectSettings == null" }
            return this
        }

        fun actionHandler(actionHandler: SnapyrActionHandler?): Builder {
            this.actionHandler = actionHandler
            return this
        }

        /**
         * The executor on which payloads are dispatched asynchronously. This is not exposed
         * publicly.
         */
        fun executor(executor: ExecutorService): Builder {
            this.executor = Utils.assertNotNull(executor, "executor")
            return this
        }

        /** Create a [Snapyr] client.  */
        fun build(): Snapyr {
            if (Utils.isNullOrEmpty(tag)) {
                tag = writeKey
            }
            if (defaultOptions == null) {
                defaultOptions = Options()
            }
            if (logLevel == null) {
                logLevel = LogLevel.NONE
            }
            if (networkExecutor == null) {
                networkExecutor = AnalyticsNetworkExecutorService()
            }
            if (connectionFactory == null) {
                connectionFactory = ConnectionFactory(snapyrEnvironment)
            }
            if (crypto == null) {
                crypto = Crypto.none()
            }
            val stats = Stats()
            val cartographer = Cartographer.INSTANCE
            val client = Client(writeKey, connectionFactory)
            val projectSettingsCache = ProjectSettings.Cache(application, cartographer, tag)
            val optOut = BooleanPreference(
                Utils.getSnapyrSharedPreferences(application, tag),
                OPT_OUT_PREFERENCE_KEY,
                false
            )
            val traitsCache = Traits.Cache(application, cartographer, tag)
            if (!traitsCache.isSet || traitsCache.get() == null) {
                val traits = Traits.create()
                traitsCache.set(traits)
            }
            val logger = Logger.with(logLevel)
            val snapyrContext = SnapyrContext.create(application, traitsCache.get(), collectDeviceID)
            val advertisingIdLatch = CountDownLatch(1)
            snapyrContext.attachAdvertisingId(application, advertisingIdLatch, logger)
            var executor = executor
            if (executor == null) {
                executor = Executors.newSingleThreadExecutor()
            }
            val lifecycle = ProcessLifecycleOwner.get().lifecycle
            return Snapyr(
                application,
                networkExecutor!!,
                stats,
                traitsCache,
                snapyrContext,
                defaultOptions!!,
                logger,
                tag,
                client,
                cartographer,
                projectSettingsCache,
                writeKey,
                flushQueueSize,
                flushIntervalInMillis,
                executor,
                trackApplicationLifecycleEvents,
                actionHandler,
                advertisingIdLatch,
                recordScreenViews,
                trackDeepLinks,
                optOut,
                crypto,
                defaultProjectSettings,
                lifecycle,
                nanosecondTimestamps,
                useNewLifecycleMethods,
                snapyrPush
            )
        }
    }

    companion object {
        @JvmField
        val HANDLER: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                throw AssertionError("Unknown handler message received: " + msg.what)
            }
        }

        @Private
        val OPT_OUT_PREFERENCE_KEY = "opt-out"
        const val WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key"

        @Private
        val EMPTY_PROPERTIES = Properties()
        private const val VERSION_KEY = "version"
        private const val BUILD_KEY = "build"
        private const val TRAITS_KEY = "traits"

        // Handler Logic.
        private const val SETTINGS_REFRESH_INTERVAL = (1000 * 60 * 60 * 24 // 24 hours
                ).toLong()
        private const val SETTINGS_RETRY_INTERVAL = (1000 * 60 // 1 minute
                ).toLong()

        /* This is intentional since we're only using the application context. */
        @SuppressLint("StaticFieldLeak")
        @Volatile
        var singleton: Snapyr? = null

        /**
         * Return a reference to the global default [Snapyr] instance.
         *
         *
         * This instance is automatically initialized with defaults that are suitable to most
         * implementations.
         *
         *
         * If these settings do not meet the requirements of your application, you can override
         * defaults in `analytics.xml`, or you can construct your own instance with full control
         * over the configuration by using [Builder].
         *
         *
         * By default, events are uploaded every 30 seconds, or every 20 events (whichever occurs
         * first), and debugging is disabled.
         */
        @JvmStatic
        fun with(context: Context?): Snapyr {
            if (singleton == null) {
                requireNotNull(context) { "Context must not be null." }
                synchronized(Snapyr::class.java) {
                    if (singleton == null) {
                        val writeKey = Utils.getResourceString(context, WRITE_KEY_RESOURCE_IDENTIFIER)
                        val builder = Builder(context, writeKey)
                        try {
                            val packageName = context.packageName
                            val flags = context.packageManager
                                .getApplicationInfo(packageName, 0).flags
                            val debugging = flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                            if (debugging) {
                                builder.logLevel(LogLevel.INFO)
                            }
                        } catch (ignored: PackageManager.NameNotFoundException) {
                        }
                        singleton = builder.build()
                    }
                }
            }
            return singleton!!
        }

        fun Valid(): Boolean {
            return singleton != null
        }

        /**
         * Set the global instance returned from [.with].
         *
         *
         * This method must be called before any calls to [.with] and may only be called once.
         */
        fun clearSingleton() {
            synchronized(Snapyr::class.java) { singleton = null }
        }

        /**
         * Set the global instance returned from [.with].
         *
         *
         * This method must be called before any calls to [.with] and may only be called once.
         */
        @JvmStatic
        fun setSingletonInstance(analytics: Snapyr?) {
            synchronized(Snapyr::class.java) {
                check(singleton == null) { "Singleton instance already exists." }
                singleton = analytics
            }
        }

        fun getPackageInfo(context: Context?): PackageInfo {
            val packageManager = context!!.packageManager
            return try {
                packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                throw AssertionError("Package not found: " + context.packageName)
            }
        }
    }
}