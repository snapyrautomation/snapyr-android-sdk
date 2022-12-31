package com.snapyr.sdk.notifications;

import android.os.Bundle;

//import com.google.android.gms.cloudmessaging.zzq;

final class GoogleZzsExtendsZzq extends GoogleZzq<Bundle> {
    GoogleZzsExtendsZzq(int var1, int var2, Bundle var3) {
        super(var1, 1, var3);
    }

    final boolean zza() {
        return false;
    }

    void zzb(Bundle var1) {
        Bundle var2;
        if ((var2 = var1.getBundle("data")) == null) {
            var2 = Bundle.EMPTY;
        }

        this.zzb(var2);
    }
}
