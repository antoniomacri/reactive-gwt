package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.StatusCodeException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;


/**
 * Check if the serialization policy changed after an internal server error and retry the call
 * if that's the case.
 * <p>
 * If the GWT backend is redeployed, then the serialization policy name may change. For instance,
 * this happens when a new method is added to the remote service interface with new input/output
 * objects (because these are added to the policy producing a different policy name). We handle
 * this case by refetching the serialization policy and retrying the service call with the new
 * policy.
 * <p>
 * Of course, we won't be able to call the new service method until it is imported in our project
 * and the proxy is instantiated with the remote service interface having the new method.
 */
public class OrderServiceUpdatedTest {
    protected static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";

    WireMockServer wm;
    RpcPolicyFinder policyFinder;


    @BeforeEach
    public void setUp() {
        wm = new WireMockServer(wireMockConfig().dynamicPort());
        wm.start();

        ReactiveGWT.suppressRelativePathWarning(true);

        policyFinder = spy(new RpcPolicyFinder(getModuleBaseURL()));
    }

    private String getModuleBaseURL() {
        return wm.baseUrl() + "/" + MODULE_RELATIVE_PATH;
    }

    private OrderServiceAsync getService() {
        ProxySettings settings = new ProxySettings(getModuleBaseURL(), OrderService.class.getName(), policyFinder);
        OrderServiceAsync service = ReactiveGWT.create(OrderService.class, settings);
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "orders");
        return service;
    }

    @AfterEach
    public final void afterEach() {
        wm.shutdownServer();
    }


    @Test
    public void when500IsReceivedThenShouldRefetchSerializationPolicyAndIfChangedRetryCall() throws InterruptedException, IOException {
        serveOrderService();

        OrderServiceAsync service = getService();
        AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        AtomicReference<OrderItem> resultRef = new AtomicReference<>();

        OrderItem orderItem = new OrderItem();
        orderItem.setDescription("TONNO");
        orderItem.setId(2806670);

        // The first call is OK
        CountDownLatch latch = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch.countDown();
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNull();
        assertThat(resultRef.get()).isNotNull().usingRecursiveComparison().isEqualTo(orderItem);

        InOrder inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));
        inOrder.verifyNoMoreInteractions();

        wm.resetAll();
        Mockito.reset(policyFinder);
        exceptionRef.set(null);
        resultRef.set(null);

        serveOrderServiceUpdated();

        // The second call obtains an internal server error, so the new serialization policy is fetched (*)
        // and then the call is retried
        CountDownLatch latch2 = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch2.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch2.countDown();
            }
        });
        assertThat(latch2.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNull();
        assertThat(resultRef.get()).isNotNull().usingRecursiveComparison().isEqualTo(orderItem);

        inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());  // (*)
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49CEE67A18790BD431604E4192544D7F"));
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void when500IsReceivedThenShouldRefetchSerializationPolicyAndIfNotChangedReturnError() throws InterruptedException, IOException {
        serveOrderService();

        OrderServiceAsync service = getService();
        AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        AtomicReference<OrderItem> resultRef = new AtomicReference<>();

        OrderItem orderItem = new OrderItem();
        orderItem.setDescription("TONNO");
        orderItem.setId(2806670);

        // The first call is OK
        CountDownLatch latch = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch.countDown();
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNull();
        assertThat(resultRef.get()).isNotNull().usingRecursiveComparison().isEqualTo(orderItem);

        InOrder inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));
        inOrder.verifyNoMoreInteractions();

        Mockito.reset(policyFinder);
        exceptionRef.set(null);
        resultRef.set(null);

        serveInternalServerError();

        // The second call obtains an internal server error, so we check for a new serialization policy (*)
        // but, since it has not changed, the call is not retried, and we still get the 500 (**)
        CountDownLatch latch2 = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch2.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch2.countDown();
            }
        });
        assertThat(latch2.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNotNull().isInstanceOf(StatusCodeException.class);
        assertThat(((StatusCodeException) exceptionRef.get()).getStatusCode()).isEqualTo(500);  // (**)
        assertThat(resultRef.get()).isNull();

        inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));  // (*)
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verifyNoMoreInteractions();

        wm.resetAll();
        Mockito.reset(policyFinder);
        exceptionRef.set(null);
        resultRef.set(null);

        serveOrderService();

        // Now the backend is working again, and we get a success response (*)
        CountDownLatch latch3 = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch3.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch3.countDown();
            }
        });
        assertThat(latch3.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNull();
        assertThat(resultRef.get()).isNotNull().usingRecursiveComparison().isEqualTo(orderItem);  // (*)

        inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void when500IsReceivedThenShouldRefetchSerializationPolicyAndIfChangedRetryCallWith500Again() throws InterruptedException, IOException {
        serveOrderService();

        OrderServiceAsync service = getService();
        AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        AtomicReference<OrderItem> resultRef = new AtomicReference<>();

        OrderItem orderItem = new OrderItem();
        orderItem.setDescription("TONNO");
        orderItem.setId(2806670);

        // The first call is OK
        CountDownLatch latch = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch.countDown();
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNull();
        assertThat(resultRef.get()).isNotNull().usingRecursiveComparison().isEqualTo(orderItem);

        InOrder inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));
        inOrder.verifyNoMoreInteractions();

        wm.resetAll();
        Mockito.reset(policyFinder);
        exceptionRef.set(null);
        resultRef.set(null);

        serveOrderServiceUpdatedReturning500();

        // The second call obtains an internal server error, so the new serialization policy is fetched (*)
        // and then the call is retried, but we still get a 500. As a consequence, we again try to fetch
        // the serialization policy (**), but since it has not changed now, the process stops and we get a
        // final 500 (***)
        CountDownLatch latch2 = new CountDownLatch(1);
        service.echo(orderItem, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch2.countDown();
            }

            @Override
            public void onSuccess(OrderItem result) {
                resultRef.set(result);
                latch2.countDown();
            }
        });
        assertThat(latch2.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNotNull().isInstanceOf(StatusCodeException.class);
        assertThat(((StatusCodeException) exceptionRef.get()).getStatusCode()).isEqualTo(500);  // (***)
        assertThat(resultRef.get()).isNull();

        inOrder = inOrder(policyFinder);
        inOrder.verify(policyFinder, times(1)).getOrFetchPolicyNameAsync(eq(OrderService.class.getName()), any());
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49500BE2C20C979AD4B0A80E6F04FA03"));
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());  // (*)
        inOrder.verify(policyFinder, times(1)).getSerializationPolicy(eq("49CEE67A18790BD431604E4192544D7F"));
        inOrder.verify(policyFinder, times(1)).fetchPolicyNameAsync(eq(OrderService.class.getName()), any());  // (**)
        inOrder.verifyNoMoreInteractions();
    }


    private void serveOrderService() throws IOException {
        serveStaticFile("orders", "AppModule.nocache.js");
        serveStaticFile("orders", "compilation-mappings.txt");
        serveStaticFile("orders", "3321E14570191EE254AB43C1F1A74C9E.cache.js");
        serveStaticFile("orders", "49500BE2C20C979AD4B0A80E6F04FA03.gwt.rpc");

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .withRequestBody(new EqualToPattern(
                        "7|0|8|" + wm.baseUrl() + "/AppRoot/AppModule/|" +
                        "49500BE2C20C979AD4B0A80E6F04FA03|com.github.antoniomacri.reactivegwt.proxy.OrderService|echo" +
                        "|com.github.antoniomacri.reactivegwt.proxy.OrderItem|com.github.antoniomacri.reactivegwt.pro" +
                        "xy.OrderItem/2265425062|TONNO|java.lang.Integer/3438268394|1|2|3|4|1|5|6|0|7|8|2806670|"))
                .willReturn(aResponse()
                        .withBody("//OK[2806670,4,3,2,1,[\"com.github.antoniomacri.reactivegwt.proxy.OrderItem/226542" +
                                  "5062\",\"rO0ABXcEAAAAAA\\u003D\\u003D\",\"TONNO\",\"java.lang.Integer/3438268394\"" +
                                  "],0,7]")
                ));
    }

    private void serveInternalServerError() {
        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                ));
    }

    private void serveOrderServiceUpdated() throws IOException {
        serveStaticFile("orders-updated", "AppModule.nocache.js");
        serveStaticFile("orders-updated", "compilation-mappings.txt");
        serveStaticFile("orders-updated", "F46FD829C9E33DC26898B6707DE48047.cache.js");
        serveStaticFile("orders-updated", "49CEE67A18790BD431604E4192544D7F.gwt.rpc");

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .withRequestBody(new EqualToPattern(
                        "7|0|8|" + wm.baseUrl() + "/AppRoot/AppModule/|" +
                        "49500BE2C20C979AD4B0A80E6F04FA03|com.github.antoniomacri.reactivegwt.proxy.OrderService|echo" +
                        "|com.github.antoniomacri.reactivegwt.proxy.OrderItem|com.github.antoniomacri.reactivegwt.pro" +
                        "xy.OrderItem/2265425062|TONNO|java.lang.Integer/3438268394|1|2|3|4|1|5|6|0|7|8|2806670|"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                ));

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .withRequestBody(new EqualToPattern(
                        "7|0|8|" + wm.baseUrl() + "/AppRoot/AppModule/|" +
                        "49CEE67A18790BD431604E4192544D7F|com.github.antoniomacri.reactivegwt.proxy.OrderService|echo" +
                        "|com.github.antoniomacri.reactivegwt.proxy.OrderItem|com.github.antoniomacri.reactivegwt.pro" +
                        "xy.OrderItem/2265425062|TONNO|java.lang.Integer/3438268394|1|2|3|4|1|5|6|0|7|8|2806670|"))
                .willReturn(aResponse()
                        .withBody("//OK[2806670,4,3,2,1,[\"com.github.antoniomacri.reactivegwt.proxy.OrderItem/226542" +
                                  "5062\",\"rO0ABXcEAAAAAA\\u003D\\u003D\",\"TONNO\",\"java.lang.Integer/3438268394\"" +
                                  "],0,7]")
                ));
    }

    private void serveOrderServiceUpdatedReturning500() throws IOException {
        serveStaticFile("orders-updated", "AppModule.nocache.js");
        serveStaticFile("orders-updated", "compilation-mappings.txt");
        serveStaticFile("orders-updated", "F46FD829C9E33DC26898B6707DE48047.cache.js");
        serveStaticFile("orders-updated", "49CEE67A18790BD431604E4192544D7F.gwt.rpc");

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .withRequestBody(new EqualToPattern(
                        "7|0|8|" + wm.baseUrl() + "/AppRoot/AppModule/|" +
                        "49500BE2C20C979AD4B0A80E6F04FA03|com.github.antoniomacri.reactivegwt.proxy.OrderService|echo" +
                        "|com.github.antoniomacri.reactivegwt.proxy.OrderItem|com.github.antoniomacri.reactivegwt.pro" +
                        "xy.OrderItem/2265425062|TONNO|java.lang.Integer/3438268394|1|2|3|4|1|5|6|0|7|8|2806670|"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                ));

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .withRequestBody(new EqualToPattern(
                        "7|0|8|" + wm.baseUrl() + "/AppRoot/AppModule/|" +
                        "49CEE67A18790BD431604E4192544D7F|com.github.antoniomacri.reactivegwt.proxy.OrderService|echo" +
                        "|com.github.antoniomacri.reactivegwt.proxy.OrderItem|com.github.antoniomacri.reactivegwt.pro" +
                        "xy.OrderItem/2265425062|TONNO|java.lang.Integer/3438268394|1|2|3|4|1|5|6|0|7|8|2806670|"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                ));
    }

    private void serveStaticFile(String resourceFolder, String fileName) throws IOException {
        String resourcePath = resourceFolder + "/" + fileName;
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            wm.stubFor(get("/" + MODULE_RELATIVE_PATH + fileName)
                    .willReturn(aResponse().withBody(Objects.requireNonNull(inputStream).readAllBytes())));
        }
    }
}
