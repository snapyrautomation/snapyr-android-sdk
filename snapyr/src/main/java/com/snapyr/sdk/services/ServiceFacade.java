/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.snapyr.sdk.services;

import android.app.Application;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.SnapyrContext;
import com.snapyr.sdk.http.ConnectionFactory;
import java.util.concurrent.ExecutorService;

public class ServiceFacade {
    public static ServiceFacade instance;
    ExecutorService networkExecutor;
    Application application;
    Cartographer cartographer;
    Logger logger;
    Crypto crypto;
    ConnectionFactory connectionFactory;
    SnapyrContext snapyrContext;

    public static ServiceFacade getInstance() {
        if (ServiceFacade.instance == null) {
            ServiceFacade.instance = new ServiceFacade();
            ServiceFacade.instance.logger = Logger.with(Snapyr.LogLevel.NONE);
            ServiceFacade.instance.crypto = Crypto.none();
        }
        return ServiceFacade.instance;
    }

    public ServiceFacade setSnapyrContext(SnapyrContext snapyrContext) {
        this.snapyrContext = snapyrContext;
        return this;
    }

    public static SnapyrContext getSnapyrContext() {
        return getInstance().snapyrContext;
    }

    public ServiceFacade setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        return this;
    }

    public static ConnectionFactory getConnectionFactory() {
        return getInstance().connectionFactory;
    }

    public ServiceFacade setCrypto(Crypto crypto) {
        this.crypto = crypto;
        return this;
    }

    public static Crypto getCrypto() {
        return getInstance().crypto;
    }

    public ServiceFacade setCartographer(Cartographer cartographer) {
        this.cartographer = cartographer;
        return this;
    }

    public static Cartographer getCartographer() {
        return getInstance().cartographer;
    }

    public ServiceFacade setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public static Logger getLogger() {
        return getInstance().logger;
    }

    public ServiceFacade setApplication(Application application) {
        this.application = application;
        return this;
    }

    public static Application getApplication() {
        return getInstance().application;
    }

    public ServiceFacade setNetworkExecutor(ExecutorService networkExecutor) {
        this.networkExecutor = networkExecutor;
        return this;
    }

    public static ExecutorService getNetworkExecutor() {
        return getInstance().networkExecutor;
    }
}
