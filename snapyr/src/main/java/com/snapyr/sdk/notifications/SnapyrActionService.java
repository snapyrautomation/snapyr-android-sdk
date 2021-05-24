package com.snapyr.sdk.notifications;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.Nullable;

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
        String x = intent.getStringExtra("snapyrDeepLink");
        String y = intent.getStringExtra("actionId");

        Uri uri = Uri.parse(x);
        Intent openDeepLinkIntent = new Intent(Intent.ACTION_VIEW, uri);
        openDeepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Snapyr.with(this).getApplication().startActivity(openDeepLinkIntent);
//        startActivity(openDeepLinkIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
