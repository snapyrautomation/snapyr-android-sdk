package com.snapyr.sdk.inapp;

public class  InAppFacade {
    enum InAppState{
        IN_APP_STATE_SUPPRESSED,
        IN_APP_STATE_ALLOWED,
    }

    private static InAppState inappState = InAppState.IN_APP_STATE_ALLOWED;
    private static InAppIFace impl = new NoopInApp();

    public static InAppIFace CreateInapp(InAppConfig config){
        if ((InAppManager)InAppFacade.impl != null){
            config.Logger.info("inapp already initialized");
            return impl;
        }

        InAppFacade.impl = new InAppManager(config);
        return InAppFacade.impl;
    }


    /**
     *  Suppresses all snapyr-rendered in-app creatives from rendering
     */
    public static void SuppressInApp(){
        InAppFacade.inappState = InAppState.IN_APP_STATE_SUPPRESSED;
    }

    /**
     *  Allows snapyr-rendered in-app creatives to render
     */
    public static void AllowInApp(){
        InAppFacade.inappState = InAppState.IN_APP_STATE_ALLOWED;
    }

    public static void ProcessTrackResponse(){

    }
}
