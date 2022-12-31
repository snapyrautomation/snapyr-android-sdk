package com.snapyr.sdk.notifications;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface GoogleIMessengerCompat extends IInterface {
    String DESCRIPTOR = "com.google.android.gms.iid.IMessengerCompat";
    int TRANSACTION_SEND = 1;

    void send(@NonNull Message var1) throws RemoteException;

    public static class Impl extends Binder implements GoogleIMessengerCompat {
        public void send(@NonNull Message var1) throws RemoteException {
            var1.arg2 = Binder.getCallingUid();
//            null.dispatchMessage(var1);
        }

        public boolean onTransact(int var1, @NonNull Parcel var2, @Nullable Parcel var3, int var4) throws RemoteException {
            var2.enforceInterface(this.getInterfaceDescriptor());
            if (var1 == 1) {
                Message var5 = var2.readInt() != 0 ? (Message)Message.CREATOR.createFromParcel(var2) : null;
                this.send(var5);
                return true;
            } else {
                return false;
            }
        }

        @NonNull
        public IBinder asBinder() {
            return this;
        }
    }

    public static class Proxy implements GoogleIMessengerCompat {
        private final IBinder zza;

        Proxy(@NonNull IBinder var1) {
            this.zza = var1;
        }

        public void send(@NonNull Message var1) throws RemoteException {
            Parcel var2;
            (var2 = Parcel.obtain()).writeInterfaceToken("com.google.android.gms.iid.IMessengerCompat");
            var2.writeInt(1);
            var1.writeToParcel(var2, 0);

            try {
                this.zza.transact(1, var2, (Parcel)null, 1);
            } finally {
                var2.recycle();
            }

        }

        @NonNull
        public IBinder asBinder() {
            return this.zza;
        }
    }
}
