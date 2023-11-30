package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.CollectionsTestService;
import com.google.gwt.user.client.rpc.CollectionsTestServiceAsync;
import com.google.gwt.user.client.rpc.RpcAsyncTestBase;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.TestSetFactory;
import com.google.gwt.user.client.rpc.ValueTypesTestService;
import com.google.gwt.user.client.rpc.ValueTypesTestServiceAsync;
import com.google.gwt.user.server.rpc.CollectionsTestServiceImpl;
import com.google.gwt.user.server.rpc.ValueTypesTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;


public class ConcurrentServicesTest extends RpcAsyncTestBase<ValueTypesTestService, ValueTypesTestServiceAsync> {

    private static final int ITERATIONS = 100;


    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(ValueTypesTestService.class, Map.of(
                ValueTypesTestServiceImpl.class, Optional.of("valuetypes"),
                CollectionsTestServiceImpl.class, Optional.of("collections")
        ), null, "concurrent");
    }


    public ConcurrentServicesTest() {
        super(ValueTypesTestService.class, "valuetypes");
    }


    @Test
    public void testWithSingleThreadExecutor() throws InterruptedException {
        service = getValueTypesService(Executors.newSingleThreadExecutor());
        CountDownLatch latch = new CountDownLatch(ITERATIONS);
        List<Throwable> exceptions = new ArrayList<>();

        Thread t = getThread(service::echo, i -> i, ITERATIONS, exceptions, latch);
        t.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptions).isEmpty();
    }

    @Test
    public void testWithMultipleThreadsSingleService() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        service = getValueTypesService(executorService);
        int iterations = ITERATIONS / 3;
        CountDownLatch latch = new CountDownLatch(3 * iterations);
        List<Throwable> exceptions = new ArrayList<>();

        List.of(
                getThread(service::echo, i -> (byte) i, iterations, exceptions, latch),
                getThread(service::echo, i -> (double) i, iterations, exceptions, latch),
                getThread(service::echo, String::valueOf, iterations, exceptions, latch)
        ).forEach(Thread::start);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptions).isEmpty();
    }

    @Test
    public void testWithMultipleThreadsDifferentServices() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        service = getValueTypesService(executorService);
        var collectionService = getCollectionsService(executorService);
        int iterations = ITERATIONS / 8;
        CountDownLatch latch = new CountDownLatch(8 * iterations);
        List<Throwable> exceptions = new ArrayList<>();

        List.of(
                getThread(service::echo, i -> (byte) i, iterations, exceptions, latch),
                getThread(service::echo, i -> (double) i, iterations, exceptions, latch),
                getThread(service::echo, i -> (char) i, iterations, exceptions, latch),
                getThread(service::echo, String::valueOf, iterations, exceptions, latch),
                getThread(collectionService::echo, i -> TestSetFactory.createArrayList(), iterations, exceptions, latch),
                getThread(collectionService::echo, i -> TestSetFactory.createHashMap(), iterations, exceptions, latch),
                getThread(collectionService::echo, i -> TestSetFactory.createFloatArray(), iterations, exceptions, latch),
                getThread(collectionService::echo, i -> TestSetFactory.createBooleanArray(), iterations, exceptions, latch)
        ).forEach(Thread::start);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptions).isEmpty();
    }


    @Override
    public ValueTypesTestServiceAsync getService() {
        ProxySettings settings = createSettings(ValueTypesTestService.class);
        settings.setExecutor(Executors.newSingleThreadExecutor());
        ValueTypesTestServiceAsync service = ReactiveGWT.create(ValueTypesTestService.class, settings);
        return service;
    }

    private ValueTypesTestServiceAsync getValueTypesService(ExecutorService executor) {
        ProxySettings settings = createSettings(ValueTypesTestService.class);
        settings.setExecutor(executor);
        ValueTypesTestServiceAsync service = ReactiveGWT.create(ValueTypesTestService.class, settings);
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "valuetypes");
        return service;
    }

    private CollectionsTestServiceAsync getCollectionsService(ExecutorService executor) {
        ProxySettings settings = createSettings(CollectionsTestService.class);
        settings.setExecutor(executor);
        CollectionsTestServiceAsync service = ReactiveGWT.create(CollectionsTestService.class, settings);
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "collections");
        return service;
    }

    private <T> Thread getThread(BiConsumer<T, AsyncCallback<T>> function, IntFunction<T> mapper, int iterations, List<Throwable> exceptions, CountDownLatch latch) {
        return new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                final int id = i;
                function.accept(mapper.apply(i), new AsyncCallback<>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        exceptions.add(caught);
                        latch.countDown();
                    }

                    @Override
                    public void onSuccess(T result) {
                        try {
                            assertThat(result).isNotNull();
                            assertThat(result).isEqualTo(mapper.apply(id));
                        } catch (Throwable e) {
                            exceptions.add(e);
                        }
                        latch.countDown();
                    }
                });
            }
        });
    }
}
