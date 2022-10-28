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

import static com.snapyr.sdk.notifications.SnapyrNotificationHandler.showFlags;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.Traits;
import com.snapyr.sdk.inapp.InAppMessage;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final String ANALYTICS_WRITE_KEY =
            "Kl34fEmzG753oODf9UhGz76wYMXW6Gia"; // Paul's push/in-app testing WS - PROD

    private String currentActionToken;

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

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Get extra data included in the Intent
                    String message = intent.getStringExtra("message");
                    InAppMessage inAppMessage =
                            (InAppMessage) intent.getParcelableExtra("inAppMessage");
                    Log.d(
                            "YYY",
                            MessageFormat.format(
                                    "Got message: {0} inAppMessage: {1}", message, inAppMessage));
                    currentActionToken = inAppMessage.ActionToken;
                }
            };

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.e("YYY", MessageFormat.format("onSaveInstanceState: {0}", outState));
        outState.putString("paul", "test123");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.e("YYY", MessageFormat.format("onRestoreInstanceState: {0}", savedInstanceState));
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("XXX", MessageFormat.format("MainActivity: onCreate, instancestate: {0}", savedInstanceState));
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver, new IntentFilter("sample-intent-passer"));

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



        // For debugging - destroys the FB token so a new one will be created. Probably does some
        // other stuff too
        //        FirebaseInstallations.getInstance().delete();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent != null) {
            showFlags(intent);
            handleOpenIntent(intent);
        }

        addLog("onCreate", "activity created");

        Log.e("XXX", "MainActivity: onCreate DONE");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e("XXX", MessageFormat.format("MainActivity: onNewIntent: {0} data: {1} extras: {2}", intent, intent.getData(), intent.getExtras()));
        addLog("onNewIntent", MessageFormat.format("received intent: {0}", intent));
        showFlags(intent);
        handleOpenIntent(intent);
    }

    @Override
    protected void onStart() {
        Log.e("XXX", MessageFormat.format("MainActivity: onStart. intent: {0} extras: {1}", this.getIntent(), this.getIntent().getExtras()));
        showFlags(this.getIntent());
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
            addLog("handleOpenIntent", "No deep link info provided");
            return;
        }
        addLog("handleOpenIntent", "deep link Uri: " + data);
        List<String> paths = data.getPathSegments();
        if (paths.size() > 1) {
            String response = paths.get(0);
            String text = paths.get(1);
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            Log.e("Snapyr", "Sample app open intent data: " + data);
            Log.e("Snapyr", "SHOULD SHOW TOAST SAYING::: " + text);
        }
    }

    @OnClick(R.id.send_sample_broadcast)
    void onSendBroadcast() {
        sendBroadcast("com.snapyr.sdk.sample.ACTION_DEEPLINK");
    }

    @OnClick(R.id.send_sdk_broadcast)
    void onSendSdkBroadcast() {
        sendBroadcast("com.snapyr.sdk.ACTION_DEEPLINK");
    }

    @OnClick(R.id.send_notif_broadcast)
    void onSendNotifBroadcast() {
        sendBroadcast("com.snapyr.sdk.notifications.ACTION_DEEPLINK");
    }



    @SuppressLint("NewApi")
    private void sendBroadcast(String action) {
        Log.e("YYY", "Send SDK broadcast 13");
        Intent intent = new Intent(action);

        PackageManager packageManager = this.getApplicationContext().getPackageManager();
        List<ResolveInfo> receivers = packageManager.queryBroadcastReceivers(intent, 0);

        for (ResolveInfo receiver : receivers) {
            Log.e(
                    "YYY",
                    MessageFormat.format(
                            "Receiver: {0} Packagename: {1} ServiceInfo: {2} ActivityInfo: {3} ProviderInfo: {4} isDefault: {5}",
                            receiver,
                            receiver.resolvePackageName,
                            receiver.serviceInfo,
                            receiver.activityInfo,
                            receiver.providerInfo,
                            receiver.isDefault));
        }

        intent.setPackage(this.getApplicationContext().getPackageName());
        sendBroadcast(intent);
        Log.e("YYY", MessageFormat.format("BROADCAST SENT FROM ACTIVITY: {0}", intent));
        addLog("broadcast", MessageFormat.format("broadcast sent from activity: {0}", intent));
    }

    private void addLog(String name, String description) {
        TextView logView = (TextView) this.findViewById(R.id.event_log);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        Date date = new Date();
        Spanned newEntry = Html.fromHtml(String.format("%s: <b>%s</b>: %s<br />", formatter.format(date), name, description));
        if (logView == null) {
            Log.e("Snapyr.sample", "addLog: could not find view. Log: " + newEntry);
            return;
        }

//        logView.p
//        logView.setText(String.format("%s<br />%s", newEntry, logView.getText()));
        Editable editableText = logView.getEditableText();
        editableText.insert(0, newEntry);
        logView.setText(editableText);
//        logView.append(newEntry);
    }

    @OnClick(R.id.track_impression)
    void onTrackImpressionClicked() {
        Snapyr.with(this).trackInAppMessageImpression(currentActionToken);
        Snapyr.with(this).flush();
    }

    @OnClick(R.id.track_click)
    void onTrackClickClicked() {
        Snapyr.with(this).trackInAppMessageClick(currentActionToken);
        Snapyr.with(this).flush();
    }

    @OnClick(R.id.track_dismiss)
    void onTrackDismissClicked() {
        Snapyr.with(this).trackInAppMessageDismiss(currentActionToken);
        Snapyr.with(this).flush();
    }

    @OnClick(R.id.action_track_a)
    void onButtonAClicked() {
        Snapyr.with(this).track("pushTest");
        Snapyr.with(this).flush();
        addLog("track", "pushTest");
    }

    @OnClick(R.id.action_track_b)
    void onButtonBClicked() {
        Snapyr.with(this).track("pushTestAll");
        Snapyr.with(this).flush();
        addLog("track", "pushTestAll");
    }

    @OnClick(R.id.action_show_notification)
    void onShowNotifyClicked() {
        Snapyr.with(this).getNotificationHandler().showSampleNotification();
        addLog("notification", "triggered sample notification");
    }

    @OnClick(R.id.action_identify)
    void onIdentifyButtonClicked() {
        String id = userId.getText().toString();
        if (isNullOrEmpty(id)) {
            Toast.makeText(this, R.string.id_required, Toast.LENGTH_LONG).show();
        } else {
            Traits traits = new Traits().putValue("testAmount", 100);
            Snapyr.with(this).identify(id, traits, null);
            Snapyr.with(this).flush();
            addLog("identify", "identified user id: " + id);
        }
    }

    @OnClick(R.id.action_flush)
    void onFlushButtonClicked() {
        Snapyr.with(this).flush();
        addLog("flush", "");
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
