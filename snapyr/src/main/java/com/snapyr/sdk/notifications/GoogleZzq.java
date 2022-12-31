package com.snapyr.sdk.notifications;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cloudmessaging.zzp;
import com.google.android.gms.tasks.TaskCompletionSource;

abstract class GoogleZzq<T> {
    final int zza;
    final TaskCompletionSource<T> zzb = new TaskCompletionSource();
    final int zzc;
    final Bundle zzd;

    GoogleZzq(int var1, int var2, Bundle var3) {
        this.zza = var1;
        this.zzc = var2;
        this.zzd = var3;
    }

    abstract boolean zza();

    final void zza(T var1) {
        if (Log.isLoggable("MessengerIpcClient", Log.DEBUG)) {
            String var2 = String.valueOf(this);
            String var3 = String.valueOf(var1);
            Log.d("MessengerIpcClient", (new StringBuilder(16 + String.valueOf(var2).length() + String.valueOf(var3).length())).append("Finishing ").append(var2).append(" with ").append(var3).toString());
        }

        this.zzb.setResult(var1);
    }

    final void zza(zzp var1) {
        if (Log.isLoggable("MessengerIpcClient", Log.DEBUG)) {
            String var2 = String.valueOf(this);
            String var3 = String.valueOf(var1);
            Log.d("MessengerIpcClient", (new StringBuilder(14 + String.valueOf(var2).length() + String.valueOf(var3).length())).append("Failing ").append(var2).append(" with ").append(var3).toString());
        }

        this.zzb.setException(var1);
    }

    abstract void zzb(Bundle var1);

    public String toString() {
        int var1 = this.zzc;
        int var2 = this.zza;
        boolean var3 = this.zza();
        return (new StringBuilder(55)).append("Request { what=").append(var1).append(" id=").append(var2).append(" oneWay=").append(var3).append("}").toString();
    }
}
