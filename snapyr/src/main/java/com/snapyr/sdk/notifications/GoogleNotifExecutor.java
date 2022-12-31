package com.snapyr.sdk.notifications;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class GoogleNotifExecutor implements GoogleZzbNotifExecutorInterface {
    GoogleNotifExecutor() {
    }

    @NonNull
    public final ExecutorService zza(ThreadFactory var1, int var2) {
        ThreadPoolExecutor executor;
        (executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), var1)).allowCoreThreadTimeOut(true);
        return Executors.unconfigurableExecutorService(executor);
    }

    @NonNull
    public final ScheduledExecutorService zza(int var1, ThreadFactory var2, int var3) {
        return Executors.unconfigurableScheduledExecutorService(Executors.newScheduledThreadPool(1, var2));
    }
}
