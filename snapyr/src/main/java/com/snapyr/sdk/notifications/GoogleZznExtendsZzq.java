package com.snapyr.sdk.notifications;

import android.os.Bundle;

import com.google.android.gms.cloudmessaging.zzp;
//import com.google.android.gms.cloudmessaging.zzq;

final class GoogleZznExtendsZzq extends GoogleZzq<Void> {
    GoogleZznExtendsZzq(int var1, int var2, Bundle var3) {
        super(var1, 2, var3);
    }

    final boolean zza() {
        return true;
    }

    final void zzb(Bundle var1) {
        if (var1.getBoolean("ack", false)) {
            this.zza((zzp)null);
        } else {
            this.zza(new zzp(4, "Invalid response to one way request"));
        }
    }
}
