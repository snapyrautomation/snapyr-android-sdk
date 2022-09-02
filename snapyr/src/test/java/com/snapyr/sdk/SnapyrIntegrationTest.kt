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

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.ConnectivityManager
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.snapyr.sdk.PayloadQueue.PersistentQueue
import com.snapyr.sdk.SnapyrWriteQueue.*
import com.snapyr.sdk.TestUtils.*
import com.snapyr.sdk.http.Client
import com.snapyr.sdk.http.WriteConnection
import com.snapyr.sdk.integrations.Logger
import com.snapyr.sdk.integrations.Logger.with
import com.snapyr.sdk.integrations.TrackPayload.Builder
import com.snapyr.sdk.internal.Cartographer
import com.snapyr.sdk.internal.Utils.DEFAULT_FLUSH_INTERVAL
import com.snapyr.sdk.internal.Utils.DEFAULT_FLUSH_QUEUE_SIZE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.*
import java.net.HttpURLConnection
import java.util.concurrent.ExecutorService

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class snapyrQueueTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()
    private lateinit var queueFile: QueueFile

    private fun mockConnection(): WriteConnection {
        return mockConnection(mock(HttpURLConnection::class.java))
    }

    private fun mockConnection(connection: HttpURLConnection): WriteConnection {
        return object : WriteConnection(connection) {
            @Throws(IOException::class)
            override fun getInputStream(): InputStream {
                return mock(InputStream::class.java)
            }

            override fun getOutputStream(): OutputStream {
                return mock(OutputStream::class.java)
            }
        }
    }

    @Before
    @Throws(IOException::class)
    fun setUp() {
        queueFile = QueueFile(File(folder.root, "queue-file"))
    }

    @After
    fun tearDown() {
        assertThat(ShadowLog.getLogs()).isEmpty()
    }

    @Test
    @Throws(IOException::class)
    fun enqueueAddsToQueueFile() {
        val payloadQueue = PersistentQueue(queueFile)
        val snapyrQueue = SnapyrBuilder().payloadQueue(payloadQueue).build()
        snapyrQueue.performEnqueue(TRACK_PAYLOAD)
        assertThat(payloadQueue.size()).isEqualTo(1)
    }

    @Test
    @Throws(IOException::class)
    fun enqueueLimitsQueueSize() {
        val payloadQueue = mock(PayloadQueue::class.java)
        // We want to trigger a remove, but not a flush.
        whenever(payloadQueue.size()).thenReturn(0, MAX_QUEUE_SIZE, MAX_QUEUE_SIZE, 0)
        val snapyrQueue = SnapyrBuilder().payloadQueue(payloadQueue).build()

        snapyrQueue.performEnqueue(TRACK_PAYLOAD)

        verify(payloadQueue).remove(1) // Oldest entry is removed.
        verify(payloadQueue).add(any(ByteArray::class.java)) // Newest entry is added.
    }

    @Test
    @Throws(IOException::class)
    fun exceptionIgnoredIfFailedToRemove() {
        val payloadQueue = mock(PayloadQueue::class.java)
        doThrow(IOException("no remove for you.")).whenever(payloadQueue).remove(1)
        whenever(payloadQueue.size()).thenReturn(MAX_QUEUE_SIZE) // trigger a remove
        val snapyrQueue = SnapyrBuilder().payloadQueue(payloadQueue).build()

        try {
            snapyrQueue.performEnqueue(TRACK_PAYLOAD)
        } catch (unexpected: IOError) {
            fail("did not expect QueueFile to throw an error.")
        }

        verify(payloadQueue, never()).add(any(ByteArray::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun enqueueMaxTriggersFlush() {
        val payloadQueue = PersistentQueue(queueFile)
        val client = mock(Client::class.java)
        val connection = mockConnection()
        whenever(client.upload()).thenReturn(connection)
        val snapyrQueue =
            SnapyrBuilder()
                .client(client)
                .flushSize(5)
                .payloadQueue(payloadQueue)
                .build()
        for (i in 0 until 4) {
            snapyrQueue.performEnqueue(TRACK_PAYLOAD)
        }
        verifyZeroInteractions(client)
        // Only the last enqueue should trigger an upload.
        snapyrQueue.performEnqueue(TRACK_PAYLOAD)

        verify(client).upload()
    }

    @Test
    @Throws(IOException::class)
    fun flushRemovesItemsFromQueue() {
        val payloadQueue = PersistentQueue(queueFile)
        val client = mock(Client::class.java)
        val os = mock(OutputStream::class.java)
        val conn = mock(WriteConnection::class.java)
        `when`(conn.outputStream).thenReturn(os)
        whenever(client.upload()).thenReturn(conn)
        val snapyrQueue =
            SnapyrBuilder()
                .client(client)
                .payloadQueue(payloadQueue)
                .build()
        val bytes = TRACK_PAYLOAD_JSON.toByteArray()
        for (i in 0 until 4) {
            queueFile.add(bytes)
        }

        snapyrQueue.submitFlush()

        assertThat(queueFile.size()).isEqualTo(0)
    }

    @Test
    @Throws(IOException::class)
    fun flushSubmitsToExecutor() {
        val executor = spy(TestUtils.SynchronousExecutor())
        val payloadQueue = mock(PayloadQueue::class.java)
        whenever(payloadQueue.size()).thenReturn(1)
        val dispatcher =
            SnapyrBuilder()
                .payloadQueue(payloadQueue)
                .networkExecutor(executor)
                .build()

        dispatcher.submitFlush()

        verify(executor).submit(any(Runnable::class.java))
    }

    @Test
    fun flushChecksIfExecutorIsShutdownFirst() {
        val executor = spy(TestUtils.SynchronousExecutor())
        val payloadQueue = mock(PayloadQueue::class.java)
        whenever(payloadQueue.size()).thenReturn(1)
        val dispatcher =
            SnapyrBuilder()
                .payloadQueue(payloadQueue)
                .networkExecutor(executor)
                .build()

        dispatcher.shutdown()
        executor.shutdown()
        dispatcher.submitFlush()

        verify(executor, never()).submit(any(Runnable::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun flushWhenDisconnectedSkipsUpload() {
        val networkInfo = mock(android.net.NetworkInfo::class.java)
        whenever(networkInfo.isConnectedOrConnecting).thenReturn(false)
        val connectivityManager = mock(ConnectivityManager::class.java)
        whenever(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        val context: Context = mockApplication()
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
            connectivityManager
        )
        val client = mock(Client::class.java)
        val snapyrQueue = SnapyrBuilder().context(context).client(client).build()

        snapyrQueue.submitFlush()

        verify(client, never()).upload()
    }

    @Test
    @Throws(IOException::class)
    fun flushWhenQueueSizeIsLessThanOneSkipsUpload() {
        val payloadQueue = mock(PayloadQueue::class.java)
        whenever(payloadQueue.size()).thenReturn(0)
        val context: Context = mockApplication()
        val client = mock(Client::class.java)
        val snapyrQueue = SnapyrBuilder()
            .payloadQueue(payloadQueue)
            .context(context)
            .client(client)
            .build()

        snapyrQueue.submitFlush()

        verifyZeroInteractions(context)
        verify(client, never()).upload()
    }

    @Test
    @Throws(IOException::class)
    fun flushDisconnectsConnection() {
        val client = mock(Client::class.java)
        val payloadQueue = PersistentQueue(queueFile)
        queueFile.add(TRACK_PAYLOAD_JSON.toByteArray())
        val urlConnection = mock(HttpURLConnection::class.java)
        val connection = mockConnection(urlConnection)
        whenever(client.upload()).thenReturn(connection)
        val snapyrQueue =
            SnapyrBuilder()
                .client(client)
                .payloadQueue(payloadQueue)
                .build()

        snapyrQueue.submitFlush()

        verify(urlConnection, times(1)).disconnect()
    }

    @Test
    @Throws(IOException::class)
    fun removesRejectedPayloads() {
        // todo: rewrite using mockwebserver.
        val client = mock(Client::class.java)
        val payloadQueue = PersistentQueue(queueFile)
        val os = mock(OutputStream::class.java)
        val conn = mock(WriteConnection::class.java)
        `when`(conn.outputStream).thenReturn(os)
        `when`(conn.close()).thenThrow(Client.HTTPException(400, "Bad Request", "bad request"))

        whenever(client.upload()).thenReturn(conn)

        val snapyrQueue =
            SnapyrBuilder()
                .client(client)
                .payloadQueue(payloadQueue)
                .build()
        for (i in 0..3) {
            payloadQueue.add(TRACK_PAYLOAD_JSON.toByteArray())
        }

        snapyrQueue.submitFlush()

        assertThat(queueFile.size()).isEqualTo(0)
        verify(client).upload()
    }

    @Test
    @Throws(IOException::class)
    fun ignoresServerError() {
        // todo: rewrite using mockwebserver.
        val payloadQueue: PayloadQueue = PersistentQueue(queueFile)
        val os = mock(OutputStream::class.java)
        val conn = mock(WriteConnection::class.java)
        `when`(conn.outputStream).thenReturn(os)
        `when`(conn.close()).thenThrow(
            Client.HTTPException(
                500,
                "Internal Server Error",
                "internal server error"
            )
        )
        val client = mock(Client::class.java)

        whenever(client.upload()).thenReturn(conn)
        val snapyrQueue = SnapyrBuilder()
            .client(client)
            .payloadQueue(payloadQueue)
            .build()
        for (i in 0..3) {
            payloadQueue.add(TRACK_PAYLOAD_JSON.toByteArray())
        }
        snapyrQueue.submitFlush()
        assertThat(queueFile.size()).isEqualTo(4)
        verify(client).upload()
    }

    @Test
    @Throws(IOException::class)
    fun ignoresHTTP429Error() {
        // todo: rewrite using mockwebserver.
        val payloadQueue: PayloadQueue = PersistentQueue(queueFile)
        val conn = mock(WriteConnection::class.java)
        val os = mock(OutputStream::class.java)
        `when`(conn.outputStream).thenReturn(os)
        `when`(conn.close()).thenThrow(
            Client.HTTPException(
                429,
                "Too Many Requests",
                "too many requests"
            )
        )
        val client = mock(Client::class.java)
        whenever(client.upload()).thenReturn(conn)
        val snapyrQueue = SnapyrBuilder()
            .client(client)
            .payloadQueue(payloadQueue)
            .build()
        for (i in 0..3) {
            payloadQueue.add(TRACK_PAYLOAD_JSON.toByteArray())
        }
        snapyrQueue.submitFlush()

        // Verify that messages were not removed from the queue when server returned a 429.
        assertThat(queueFile.size()).isEqualTo(4)
        verify(client).upload()
    }

    @Test
    @Throws(IOException::class)
    fun serializationErrorSkipsAddingPayload() {
        val payloadQueue = mock(PayloadQueue::class.java)
        val cartographer = mock(Cartographer::class.java)
        val payload = Builder().event("event").userId("userId").build()
        val snapyrQueue = SnapyrBuilder()
            .cartographer(cartographer)
            .payloadQueue(payloadQueue)
            .build()

        // Serialized json is null.
        whenever(cartographer.toJson(any(Map::class.java))).thenReturn(null)
        snapyrQueue.performEnqueue(payload)
        verify(payloadQueue, never()).add(any<Any>() as ByteArray?)

        // Serialized json is empty.
        whenever(cartographer.toJson(any(Map::class.java))).thenReturn("")
        snapyrQueue.performEnqueue(payload)
        verify(payloadQueue, never()).add(any<Any>() as ByteArray?)

        // Serialized json is too large (> MAX_PAYLOAD_SIZE).
        val stringBuilder = StringBuilder()
        for (i in 0..MAX_PAYLOAD_SIZE) {
            stringBuilder.append("a")
        }
        whenever(cartographer.toJson(any(Map::class.java))).thenReturn(stringBuilder.toString())
        snapyrQueue.performEnqueue(payload)
        verify(payloadQueue, never()).add(any<Any>() as ByteArray?)
    }

    @Test
    @Throws(IOException::class)
    fun shutDown() {
        val payloadQueue = mock(PayloadQueue::class.java)
        val snapyrQueue = SnapyrBuilder().payloadQueue(payloadQueue).build()

        snapyrQueue.shutdown()

        verify(payloadQueue).close()
    }

    @Test
    @Throws(IOException::class)
    fun payloadVisitorReadsOnly475KB() {
        val payloadWriter = SnapyrWriteQueue.PayloadWriter(
            mock(SnapyrWriteQueue.BatchPayloadWriter::class.java), Crypto.none()
        )
        val bytes =
            """{
        "context": {
          "library": "analytics-android",
          "libraryVersion": "0.4.4",
          "telephony": {
            "radio": "gsm",
            "carrier": "FI elisa"
          },
          "wifi": {
            "connected": false,
            "available": false
          },
          "providers": {
            "Tapstream": false,
            "Amplitude": false,
            "Localytics": false,
            "Flurry": false,
            "Countly": false,
            "Bugsnag": false,
            "Quantcast": false,
            "Crittercism": false,
            "Google Analytics": false,
            "Omniture": false,
            "Mixpanel": false
          },
          "location": {
            "speed": 0,
            "longitude": 24.937207,
            "latitude": 60.2495497
          },
          "locale": {
            "carrier": "FI elisa",
            "language": "English",
            "country": "United States"
          },
          "device": {
            "userId": "123",
            "brand": "samsung",
            "release": "4.2.2",
            "manufacturer": "samsung",
            "sdk": 17
          },
          "display": {
            "density": 1.5,
            "width": 800,
            "height": 480
          },
          "build": {
            "name": "1.0",
            "code": 1
          },
          "ip": "80.186.195.102",
          "inferredIp": true
        }
      }""".toByteArray() // length 1432
        // Fill the payload with (1432 * 500) = ~716kb of data

        for (i in 0..499) {
            queueFile.add(bytes)
        }

        queueFile.forEach(payloadWriter)

        // Verify only (331 * 1432) = 473992 < 475KB bytes are read
        assertThat(payloadWriter.payloadCount).isEqualTo(331)
    }

    internal class SnapyrBuilder {
        var client: Client? = null
        var stats: Stats? = null
        var payloadQueue: PayloadQueue? = null
        var context: Context? = null
        var cartographer: Cartographer? = null
        var integrations: Map<String, Boolean>? = null
        var flushInterval = DEFAULT_FLUSH_INTERVAL
        var flushSize = DEFAULT_FLUSH_QUEUE_SIZE
        var logger = with(Snapyr.LogLevel.NONE)
        var networkExecutor: ExecutorService? = null
        var actionHandler: SnapyrActionHandler? = null

        fun SnapyrBuilder() {
            initMocks(this)
            context = mockApplication()
            whenever(context!!.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)) //
                .thenReturn(PERMISSION_DENIED)
            cartographer = Cartographer.INSTANCE
        }

        fun client(client: Client): SnapyrBuilder {
            this.client = client
            return this
        }

        fun stats(stats: Stats): SnapyrBuilder {
            this.stats = stats
            return this
        }

        fun payloadQueue(payloadQueue: PayloadQueue): SnapyrBuilder {
            this.payloadQueue = payloadQueue
            return this
        }

        fun context(context: Context): SnapyrBuilder {
            this.context = context
            return this
        }

        fun cartographer(cartographer: Cartographer): SnapyrBuilder {
            this.cartographer = cartographer
            return this
        }

        fun integrations(integrations: Map<String, Boolean>): SnapyrBuilder {
            this.integrations = integrations
            return this
        }

        fun flushInterval(flushInterval: Int): SnapyrBuilder {
            this.flushInterval = flushInterval
            return this
        }

        fun flushSize(flushSize: Int): SnapyrBuilder {
            this.flushSize = flushSize
            return this
        }

        fun log(logger: Logger): SnapyrBuilder {
            this.logger = logger
            return this
        }

        fun networkExecutor(networkExecutor: ExecutorService): SnapyrBuilder {
            this.networkExecutor = networkExecutor
            return this
        }

        fun build(): SnapyrWriteQueue {
            if (context == null) {
                context = mockApplication()
                whenever(context!!.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE))
                    .thenReturn(PERMISSION_DENIED)
            }
            if (client == null) {
                client = mock(Client::class.java)
            }
            if (actionHandler == null) {
                actionHandler = mock(SnapyrActionHandler::class.java)
            }
            if (cartographer == null) {
                cartographer = Cartographer.INSTANCE
            }
            if (payloadQueue == null) {
                payloadQueue = mock(PayloadQueue::class.java)
            }
            if (stats == null) {
                stats = mock(Stats::class.java)
            }
            if (integrations == null) {
                integrations = emptyMap()
            }
            if (networkExecutor == null) {
                networkExecutor = TestUtils.SynchronousExecutor()
            }
            return SnapyrWriteQueue(
                context,
                client,
                cartographer,
                networkExecutor,
                stats,
                flushInterval.toLong(),
                flushSize,
                logger,
                Crypto.none(),
                payloadQueue,
                actionHandler
            )
        }
    }
}
