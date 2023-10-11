package com.google.gwt.user.client.rpc;

import com.github.antoniomacri.reactivegwt.proxy.SyncProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for asynchronous RPC tests.
 * <p>
 *
 * @author Antonio Macrì
 */
public abstract class RpcAsyncTestBase<T extends RemoteService, TAsync> extends RpcTestBase {

    private final Class<T> serviceClass;
    private final String remoteServiceRelativePath;
    private final AtomicReference<Throwable> exceptionReference = new AtomicReference<>();
    private CountDownLatch latch;

    protected TAsync service;
    protected int timeoutSeconds = 1;


    protected RpcAsyncTestBase(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
        this.remoteServiceRelativePath = null;
    }

    protected RpcAsyncTestBase(Class<T> serviceClass, String remoteServiceRelativePath) {
        this.serviceClass = serviceClass;
        this.remoteServiceRelativePath = remoteServiceRelativePath;
        SyncProxy.suppressRelativePathWarning(true);
    }

    @BeforeEach
    public final void setUpAsyncTestBase() {
        this.service = getService();
        if (remoteServiceRelativePath != null) {
            ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + remoteServiceRelativePath);
        }
    }

    @AfterEach
    public final void afterEach() throws Throwable {
        if (latch != null) {
            assertThat(latch.await(timeoutSeconds, TimeUnit.SECONDS)).isTrue();
            Throwable t = exceptionReference.get();
            if (t != null) {
                throw t;
            }
        }
    }


    protected TAsync getService() {
        return SyncProxy.create(serviceClass, getModuleBaseURL());
    }

    protected <V> AsyncCallback<V> createCallback(Consumer<V> asserts) {
        this.latch = new CountDownLatch(1);
        return new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionReference.set(caught);
                latch.countDown();
            }

            @Override
            public void onSuccess(V result) {
                try {
                    asserts.accept(result);
                } catch (Throwable rethrown) {
                    exceptionReference.set(rethrown);
                }
                latch.countDown();
            }
        };
    }

    protected <V> AsyncCallback<V> createCallback(AsyncCallback<V> callback) {
        this.latch = new CountDownLatch(1);
        return new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                try {
                    callback.onFailure(caught);
                } catch (Throwable rethrown) {
                    exceptionReference.set(rethrown);
                }
                latch.countDown();
            }

            @Override
            public void onSuccess(V result) {
                try {
                    callback.onSuccess(result);
                } catch (Throwable rethrown) {
                    exceptionReference.set(rethrown);
                }
                latch.countDown();
            }
        };
    }

    protected <R> void waitForServiceCall(Consumer<AsyncCallback<R>> call) {
        var l = new CountDownLatch(1);
        call.accept(createCallback(ignored -> l.countDown()));
        try {
            l.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected <R> R getFromServiceCall(Consumer<AsyncCallback<R>> call) {
        var l = new CountDownLatch(1);
        AtomicReference<R> resultRef = new AtomicReference<>();
        call.accept(createCallback(result -> {
            resultRef.set(result);
            l.countDown();
        }));
        try {
            l.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return resultRef.get();
    }
}
