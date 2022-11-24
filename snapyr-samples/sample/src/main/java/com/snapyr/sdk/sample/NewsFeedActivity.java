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
package com.snapyr.sdk.sample;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.notifications.SnapyrNotification;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NewsFeedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // For debugging - destroys the FB token so a new one will be created. Probably does some
        // other stuff too
        //        FirebaseInstallations.getInstance().delete();

        setContentView(R.layout.activity_newsfeed);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        SnapyrNotification snapyrNotification =
                intent.getExtras().getParcelable("snapyr.notification");
        addLog(
                "onCreate",
                MessageFormat.format("intent: {0}\nsnapyrNotif:{1}", intent, snapyrNotification));
        if (intent != null) {
            handleOpenIntent(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("Snapyr.Sample", "onStart: taskId: " + getTaskId());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        SnapyrNotification snapyrNotification =
                intent.getExtras().getParcelable("snapyr.notification");
        addLog("onNewIntent", MessageFormat.format("intent: {0}\n{1}", intent, snapyrNotification));
        handleOpenIntent(intent);
        // `onNewIntent` doesn't automatically update the intent on the activity. Do that explicitly
        // so any later activity calls to `getIntent()` get this new version
        this.setIntent(intent);

        super.onNewIntent(intent);
    }

    public void handleOpenIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (data == null) {
            Toast.makeText(this, "No deep link info provided", Toast.LENGTH_LONG).show();
            return;
        }
        List<String> paths = data.getPathSegments();
        if (paths.size() > 1) {
            String response = paths.get(0);
            String text = paths.get(1);
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            Log.e("Snapyr", "Sample app open intent data: " + data);
        }
    }

    @OnClick(R.id.action_track_a_newsfeed)
    void onButtonAClicked() {
        Snapyr.with(this).track("chPushTestHomescreen");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_view_docs) {
            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://developers.snapyr.com/android.html"));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    private void addLog(String name) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        Date date = new Date();
        Spanned newEntry =
                Html.fromHtml(String.format("<p>%s: <b>%s</b></p>", formatter.format(date), name));
        prependLogEntry(newEntry);
    }

    private void addLog(String name, String description) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        Date date = new Date();
        Spanned newEntry =
                Html.fromHtml(
                        String.format(
                                "<p>%s: <b>%s</b>: %s</p>",
                                formatter.format(date), name, description));
        prependLogEntry(newEntry);
    }

    private void prependLogEntry(Spanned newEntry) {
        TextView logView = this.findViewById(R.id.event_log_newsfeed);
        Log.i("Snapyr.Sample", String.valueOf(newEntry));
        if (logView == null) {
            Log.e("Snapyr.sample", "addLog: could not find event_log view");
            return;
        }
        // Prepend so the latest entry shows up on top
        Editable editableText = logView.getEditableText();
        editableText.insert(0, newEntry);
        logView.setText(editableText);
    }
}
