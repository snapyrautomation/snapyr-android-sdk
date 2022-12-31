package com.snapyr.sdk.notifications;

//import com.google.android.gms.internal.cloudmessaging.zzc;
//import com.google.android.gms.internal.cloudmessaging.zzd;

public final class GoogleZzaNotifExecutorHelper {
    private static final GoogleZzbNotifExecutorInterface otherExecutorIdunno;
    private static volatile GoogleZzbNotifExecutorInterface notifExecutor;

    public static GoogleZzbNotifExecutorInterface zza() {
        return notifExecutor;
    }

    static {
//        zzb = GoogleZzaParcelable = new zzc((zzd)null);
        notifExecutor = otherExecutorIdunno = new GoogleNotifExecutor();
    }
}