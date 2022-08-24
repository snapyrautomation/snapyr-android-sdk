package com.snapyr.sdk.inapp;

import com.snapyr.sdk.integrations.Logger;

public class InAppConfig {
    public Logger Logger = null;
    public int PollingDelayMs = 5000; // 5 seconds by default

    public InAppConfig SetLogger(Logger logger){
        this.Logger = logger;
        return this;
    }
    public InAppConfig SetPollingRate(int pollingDelayMs){
        this.PollingDelayMs = pollingDelayMs;
        return this;
    }
}
