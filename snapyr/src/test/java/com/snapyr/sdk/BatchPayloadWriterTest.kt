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

import com.snapyr.sdk.http.BatchQueue
import com.snapyr.sdk.http.BatchUploadRequest
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BatchPayloadWriterTest {

    @Test
    @Throws(IOException::class)
    fun batchPayloadWriter() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val queue = BatchQueue.MemoryQueue()
        queue.add("{ \"item_1\" : 10 }".toByteArray())
        queue.add("{ \"item_2\" : 11 }".toByteArray())
        val written = BatchUploadRequest.execute(queue, byteArrayOutputStream, Crypto.none())

        assertThat(written == 2)
        assertThat(byteArrayOutputStream.toString())
            .contains("\"item_1\" : 10")
        assertThat(byteArrayOutputStream.toString())
            .contains("\"item_2\" : 11")
    }

    @Test
    @Throws(IOException::class)
    fun batchPayloadWriterSingleItem() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val queue = BatchQueue.MemoryQueue()
        queue.add("{ \"foobarbazqux\" : 10 }".toByteArray())
        val written = BatchUploadRequest.execute(queue, byteArrayOutputStream, Crypto.none())
        assertThat(written == 1)

        assertThat(byteArrayOutputStream.toString())
            .contains("\"batch\":[{ \"foobarbazqux\" : 10 }]")
    }

    @Test
    @Throws(IOException::class)
    fun batchPayloadWriterFailsForNoItem() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val queue = BatchQueue.MemoryQueue()
        try {
            BatchUploadRequest.execute(queue, byteArrayOutputStream, Crypto.none())
        } catch (exception: IOException) {
            assertThat(exception).hasMessage("Incomplete document")
        }
    }
}
