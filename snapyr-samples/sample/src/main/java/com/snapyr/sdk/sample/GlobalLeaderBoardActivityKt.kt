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
package com.snapyr.sdk.sample

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import butterknife.OnClick
import com.snapyr.sdk.Snapyr
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.Date

class GlobalLeaderBoardActivityKt : Activity() {
//    @JvmField
//    @BindView(R.id.user_id)
//    var userId: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For debugging - destroys the FB token so a new one will be created. Probably does some
        // other stuff too
        //        FirebaseInstallations.getInstance().delete();
        setContentView(R.layout.activity_global_leaderboard)
//        ButterKnife.bind(this)
        val intent = intent
        this.addLog("onCreate", MessageFormat.format("intent: {0}", intent))
        intent?.let { handleOpenIntent(it) }
    }

    override fun onStart() {
        super.onStart()
        Log.i("Snapyr.Sample", "onStart: taskId: $taskId")
    }

    override fun onNewIntent(intent: Intent) {
        addLog("onNewIntent", MessageFormat.format("intent: {0}", intent))
        handleOpenIntent(intent)
        // `onNewIntent` doesn't automatically update the intent on the activity. Do that explicitly
        // so any later activity calls to `getIntent()` get this new version
        this.intent = intent
        super.onNewIntent(intent)
    }

    fun handleOpenIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        if (data == null) {
            Toast.makeText(this, "No deep link info provided", Toast.LENGTH_LONG).show()
            return
        }
        val paths = data.pathSegments
        if (paths.size > 1) {
            val response = paths[0]
            val text = paths[1]
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            Log.e("Snapyr", "Sample app open intent data: $data")
        }
    }

    @OnClick(R.id.action_track_a_leaderboard)
    fun onButtonAClicked() {
        Snapyr.with(this).track("pushTest")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_view_docs) {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://developers.snapyr.com/android.html"))
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    private fun addLog(name: String) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS")
        val date = Date()
        val newEntry =
            Html.fromHtml(String.format("<p>%s: <b>%s</b></p>", formatter.format(date), name))
        prependLogEntry(newEntry)
    }

    private fun addLog(name: String, description: String) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS")
        val date = Date()
        val newEntry = Html.fromHtml(
            String.format(
                "<p>%s: <b>%s</b>: %s</p>",
                formatter.format(date), name, description
            )
        )
        prependLogEntry(newEntry)
    }

    private fun prependLogEntry(newEntry: Spanned) {
        val logView = findViewById<TextView>(R.id.event_log_leaderboard)
        Log.i("Snapyr.Sample", newEntry.toString())
        if (logView == null) {
            Log.e("Snapyr.sample", "addLog: could not find event_log view")
            return
        }
        // Prepend so the latest entry shows up on top
        val editableText = logView.editableText
        editableText.insert(0, newEntry)
        logView.text = editableText
    }

    companion object {
        /** Returns true if the string is null, or empty (when trimmed).  */
        fun isNullOrEmpty(text: String): Boolean {
            return TextUtils.isEmpty(text) || text.trim { it <= ' ' }.length == 0
        }
    }
}
