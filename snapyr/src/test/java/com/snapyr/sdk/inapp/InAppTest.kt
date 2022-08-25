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

import com.snapyr.sdk.SnapyrAction
import com.snapyr.sdk.internal.Cartographer
import java.io.IOException
import org.junit.Test

class InAppTest {
    val MESSAGE_PAYLOAD_JSON =
        """
        {
            "message": {
                "content": "{}",
                "type": 'custom-json',
                "config": "{}",
                "actionToken": "actionToken-0817645f-0953-4c49-9743-d3ae887f530c.paul0817.1660936988",
                "timestamp": 1660936988
            }
        }
        """.trimIndent()

    @Test
    @Throws(IOException::class)
    fun testMessageFromJson() {
        return  // fix later brandon
        val raw = Cartographer.INSTANCE.parseJson(MESSAGE_PAYLOAD_JSON)
        val action = SnapyrAction.create(raw as Map<String, Object>)
        val inAppMessage = InAppMessage(action)
        check(inAppMessage.ActionToken == "actionToken-0817645f-0953-4c49-9743-d3ae887f530c.paul0817.1660936988")
    }
}
