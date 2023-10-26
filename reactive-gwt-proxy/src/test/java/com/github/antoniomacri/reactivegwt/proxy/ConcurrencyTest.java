package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.ValueTypesTestService;
import com.google.gwt.user.client.rpc.ValueTypesTestServiceAsync;
import com.ibm.icu.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


public class ConcurrencyTest {
    protected static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";
    private static final int ITERATIONS = 100;

    WireMockServer wm;


    @BeforeEach
    public void setUp() throws IOException {
        wm = new WireMockServer(wireMockConfig()
                .dynamicPort()
                .containerThreads(50 + ITERATIONS)); // add a margin
        wm.start();

        serveStaticFile("/" + MODULE_RELATIVE_PATH + "AppModule.nocache.js", "valuetypes/AppModule.nocache.js");
        serveStaticFile("/" + MODULE_RELATIVE_PATH + "70D388D29300BB59D229D3DFFAF15FE7.cache.js", "valuetypes/70D388D29300BB59D229D3DFFAF15FE7.cache.js");
        serveStaticFile("/" + MODULE_RELATIVE_PATH + "compilation-mappings.txt", "valuetypes/compilation-mappings.txt");
        serveStaticFile("/" + MODULE_RELATIVE_PATH + "830F71E6019A18DF7715476EFEFF802A.gwt.rpc", "valuetypes/830F71E6019A18DF7715476EFEFF802A.gwt.rpc");

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "valuetypes").willReturn(aResponse()
                .withBody("//OK[13.0,[],0,7]")
                .withFixedDelay(1000)));

        ReactiveGWT.suppressRelativePathWarning(true);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private void serveStaticFile(String url, String fileName) throws IOException {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
            wm.stubFor(get(url).willReturn(aResponse().withBody(Objects.requireNonNull(inputStream).readAllBytes())));
        }
    }

    @AfterEach
    public final void afterEach() {
        wm.shutdownServer();
    }


    @Test
    public void testWaitingResponseDoesNotBlockThread() throws InterruptedException {
        ValueTypesTestServiceAsync service = getService();
        CountDownLatch latch = new CountDownLatch(ITERATIONS);
        Collection<Throwable> exceptions = new ConcurrentLinkedQueue<>();

        Instant start = Instant.now();
        for (int i = 0; i < ITERATIONS; i++) {
            service.echo(13.0, new AsyncCallback<>() {
                @Override
                public void onFailure(Throwable caught) {
                    exceptions.add(caught);
                    latch.countDown();
                }

                @Override
                public void onSuccess(Double result) {
                    try {
                        assertThat(result).isNotNull();
                        assertThat(result).isEqualTo(13.0);
                    } catch (Throwable e) {
                        exceptions.add(e);
                    }
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(Duration.between(start, Instant.now())).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
        assertThat(exceptions).isEmpty();
    }


    protected String getModuleBaseURL() {
        return wm.baseUrl() + "/" + MODULE_RELATIVE_PATH;
    }

    private ValueTypesTestServiceAsync getService() {
        ValueTypesTestServiceAsync service = ReactiveGWT.create(ValueTypesTestService.class, getModuleBaseURL());
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "valuetypes");
        return service;
    }
}
