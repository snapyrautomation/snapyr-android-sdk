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
package com.snapyr.sdk.notifications

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import com.snapyr.sdk.notifications.SnapyrNotification.NonSnapyrMessageException
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SnapyrNotificationTest {
    // stupid spotless
    private val TEST_ACTION_TOKEN = "Zjk1OTkxZGEtZWE5Yy00ZTQ0LTk5OGQtNWZmNWY0Y2EwNGQzOmIyOWY5MDE4LWVhMjUtNGJmNS04YTl" +
        "iLWQ4ZDZhOGJjMjlhMTpmYmYxMTljMS1lMjNlLTQxZDEtODkyMi05MjE5M2RlNGRkODk6NGZjOGE5NGQtNGU0Zi00NTQ4LWE3NjYtMz" +
        "M1NDY2YmViMzBjOnB1Ymxpc2g6NGY5YTA0NzMtYWY4OS00M2QyLWEyZTMtZmFiN2FkNmU0MzUyOnBhdWwxMTExYTp0YmQ6MTY2ODY0O" +
        "DQzOTphY3Rpb25Ub2tlbi00ZjlhMDQ3My1hZjg5LTQzZDItYTJlMy1mYWI3YWQ2ZTQzNTIucGF1bDExMTFhLjE2Njg2NDg0Mzk="

    @Test
    @Throws(IOException::class)
    fun testGoodNotification() {
        val dataMap = HashMap<String, String>()
        dataMap.set(
            "snapyr",
            """
            {
                "title": "Tap this",
                "subtitle": "Please",
                "contentText": "We really want you to tap this notification",
                "deepLinkUrl": "snapyrsample://test/123",
                "imageUrl": "https://images-na.ssl-images-amazon.com/images/S/pv-target-images/fb1fd46fbac48892ef9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg",
                "pushTemplate": {
                    "id": "b29f9018-ea25-4bf5-8a9b-d8d6a8bc29a1",
                    "modified": "2022-11-10T21:20:15.409Z"
                },
                "actionToken": "Zjk1OTkxZGEtZWE5Yy00ZTQ0LTk5OGQtNWZmNWY0Y2EwNGQzOmIyOWY5MDE4LWVhMjUtNGJmNS04YTliLWQ4ZDZhOGJjMjlhMTpmYmYxMTljMS1lMjNlLTQxZDEtODkyMi05MjE5M2RlNGRkODk6NGZjOGE5NGQtNGU0Zi00NTQ4LWE3NjYtMzM1NDY2YmViMzBjOnB1Ymxpc2g6NGY5YTA0NzMtYWY4OS00M2QyLWEyZTMtZmFiN2FkNmU0MzUyOnBhdWwxMTExYTp0YmQ6MTY2ODY0ODQzOTphY3Rpb25Ub2tlbi00ZjlhMDQ3My1hZjg5LTQzZDItYTJlMy1mYWI3YWQ2ZTQzNTIucGF1bDExMTFhLjE2Njg2NDg0Mzk="
            }
        """.trimMargin()
        )
        val remoteMessage = RemoteMessage.Builder("fakefirebasetoken")
            .setData(dataMap)
            .build()
        val snapyrNotification = SnapyrNotification(remoteMessage)

        // test parcel/unparcel, then ensure values are correct on the unparceled/copied object
        val bundle = Bundle()
        bundle.putParcelable("snapyrNotification", snapyrNotification)
        val unparceledNotification = bundle.getParcelable<SnapyrNotification>("snapyrNotification")

        assertThat(unparceledNotification).isNotNull()
        assertThat(unparceledNotification!!.notificationId).isGreaterThan(0)
        assertThat(unparceledNotification!!.titleText).isEqualTo("Tap this")
        assertThat(unparceledNotification!!.subtitleText).isEqualTo("Please")
        assertThat(unparceledNotification!!.contentText).isEqualTo("We really want you to tap this notification")
        assertThat(unparceledNotification!!.actionToken).isEqualTo(TEST_ACTION_TOKEN)
        assertThat(unparceledNotification!!.deepLinkUrl.toString()).isEqualTo("snapyrsample://test/123")
        assertThat(unparceledNotification!!.imageUrl).isEqualTo(
            "https://images-na.ssl-images-amazon.com/images/S/pv" +
                "-target-images/fb1fd46fbac48892ef9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg"
        )

        val notificationMap = unparceledNotification.asValueMap()
        assertThat(notificationMap.getInt("notificationId", -1)).isEqualTo(snapyrNotification.notificationId)
        assertThat(notificationMap.getString("titleText")).isEqualTo("Tap this")
        assertThat(notificationMap.getString("subtitleText")).isEqualTo("Please")
        assertThat(notificationMap.getString("contentText")).isEqualTo("We really want you to tap this notification")
        assertThat(notificationMap.getString("actionToken")).isEqualTo(TEST_ACTION_TOKEN)
        assertThat(notificationMap.getString("deepLinkUrl")).isEqualTo("snapyrsample://test/123")
        assertThat(notificationMap.getString("imageUrl")).isEqualTo(
            "https://images-na.ssl-images-amazon.com/images/S/pv" +
                "-target-images/fb1fd46fbac48892ef9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg"
        )
    }

    @Test
    @Throws(IOException::class)
    fun testNotificationMissingOptionalFields() {
        val dataMap = HashMap<String, String>()
        dataMap.set(
            "snapyr",
            """
            {
                "title": "Tap this",
                "contentText": "We really want you to tap this notification",
                "actionToken": "Zjk1OTkxZGEtZWE5Yy00ZTQ0LTk5OGQtNWZmNWY0Y2EwNGQzOmIyOWY5MDE4LWVhMjUtNGJmNS04YTliLWQ4ZDZhOGJjMjlhMTpmYmYxMTljMS1lMjNlLTQxZDEtODkyMi05MjE5M2RlNGRkODk6NGZjOGE5NGQtNGU0Zi00NTQ4LWE3NjYtMzM1NDY2YmViMzBjOnB1Ymxpc2g6NGY5YTA0NzMtYWY4OS00M2QyLWEyZTMtZmFiN2FkNmU0MzUyOnBhdWwxMTExYTp0YmQ6MTY2ODY0ODQzOTphY3Rpb25Ub2tlbi00ZjlhMDQ3My1hZjg5LTQzZDItYTJlMy1mYWI3YWQ2ZTQzNTIucGF1bDExMTFhLjE2Njg2NDg0Mzk="
            }
        """.trimMargin()
        )
        val remoteMessage = RemoteMessage.Builder("fakefirebasetoken")
            .setData(dataMap)
            .build()
        val snapyrNotification = SnapyrNotification(remoteMessage)
        assertThat(snapyrNotification).isNotNull()
        assertThat(snapyrNotification.notificationId).isGreaterThan(0)
        assertThat(snapyrNotification.titleText).isEqualTo("Tap this")
        assertThat(snapyrNotification.subtitleText).isNull()
        assertThat(snapyrNotification.contentText).isEqualTo("We really want you to tap this notification")
        assertThat(snapyrNotification.actionToken).isEqualTo(TEST_ACTION_TOKEN)
        assertThat(snapyrNotification.deepLinkUrl).isNull()
        assertThat(snapyrNotification.imageUrl).isNull()

        val notificationMap = snapyrNotification.asValueMap()
        assertThat(notificationMap.getInt("notificationId", -1)).isEqualTo(snapyrNotification.notificationId)
        assertThat(notificationMap.getString("titleText")).isEqualTo("Tap this")
        assertThat(notificationMap.getString("subtitleText")).isNull()
        assertThat(notificationMap.getString("contentText")).isEqualTo("We really want you to tap this notification")
        assertThat(notificationMap.getString("actionToken")).isEqualTo(TEST_ACTION_TOKEN)
        assertThat(notificationMap.getString("deepLinkUrl")).isNull()
        assertThat(notificationMap.getString("imageUrl")).isNull()
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    @Throws(IOException::class)
    fun testNotificationMissingRequiredField() {
        val dataMap = HashMap<String, String>()
        dataMap.set(
            "snapyr",
            """
            {
                "subtitle": "Please",
                "contentText": "We really want you to tap this notification",
                "deepLinkUrl": "snapyrsample://test/123",
                "imageUrl": "https://images-na.ssl-images-amazon.com/images/S/pv-target-images/fb1fd46fbac48892ef9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg",
                "pushTemplate": {
                    "id": "b29f9018-ea25-4bf5-8a9b-d8d6a8bc29a1",
                    "modified": "2022-11-10T21:20:15.409Z"
                },
                "actionToken": "Zjk1OTkxZGEtZWE5Yy00ZTQ0LTk5OGQtNWZmNWY0Y2EwNGQzOmIyOWY5MDE4LWVhMjUtNGJmNS04YTliLWQ4ZDZhOGJjMjlhMTpmYmYxMTljMS1lMjNlLTQxZDEtODkyMi05MjE5M2RlNGRkODk6NGZjOGE5NGQtNGU0Zi00NTQ4LWE3NjYtMzM1NDY2YmViMzBjOnB1Ymxpc2g6NGY5YTA0NzMtYWY4OS00M2QyLWEyZTMtZmFiN2FkNmU0MzUyOnBhdWwxMTExYTp0YmQ6MTY2ODY0ODQzOTphY3Rpb25Ub2tlbi00ZjlhMDQ3My1hZjg5LTQzZDItYTJlMy1mYWI3YWQ2ZTQzNTIucGF1bDExMTFhLjE2Njg2NDg0Mzk="
            }
        """.trimMargin()
        )
        val remoteMessage = RemoteMessage.Builder("fakefirebasetoken")
            .setData(dataMap)
            .build()
        val snapyrNotification = SnapyrNotification(remoteMessage)
    }

    @Test(expected = NonSnapyrMessageException::class)
    @Throws(IOException::class)
    fun testNonSnapyrNotification() {
        val dataMap = HashMap<String, String>()

        dataMap.set("subtitle", "Please")
        dataMap.set("contentText", "We really want you to tap this notification")
        dataMap.set("deepLinkUrl", "snapyrsample://test/123")
        dataMap.set(
            "imageUrl",
            "https://images-na.ssl-images-amazon.com/images/S/pv-target-images/fb1fd46fbac48892e" +
                "f9ba8c78f1eb6fa7d005de030b2a3d17b50581b2935832f._SX1080_.jpg"
        )
        dataMap.set(
            "pushTemplate",
            """{
            "id": "b29f9018-ea25-4bf5-8a9b-d8d6a8bc29a1",
            "modified": "2022-11-10T21:20:15.409Z"
        }"""
        )
        dataMap.set("actionToken", TEST_ACTION_TOKEN)

        val remoteMessage = RemoteMessage.Builder("fakefirebasetoken")
            .setData(dataMap)
            .build()
        val snapyrNotification = SnapyrNotification(remoteMessage)
    }
}
