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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.Traits;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import java.util.List;

public class MainActivity extends Activity {
    private static final String ANALYTICS_WRITE_KEY =
            "Kl34fEmzG753oODf9UhGz76wYMXW6Gia"; // Paul's push/in-app testing WS - PROD

    @BindView(R.id.user_id)
    EditText userId;

    public MainActivity() {
        Log.e("XXX", "MainActivity: CONSTRUCTOR");
        //        android.os.Debug.waitForDebugger();
        //        Log.e("XXX", "SampleApp: onCreate - resuming.");
    }

    @Override
    protected void finalize() throws Throwable {
        Log.e("XXX", "MainActivity: FINALIZE");
        super.finalize();
    }

    /** Returns true if the string is null, or empty (when trimmed). */
    public static boolean isNullOrEmpty(String text) {
        return TextUtils.isEmpty(text) || text.trim().length() == 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("XXX", "MainActivity: onCreate");
        super.onCreate(savedInstanceState);

        //                Snapyr.Builder builder =
        //                        new Snapyr.Builder(this, ANALYTICS_WRITE_KEY)
        //        //                        .enableDevEnvironment()
        //                                .experimentalNanosecondTimestamps()
        //                                .trackApplicationLifecycleEvents()
        //                                .trackDeepLinks()
        //                                .logLevel(Snapyr.LogLevel.VERBOSE)
        //                                .defaultProjectSettings(
        //                                        new ValueMap()
        //                                                .putValue(
        //                                                        "integrations",
        //                                                        new ValueMap()
        //                                                                .putValue(
        //                                                                        "Snapyr",
        //                                                                        new ValueMap()
        //                                                                                .putValue(
        //
        // "apiKey",
        //
        //         ANALYTICS_WRITE_KEY)
        //                                                                                .putValue(
        //
        //         "trackAttributionData",
        //
        // true))))
        //                                .flushQueueSize(1)
        //                                .enableSnapyrPushHandling()
        //                                .recordScreenViews();
        //
        //                // Set the initialized instance as a globally accessible instance.
        //                Snapyr.setSingletonInstance(builder.build());

        Intent intent = getIntent();
        if (intent != null) {
            handleOpenIntent(intent);
        }

        // For debugging - destroys the FB token so a new one will be created. Probably does some
        // other stuff too
        //        FirebaseInstallations.getInstance().delete();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Log.e("XXX", "MainActivity: onCreate DONE");
    }

    @Override
    protected void onStart() {
        Log.e("XXX", "MainActivity: onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.e("XXX", "MainActivity: onResume");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.e("XXX", "MainActivity: onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("XXX", "MainActivity: onDestroy");
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
            Log.e("Snapyr", "SHOULD SHOW TOAST SAYING::: " + text);
        }
    }

    @OnClick(R.id.action_track_a)
    void onButtonAClicked() {
        Snapyr.with(this).track("pushTest");
    }

    @OnClick(R.id.action_track_b)
    void onButtonBClicked() {
        Snapyr.with(this).track("pushTestAll");
    }

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
}
