package com.google.gwt.user.client.rpc;

import com.github.antoniomacri.reactivegwt.proxy.ProxySettings;
import com.github.antoniomacri.reactivegwt.proxy.ReactiveGWT;
import com.github.antoniomacri.reactivegwt.proxy.RpcPolicyFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Queue;
import java.util.concurrent.*;
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
    private final Queue<CountDownLatch> latches = new ConcurrentLinkedQueue<>();

    protected TAsync service;
    protected int timeoutSeconds = 1;


    protected RpcAsyncTestBase(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
        this.remoteServiceRelativePath = null;
    }

    protected RpcAsyncTestBase(Class<T> serviceClass, String remoteServiceRelativePath) {
        this.serviceClass = serviceClass;
        this.remoteServiceRelativePath = remoteServiceRelativePath;
        ReactiveGWT.suppressRelativePathWarning(true);
    }

    @BeforeEach
    public final void setUpAsyncTestBase() {
        this.service = getService();
        if (remoteServiceRelativePath != null) {
            ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + remoteServiceRelativePath);
        }
    }

    @AfterEach
    public void afterEach() throws Throwable {
        CountDownLatch latch = latches.poll();
        while (latch != null) {
            assertThat(latch.await(timeoutSeconds, TimeUnit.SECONDS)).isTrue();
            latch = latches.poll();
        }

        Throwable t = exceptionReference.get();
        if (t != null) {
            throw t;
        }
    }


    protected <TService> ProxySettings createSettings(Class<TService> serviceClass) {
        RpcPolicyFinder policyFinder = new RpcPolicyFinder(getModuleBaseURL());
        ProxySettings settings = new ProxySettings(getModuleBaseURL(), serviceClass.getName(), policyFinder);
        return settings;
    }

    protected TAsync getService() {
        return ReactiveGWT.create(serviceClass, getModuleBaseURL());
    }

    protected <V> AsyncCallback<V> waitedCallback(Consumer<V> asserts) {
        var latch = new CountDownLatch(1);
        latches.add(latch);

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

    protected <V> AsyncCallback<V> waitedCallback(AsyncCallback<V> callback) {
        var latch = new CountDownLatch(1);
        latches.add(latch);

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
        getFromServiceCall(call);
    }

    protected <R> R getFromServiceCall(Consumer<AsyncCallback<R>> call) {
        CompletableFuture<R> completableFuture = new CompletableFuture<>();
        call.accept(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                completableFuture.completeExceptionally(caught);
            }

            @Override
            public void onSuccess(R result) {
                completableFuture.complete(result);
            }
        });

        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
