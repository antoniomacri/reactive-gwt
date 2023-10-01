package com.google.gwt.user.client.rpc;

import com.github.antoniomacri.reactivegwt.proxy.SyncProxy;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for synchronous RPC tests.
 * <p>
 *
 * @author Antonio Macrì
 */
public abstract class RpcSyncTestBase<T extends RemoteService> extends RpcTestBase {

    private final Class<T> serviceClass;

    protected T service;


    protected RpcSyncTestBase(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @BeforeEach
    public final void setUpSyncTestBase() {
        this.service = getService();
    }


    protected T getService() {
        return SyncProxy.createSync(serviceClass);
    }
}
