package com.snapyr.sdk.notifications;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.IntentCompat;

import com.snapyr.sdk.Properties;
import com.snapyr.sdk.Snapyr;

public class SnapyrActionService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deepLinkUrl = intent.getStringExtra(SnapyrNotificationHandler.NOTIF_DEEP_LINK_KEY);
        String actionId = intent.getStringExtra(SnapyrNotificationHandler.ACTION_ID_KEY);
        int notificationId = intent.getIntExtra("notificationId", 0);

        SnapyrNotificationHandler.INTERACTION_TYPE interactionType = (SnapyrNotificationHandler.INTERACTION_TYPE) intent.getSerializableExtra(SnapyrNotificationHandler.INTERACTION_KEY);
        String token;

        Properties props = new Properties()
                .putValue("deepLinkUrl", deepLinkUrl)
                .putValue("actionId", actionId)
                .putValue("testVersion", 6);

        switch (interactionType) {
            case NOTIFICATION_PRESS:
                token = intent.getStringExtra(SnapyrNotificationHandler.NOTIF_TOKEN_KEY);
                props.putValue(SnapyrNotificationHandler.NOTIF_TOKEN_KEY, token)
                    .putValue("interactionType", "notificationPressed");
                break;
            case ACTION_BUTTON_PRESS:
                token = intent.getStringExtra(SnapyrNotificationHandler.ACTION_TOKEN_KEY);
                props.putValue(SnapyrNotificationHandler.ACTION_TOKEN_KEY, token)
                        .putValue("interactionType", "actionButtonPressed");
                break;
        }

        // if autocancel = true....
        // Dismiss source notification
        NotificationManagerCompat.from(this.getApplicationContext()).cancel(notificationId);
        // Close notification drawer (so newly opened activity isn't behind anything)
        this.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        Snapyr.with(this).pushNotificationClicked(props);

        Uri uri = Uri.parse(deepLinkUrl);
        Intent openDeepLinkIntent = new Intent(Intent.ACTION_VIEW, uri);
        openDeepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Snapyr.with(this).getApplication().startActivity(openDeepLinkIntent);
        // Snapyr.with(this.getApplicationContext()).getApplication().startActivity(openDeepLinkIntent);
        // this.getApplication().startActivity(openDeepLinkIntent);
        // startActivity(openDeepLinkIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @return an intent that launches the default activity for the app, i.e. "open the app"
     * when the notification is clicked.
     */
    public PendingIntent getDefaultIntent() {
        String packageName = this.getPackageName();
        Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);
        return PendingIntent.getActivity(this.getApplicationContext(), 0, launchIntent, 0);
    }
}
