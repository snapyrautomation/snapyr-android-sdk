package com.snapyr.sdk.notifications;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public interface GoogleZzbNotifExecutorInterface {
    ExecutorService zza(ThreadFactory var1, int var2);

    ScheduledExecutorService zza(int var1, ThreadFactory var2, int var3);
}
