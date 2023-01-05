package com.snapyr.sdk.notifications;

import android.app.PendingIntent;
//import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.content.BroadcastReceiver.PendingResult;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.android.gms.cloudmessaging.CloudMessage;
import com.google.android.gms.common.util.concurrent.NamedThreadFactory;
//import com.google.android.gms.internal.cloudmessaging.GoogleZzaParcelable;
import com.google.android.gms.internal.cloudmessaging.zzf;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;



public abstract class BaseCloudMessagingReceiver extends BroadcastReceiver {
    private final ExecutorService zza;

    public BaseCloudMessagingReceiver() {
        this.zza = GoogleZzaNotifExecutorHelper.zza().zza(new NamedThreadFactory("firebase-iid-executor"), /*GoogleZzf.GoogleZzaParcelable*/ 1);
    }

    @NonNull
    protected Executor getBroadcastExecutor() {
        return this.zza;
    }

    @WorkerThread
    protected abstract int onMessageReceive(@NonNull Context var1, @NonNull CloudMessage var2);

    @WorkerThread
    protected void onNotificationOpen(@NonNull Context var1, @NonNull Bundle var2) {
    }

    @WorkerThread
    protected void onNotificationDismissed(@NonNull Context var1, @NonNull Bundle var2) {
    }

    public final void onReceive(@NonNull Context var1, @NonNull Intent var2) {
        if (var2 != null) {
            boolean var3 = this.isOrderedBroadcast();
            PendingResult var4 = this.goAsync();
            this.getBroadcastExecutor().execute(new GoogleRunnableThing(this, var2, var1, var3, var4));
        }
    }

    @WorkerThread
    final int zza(@NonNull Context var1, @NonNull Intent var2) {
        PendingIntent var3;
        if ((var3 = (PendingIntent)var2.getParcelableExtra("pending_intent")) != null) {
            try {
                var3.send();
            } catch (PendingIntent.CanceledException var5) {
                Log.e("CloudMessagingReceiver", "Notification pending intent canceled");
            }
        }

        Bundle var4;
        if ((var4 = var2.getExtras()) != null) {
            var4.remove("pending_intent");
        } else {
            var4 = new Bundle();
        }

        if ("com.google.firebase.messaging.NOTIFICATION_OPEN".equals(var2.getAction())) {
            this.onNotificationOpen(var1, var4);
        } else {
            if (!"com.google.firebase.messaging.NOTIFICATION_DISMISS".equals(var2.getAction())) {
                Log.e("CloudMessagingReceiver", "Unknown notification action");
                return 500;
            }

            this.onNotificationDismissed(var1, var4);
        }

        return -1;
    }

    @WorkerThread
    private final int zzb(@NonNull Context var1, @NonNull Intent var2) {
        if (var2.getExtras() == null) {
            return 500;
        } else {
            String var3 = var2.getStringExtra("google.message_id");
            Task var10000;
            if (TextUtils.isEmpty(var3)) {
                var10000 = Tasks.forResult((Object)null);
            } else {
                Bundle var10;
                (var10 = new Bundle()).putString("google.message_id", var3);
                var10000 = GoogleZzeTaskThing.singleton(var1).zza(2, var10);
            }

            Task var4 = var10000;
            int var5 = this.onMessageReceive(var1, new CloudMessage(var2));

            try {
                Tasks.await(var4, TimeUnit.SECONDS.toMillis(1L), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException var11) {
                String var7 = String.valueOf(var11);
                Log.w("CloudMessagingReceiver", (new StringBuilder(20 + String.valueOf(var7).length())).append("Message ack failed: ").append(var7).toString());
            }

            return var5;
        }
    }

    public static final class IntentKeys {
        @NonNull
        public static final String PENDING_INTENT = "pending_intent";
        @NonNull
        public static final String WRAPPED_INTENT = "wrapped_intent";

        private IntentKeys() {
        }
    }

    public static final class IntentActionKeys {
        @NonNull
        public static final String NOTIFICATION_OPEN = "com.google.firebase.messaging.NOTIFICATION_OPEN";
        @NonNull
        public static final String NOTIFICATION_DISMISS = "com.google.firebase.messaging.NOTIFICATION_DISMISS";

        private IntentActionKeys() {
        }
    }
}

