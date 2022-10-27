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

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.snapyr.sdk.ProjectSettings.create
import com.snapyr.sdk.TestUtils.NoDescriptionMatcher
import com.snapyr.sdk.TestUtils.grantPermission
import com.snapyr.sdk.TestUtils.mockApplication
import com.snapyr.sdk.http.ConnectionFactory
import com.snapyr.sdk.http.SettingsRequest
import com.snapyr.sdk.internal.TrackPayload
import com.snapyr.sdk.internal.Utils.AnalyticsNetworkExecutorService
import com.snapyr.sdk.internal.Utils.DEFAULT_FLUSH_INTERVAL
import com.snapyr.sdk.internal.Utils.DEFAULT_FLUSH_QUEUE_SIZE
import com.snapyr.sdk.internal.Utils.isNullOrEmpty
import com.snapyr.sdk.services.Cartographer
import com.snapyr.sdk.services.Crypto
import com.snapyr.sdk.services.Logger
import com.snapyr.sdk.services.ServiceFacade
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.MapEntry
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.initMocks
import org.mockito.Spy
import org.mockito.hamcrest.MockitoHamcrest.argThat
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
open class SnapyrTest {
    private val SETTINGS =
        """
            |{
            |  "integrations": {
            |    "test": { 
            |      "foo": "bar"
            |    }
            |  },
            | "plan": {
            |
            |  }
            |}
            """.trimMargin()

    @Mock
    private lateinit var traitsCache: Traits.Cache

    @Spy
    private lateinit var networkExecutor: AnalyticsNetworkExecutorService

    @Spy
    private var analyticsExecutor: ExecutorService = TestUtils.SynchronousExecutor()

    @Mock
    private lateinit var projectSettingsCache: ProjectSettings.Cache

    @Mock
    private lateinit var crpyto: Crypto

    @Mock
    private lateinit var connectionFactory: ConnectionFactory

    @Mock
    lateinit var lifecycle: Lifecycle
    private lateinit var defaultOptions: Options
    private lateinit var optOut: BooleanPreference
    private lateinit var application: Application
    private lateinit var traits: Traits
    private lateinit var snapyrContext: SnapyrContext

    fun makeAnalytics(): Snapyr {
        val created = Snapyr(
            application,
            null,
            networkExecutor,
            traitsCache,
            snapyrContext,
            defaultOptions,
            Logger.with(Snapyr.LogLevel.NONE),
            "qaz",
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            ConnectionFactory.Environment.DEV,
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            true,
            optOut,
            crpyto,
            ValueMap(),
            lifecycle,
            false,
            true,
            false,
            null,
            false
        )
        connectionFactory = `mock`(ConnectionFactory::class.java)
        ServiceFacade.getInstance().setConnectionFactory(connectionFactory)
        return created
    }

    @Before
    @Throws(IOException::class, NameNotFoundException::class)
    fun setUp() {
        Snapyr.singleton = null // clear the singleton instance

        initMocks(this)
        defaultOptions = Options()
        application = mockApplication()
        traits = Traits.create()
        whenever(traitsCache.get()).thenReturn(traits)

        val packageInfo = PackageInfo()
        packageInfo.versionCode = 100
        packageInfo.versionName = "1.0.0"

        val packageManager = Mockito.mock(PackageManager::class.java)
        whenever(packageManager.getPackageInfo("com.foo", 0)).thenReturn(packageInfo)
        whenever(application.packageName).thenReturn("com.foo")
        whenever(application.packageManager).thenReturn(packageManager)

        snapyrContext = Utils.createContext(traits)
        whenever(projectSettingsCache.get())
            .thenReturn(create(Cartographer.INSTANCE.fromJson(SETTINGS)))

        val sharedPreferences =
            RuntimeEnvironment.application
                .getSharedPreferences("analytics-test-qaz", MODE_PRIVATE)
        optOut = BooleanPreference(sharedPreferences, "opt-out-test", false)

        // Used by singleton tests.
        grantPermission(RuntimeEnvironment.application, android.Manifest.permission.INTERNET)
    }

    @After
    fun tearDown() {
        RuntimeEnvironment.application
            .getSharedPreferences("analytics-android-qaz", MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun invalidIdentity() {
        try {
            makeAnalytics().identify(null, null, null)
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessage("Either userId or some traits must be provided.")
        }
    }

    @Test
    fun identifyUpdatesCache() {
        makeAnalytics().identify("foo", Traits().putValue("bar", "qaz"), null)

        assertThat(traits).contains(MapEntry.entry("userId", "foo"))
        assertThat(traits).contains(MapEntry.entry("bar", "qaz"))
        assertThat(snapyrContext.traits()).contains(MapEntry.entry("userId", "foo"))
        assertThat(snapyrContext.traits()).contains(MapEntry.entry("bar", "qaz"))
        verify(traitsCache).set(traits)
    }

    @Test
    fun identifyNullTraits() {
        makeAnalytics().identify("userId", null, null)

        assertThat(traits.userId()).isEqualTo("userId")
        assertThat(traits.username()).isNull()
    }

    @Test
    fun identifySavesPreviousTraits() {
        var analytics = makeAnalytics()
        analytics.identify("userId", Traits().putUsername("username"), null)
        analytics.identify("userId")

        assertThat(traits.userId()).isEqualTo("userId")
        assertThat(traits.username()).isEqualTo("username")
    }

    @Test
    @Nullable
    fun invalidGroup() {
        try {
            makeAnalytics().group("")
            fail("empty groupId and name should throw exception")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("groupId must not be null or empty.")
        }
    }

    @Test
    fun group() {
        makeAnalytics().group("snapyr", Traits().putEmployees(42), null)

        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    fun invalidTrack() {
        try {
            makeAnalytics().track(null.toString())
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("event must not be null or empty.")
        }
        try {
            makeAnalytics().track("   ")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("event must not be null or empty.")
        }
    }

    @Test
    fun track() {
        val ana = makeAnalytics()
        ana.track("wrote tests", Properties().putUrl("github.com"))

        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun invalidScreen() {
        try {
            makeAnalytics().screen(null, null as String?)
            fail("null category and name should throw exception")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("either category or name must be provided.")
        }

        try {
            makeAnalytics().screen("", "")
            fail("empty category and name should throw exception")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessage("either category or name must be provided.")
        }
    }

    @Test
    fun screen() {
        makeAnalytics().screen("android", "saw tests", Properties().putUrl("github.com"))

        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    fun optionsCustomContext() {
        makeAnalytics().track("foo", null, Options().putContext("from_tests", true))

        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun optOutDisablesEvents() {
        var analytics = makeAnalytics()
        analytics.optOut(true)
        analytics.track("foo")

        verifyNoMoreInteractions(crpyto)
    }

    @Test
    @Throws(IOException::class)
    fun emptyTrackingPlan() {
        var analytics = makeAnalytics()
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                                      |{
                                      |  "integrations": {
                                      |    "test": {
                                      |      "foo": "bar"
                                      |    }
                                      |  },
                                      |  "plan": {
                                      |  }
                                      |}
                                      """.trimMargin()
            )
        )

        analytics.track("foo")
        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun emptyEventPlan() {
        var analytics = makeAnalytics()
        analytics.projectSettings = create(
            Cartographer.INSTANCE.fromJson(
                """
                              |{
                              |  "integrations": {
                              |    "test": {
                              |      "foo": "bar"
                              |    }
                              |  },
                              |  "plan": {
                              |    "track": {
                              |    }
                              |  }
                              |}
                              """.trimMargin()
            )
        )
        analytics.track("foo")
        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    fun logoutClearsTraitsAndUpdatesContext() {
        snapyrContext.setTraits(Traits().putAge(20).putAvatar("bar"))

        makeAnalytics().logout()

        verify(traitsCache).delete()
        verify(traitsCache)
            .set(
                argThat(
                    object : TypeSafeMatcher<Traits>() {
                        override fun matchesSafely(traits: Traits): Boolean {
                            return !isNullOrEmpty(traits.anonymousId())
                        }

                        override fun describeTo(description: Description) {}
                    })
            )
        assertThat(snapyrContext.traits()).hasSize(1)
        assertThat(snapyrContext.traits()).containsKey("anonymousId")
    }

    @Test
    fun shutdown() {
        var analytics = makeAnalytics()
        assertThat(analytics.shutdown).isFalse
        analytics.shutdown()
        verify(application).unregisterActivityLifecycleCallbacks(analytics.activityLifecycleCallback)
        verify(networkExecutor).shutdown()
        assertThat(analytics.shutdown).isTrue
        try {
            analytics.track("foo")
            fail("Enqueuing a message after shutdown should throw.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Cannot enqueue messages after client is shutdown.")
        }

        try {
            analytics.flush()
            fail("Enqueuing a message after shutdown should throw.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Cannot enqueue messages after client is shutdown.")
        }
    }

    @Test
    fun shutdownTwice() {
        var analytics = makeAnalytics()
        assertThat(analytics.shutdown).isFalse
        analytics.shutdown()
        analytics.shutdown()
        assertThat(analytics.shutdown).isTrue
    }

    @Test
    @Throws(Exception::class)
    fun shutdownDisallowedOnCustomSingletonInstance() {
        Snapyr.singleton = null
        try {
            val analytics = Snapyr.Builder(RuntimeEnvironment.application, "foo").build()
            Snapyr.setSingletonInstance(analytics)
            analytics.shutdown()
            fail("Calling shutdown() on static singleton instance should throw")
        } catch (ignored: UnsupportedOperationException) {
        }
    }

    @Test
    fun setSingletonInstanceMayOnlyBeCalledOnce() {
        Snapyr.singleton = null

        val analytics = Snapyr.Builder(RuntimeEnvironment.application, "foo").build()
        Snapyr.setSingletonInstance(analytics)

        try {
            Snapyr.setSingletonInstance(analytics)
            fail("Can't set singleton instance twice.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Singleton instance already exists.")
        }
    }

    @Test
    fun setSingletonInstanceAfterWithFails() {
        Snapyr.singleton = null
        Snapyr.setSingletonInstance(Snapyr.Builder(RuntimeEnvironment.application, "foo").build())

        val analytics = Snapyr.Builder(RuntimeEnvironment.application, "bar").build()
        try {
            Snapyr.setSingletonInstance(analytics)
            fail("Can't set singleton instance after with().")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessage("Singleton instance already exists.")
        }
    }

    @Test
    fun setSingleInstanceReturnedFromWith() {
        Snapyr.clearSingleton()
        val analytics = Snapyr.Builder(RuntimeEnvironment.application, "foo").build()
        Snapyr.setSingletonInstance(analytics)
        assertThat(Snapyr.with(RuntimeEnvironment.application)).isSameAs(analytics)
    }

    @Test
    @Throws(Exception::class)
    fun multipleInstancesWithSameTagIsAllowedAfterShutdown() {
        Snapyr.Builder(RuntimeEnvironment.application, "foo").build().shutdown()
        Snapyr.Builder(RuntimeEnvironment.application, "bar").tag("foo").build()
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsInstalled() {
        val callback = AtomicReference<DefaultLifecycleObserver>()
        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        var analytics = Snapyr(
            application,
            null,
            networkExecutor,
            traitsCache,
            snapyrContext,
            defaultOptions,
            Logger.with(Snapyr.LogLevel.NONE),
            "qaz",
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            ConnectionFactory.Environment.DEV,
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            false,
            optOut,
            crpyto,
            ValueMap(),
            lifecycle,
            false,
            true,
            false,
            null,
            false
        )

        callback.get().onCreate(mockLifecycleOwner)
        verify(crpyto).encrypt(any(OutputStream::class.java))
        callback.get().onCreate(mockLifecycleOwner)
        verifyNoMoreInteractions(crpyto) // Application Installed is not duplicated
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsUpdated() {
        val packageInfo = PackageInfo()
        packageInfo.versionCode = 101
        packageInfo.versionName = "1.0.1"

        val sharedPreferences =
            RuntimeEnvironment.application.getSharedPreferences(
                "analytics-android-qaz",
                MODE_PRIVATE
            )
        val editor = sharedPreferences.edit()
        editor.putInt("build", 100)
        editor.putString("version", "1.0.0")
        editor.apply()
        whenever(application.getSharedPreferences("analytics-android-qaz", MODE_PRIVATE))
            .thenReturn(sharedPreferences)

        val packageManager = Mockito.mock(PackageManager::class.java)
        whenever(packageManager.getPackageInfo("com.foo", 0)).thenReturn(packageInfo)
        whenever(application.packageName).thenReturn("com.foo")
        whenever(application.packageManager).thenReturn(packageManager)

        val callback = AtomicReference<DefaultLifecycleObserver>()
        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)
        var analytics = makeAnalytics()
        callback.get().onCreate(mockLifecycleOwner)
        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun recordScreenViews() {

        val callback = AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        var analytics = Snapyr(
            application,
            null,
            networkExecutor,
            traitsCache,
            snapyrContext,
            defaultOptions,
            Logger.with(Snapyr.LogLevel.NONE),
            "qaz",
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            ConnectionFactory.Environment.DEV,
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            true,
            optOut,
            crpyto,
            ValueMap(),
            lifecycle,
            false,
            true,
            false,
            null,
            false
        )

        val activity = Mockito.mock(Activity::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        val info = Mockito.mock(ActivityInfo::class.java)

        whenever(activity.packageManager).thenReturn(packageManager)
        //noinspection WrongConstant
        whenever(
            packageManager.getActivityInfo(
                any(ComponentName::class.java),
                eq(PackageManager.GET_META_DATA)
            )
        )
            .thenReturn(info)
        whenever(info.loadLabel(packageManager)).thenReturn("Foo")

        callback.get().onActivityStarted(activity)

        analytics.screen("Foo")
        verify(crpyto).encrypt(any(OutputStream::class.java))
    }

    @Test
    fun trackDeepLinks() {
        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()
        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        var analytics = Snapyr(
            application,
            null,
            networkExecutor,
            traitsCache,
            snapyrContext,
            defaultOptions,
            Logger.with(Snapyr.LogLevel.NONE),
            "qaz",
            Cartographer.INSTANCE,
            projectSettingsCache,
            "foo",
            ConnectionFactory.Environment.DEV,
            DEFAULT_FLUSH_QUEUE_SIZE,
            DEFAULT_FLUSH_INTERVAL.toLong(),
            analyticsExecutor,
            true,
            CountDownLatch(0),
            false,
            true,
            optOut,
            Crypto.none(),
            ValueMap(),
            lifecycle,
            false,
            true,
            false,
            null,
            false
        )
        Snapyr.setSingletonInstance(analytics)

        val expectedURL = "app://track.com/open?utm_id=12345&gclid=abcd&nope="

        val activity = Mockito.mock(Activity::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val uri = Uri.parse(expectedURL)

        whenever(intent.data).thenReturn(uri)
        whenever(activity.intent).thenReturn(intent)

        callback.get().onActivityCreated(activity, Bundle())
    }

    @Test
    fun trackDeepLinks_disabled() {

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        var analytics = makeAnalytics()

        val expectedURL = "app://track.com/open?utm_id=12345&gclid=abcd&nope="

        val activity = Mockito.mock(Activity::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val uri = Uri.parse(expectedURL)

        whenever(intent.data).thenReturn(uri)
        whenever(activity.intent).thenReturn(intent)
    }

    @Test
    fun trackDeepLinks_null() {

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        var analytics = makeAnalytics()

        val activity = Mockito.mock(Activity::class.java)

        whenever(activity.intent).thenReturn(null)

        callback.get().onActivityCreated(activity, Bundle())
    }

    @Test
    fun trackDeepLinks_nullData() {

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        var analytics = makeAnalytics()

        val activity = Mockito.mock(Activity::class.java)

        val intent = Mockito.mock(Intent::class.java)

        whenever(activity.intent).thenReturn(intent)
        whenever(intent.data).thenReturn(null)

        callback.get().onActivityCreated(activity, Bundle())
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun registerActivityLifecycleCallbacks() {

        val callback =
            AtomicReference<ActivityLifecycleCallbacks>()

        doNothing()
            .whenever(application)
            .registerActivityLifecycleCallbacks(
                argThat<ActivityLifecycleCallbacks>(
                    object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                        override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                            callback.set(item)
                            return true
                        }
                    })
            )

        var analytics = makeAnalytics()

        /*      val activity = Mockito.mock(Activity::class.java)
              val bundle = Bundle()

              callback.get().onActivityCreated(activity, bundle)
              verify(actionHandler).onActivityCreated(activity, bundle)

              callback.get().onActivityStarted(activity)
              verify(actionHandler).onActivityStarted(activity)

              callback.get().onActivityResumed(activity)
              verify(actionHandler).onActivityResumed(activity)

              callback.get().onActivityPaused(activity)
              verify(actionHandler).onActivityPaused(activity)

              callback.get().onActivityStopped(activity)
              verify(actionHandler).onActivityStopped(activity)

              callback.get().onActivitySaveInstanceState(activity, bundle)
              verify(actionHandler).onActivitySaveInstanceState(activity, bundle)

              callback.get().onActivityDestroyed(activity)
              verify(actionHandler).onActivityDestroyed(activity)

              verifyNoMoreInteractions(integration)*/
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsApplicationOpened() {

        val callback =
            AtomicReference<DefaultLifecycleObserver>()

        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        var analytics = makeAnalytics()

        callback.get().onCreate(mockLifecycleOwner)
        callback.get().onStart(mockLifecycleOwner)
    }

    @Test
    @Throws(NameNotFoundException::class)
    fun trackApplicationLifecycleEventsApplicationBackgrounded() {

        val callback =
            AtomicReference<DefaultLifecycleObserver>()

        doNothing()
            .whenever(lifecycle)
            .addObserver(
                argThat<LifecycleObserver>(
                    object : NoDescriptionMatcher<LifecycleObserver>() {
                        override fun matchesSafely(item: LifecycleObserver): Boolean {
                            callback.set(item as DefaultLifecycleObserver)
                            return true
                        }
                    })
            )

        val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

        var analytics = makeAnalytics()

        val backgroundedActivity = Mockito.mock(Activity::class.java)
        whenever(backgroundedActivity.isChangingConfigurations).thenReturn(false)

        callback.get().onCreate(mockLifecycleOwner)
        callback.get().onStart(mockLifecycleOwner)
        callback.get().onResume(mockLifecycleOwner)
        callback.get().onStop(mockLifecycleOwner)

        @Test
        @Throws(NameNotFoundException::class)
        fun trackApplicationLifecycleEventsApplicationForegrounded() {

            val callback =
                AtomicReference<DefaultLifecycleObserver>()

            doNothing()
                .whenever(lifecycle)
                .addObserver(
                    argThat<LifecycleObserver>(
                        object : NoDescriptionMatcher<LifecycleObserver>() {
                            override fun matchesSafely(item: LifecycleObserver): Boolean {
                                callback.set(item as DefaultLifecycleObserver)
                                return true
                            }
                        })
                )

            val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

            var analytics = makeAnalytics()

            callback.get().onCreate(mockLifecycleOwner)
            callback.get().onStart(mockLifecycleOwner)
            callback.get().onStop(mockLifecycleOwner)
            callback.get().onStart(mockLifecycleOwner)
        }

        @Test
        @Throws(NameNotFoundException::class)
        fun unregisterActivityLifecycleCallbacks() {
            val registeredActivityCallback = AtomicReference<ActivityLifecycleCallbacks>()
            val unregisteredActivityCallback = AtomicReference<ActivityLifecycleCallbacks>()

            doNothing()
                .whenever(application)
                .registerActivityLifecycleCallbacks(
                    argThat<ActivityLifecycleCallbacks>(
                        object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                            override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                                if (item is SnapyrActivityLifecycleCallbacks) {
                                    registeredActivityCallback.set(item)
                                }
                                return true
                            }
                        })
                )
            doNothing()
                .whenever(application)
                .unregisterActivityLifecycleCallbacks(
                    argThat<ActivityLifecycleCallbacks>(
                        object : NoDescriptionMatcher<ActivityLifecycleCallbacks>() {
                            override fun matchesSafely(item: ActivityLifecycleCallbacks): Boolean {
                                if (item is SnapyrActivityLifecycleCallbacks) {
                                    unregisteredActivityCallback.set(item)
                                }
                                return true
                            }
                        })
                )

            var analytics = Snapyr(
                application,
                null,
                networkExecutor,
                traitsCache,
                snapyrContext,
                defaultOptions,
                Logger.with(Snapyr.LogLevel.NONE),
                "qaz",
                Cartographer.INSTANCE,
                projectSettingsCache,
                "foo",
                ConnectionFactory.Environment.DEV,
                DEFAULT_FLUSH_QUEUE_SIZE,
                DEFAULT_FLUSH_INTERVAL.toLong(),
                analyticsExecutor,
                true,
                CountDownLatch(0),
                false,
                false,
                optOut,
                Crypto.none(),
                ValueMap(),
                lifecycle,
                false,
                true,
                false,
                null,
                false
            )

            assertThat(analytics.shutdown).isFalse
            analytics.shutdown()

            // Same callback was registered and unregistered
            assertThat(analytics.activityLifecycleCallback).isSameAs(registeredActivityCallback.get())
            assertThat(analytics.activityLifecycleCallback).isSameAs(unregisteredActivityCallback.get())
        }

        @Test
        @Throws(NameNotFoundException::class)
        fun removeLifecycleObserver() {

            val registeredCallback = AtomicReference<DefaultLifecycleObserver>()
            val unregisteredCallback = AtomicReference<DefaultLifecycleObserver>()

            doNothing()
                .whenever(lifecycle)
                .addObserver(
                    argThat<LifecycleObserver>(
                        object : NoDescriptionMatcher<LifecycleObserver>() {
                            override fun matchesSafely(item: LifecycleObserver): Boolean {
                                registeredCallback.set(item as DefaultLifecycleObserver)
                                return true
                            }
                        })
                )
            doNothing()
                .whenever(lifecycle)
                .removeObserver(
                    argThat<LifecycleObserver>(
                        object : NoDescriptionMatcher<LifecycleObserver>() {
                            override fun matchesSafely(item: LifecycleObserver): Boolean {
                                unregisteredCallback.set(item as DefaultLifecycleObserver)
                                return true
                            }
                        })
                )
            val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

            var analytics = Snapyr(
                application,
                null,
                networkExecutor,
                traitsCache,
                snapyrContext,
                defaultOptions,
                Logger.with(Snapyr.LogLevel.NONE),
                "qaz",
                Cartographer.INSTANCE,
                projectSettingsCache,
                "foo",
                ConnectionFactory.Environment.DEV,
                DEFAULT_FLUSH_QUEUE_SIZE,
                DEFAULT_FLUSH_INTERVAL.toLong(),
                analyticsExecutor,
                false,
                CountDownLatch(0),
                false,
                false,
                optOut,
                Crypto.none(),
                ValueMap(),
                lifecycle,
                false,
                true,
                false,
                null,
                false
            )

            assertThat(analytics.shutdown).isFalse
            analytics.shutdown()
            val lifecycleObserverSpy = spy(analytics.activityLifecycleCallback)
            // Same callback was registered and unregistered
            assertThat(analytics.activityLifecycleCallback).isSameAs(registeredCallback.get())
            assertThat(analytics.activityLifecycleCallback).isSameAs(unregisteredCallback.get())

            // Verify callbacks do not call through after shutdown
            registeredCallback.get().onCreate(mockLifecycleOwner)
            verify(lifecycleObserverSpy, never()).onCreate(mockLifecycleOwner)

            registeredCallback.get().onStop(mockLifecycleOwner)
            verify(lifecycleObserverSpy, never()).onStop(mockLifecycleOwner)

            registeredCallback.get().onStart(mockLifecycleOwner)
            verify(lifecycleObserverSpy, never()).onStart(mockLifecycleOwner)

            verifyNoMoreInteractions(lifecycleObserverSpy)
        }

        @Test
        @Throws(IOException::class)
        fun loadNonEmptyDefaultProjectSettingsOnNetworkError() {

            // Make project download empty map and thus use default settings
            whenever(projectSettingsCache.get()).thenReturn(null)
            whenever(SettingsRequest.execute()).thenThrow(IOException::class.java) // Simulate network error

            val defaultProjectSettings =
                ValueMap()
                    .putValue(
                        "integrations",
                        ValueMap()
                            .putValue(
                                "Adjust",
                                ValueMap()
                                    .putValue("appToken", "<>")
                                    .putValue("trackAttributionData", true)
                            )
                    )

            var analytics = Snapyr(
                application,
                null,
                networkExecutor,
                traitsCache,
                snapyrContext,
                defaultOptions,
                Logger.with(Snapyr.LogLevel.NONE),
                "qaz",
                Cartographer.INSTANCE,
                projectSettingsCache,
                "foo",
                ConnectionFactory.Environment.DEV,
                DEFAULT_FLUSH_QUEUE_SIZE,
                DEFAULT_FLUSH_INTERVAL.toLong(),
                analyticsExecutor,
                true,
                CountDownLatch(0),
                false,
                false,
                optOut,
                Crypto.none(),
                defaultProjectSettings,
                lifecycle,
                false,
                true,
                false,
                null,
                false
            )

            assertThat(analytics.projectSettings).hasSize(3)
            assertThat(analytics.projectSettings.integrations()).containsKey("Snapyr")
            assertThat(analytics.projectSettings.integrations()).containsKey("Adjust")
        }

        @Test
        @Throws(IOException::class)
        fun loadEmptyDefaultProjectSettingsOnNetworkError() {

            // Make project download empty map and thus use default settings
            whenever(projectSettingsCache.get()).thenReturn(null)
            whenever(SettingsRequest.execute()).thenThrow(IOException::class.java) // Simulate network error

            val defaultProjectSettings = ValueMap()
            var analytics = Snapyr(
                application,
                null,
                networkExecutor,
                traitsCache,
                snapyrContext,
                defaultOptions,
                Logger.with(Snapyr.LogLevel.NONE),
                "qaz",
                Cartographer.INSTANCE,
                projectSettingsCache,
                "foo",
                ConnectionFactory.Environment.DEV,
                DEFAULT_FLUSH_QUEUE_SIZE,
                DEFAULT_FLUSH_INTERVAL.toLong(),
                analyticsExecutor,
                true,
                CountDownLatch(0),
                false,
                false,
                optOut,
                Crypto.none(),
                defaultProjectSettings,
                lifecycle,
                false,
                true,
                false,
                null,
                false
            )

            assertThat(analytics.projectSettings).hasSize(3)
            assertThat(analytics.projectSettings).containsKey("integrations")
            assertThat(analytics.projectSettings.integrations()).hasSize(1)
            assertThat(analytics.projectSettings.integrations()).containsKey("Snapyr")
        }

        @Test
        @Throws(IOException::class)
        fun overwriteSnapyrIoIntegration() {

            // Make project download empty map and thus use default settings
            whenever(projectSettingsCache.get()).thenReturn(null)
            whenever(SettingsRequest.execute()).thenThrow(IOException::class.java) // Simulate network error

            val defaultProjectSettings = ValueMap()
                .putValue(
                    "integrations",
                    ValueMap()
                        .putValue(
                            "Snapyr",
                            ValueMap()
                                .putValue("appToken", "<>")
                                .putValue("trackAttributionData", true)
                        )
                )
            var analytics = Snapyr(
                application,
                null,
                networkExecutor,
                traitsCache,
                snapyrContext,
                defaultOptions,
                Logger.with(Snapyr.LogLevel.NONE),
                "qaz",
                Cartographer.INSTANCE,
                projectSettingsCache,
                "foo",
                ConnectionFactory.Environment.DEV,
                DEFAULT_FLUSH_QUEUE_SIZE,
                DEFAULT_FLUSH_INTERVAL.toLong(),
                analyticsExecutor,
                true,
                CountDownLatch(0),
                false,
                false,
                optOut,
                Crypto.none(),
                defaultProjectSettings,
                lifecycle,
                false,
                true,
                false,
                null,
                false
            )

            assertThat(analytics.projectSettings).hasSize(3)
            assertThat(analytics.projectSettings).containsKey("integrations")
            assertThat(analytics.projectSettings.integrations()).containsKey("Snapyr")
            assertThat(analytics.projectSettings.integrations()).hasSize(1)
            assertThat(analytics.projectSettings.integrations().getValueMap("Snapyr"))
                .hasSize(3)
            assertThat(analytics.projectSettings.integrations().getValueMap("Snapyr"))
                .containsKey("apiKey")
            assertThat(analytics.projectSettings.integrations().getValueMap("Snapyr"))
                .containsKey("appToken")
            assertThat(analytics.projectSettings.integrations().getValueMap("Snapyr"))
                .containsKey("trackAttributionData")
        }

        @Test
        fun overridingOptionsDoesNotModifyGlobalAnalytics() {
            var analytics = makeAnalytics()

            analytics.track("event", null, Options().putContext("testProp", true))
            val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
            assertThat(payload.value.context()).containsKey("testProp")
            assertThat(payload.value.context()["testProp"]).isEqualTo(true)
            assertThat(analytics.snapyrContext).doesNotContainKey("testProp")
        }

        @Test
        fun enableExperimentalNanosecondResolutionTimestamps() {
            var analytics = Snapyr(
                application,
                null,
                networkExecutor,
                traitsCache,
                snapyrContext,
                defaultOptions,
                Logger.with(Snapyr.LogLevel.NONE),
                "qaz",
                Cartographer.INSTANCE,
                projectSettingsCache,
                "foo",
                ConnectionFactory.Environment.DEV,
                DEFAULT_FLUSH_QUEUE_SIZE,
                DEFAULT_FLUSH_INTERVAL.toLong(),
                analyticsExecutor,
                true,
                CountDownLatch(0),
                false,
                false,
                optOut,
                Crypto.none(),
                ValueMap(),
                lifecycle,
                true,
                true,
                false,
                null,
                false
            )

            analytics.track("event")
            val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
            val timestamp = payload.value["timestamp"] as String
            assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{9}Z")
        }

        @Test
        fun disableExperimentalNanosecondResolutionTimestamps() {
            makeAnalytics().track("event")
            val payload = ArgumentCaptor.forClass(TrackPayload::class.java)
            val timestamp = payload.value["timestamp"] as String
            assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z")
        }
    }
}
