package com.snapyr.sdk.notifications;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;

//import com.google.android.gms.cloudmessaging.zzo;

final class GoogleZzo {
    @Nullable
    private final Messenger zza;
    @Nullable
    private final GoogleZzaParcelable zzb;

    GoogleZzo(IBinder var1) throws RemoteException {
        String var2 = var1.getInterfaceDescriptor();
        if ("android.os.IMessenger".equals(var2)) {
            this.zza = new Messenger(var1);
            this.zzb = null;
        } else if ("com.google.android.gms.iid.IMessengerCompat".equals(var2)) {
            this.zzb = new GoogleZzaParcelable(var1);
            this.zza = null;
        } else {
            this.zza = null;
            this.zzb = null;
            String var10002 = String.valueOf(var2);
            String var10001;
            if (var10002.length() != 0) {
                var10001 = "Invalid interface descriptor: ".concat(var10002);
            } else {
                String var10003 = new String;
                var10001 = var10003;
//                var10003.<init>("Invalid interface descriptor: ");
            }

            Log.w("MessengerIpcClient", var10001);
            throw new RemoteException();
        }
    }

    final void zza(Message var1) throws RemoteException {
        if (this.zza != null) {
            this.zza.send(var1);
        } else if (this.zzb != null) {
            this.zzb.zza(var1);
        } else {
            throw new IllegalStateException("Both messengers are null");
        }
    }
}
