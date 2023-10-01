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
    private CountDownLatch latch;

    protected TAsync service;
    protected int timeoutSeconds = 1;


    protected RpcAsyncTestBase(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @BeforeEach
    public final void setUpAsyncTestBase() {
        this.latch = new CountDownLatch(1);
        this.service = getService();
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        if (latch != null) {
            assertThat(latch.await(timeoutSeconds, TimeUnit.SECONDS)).isTrue();
        }
    }


    protected TAsync getService() {
        return SyncProxy.create(serviceClass);
    }

    protected <V> AsyncCallback<V> createCallback(Consumer<V> asserts) {
        return new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                latch.countDown();
                TestSetValidator.rethrowException(caught);
            }

            @Override
            public void onSuccess(V result) {
                latch.countDown();
                asserts.accept(result);
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
