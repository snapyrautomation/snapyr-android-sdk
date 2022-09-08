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

import static com.snapyr.sdk.internal.Utils.isNullOrEmpty;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.snapyr.sdk.http.BatchUploadQueue;
import com.snapyr.sdk.http.ConnectionFactory;
import com.snapyr.sdk.http.SettingsRequest;
import com.snapyr.sdk.inapp.InAppConfig;
import com.snapyr.sdk.inapp.InAppFacade;
import com.snapyr.sdk.internal.AliasPayload;
import com.snapyr.sdk.internal.BasePayload;
import com.snapyr.sdk.internal.GroupPayload;
import com.snapyr.sdk.internal.IdentifyPayload;
import com.snapyr.sdk.internal.NanoDate;
import com.snapyr.sdk.internal.Private;
import com.snapyr.sdk.internal.PushTemplate;
import com.snapyr.sdk.internal.ScreenPayload;
import com.snapyr.sdk.internal.TrackPayload;
import com.snapyr.sdk.internal.Utils;
import com.snapyr.sdk.notifications.SnapyrNotificationHandler;
import com.snapyr.sdk.notifications.SnapyrNotificationLifecycleCallbacks;
import com.snapyr.sdk.services.Cartographer;
import com.snapyr.sdk.services.Crypto;
import com.snapyr.sdk.services.Logger;
import com.snapyr.sdk.services.ServiceFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The entry point into the Snapyr for Android SDK.
 *
 * <p>The idea is simple: one pipeline for all your data. Snapyr is the single hub to collect,
 * translate and route your data with the flip of a switch.
 *
 * <p>Analytics for Android will automatically batch events, queue them to disk, and upload it
 * periodically to Snapyr for you. It will also look up your project's settings (that you've
 * configured in the web interface), specifically looking up settings for bundled integrations, and
 * then initialize them for you on the user's phone, and mapping our standardized events to formats
 * they can all understand. You only need to instrument Snapyr once, then flip a switch to install
 * new tools.
 *
 * <p>This class is the main entry point into the client API. Use {@link
 * #with(android.content.Context)} for the global singleton instance or construct your own instance
 * with {@link Builder}.
 *
 * @see <a href="https://snapyr.com/">Snapyr</a>
 */
public class Snapyr {
    static final Handler HANDLER =
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    throw new AssertionError("Unknown handler message received: " + msg.what);
                }
            };

    @Private static final String OPT_OUT_PREFERENCE_KEY = "opt-out";
    static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
    @Private static final Properties EMPTY_PROPERTIES = new Properties();
    private static final String VERSION_KEY = "version";
    private static final String BUILD_KEY = "build";
    private static final String TRAITS_KEY = "traits";
    // Handler Logic.
    private static final long SETTINGS_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
    private static final long SETTINGS_RETRY_INTERVAL = 1000 * 60; // 1 minute
    /* This is intentional since we're only using the application context. */
    @SuppressLint("StaticFieldLeak")
    static volatile Snapyr singleton = null;

    @Private final Options defaultOptions;
    @Private final Traits.Cache traitsCache;
    final String tag;
    final Cartographer cartographer;
    @Private final SnapyrActivityLifecycleCallbacks activityLifecycleCallback;
    @Private final SnapyrNotificationLifecycleCallbacks notificationLifecycleCallbacks;
    @Private final Lifecycle lifecycle;
    @Private final String writeKey;
    final int flushQueueSize;
    final long flushIntervalInMillis;
    @Private final boolean nanosecondTimestamps;
    @Private final boolean useNewLifecycleMethods;
    private final ProjectSettings.Cache projectSettingsCache;
    // Retrieving the advertising ID is asynchronous. This latch helps us wait to ensure the
    // advertising ID is ready.
    private final CountDownLatch advertisingIdLatch;
    private final ExecutorService analyticsExecutor;
    private final BooleanPreference optOut;
    ProjectSettings projectSettings; // todo: make final (non-final for testing).
    volatile boolean shutdown;
    private SnapyrNotificationHandler notificationHandler;
    private String pushToken;
    private Map<String, PushTemplate> PushTemplates;
    final BatchUploadQueue sendQueue;

    Snapyr(
            Application application,
            ExecutorService networkExecutor,
            Traits.Cache traitsCache,
            SnapyrContext snapyrContext,
            Options defaultOptions,
            @NonNull Logger logger,
            String tag,
            Cartographer cartographer,
            ProjectSettings.Cache projectSettingsCache,
            String writeKey,
            ConnectionFactory.Environment environment,
            int flushQueueSize,
            long flushIntervalInMillis,
            final ExecutorService analyticsExecutor,
            final boolean shouldTrackApplicationLifecycleEvents,
            CountDownLatch advertisingIdLatch,
            final boolean shouldRecordScreenViews,
            final boolean trackDeepLinks,
            BooleanPreference optOut,
            Crypto crypto,
            @NonNull final ValueMap defaultProjectSettings,
            @NonNull Lifecycle lifecycle,
            boolean nanosecondTimestamps,
            boolean useNewLifecycleMethods,
            boolean enableSnapyrPushHandling,
            InAppConfig inAppConfig) {

        // setup the references to the static things used everywhere
        ServiceFacade.getInstance()
                .setNetworkExecutor(networkExecutor)
                .setApplication(application)
                .setLogger(logger)
                .setCartographer(cartographer)
                .setConnectionFactory(new ConnectionFactory(writeKey, environment))
                .setSnapyrContext(snapyrContext)
                .setCrypto(crypto);

        this.traitsCache = traitsCache;
        this.defaultOptions = defaultOptions;
        this.tag = tag;
        this.cartographer = cartographer;
        this.projectSettingsCache = projectSettingsCache;
        this.writeKey = writeKey;
        this.flushQueueSize = flushQueueSize;
        this.flushIntervalInMillis = flushIntervalInMillis;
        this.advertisingIdLatch = advertisingIdLatch;
        this.optOut = optOut;
        this.analyticsExecutor = analyticsExecutor;
        this.lifecycle = lifecycle;
        this.nanosecondTimestamps = nanosecondTimestamps;
        this.useNewLifecycleMethods = useNewLifecycleMethods;
        this.PushTemplates = null;
        this.sendQueue =
                new BatchUploadQueue(application, flushIntervalInMillis, flushQueueSize, null);

        namespaceSharedPreferences();

        analyticsExecutor.submit(
                new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        RefreshConfiguration(false);
                        if (isNullOrEmpty(projectSettings)) {
                            // Backup mode - Enable the Snapyr integration and load the provided
                            // defaultProjectSettings

                            if (!defaultProjectSettings.containsKey("integrations")) {
                                defaultProjectSettings.put("integrations", new ValueMap());
                            }
                            if (!defaultProjectSettings
                                    .getValueMap("integrations")
                                    .containsKey("Snapyr")) {
                                defaultProjectSettings
                                        .getValueMap("integrations")
                                        .put("Snapyr", new ValueMap());
                            }
                            if (!defaultProjectSettings
                                    .getValueMap("integrations")
                                    .getValueMap("Snapyr")
                                    .containsKey("apiKey")) {
                                defaultProjectSettings
                                        .getValueMap("integrations")
                                        .getValueMap("Snapyr")
                                        .putValue("apiKey", Snapyr.this.writeKey);
                            }
                            if (!defaultProjectSettings.containsKey("metadata")) {
                                defaultProjectSettings.put("metadata", new ValueMap());
                            }
                            if (!defaultProjectSettings
                                    .getValueMap("metadata")
                                    .containsKey("platform")) {
                                defaultProjectSettings
                                        .getValueMap("metadata")
                                        .put("platform", "Android");
                            }
                            projectSettings = ProjectSettings.create(defaultProjectSettings);
                        }
                    }
                });

        logger.debug("Created analytics client for project with tag:%s.", tag);

        activityLifecycleCallback =
                new SnapyrActivityLifecycleCallbacks.Builder()
                        .snapyr(this)
                        .analyticsExecutor(analyticsExecutor)
                        .shouldTrackApplicationLifecycleEvents(
                                shouldTrackApplicationLifecycleEvents)
                        .trackDeepLinks(trackDeepLinks)
                        .shouldRecordScreenViews(shouldRecordScreenViews)
                        .packageInfo(getPackageInfo(application))
                        .useNewLifecycleMethods(useNewLifecycleMethods)
                        .build();

        application.registerActivityLifecycleCallbacks(activityLifecycleCallback);
        if (useNewLifecycleMethods) {
            analyticsExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            HANDLER.post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            lifecycle.addObserver(activityLifecycleCallback);
                                        }
                                    });
                        }
                    });
        }

        notificationLifecycleCallbacks =
                new SnapyrNotificationLifecycleCallbacks(
                        this, ServiceFacade.getLogger(), trackDeepLinks);
        application.registerActivityLifecycleCallbacks(notificationLifecycleCallbacks);

        if (inAppConfig != null) {
            InAppFacade.allowInApp();
            InAppFacade.createInApp(inAppConfig, application);
        }

        if (enableSnapyrPushHandling) {
            this.notificationHandler = new SnapyrNotificationHandler(application);
            notificationHandler.autoRegisterFirebaseToken(this);

            // Add lifecycle callback observer so we can track user behavior on notifications
            // (i.e. tapping a notification or tapping an action button on notification)

            analyticsExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            HANDLER.post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            lifecycle.addObserver(notificationLifecycleCallbacks);
                                        }
                                    });
                        }
                    });
        }
    }

    /**
     * Return a reference to the global default {@link Snapyr} instance.
     *
     * <p>This instance is automatically initialized with defaults that are suitable to most
     * implementations.
     *
     * <p>If these settings do not meet the requirements of your application, you can override
     * defaults in {@code analytics.xml}, or you can construct your own instance with full control
     * over the configuration by using {@link Builder}.
     *
     * <p>By default, events are uploaded every 30 seconds, or every 20 events (whichever occurs
     * first), and debugging is disabled.
     */
    public static Snapyr with(Context context) {
        if (singleton == null) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            synchronized (Snapyr.class) {
                if (singleton == null) {
                    String writeKey =
                            Utils.getResourceString(context, WRITE_KEY_RESOURCE_IDENTIFIER);
                    Builder builder = new Builder(context, writeKey);

                    try {
                        String packageName = context.getPackageName();
                        int flags =
                                context.getPackageManager()
                                        .getApplicationInfo(packageName, 0)
                                        .flags;
                        boolean debugging = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                        if (debugging) {
                            builder.logLevel(LogLevel.INFO);
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }

                    singleton = builder.build();
                }
            }
        }
        return singleton;
    }

    public static boolean Valid() {
        return singleton != null;
    }

    /**
     * Set the global instance returned from {@link #with}.
     *
     * <p>This method must be called before any calls to {@link #with} and may only be called once.
     */
    public static void clearSingleton() {
        synchronized (Snapyr.class) {
            singleton = null;
        }
    }

    /**
     * Set the global instance returned from {@link #with}.
     *
     * <p>This method must be called before any calls to {@link #with} and may only be called once.
     */
    public static void setSingletonInstance(Snapyr analytics) {
        synchronized (Snapyr.class) {
            if (singleton != null) {
                throw new IllegalStateException("Singleton instance already exists.");
            }
            singleton = analytics;
        }
    }

    static PackageInfo getPackageInfo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError("Package not found: " + context.getPackageName());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void RefreshConfiguration(boolean force) {
        ProjectSettings newSettings = getSettings(force);
        if (!isNullOrEmpty(newSettings)) {
            this.projectSettings = newSettings;
            ValueMap metadata = projectSettings.getValueMap("metadata");
            ServiceFacade.getSnapyrContext().putSdkMeta(metadata);
            this.PushTemplates = PushTemplate.ParseTemplate(metadata);
        }
    }

    public SnapyrNotificationHandler getNotificationHandler() {
        return notificationHandler;
    }

    public Map<String, PushTemplate> getPushTemplates() {
        return this.PushTemplates;
    }

    @Private
    void trackApplicationLifecycleEvents() {
        // Get the current version.
        PackageInfo packageInfo = getPackageInfo(ServiceFacade.getApplication());
        String currentVersion = packageInfo.versionName;
        int currentBuild = packageInfo.versionCode;

        // Get the previous recorded version.
        SharedPreferences sharedPreferences =
                Utils.getSnapyrSharedPreferences(ServiceFacade.getApplication(), tag);
        String previousVersion = sharedPreferences.getString(VERSION_KEY, null);
        int previousBuild = sharedPreferences.getInt(BUILD_KEY, -1);

        // Check and track Application Installed or Application Updated.
        if (previousBuild == -1) {
            track(
                    "Application Installed",
                    new Properties() //
                            .putValue(VERSION_KEY, currentVersion)
                            .putValue(BUILD_KEY, String.valueOf(currentBuild)));
        } else if (currentBuild != previousBuild) {
            track(
                    "Application Updated",
                    new Properties() //
                            .putValue(VERSION_KEY, currentVersion)
                            .putValue(BUILD_KEY, String.valueOf(currentBuild))
                            .putValue("previous_" + VERSION_KEY, previousVersion)
                            .putValue("previous_" + BUILD_KEY, String.valueOf(previousBuild)));
        }

        // Update the recorded version.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VERSION_KEY, currentVersion);
        editor.putInt(BUILD_KEY, currentBuild);
        editor.apply();
    }

    // Analytics API

    @Private
    void recordScreenViews(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        try {
            ActivityInfo info =
                    packageManager.getActivityInfo(
                            activity.getComponentName(), PackageManager.GET_META_DATA);
            CharSequence activityLabel = info.loadLabel(packageManager);
            screen(activityLabel.toString());
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError("Activity Not Found: " + e);
        } catch (Exception e) {
            ServiceFacade.getLogger()
                    .error(e, "Unable to track screen view for %s", activity.toString());
        }
    }

    /** @see #identify(String, Traits, Options) */
    public void identify(@NonNull IdentifyPayload payload) {
        identify(payload.userId(), payload.traits(), null);
    }

    /** @see #identify(String, Traits, Options) */
    public void identify(@NonNull String userId) {
        identify(userId, null, null);
    }

    /** @see #identify(String, Traits, Options) */
    public void identify(@NonNull Traits traits) {
        identify(null, traits, null);
    }

    /**
     * Identify lets you tie one of your users and their actions to a recognizable {@code userId}.
     * It also lets you record {@code traits} about the user, like their email, name, account type,
     * etc.
     *
     * <p>Traits and userId will be automatically cached and available on future sessions for the
     * same user. To update a trait on the server, call identify with the same user id (or null).
     * You can also use {@link #identify(Traits)} for this purpose.
     *
     * @param userId Unique identifier which you recognize a user by in your own database. If this
     *     is null or empty, any previous id we have (could be the anonymous id) will be used.
     * @param newTraits Traits about the user.
     * @param options To configure the call, these override the defaultOptions, to extend use
     *     #getDefaultOptions()
     * @throws IllegalArgumentException if both {@code userId} and {@code newTraits} are not
     *     provided
     * @see <a href="https://segment.com/docs/spec/identify/">Identify Documentation</a>
     */
    public void identify(
            final @Nullable String userId,
            final @Nullable Traits newTraits,
            final @Nullable Options options) {
        assertNotShutdown();
        if (Utils.isNullOrEmpty(userId) && Utils.isNullOrEmpty(newTraits)) {
            throw new IllegalArgumentException("Either userId or some traits must be provided.");
        }
        NanoDate timestamp = new NanoDate();
        analyticsExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        Traits traits = traitsCache.get();
                        if (!Utils.isNullOrEmpty(userId)) {
                            traits.putUserId(userId);
                        }
                        if (!Utils.isNullOrEmpty(newTraits)) {
                            traits.putAll(newTraits);
                        }

                        traitsCache.set(traits); // Save the new traits
                        ServiceFacade.getSnapyrContext().setTraits(traits); // Update the references

                        IdentifyPayload.Builder builder =
                                new IdentifyPayload.Builder()
                                        .timestamp(timestamp)
                                        .traits(traitsCache.get());
                        fillAndEnqueue(builder, options);
                    }
                });

        if (pushToken != null) {
            track("snapyr.hidden.fcmTokenSet", new Properties().putValue("token", pushToken));
        }
    }

    /** @see #group(String, Traits, Options) */
    public void group(@NonNull GroupPayload payload) {
        group(payload.groupId(), payload.traits(), null);
    }

    /** @see #group(String, Traits, Options) */
    public void group(@NonNull String groupId) {
        group(groupId, null, null);
    }

    /** @see #group(String, Traits, Options) */
    public void group(@NonNull String groupId, @Nullable Traits traits) {
        group(groupId, traits, null);
    }

    /**
     * The group method lets you associate a user with a group. It also lets you record custom
     * traits about the group, like industry or number of employees.
     *
     * <p>If you've called {@link #identify(String, Traits, Options)} before, this will
     * automatically remember the userId. If not, it will fall back to use the anonymousId instead.
     *
     * @param groupId Unique identifier which you recognize a group by in your own database. Must
     *     not be null or empty.
     * @param options To configure the call, these override the defaultOptions, to extend use
     *     #getDefaultOptions()
     * @throws IllegalArgumentException if groupId is null or an empty string.
     * @see <a href="https://segment.com/docs/spec/group/">Group Documentation</a>
     */
    public void group(
            @NonNull final String groupId,
            @Nullable final Traits groupTraits,
            @Nullable final Options options) {
        assertNotShutdown();
        if (Utils.isNullOrEmpty(groupId)) {
            throw new IllegalArgumentException("groupId must not be null or empty.");
        }
        NanoDate timestamp = new NanoDate();
        analyticsExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        final Traits finalGroupTraits;
                        if (groupTraits == null) {
                            finalGroupTraits = new Traits();
                        } else {
                            finalGroupTraits = groupTraits;
                        }

                        GroupPayload.Builder builder =
                                new GroupPayload.Builder()
                                        .timestamp(timestamp)
                                        .groupId(groupId)
                                        .traits(finalGroupTraits);
                        fillAndEnqueue(builder, options);
                    }
                });
    }

    /** @see #track(String, Properties, Options) */
    public void track(@NonNull TrackPayload payload) {
        track(payload.userId(), payload.properties(), null);
    }

    /** @see #track(String, Properties, Options) */
    public void track(@NonNull String event) {
        track(event, null, null);
    }

    /** @see #track(String, Properties, Options) */
    public void track(@NonNull String event, @Nullable Properties properties) {
        track(event, properties, null);
    }

    /**
     * @see #screen(String, String, Properties, Options)
     * @deprecated Use {@link #screen(String)} instead.
     */
    public void screen(ScreenPayload payload) {
        screen(payload.category(), payload.name(), payload.properties(), null);
    }

    /**
     * @see #screen(String, String, Properties, Options)
     * @deprecated Use {@link #screen(String)} instead.
     */
    public void screen(@Nullable String category, @Nullable String name) {
        screen(category, name, null, null);
    }
    /**
     * @see #screen(String, String, Properties, Options)
     * @deprecated Use {@link #screen(String, Properties)} instead.
     */
    public void screen(
            @Nullable String category, @Nullable String name, @Nullable Properties properties) {
        screen(category, name, properties, null);
    }
    /** @see #screen(String, String, Properties, Options) */
    public void screen(@Nullable String name) {
        screen(null, name, null, null);
    }
    /** @see #screen(String, String, Properties, Options) */
    public void screen(@Nullable String name, @Nullable Properties properties) {
        screen(null, name, properties, null);
    }

    /**
     * The screen methods let your record whenever a user sees a screen of your mobile app, and
     * attach a name, category or properties to the screen. Either category or name must be
     * provided.
     *
     * @param category A category to describe the screen. Deprecated.
     * @param name A name for the screen.
     * @param properties {@link Properties} to add extra information to this call.
     * @param options To configure the call, these override the defaultOptions, to extend use
     *     #getDefaultOptions()
     * @see <a href="https://segment.com/docs/spec/screen/">Screen Documentation</a>
     */
    public void screen(
            @Nullable final String category,
            @Nullable final String name,
            @Nullable final Properties properties,
            @Nullable final Options options) {
        assertNotShutdown();
        if (Utils.isNullOrEmpty(category) && Utils.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("either category or name must be provided.");
        }
        NanoDate timestamp = new NanoDate();
        analyticsExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        final Properties finalProperties;
                        if (properties == null) {
                            finalProperties = EMPTY_PROPERTIES;
                        } else {
                            finalProperties = properties;
                        }

                        //noinspection deprecation
                        ScreenPayload.Builder builder =
                                new ScreenPayload.Builder()
                                        .timestamp(timestamp)
                                        .name(name)
                                        .category(category)
                                        .properties(finalProperties);
                        fillAndEnqueue(builder, options);
                    }
                });
    }

    public void setPushNotificationToken(final @NonNull String token) {
        assertNotShutdown();
        if (Utils.isNullOrEmpty(token)) {
            throw new IllegalArgumentException("token must not be null or empty.");
        }
        this.pushToken = token;
        track("snapyr.hidden.fcmTokenSet", new Properties().putValue("token", token));
    }

    public void pushNotificationReceived(final @Nullable Properties properties) {
        assertNotShutdown();
        track("snapyr.observation.event.Impression", properties);
    }

    public void pushNotificationClicked(final @Nullable Properties properties) {
        assertNotShutdown();
        track("snapyr.observation.event.Behavior", properties);
    }

    /**
     * The track method is how you record any actions your users perform. Each action is known by a
     * name, like 'Purchased a T-Shirt'. You can also record properties specific to those actions.
     * For example a 'Purchased a Shirt' event might have properties like revenue or size.
     *
     * @param event Name of the event. Must not be null or empty.
     * @param properties {@link Properties} to add extra information to this call.
     * @param options To configure the call, these override the defaultOptions, to extend use
     *     #getDefaultOptions()
     * @throws IllegalArgumentException if event name is null or an empty string.
     * @see <a href="https://segment.com/docs/spec/track/">Track Documentation</a>
     */
    public void track(
            final @NonNull String event,
            final @Nullable Properties properties,
            @Nullable final Options options) {
        assertNotShutdown();
        if (Utils.isNullOrEmpty(event)) {
            throw new IllegalArgumentException("event must not be null or empty.");
        }
        NanoDate timestamp = new NanoDate();
        analyticsExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        final Properties finalProperties;
                        if (properties == null) {
                            finalProperties = EMPTY_PROPERTIES;
                        } else {
                            finalProperties = properties;
                        }

                        TrackPayload.Builder builder =
                                new TrackPayload.Builder()
                                        .timestamp(timestamp)
                                        .event(event)
                                        .properties(finalProperties);
                        fillAndEnqueue(builder, options);
                    }
                });
    }

    /** @see #alias(String, Options) */
    public void alias(@NonNull AliasPayload payload) {
        alias(payload.userId(), null);
    }

    /** @see #alias(String, Options) */
    public void alias(@NonNull String newId) {
        alias(newId, null);
    }

    /**
     * The alias method is used to merge two user identities, effectively connecting two sets of
     * user data as one. This is an advanced method, but it is required to manage user identities
     * successfully in some of our integrations.
     *
     * <p>Usage:
     *
     * <pre> <code>
     *   analytics.track("user did something");
     *   analytics.alias(newId);
     *   analytics.identify(newId);
     * </code> </pre>
     *
     * @param newId The new ID you want to alias the existing ID to. The existing ID will be either
     *     the previousId if you have called identify, or the anonymous ID.
     * @param options To configure the call, these override the defaultOptions, to extend use
     *     #getDefaultOptions()
     * @throws IllegalArgumentException if newId is null or empty
     * @see <a href="https://segment.com/docs/tracking-api/alias/">Alias Documentation</a>
     */
    public void alias(final @NonNull String newId, final @Nullable Options options) {
        assertNotShutdown();
        if (Utils.isNullOrEmpty(newId)) {
            throw new IllegalArgumentException("newId must not be null or empty.");
        }

        NanoDate timestamp = new NanoDate();
        analyticsExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        AliasPayload.Builder builder =
                                new AliasPayload.Builder()
                                        .timestamp(timestamp)
                                        .userId(newId)
                                        .previousId(
                                                ServiceFacade.getSnapyrContext()
                                                        .traits()
                                                        .currentId());
                        fillAndEnqueue(builder, options);
                    }
                });
    }

    private void waitForAdvertisingId() {
        try {
            advertisingIdLatch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ServiceFacade.getLogger()
                    .error(e, "Thread interrupted while waiting for advertising ID.");
        }
        if (advertisingIdLatch.getCount() == 1) {
            ServiceFacade.getLogger()
                    .debug(
                            "Advertising ID may not be collected because the API did not respond within 15 seconds.");
        }
    }

    @Private
    void fillAndEnqueue(BasePayload.Builder<?, ?> builder, Options options) {
        waitForAdvertisingId();

        // TODO (major version change) -> do not override, merge it with defaultOptions
        final Options finalOptions;
        if (options == null) {
            finalOptions = defaultOptions;
        } else {
            finalOptions = options;
        }

        // Create a new working copy
        SnapyrContext contextCopy =
                new SnapyrContext(new LinkedHashMap<>(ServiceFacade.getSnapyrContext().size()));
        contextCopy.putAll(ServiceFacade.getSnapyrContext());
        contextCopy.putAll(finalOptions.context());
        contextCopy = contextCopy.unmodifiableCopy();

        builder.context(contextCopy);
        builder.anonymousId(contextCopy.traits().anonymousId());
        builder.nanosecondTimestamps(nanosecondTimestamps);
        String cachedUserId = contextCopy.traits().userId();
        if (!builder.isUserIdSet() && !Utils.isNullOrEmpty(cachedUserId)) {
            // userId is not set, retrieve from cached traits and set for payload
            builder.userId(cachedUserId);
        }
        BasePayload payload = builder.build();
        if (optOut.get()) {
            return;
        }

        ServiceFacade.getLogger().verbose("Created payload %s.", payload);
        this.sendQueue.performEnqueue(payload);
    }

    /**
     * Asynchronously flushes all messages in the queue to the server, and tells bundled
     * integrations to do the same.
     */
    public void flush() {
        if (shutdown) {
            throw new IllegalStateException("Cannot enqueue messages after client is shutdown.");
        }
        sendQueue.flush();
    }

    /** Get the {@link SnapyrContext} used by this instance. */
    @SuppressWarnings("UnusedDeclaration")
    public SnapyrContext getSnapyrContext() {
        // TODO (major version change) hide internals (don't give out working copy), expose a better
        // API
        //  for modifying the global context
        return ServiceFacade.getSnapyrContext();
    }

    /**
     * Logs out the current user by clearing any information, including traits and user id.
     *
     * @deprecated Use {@link #reset()} instead
     */
    @Deprecated
    public void logout() {
        reset();
    }

    /**
     * Resets the analytics client by clearing any stored information about the user. Events queued
     * on disk are not cleared, and will be uploaded at a later time. Preserves BUILD and VERSION
     * values.
     */
    public void reset() {
        SharedPreferences sharedPreferences =
                Utils.getSnapyrSharedPreferences(ServiceFacade.getApplication(), tag);
        // LIB-1578: only remove traits, preserve BUILD and VERSION keys in order to to fix
        // over-sending
        // of 'Application Installed' events and under-sending of 'Application Updated' events
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(TRAITS_KEY + "-" + tag);
        editor.apply();

        traitsCache.delete();
        traitsCache.set(Traits.create());
        ServiceFacade.getSnapyrContext().setTraits(traitsCache.get());
    }

    /**
     * Set the opt-out status for the current device and analytics client combination. This flag is
     * persisted across device reboots, so you can simply call this once during your application
     * (such as in a screen where a user can opt out of analytics tracking).
     */
    public void optOut(boolean optOut) {
        this.optOut.set(optOut);
    }

    /**
     * Stops this instance from accepting further requests. In-flight events may not be uploaded
     * right away.
     */
    public void shutdown() {
        if (this == singleton) {
            throw new UnsupportedOperationException(
                    "Default singleton instance cannot be shutdown.");
        }
        if (shutdown) {
            return;
        }

        flush();
        sendQueue.shutdown();
        Application application = ServiceFacade.getApplication();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallback);
        application.unregisterActivityLifecycleCallbacks(notificationLifecycleCallbacks);
        if (useNewLifecycleMethods) {
            // only unregister if feature is enabled
            analyticsExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            HANDLER.post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            lifecycle.removeObserver(activityLifecycleCallback);
                                        }
                                    });
                        }
                    });
        }
        // Only supplied by us for testing, so it's ok to shut it down. If we were to make this
        // public,
        // we'll have to add a check similar to that of AnalyticsNetworkExecutorService below.
        analyticsExecutor.shutdown();
        ExecutorService networkExecutor = ServiceFacade.getNetworkExecutor();
        if (networkExecutor instanceof Utils.AnalyticsNetworkExecutorService) {
            networkExecutor.shutdown();
        }
        shutdown = true;
    }

    private void assertNotShutdown() {
        if (shutdown) {
            throw new IllegalStateException("Cannot enqueue messages after client is shutdown.");
        }
    }

    private ProjectSettings downloadSettings() {
        try {
            ProjectSettings projectSettings =
                    ServiceFacade.getNetworkExecutor()
                            .submit(
                                    new Callable<ProjectSettings>() {
                                        @Override
                                        public ProjectSettings call() throws Exception {
                                            Map<String, Object> settings =
                                                    SettingsRequest.execute();
                                            if (!settings.containsKey("integrations")) {
                                                settings.put(
                                                        "integrations",
                                                        new ValueMap()
                                                                .putValue(
                                                                        "Snapyr",
                                                                        new ValueMap()
                                                                                .putValue(
                                                                                        "apiKey",
                                                                                        writeKey)));
                                            }
                                            if (!settings.containsKey("metadata")) {
                                                settings.put(
                                                        "metadata",
                                                        new ValueMap()
                                                                .putValue("platform", "Android"));
                                            }
                                            return ProjectSettings.create(settings);
                                        }
                                    })
                            .get();
            projectSettingsCache.set(projectSettings);
            return projectSettings;
        } catch (InterruptedException e) {
            ServiceFacade.getLogger().error(e, "Thread interrupted while fetching settings.");
        } catch (ExecutionException e) {
            ServiceFacade.getLogger()
                    .error(
                            e,
                            "Unable to fetch settings. Retrying in %s ms.",
                            SETTINGS_RETRY_INTERVAL);
        }
        return null;
    }

    /**
     * Retrieve settings from the cache or the network: 1. If the cache is empty, fetch new
     * settings. 2. If the cache is not stale, use it. 2. If the cache is stale, try to get new
     * settings.
     */
    @Private
    ProjectSettings getSettings(boolean force) {
        ProjectSettings cachedSettings = projectSettingsCache.get();
        if (isNullOrEmpty(cachedSettings) || force) {
            return downloadSettings();
        }

        long expirationTime = cachedSettings.timestamp() + getSettingsRefreshInterval();
        if (expirationTime > System.currentTimeMillis()) {
            return cachedSettings;
        }

        ProjectSettings downloadedSettings = downloadSettings();
        if (isNullOrEmpty(downloadedSettings)) {
            return cachedSettings;
        }
        return downloadedSettings;
    }

    private long getSettingsRefreshInterval() {
        long returnInterval = SETTINGS_REFRESH_INTERVAL;
        if (ServiceFacade.getLogger().logLevel == LogLevel.DEBUG) {
            returnInterval = 60 * 1000; // 1 minute
        }
        return returnInterval;
    }

    /**
     * Previously (until version 4.1.7) shared preferences were not namespaced by a tag. This meant
     * that all analytics instances shared the same shared preferences. This migration checks if the
     * namespaced shared preferences instance contains {@code namespaceSharedPreferences: true}. If
     * it does, the migration is already run and does not need to be run again. If it doesn't, it
     * copies the legacy shared preferences mapping into the namespaced shared preferences, and sets
     * namespaceSharedPreferences to false.
     */
    private void namespaceSharedPreferences() {
        Application application = ServiceFacade.getApplication();
        SharedPreferences newSharedPreferences = Utils.getSnapyrSharedPreferences(application, tag);
        BooleanPreference namespaceSharedPreferences =
                new BooleanPreference(newSharedPreferences, "namespaceSharedPreferences", true);

        if (namespaceSharedPreferences.get()) {
            SharedPreferences legacySharedPreferences =
                    application.getSharedPreferences("analytics-android", Context.MODE_PRIVATE);
            Utils.copySharedPreferences(legacySharedPreferences, newSharedPreferences);
            namespaceSharedPreferences.set(false);
        }
    }

    /** Controls the level of logging. */
    public enum LogLevel {
        /** No logging. */
        NONE,
        /** Log exceptions only. */
        INFO,
        /** Log exceptions and print debug output. */
        DEBUG,
        /**
         * Log exceptions and print debug output.
         *
         * @deprecated Use {@link LogLevel#DEBUG} instead.
         */
        @Deprecated
        BASIC,
        /** Same as {@link LogLevel#DEBUG}, and log transformations in bundled integrations. */
        VERBOSE;

        public boolean log() {
            return this != NONE;
        }
    }

    /** Fluent API for creating {@link Snapyr} instances. */
    public static class Builder {

        private final Application application;
        private final String writeKey;
        private boolean collectDeviceID = Utils.DEFAULT_COLLECT_DEVICE_ID;
        private int flushQueueSize = Utils.DEFAULT_FLUSH_QUEUE_SIZE;
        private long flushIntervalInMillis = Utils.DEFAULT_FLUSH_INTERVAL;
        private Options defaultOptions;
        private String tag;
        private LogLevel logLevel;
        private ExecutorService networkExecutor;
        private ExecutorService executor;
        private boolean trackApplicationLifecycleEvents = false;
        private boolean recordScreenViews = false;
        private boolean trackDeepLinks = false;
        private boolean snapyrPushEnabled = false;
        private InAppConfig snapyrInAppConfig = null;
        private boolean nanosecondTimestamps = false;
        private Crypto crypto;
        private ValueMap defaultProjectSettings = new ValueMap();
        private boolean useNewLifecycleMethods = true; // opt-out feature
        private ConnectionFactory.Environment snapyrEnvironment =
                ConnectionFactory.Environment.PROD;

        /** Start building a new {@link Snapyr} instance. */
        public Builder(Context context, String writeKey) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            if (!Utils.hasPermission(context, Manifest.permission.INTERNET)) {
                throw new IllegalArgumentException("INTERNET permission is required.");
            }
            application = (Application) context.getApplicationContext();
            if (application == null) {
                throw new IllegalArgumentException("Application context must not be null.");
            }

            if (Utils.isNullOrEmpty(writeKey)) {
                throw new IllegalArgumentException("writeKey must not be null or empty.");
            }
            this.writeKey = writeKey;
        }

        /**
         * Set the queue size at which the client should flush events. The client will automatically
         * flush events to Snapyr when the queue reaches {@code flushQueueSize}.
         *
         * @throws IllegalArgumentException if the flushQueueSize is less than or equal to zero.
         */
        public Builder flushQueueSize(int flushQueueSize) {
            if (flushQueueSize <= 0) {
                throw new IllegalArgumentException("flushQueueSize must be greater than zero.");
            }
            // 250 is a reasonably high number to trigger queue size flushes.
            // The queue may go over this size (as much as 1000), but you should flush much before
            // then.
            if (flushQueueSize > 250) {
                throw new IllegalArgumentException(
                        "flushQueueSize must be less than or equal to 250.");
            }
            this.flushQueueSize = flushQueueSize;
            return this;
        }

        /**
         * Set the interval at which the client should flush events. The client will automatically
         * flush events to Snapyr every {@code flushInterval} duration, regardless of {@code
         * flushQueueSize}.
         *
         * @throws IllegalArgumentException if the flushInterval is less than or equal to zero.
         */
        public Builder flushInterval(long flushInterval, TimeUnit timeUnit) {
            if (timeUnit == null) {
                throw new IllegalArgumentException("timeUnit must not be null.");
            }
            if (flushInterval <= 0) {
                throw new IllegalArgumentException("flushInterval must be greater than zero.");
            }
            this.flushIntervalInMillis = timeUnit.toMillis(flushInterval);
            return this;
        }

        /**
         * Enable or disable collection of {@link android.provider.Settings.Secure#ANDROID_ID},
         * {@link android.os.Build#SERIAL} or the Telephony Identifier retrieved via
         * TelephonyManager as available. Collection of the device identifier is enabled by default.
         */
        public Builder collectDeviceId(boolean collect) {
            this.collectDeviceID = collect;
            return this;
        }

        /**
         * Set some default options for all calls. This will only be used to figure out which
         * integrations should be enabled or not for actions by default.
         *
         * @see Options
         */
        public Builder defaultOptions(Options defaultOptions) {
            if (defaultOptions == null) {
                throw new IllegalArgumentException("defaultOptions must not be null.");
            }
            // Make a defensive copy
            this.defaultOptions = new Options();
            return this;
        }

        /**
         * Set a tag for this instance. The tag is used to generate keys for caching. By default the
         * writeKey is used. You may want to specify an alternative one, if you want the instances
         * with the same writeKey to share different caches (you probably do).
         *
         * @throws IllegalArgumentException if the tag is null or empty.
         */
        public Builder tag(String tag) {
            if (Utils.isNullOrEmpty(tag)) {
                throw new IllegalArgumentException("tag must not be null or empty.");
            }
            this.tag = tag;
            return this;
        }

        /** Set a {@link LogLevel} for this instance. */
        public Builder logLevel(LogLevel logLevel) {
            if (logLevel == null) {
                throw new IllegalArgumentException("LogLevel must not be null.");
            }
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Specify the executor service for making network calls in the background.
         *
         * <p>Note: Calling {@link Snapyr#shutdown()} will not shutdown supplied executors.
         *
         * <p>Use it with care! http://bit.ly/1JVlA2e
         */
        public Builder networkExecutor(ExecutorService networkExecutor) {
            if (networkExecutor == null) {
                throw new IllegalArgumentException("Executor service must not be null.");
            }
            this.networkExecutor = networkExecutor;
            return this;
        }

        /** Configure Snapyr to use the Snapyr dev environment - internal use only */
        public Builder enableDevEnvironment() {
            this.snapyrEnvironment = ConnectionFactory.Environment.DEV;
            return this;
        }

        /** Configure Snapyr to use the Snapyr Stage environment - internal use only */
        public Builder enableStageEnvironment() {
            this.snapyrEnvironment = ConnectionFactory.Environment.STAGE;
            return this;
        }

        /**
         * Enable Snapyr's automatic push handling. This relies on Firebase Messaging and will
         * attempt to automatically register the device's Firebase token.
         */
        public Builder enableSnapyrPushHandling() {
            this.snapyrPushEnabled = true;
            return this;
        }

        /** Enable Snapyr's in-app handling with the passed configuration */
        public Builder configureInAppHandling(InAppConfig config) {
            this.snapyrInAppConfig = config;
            return this;
        }

        /** Specify the crypto interface for customizing how data is stored at rest. */
        public Builder crypto(Crypto crypto) {
            if (crypto == null) {
                throw new IllegalArgumentException("Crypto must not be null.");
            }
            this.crypto = crypto;
            return this;
        }

        /**
         * Automatically track application lifecycle events, including "Application Installed",
         * "Application Updated" and "Application Opened".
         */
        public Builder trackApplicationLifecycleEvents() {
            this.trackApplicationLifecycleEvents = true;
            return this;
        }

        /** Automatically record screen calls when activities are created. */
        public Builder recordScreenViews() {
            this.recordScreenViews = true;
            return this;
        }

        /**
         * Automatically track attribution information from enabled providers. This build option has
         * been removed. TODO (major version change)
         */
        @Deprecated
        public Builder trackAttributionInformation() {
            return this;
        }

        /** Automatically track deep links as part of the screen call. */
        public Builder trackDeepLinks() {
            this.trackDeepLinks = true;
            return this;
        }

        /**
         * Enable the use of nanoseconds timestamps for all payloads. Timestamps will be formatted
         * as yyyy-MM-ddThh:mm:ss.nnnnnnnnnZ Note: This is an experimental feature (and strictly
         * opt-in)
         */
        public Builder experimentalNanosecondTimestamps() {
            this.nanosecondTimestamps = true;
            return this;
        }

        /** Enable/Disable the use of the new Lifecycle Observer methods. Enabled by default. */
        public Builder experimentalUseNewLifecycleMethods(boolean useNewLifecycleMethods) {
            this.useNewLifecycleMethods = useNewLifecycleMethods;
            return this;
        }

        /**
         * Set the default project settings to use, if Snapyr.com cannot be reached. An example
         * configuration can be found here, using your write key: <a
         * href="https://api.snapyr.com/sdk/write-key-goes-here">
         * https://api.snapyr.com/sdk/write-key-goes-here </a>
         */
        public Builder defaultProjectSettings(ValueMap defaultProjectSettings) {
            Utils.assertNotNull(defaultProjectSettings, "defaultProjectSettings");
            this.defaultProjectSettings = defaultProjectSettings;
            return this;
        }

        /**
         * The executor on which payloads are dispatched asynchronously. This is not exposed
         * publicly.
         */
        Builder executor(ExecutorService executor) {
            this.executor = Utils.assertNotNull(executor, "executor");
            return this;
        }

        /** Create a {@link Snapyr} client. */
        public Snapyr build() {
            if (Utils.isNullOrEmpty(tag)) {
                tag = writeKey;
            }
            if (defaultOptions == null) {
                defaultOptions = new Options();
            }
            if (logLevel == null) {
                logLevel = LogLevel.NONE;
            }
            if (networkExecutor == null) {
                networkExecutor = new Utils.AnalyticsNetworkExecutorService();
            }
            if (crypto == null) {
                crypto = Crypto.none();
            }

            final Cartographer cartographer = Cartographer.INSTANCE;

            ProjectSettings.Cache projectSettingsCache =
                    new ProjectSettings.Cache(application, cartographer, tag);

            BooleanPreference optOut =
                    new BooleanPreference(
                            Utils.getSnapyrSharedPreferences(application, tag),
                            OPT_OUT_PREFERENCE_KEY,
                            false);

            Traits.Cache traitsCache = new Traits.Cache(application, cartographer, tag);
            if (!traitsCache.isSet() || traitsCache.get() == null) {
                Traits traits = Traits.create();
                traitsCache.set(traits);
            }

            Logger logger = Logger.with(logLevel);
            SnapyrContext snapyrContext =
                    SnapyrContext.create(application, traitsCache.get(), collectDeviceID);
            CountDownLatch advertisingIdLatch = new CountDownLatch(1);
            snapyrContext.attachAdvertisingId(application, advertisingIdLatch, logger);

            ExecutorService executor = this.executor;
            if (executor == null) {
                executor = Executors.newSingleThreadExecutor();
            }

            Lifecycle lifecycle = ProcessLifecycleOwner.get().getLifecycle();
            return new Snapyr(
                    application,
                    networkExecutor,
                    traitsCache,
                    snapyrContext,
                    defaultOptions,
                    logger,
                    tag,
                    cartographer,
                    projectSettingsCache,
                    writeKey,
                    snapyrEnvironment,
                    flushQueueSize,
                    flushIntervalInMillis,
                    executor,
                    trackApplicationLifecycleEvents,
                    advertisingIdLatch,
                    recordScreenViews,
                    trackDeepLinks,
                    optOut,
                    crypto,
                    defaultProjectSettings,
                    lifecycle,
                    nanosecondTimestamps,
                    useNewLifecycleMethods,
                    snapyrPushEnabled,
                    snapyrInAppConfig);
        }
    }
}
