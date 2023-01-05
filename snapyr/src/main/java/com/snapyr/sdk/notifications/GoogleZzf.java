package com.snapyr.sdk.notifications;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

//import com.google.android.gms.cloudmessaging.zzf;
//import com.google.android.gms.cloudmessaging.zzh;
//import com.google.android.gms.cloudmessaging.zzi;
//import com.google.android.gms.cloudmessaging.zzj;
//import com.google.android.gms.cloudmessaging.zzk;
//import com.google.android.gms.cloudmessaging.zzm;
//import com.google.android.gms.cloudmessaging.GoogleZzo;
import com.google.android.gms.cloudmessaging.zzh;
import com.google.android.gms.cloudmessaging.zzp;
//import com.google.android.gms.cloudmessaging.zzq;
import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.common.stats.ConnectionTracker;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
//import javax.annotation.concurrent.GuardedBy;

final class GoogleZzf implements ServiceConnection {
    private final GoogleZzeTaskThing zzf;
    //    @GuardedBy("this")
    int zza;
    final Messenger zzb;
    GoogleZzo zzc;
//    @GuardedBy("this")
    final Queue<GoogleZzq<?>> zzd;
//    @GuardedBy("this")
    final SparseArray<GoogleZzq<?>> zze;

    GoogleZzf(GoogleZzeTaskThing var1) {
        this.zzf = var1;
        this.zza = 0;
        this.zzb = new Messenger(new GoogleZzeGmsInternalCMExtendsHandler(Looper.getMainLooper(), new GoogleZzi(this)));
        this.zzd = new ArrayDeque();
        this.zze = new SparseArray();
    }

    synchronized boolean zza(GoogleZzq<?> var1) {
        switch(this.zza) {
            case 0:
                this.zzd.add(var1);
                Preconditions.checkState(this.zza == 0);
                if (Log.isLoggable("MessengerIpcClient", Log.VERBOSE)) {
                    Log.v("MessengerIpcClient", "Starting bind to GmsCore");
                }

                this.zza = 1;
                Intent var4;
                (var4 = new Intent("com.google.android.c2dm.intent.REGISTER")).setPackage("com.google.android.gms");
//                if (!ConnectionTracker.getInstance().bindService(GoogleZzeTaskThing.zza(this.zzf), var4, this, 1)) {
                if (!ConnectionTracker.getInstance().bindService(this.zzf.zzb, var4, this, 1)) {
                    this.zza(0, "Unable to bind to service");
                } else {
                    com.google.android.gms.cloudmessaging.zze.zzb(this.zzf).schedule(new GoogleZzhRunnable(this), 30L, TimeUnit.SECONDS);
//                    this.zzf.zzb(0, new Bundle()).sche
                    GoogleZzeTaskThing.singleton.zzb(this.zzf).schedule(new zzh(this), 30L, TimeUnit.SECONDS);
                }

                return true;
            case 1:
                this.zzd.add(var1);
                return true;
            case 2:
                this.zzd.add(var1);
                this.zza();
                return true;
            case 3:
            case 4:
                return false;
            default:
                int var2 = this.zza;
                throw new IllegalStateException((new StringBuilder(26)).append("Unknown state: ").append(var2).toString());
        }
    }

    final boolean zza(Message var1) {
        int var2 = var1.arg1;
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            Log.d("MessengerIpcClient", (new StringBuilder(41)).append("Received response to request: ").append(var2).toString());
        }

        GoogleZzq var3;
        synchronized(this) {
            if ((var3 = (GoogleZzq)this.zze.get(var2)) == null) {
                Log.w("MessengerIpcClient", (new StringBuilder(50)).append("Received response for unknown request: ").append(var2).toString());
                return true;
            }

            this.zze.remove(var2);
            this.zzb();
        }

        Bundle var7 = var1.getData();
        if (var7.getBoolean("unsupported", false)) {
            var3.zza(new zzp(4, "Not supported by GmsCore"));
        } else {
            var3.zzb(var7);
        }

        return true;
    }

    @MainThread
    public final void onServiceConnected(ComponentName var1, IBinder var2) {
        if (Log.isLoggable("MessengerIpcClient", Log.VERBOSE)) {
            Log.v("MessengerIpcClient", "Service connected");
        }

        GoogleZzeTaskThing.zzb(this.zzf).execute(new zzk(this, var2));
    }

    final void zza() {
        GoogleZzeTaskThing.zzb(this.zzf).execute(new zzj(this));
    }

    @MainThread
    public final void onServiceDisconnected(ComponentName var1) {
        if (Log.isLoggable("MessengerIpcClient", 2)) {
            Log.v("MessengerIpcClient", "Service disconnected");
        }

        GoogleZzeTaskThing.zzb(this.zzf).execute(new zzm(this));
    }

    final synchronized void zza(int var1, @Nullable String var2) {
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            String var10002 = String.valueOf(var2);
            String var10001;
            if (var10002.length() != 0) {
                var10001 = "Disconnected: ".concat(var10002);
            } else {
//                String var10003 = new String;
                String var10003 = "";
                var10001 = var10003;
//                var10003.<init>("Disconnected: ");
            }

            Log.d("MessengerIpcClient", var10001);
        }

        switch(this.zza) {
            case 0:
                throw new IllegalStateException();
            case 1:
            case 2:
                if (Log.isLoggable("MessengerIpcClient", 2)) {
                    Log.v("MessengerIpcClient", "Unbinding service");
                }

                this.zza = 4;
                ConnectionTracker.getInstance().unbindService(GoogleZzeTaskThing.zza(this.zzf), this);
                zzp var5 = new zzp(var1, var2);
                GoogleZzf var4 = this;
                Iterator var6 = this.zzd.iterator();

                while(var6.hasNext()) {
                    ((GoogleZzq)var6.next()).zza(var5);
                }

                this.zzd.clear();

                for(int var7 = 0; var7 < var4.zze.size(); ++var7) {
                    ((GoogleZzq)var4.zze.valueAt(var7)).zza(var5);
                }

                var4.zze.clear();
                return;
            case 3:
                this.zza = 4;
                return;
            case 4:
                return;
            default:
                int var3 = this.zza;
                throw new IllegalStateException((new StringBuilder(26)).append("Unknown state: ").append(var3).toString());
        }
    }

    final synchronized void zzb() {
        if (this.zza == 2 && this.zzd.isEmpty() && this.zze.size() == 0) {
            if (Log.isLoggable("MessengerIpcClient", Log.VERBOSE)) {
                Log.v("MessengerIpcClient", "Finished handling requests, unbinding");
            }

            this.zza = 3;
            ConnectionTracker.getInstance().unbindService(GoogleZzeTaskThing.zza(this.zzf), this);
        }

    }

    final synchronized void zzc() {
        if (this.zza == 1) {
            this.zza(1, "Timed out while binding");
        }

    }

    final synchronized void zza(int var1) {
        GoogleZzq var2;
        if ((var2 = (GoogleZzq)this.zze.get(var1)) != null) {
            Log.w("MessengerIpcClient", (new StringBuilder(31)).append("Timing out request: ").append(var1).toString());
            this.zze.remove(var1);
            var2.zza(new zzp(3, "Timed out waiting for response"));
            this.zzb();
        }

    }
}
