package com.snapyr.sdk.services;

import android.app.Application;

import java.util.concurrent.ExecutorService;

public class ServiceFacade {
    public static ServiceFacade instance;
    ExecutorService networkExecutor;
    Application application;
    Cartographer cartographer;
    Logger logger;
    Crypto crypto;

    public static ServiceFacade getInstance(){
        if (ServiceFacade.instance == null){
            ServiceFacade.instance = new ServiceFacade();
        }
        return ServiceFacade.instance;
    }

    public ServiceFacade setCrypto(Crypto crypto){
        this.crypto = crypto;
        return this;
    }

    public static Crypto getCrypto(){
        return getInstance().crypto;
    }

    public ServiceFacade setCartographer(Cartographer cartographer){
        this.cartographer = cartographer;
        return this;
    }

    public static Cartographer getCartographer(){
        return getInstance().cartographer;
    }

    public ServiceFacade setLogger(Logger logger){
        this.logger = logger;
        return this;
    }

    public static Logger getLogger(){
        return getInstance().logger;
    }

    public ServiceFacade setApplication(Application application){
        this.application = application;
        return this;
    }

    public static Application getApplication(){
        return getInstance().application;
    }

    public ServiceFacade setNetworkExecutor(ExecutorService networkExecutor){
        this.networkExecutor = networkExecutor;
        return this;
    }

    public static ExecutorService getNetworkExecutor(){
        return getInstance().networkExecutor;
    }
}
