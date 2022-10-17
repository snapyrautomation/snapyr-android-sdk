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
package com.snapyr.sdk.inapp

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import com.snapyr.sdk.TestUtils
import com.snapyr.sdk.http.ConnectionFactory
import com.snapyr.sdk.internal.SnapyrAction
import com.snapyr.sdk.services.Cartographer
import com.snapyr.sdk.services.ServiceFacade
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.Semaphore
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InAppTest {
    private val RAW_ACTION = Cartographer.INSTANCE.fromJson(
        """{
         "actionToken": "",
         "userId": "Brian",
         "timestamp": "2022-08-31T17:15:30.135145417Z",
         "actionType": "custom",
         "actionToken": "actionToken-0817645f-0953-4c49-9743-d3ae887f530c.paul0817.1660936988",
         "content": {
            "payloadType": "json",
            "payload": "{\n\"text\": \"foo bar\",\n\"param\" : \"colleen\"\n}"
         }
      }""".trimMargin()
    )

    @Test
    @Throws(IOException::class)
    fun testMessageFromJson() {
        val context = TestUtils.mockApplication()
        val connection = Mockito.mock(HttpURLConnection::class.java)
        whenever(connection.getResponseCode()).thenReturn(200)
        val connFactory = Mockito.mock(ConnectionFactory::class.java)
        whenever(connFactory.engineRequest(any(), any())).thenReturn(connection)
        ServiceFacade.getInstance()
            .setConnectionFactory(connFactory)
            .setApplication(context)
            .setNetworkExecutor(TestUtils.SynchronousExecutor())

        InAppFacade.allowInApp()
        var callbackCalled = false
        var callbackSemaphore = Semaphore(1)
        callbackSemaphore.acquire()
        InAppFacade.createInApp(
            InAppConfig()
                .setPollingRate(50)
                .setActionCallback {
                    callbackCalled = true
                    check(it.ActionToken == "actionToken-0817645f-0953-4c49-9743-d3ae887f530c.paul0817.1660936988")
                    check(it.Timestamp == InAppMessage.Formatter.parse("2022-08-31T17:15:30.135145417Z"))
                    check(it.UserId == "Brian")
                    check(it.ActionType == InAppActionType.ACTION_TYPE_CUSTOM)
                    check(it.Content.type == InAppPayloadType.PAYLOAD_TYPE_JSON)
                    check(it.Content.jsonPayload != null)
                    callbackSemaphore.release()
                },
            context
        )

        val path: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
        val method: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)

        InAppFacade.processTrackResponse(SnapyrAction.create(RAW_ACTION))
        InAppFacade.processPending(context)
        verify(connFactory).engineRequest(path.capture(), method.capture())
        // should have ack-d the request
        Assertions.assertThat(path.value).isEqualTo(
            "v1/actions/Brian?actionToken=actionToken-0817645f-0953-4c49-" +
                "9743-d3ae887f530c.paul0817.1660936988&status=delivered"
        )
        Assertions.assertThat(method.value).isEqualTo("POST")

        // should have called the user callback
        callbackSemaphore.acquire() // wait for user callback thread to complete in case it was out of order
        Assertions.assertThat(callbackCalled).isEqualTo(true)
    }
}
