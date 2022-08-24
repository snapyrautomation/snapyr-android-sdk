package com.snapyr.sdk.inapp;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.snapyr.sdk.integrations.Logger;

public class InAppManager implements InAppIFace
{
    private final int mInterval = 5000; // 5 seconds by default, can be changed later
    private Runnable backgroundThread = null;
    private Handler handler = null;
    private Logger logger = null;

    public InAppManager(@NonNull InAppConfig config){
        this.logger = config.Logger;
        this.startBackgroundThread(config.PollingDelayMs);
    }

    private void startBackgroundThread(int pollingDelayMs){
        this.handler = new Handler();
        backgroundThread = new Runnable() {
            @Override
            public void run() {
                try {
                    doPolling();
                } finally {
                    handler.postDelayed(backgroundThread, pollingDelayMs);
                }
            }
        };
        backgroundThread.run();
    }

    private void doPolling(){
        logger.info("polling for in-app content");

    }
}
