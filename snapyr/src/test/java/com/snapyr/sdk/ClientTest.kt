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

import android.net.Uri
import com.nhaarman.mockitokotlin2.whenever
import com.snapyr.sdk.http.Client
import com.snapyr.sdk.http.ConnectionFactory
import com.snapyr.sdk.internal.Private
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.mockwebserver.RecordedRequest
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClientTest {
    @Rule
    @JvmField
    val server = MockWebServer()

    @Rule
    @JvmField
    val folder = TemporaryFolder()
    private lateinit var client: Client
    private lateinit var mockClient: Client

    @Private
    lateinit var mockConnection: HttpURLConnection

    @Before
    fun setUp() {
        mockConnection = mock(HttpURLConnection::class.java)

        client = Client(
            "foo",
            object : ConnectionFactory() {
                @Throws(IOException::class)
                override fun openConnection(url: String): HttpURLConnection {
                    val path = Uri.parse(url).path
                    val mockServerURL = server.getUrl(path)
                    return super.openConnection(mockServerURL.toString())
                }
            }
        )

        mockClient = Client(
            "foo",
            object : ConnectionFactory() {
                @Throws(IOException::class)
                override fun openConnection(url: String): HttpURLConnection {
                    return mockConnection
                }
            }
        )
    }

//    @Test
//    @Throws(Exception::class)
//    fun upload() {
//        server.enqueue(MockResponse())
//
//        val connection = client.upload()
//        assertThat(connection.os).isNotNull()
//        assertThat(connection.`is`).isNull()
//        assertThat(connection.connection.responseCode).isEqualTo(200) // consume the response
//        RecordedRequestAssert.assertThat(server.takeRequest())
//            .hasRequestLine("POST /v1/import HTTP/1.1")
//            .containsHeader("User-Agent", ConnectionFactory.USER_AGENT)
//            .containsHeader("Content-Type", "application/json")
//            .containsHeader("Authorization", "Basic Zm9vOg==")
//    }

    @Test
    @Throws(Exception::class)
    fun closingUploadConnectionClosesStreams() {
        val os = mock(OutputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(200)

        val connection = mockClient.upload()
        verify(mockConnection).doOutput = true
        verify(mockConnection).setChunkedStreamingMode(0)

        connection.close()
        verify(mockConnection).disconnect()
        verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun closingUploadConnectionClosesStreamsForNon200Response() {
        val os = mock(OutputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(202)

        val connection = mockClient.upload()
        verify(mockConnection).doOutput = true
        verify(mockConnection).setChunkedStreamingMode(0)

        connection.close()
        verify(mockConnection).disconnect()
        verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun uploadFailureClosesStreamsAndThrowsException() {
        val os = mock(OutputStream::class.java)
        val input = mock(InputStream::class.java)
        whenever(mockConnection.outputStream).thenReturn(os)
        whenever(mockConnection.responseCode).thenReturn(300)
        whenever(mockConnection.responseMessage).thenReturn("bar")
        whenever(mockConnection.inputStream).thenReturn(input)

        val connection = mockClient.upload()
        verify(mockConnection).doOutput = true
        verify(mockConnection).setChunkedStreamingMode(0)

        try {
            connection.close()
            assertThat(">= 300 return code should throw an exception")
        } catch (e: Client.HTTPException) {
            assertThat(e)
                .hasMessage(
                    "HTTP 300: bar. " +
                        "Response: Could not read response body for rejected message: " +
                        "java.io.IOException: Underlying input stream returned zero bytes"
                )
        }
        verify(mockConnection).disconnect()
        verify(os).close()
    }

    @Test
    @Throws(Exception::class)
    fun fetchSettings() {
        server.enqueue(MockResponse().setBody("{\"test\" : \"brandon\"}"))

        val settings = client.fetchSettings()
        assertThat(settings["test"] == "brandon")
    }

    internal class RecordedRequestAssert constructor(actual: RecordedRequest) :
        AbstractAssert<RecordedRequestAssert,
            RecordedRequest>(actual, RecordedRequestAssert::class.java) {

        fun containsHeader(name: String, expectedHeader: String): RecordedRequestAssert {
            isNotNull
            val actualHeader = actual.getHeader(name)
            assertThat(actualHeader)
                .overridingErrorMessage(
                    "Expected header <%s> to be <%s> but was <%s>.",
                    name,
                    expectedHeader,
                    actualHeader
                )
                .isEqualTo(expectedHeader)
            return this
        }

        fun containsHeader(name: String): RecordedRequestAssert {
            isNotNull
            val actualHeader = actual.getHeader(name)
            assertThat(actualHeader)
                .overridingErrorMessage(
                    "Expected header <%s> to not be empty but was.", name, actualHeader
                )
                .isNotNull
                .isNotEmpty
            return this
        }

        fun hasRequestLine(requestLine: String): RecordedRequestAssert {
            isNotNull
            val actualRequestLine = actual.requestLine
            assertThat(actualRequestLine)
                .overridingErrorMessage(
                    "Expected requestLine <%s> to be <%s> but was not.",
                    actualRequestLine,
                    requestLine
                )
                .isEqualTo(requestLine)
            return this
        }

        companion object {
            fun assertThat(recordedRequest: RecordedRequest): RecordedRequestAssert {
                return RecordedRequestAssert(recordedRequest)
            }
        }
    }
}
