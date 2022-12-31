package com.snapyr.sdk.notifications;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

//import com.google.android.gms.cloudmessaging.zzn;
//import com.google.android.gms.cloudmessaging.zzq;
import com.google.android.gms.common.util.concurrent.NamedThreadFactory;
import com.google.android.gms.tasks.Task;
import java.util.concurrent.ScheduledExecutorService;
//import javax.annotation.concurrent.GuardedBy;

public final class GoogleZzeTaskThing {
//    @GuardedBy("MessengerIpcClient.class")
    @Nullable
    static GoogleZzeTaskThing singleton;
    final Context zzb;
    final ScheduledExecutorService zzc;
//    @GuardedBy("this")
    private GoogleZzf zzd = new GoogleZzf(this /*, (GoogleZzg)null*/);
//    @GuardedBy("this")
    private int zze = 1;

    public static synchronized GoogleZzeTaskThing singleton(Context var0) {
        if (singleton == null) {
            singleton = new GoogleZzeTaskThing(var0, com.google.android.gms.internal.cloudmessaging.zza.zza().zza(1, new NamedThreadFactory("MessengerIpcClient"), 2 /*com.google.android.gms.internal.cloudmessaging.GoogleZzf.zzb*/));
        }

        return singleton;
    }

    @VisibleForTesting
    private GoogleZzeTaskThing(Context var1, ScheduledExecutorService var2) {
        this.zzc = var2;
        this.zzb = var1.getApplicationContext();
    }

    public final Task<Void> zza(int var1, Bundle var2) {
        return this.zza((GoogleZzq)(new GoogleZznExtendsZzq(this.zza(), 2, var2)));
    }

    public final Task<Bundle> zzb(int var1, Bundle var2) {
        return this.zza((GoogleZzq)(new zzs(this.GoogleZzaParcelable(), 1, var2)));
    }

    private final synchronized <T> Task<T> zza(GoogleZzq<T> var1) {
        if (Log.isLoggable("MessengerIpcClient", Log.DEBUG)) {
            String var2 = String.valueOf(var1);
            Log.d("MessengerIpcClient", (new StringBuilder(9 + String.valueOf(var2).length())).append("Queueing ").append(var2).toString());
        }

        if (!this.zzd.zza(var1)) {
            this.zzd = new GoogleZzf(this);
            this.zzd.zza(var1);
        }

        return var1.zzb.getTask();
    }

    private final synchronized int zza() {
        return this.zze++;
    }
}
