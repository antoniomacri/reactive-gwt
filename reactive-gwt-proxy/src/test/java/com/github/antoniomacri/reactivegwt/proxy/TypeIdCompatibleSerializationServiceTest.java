package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * With collection types, such as ArrayLists, the typeName written into RPC requests
 * should be the one received from the server, not the local one.
 * <p>
 * This is to avoid incompatibility errors in those cases when this check seems superfluous.
 */
public class TypeIdCompatibleSerializationServiceTest {
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

    private TypeIdCompatibleSerializationServiceAsync getService() {
        ProxySettings settings = new ProxySettings(getModuleBaseURL(), TypeIdCompatibleSerializationService.class.getName());
        settings.setSerializationStreamVersion(SERIALIZATION_VERSION);
        TypeIdCompatibleSerializationServiceAsync service = ReactiveGWT.create(TypeIdCompatibleSerializationService.class, settings);
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "typeIds");
        return service;
    }

    @AfterEach
    public final void afterEach() {
        wm.shutdownServer();
    }


    @Test
    public void shoudUseTypeNameFromSerializationPolicy() throws InterruptedException, IOException {
        serveTypeIdCompatibleSerializationService();

        TypeIdCompatibleSerializationServiceAsync service = getService();
        AtomicReference<Throwable> exceptionRef = new AtomicReference<>();

        List<Integer> list = new ArrayList<>();
        list.add(7);
        list.add(8);

        CountDownLatch latch = new CountDownLatch(1);
        service.put(list, new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                exceptionRef.set(caught);
                latch.countDown();
            }

            @Override
            public void onSuccess(Void result) {
                latch.countDown();
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptionRef.get()).isNull();
    }


    private void serveTypeIdCompatibleSerializationService() throws IOException {
        serveStaticFile("typeIds", "AppModule.nocache.js");
        serveStaticFile("typeIds", "04A024FDF2741537D7BD60D7FFD3A31B.cache.html");
        serveStaticFile("typeIds", "6B3923F92424A3F6B42B2AA4B1C3121E.gwt.rpc");

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "typeIds")
                .withRequestBody(containing("java.util.ArrayList/3821976829"))
                .willReturn(aResponse().withBody("//OK[13.0,[],0,7]")));
    }

    private void serveStaticFile(String resourceFolder, String fileName) throws IOException {
        String resourcePath = resourceFolder + "/" + fileName;
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            wm.stubFor(get("/" + MODULE_RELATIVE_PATH + fileName)
                    .willReturn(aResponse().withBody(Objects.requireNonNull(inputStream).readAllBytes())));
        }
    }
}
