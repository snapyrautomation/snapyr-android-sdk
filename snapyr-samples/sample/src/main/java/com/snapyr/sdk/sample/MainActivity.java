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
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.Traits;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    @BindView(R.id.user_id)
    EditText userId;

    /** Returns true if the string is null, or empty (when trimmed). */
    public static boolean isNullOrEmpty(String text) {
        return TextUtils.isEmpty(text) || text.trim().length() == 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // For debugging - destroys the FB token so a new one will be created. Probably does some
        // other stuff too
        //        FirebaseInstallations.getInstance().delete();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        this.addLog("onCreate", MessageFormat.format("intent: {0}", intent));
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
        addLog("onNewIntent", MessageFormat.format("intent: {0}", intent));
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

    @OnClick(R.id.action_track_a)
    void onButtonAClicked() {
        Snapyr.with(this).track("pushTest");
    }

    @OnClick(R.id.action_track_b)
    void onButtonBClicked() {
        Snapyr.with(this).track("Button B Clicked");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @OnClick(R.id.action_show_notification)
    void onShowNotifyClicked() {
        Snapyr.with(this).getNotificationHandler().showSampleNotification();
    }

    @OnClick(R.id.action_identify)
    void onIdentifyButtonClicked() {
        String id = userId.getText().toString();
        if (isNullOrEmpty(id)) {
            Toast.makeText(this, R.string.id_required, Toast.LENGTH_LONG).show();
        } else {
            Traits traits = new Traits().putValue("testAmount", 100);
            Snapyr.with(this).identify(id, traits, null);
        }
    }

    @OnClick(R.id.action_flush)
    void onFlushButtonClicked() {
        Snapyr.with(this).flush();
    }

    @OnClick(R.id.action_reset_pushtoken)
    void onResetPushTokenClicked() {
        new Thread() {
            @Override
            public void run() {
                FirebaseInstallations installationInst = FirebaseInstallations.getInstance();
                Task<Void> deleteInstTask = installationInst.delete();
                try {
                    Tasks.await(deleteInstTask);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Snapyr", "Error deleting FB instance", e);
                    addLog("Push token reset", "ERROR deleting FB instance");
                    return;
                }

                FirebaseMessaging messagingInst = FirebaseMessaging.getInstance();

                Task<Void> deleteTokenTask = messagingInst.deleteToken();
                try {
                    Tasks.await(deleteTokenTask);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Snapyr", "Error deleting previous FB token", e);
                    addLog("Push token reset", "ERROR deleting previous token");
                    return;
                }

                Task<String> tokenTask = messagingInst.getToken();
                try {
                    String token = Tasks.await(tokenTask);
                    Log.e("Snapyr", "New FB token: " + token);
                    addLog("Push token reset", "New token:\n\n" + token);
                } catch (Exception e) {
                    e.printStackTrace();
                    addLog("Push token reset", "ERROR getting new token");
                }

                //                installationInst.registerFidListener()
                boolean autoInitEnabled = messagingInst.isAutoInitEnabled();
            }
        }.start();
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
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://snapyr.com/docs/"));
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
        TextView logView = this.findViewById(R.id.event_log);
        Log.i("Snapyr.Sample", String.valueOf(newEntry));
        if (logView == null) {
            Log.e("Snapyr.sample", "addLog: could not find event_log view");
            return;
        }
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Prepend so the latest entry shows up on top
                        Editable editableText = logView.getEditableText();
                        editableText.insert(0, newEntry);
                        logView.setText(editableText);
                    }
                });
    }
}
