package com.snapyr.sdk.notifications;

import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;

public class GoogleZzaParcelable implements Parcelable {
    private Messenger zza;
    private GoogleIMessengerCompat zzb;
    public static final Creator<GoogleZzaParcelable> CREATOR = new GoogleZzcParcelableCreator();

    public GoogleZzaParcelable(IBinder var1) {
        if (Build.VERSION.SDK_INT >= 21) {
            this.zza = new Messenger(var1);
        } else {
            this.zzb = new GoogleIMessengerCompat.Proxy(var1);
        }
    }

    public final void zza(Message var1) throws RemoteException {
        if (this.zza != null) {
            this.zza.send(var1);
        } else {
            this.zzb.send(var1);
        }
    }

    private final IBinder zza() {
        return this.zza != null ? this.zza.getBinder() : this.zzb.asBinder();
    }

    public boolean equals(@Nullable Object var1) {
        if (var1 == null) {
            return false;
        } else {
            try {
                return this.zza().equals(((GoogleZzaParcelable)var1).zza());
            } catch (ClassCastException var2) {
                return false;
            }
        }
    }

    public int hashCode() {
        return this.zza().hashCode();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel var1, int var2) {
        if (this.zza != null) {
            var1.writeStrongBinder(this.zza.getBinder());
        } else {
            var1.writeStrongBinder(this.zzb.asBinder());
        }
    }

    public static final class zza extends ClassLoader {
        public zza() {
        }

        protected final Class<?> loadClass(String var1, boolean var2) throws ClassNotFoundException {
            if (!"com.google.android.gms.iid.MessengerCompat".equals(var1)) {
                return super.loadClass(var1, var2);
            } else {
                if (Log.isLoggable("CloudMessengerCompat", Log.DEBUG) || Build.VERSION.SDK_INT == 23 && Log.isLoggable("CloudMessengerCompat", Log.DEBUG)) {
                    Log.d("CloudMessengerCompat", "Using renamed FirebaseIidMessengerCompat class");
                }

                return com.google.android.gms.cloudmessaging.zza.class;
            }
        }
    }
}
