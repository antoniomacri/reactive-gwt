package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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


/**
 * Test serialization with version 5 of the protocol.
 * <p>
 * Basically, we only check that the version is 5 instead of the default 7.
 */
public class OrderServiceVersion5Test {
    protected static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";
    private static final int SERIALIZATION_VERSION = 5;

    WireMockServer wm;


    @BeforeEach
    public void setUp() {
        wm = new WireMockServer(wireMockConfig().dynamicPort());
        wm.start();

        ReactiveGWT.suppressRelativePathWarning(true);
    }

    private String getModuleBaseURL() {
        return wm.baseUrl() + "/" + MODULE_RELATIVE_PATH;
    }

    private OrderServiceAsync getService() {
        ProxySettings settings = new ProxySettings(getModuleBaseURL(), OrderService.class.getName());
        settings.setSerializationStreamVersion(5);
        OrderServiceAsync service = ReactiveGWT.create(OrderService.class, settings);
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "orders");
        return service;
    }

    @AfterEach
    public final void afterEach() {
        wm.shutdownServer();
    }


    @Test
    public void shouldSetOverriddenSerializationVersionInPayload() throws InterruptedException, IOException {
        serveOrderService();

        OrderServiceAsync service = getService();
        AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        AtomicReference<OrderItem> resultRef = new AtomicReference<>();

        OrderItem orderItem = new OrderItem();
        orderItem.setDescription("TONNO");
        orderItem.setId(2806670);

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
    }


    private void serveOrderService() throws IOException {
        serveStaticFile("orders", "AppModule.nocache.js");
        serveStaticFile("orders", "compilation-mappings.txt");
        serveStaticFile("orders", "3321E14570191EE254AB43C1F1A74C9E.cache.js");
        serveStaticFile("orders", "49500BE2C20C979AD4B0A80E6F04FA03.gwt.rpc");

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "orders")
                .withRequestBody(new EqualToPattern(
                        SERIALIZATION_VERSION + "|0|8|" + wm.baseUrl() + "/AppRoot/AppModule/|" +
                        "49500BE2C20C979AD4B0A80E6F04FA03|com.github.antoniomacri.reactivegwt.proxy.OrderService|echo" +
                        "|com.github.antoniomacri.reactivegwt.proxy.OrderItem|com.github.antoniomacri.reactivegwt.pro" +
                        "xy.OrderItem/2265425062|TONNO|java.lang.Integer/3438268394|1|2|3|4|1|5|6|0|7|8|2806670|"))
                .willReturn(aResponse()
                        .withBody("//OK[2806670,4,3,2,1,[\"com.github.antoniomacri.reactivegwt.proxy.OrderItem/226542" +
                                  "5062\",\"rO0ABXcEAAAAAA\\u003D\\u003D\",\"TONNO\",\"java.lang.Integer/3438268394\"" +
                                  "],0,7]")
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
