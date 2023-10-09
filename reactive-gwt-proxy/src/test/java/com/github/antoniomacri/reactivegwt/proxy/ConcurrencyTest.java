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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


public class ConcurrencyTest {
    private static final Logger log = LoggerFactory.getLogger(ConcurrencyTest.class);
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

        SyncProxy.suppressRelativePathWarning(true);
        SyncProxy.setBaseURL(getModuleBaseURL());
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
    public void test() throws InterruptedException {
        ValueTypesTestServiceAsync service = getService();
        CountDownLatch latch = new CountDownLatch(ITERATIONS);
        List<Throwable> exceptions = new ArrayList<>();

        for (int i = 0; i < ITERATIONS; i++) {
            final int id = i;
            long start = System.currentTimeMillis();
            log.info("{}: Sending request.", id);
            service.echo(13.0, new AsyncCallback<>() {
                @Override
                public void onFailure(Throwable caught) {
                    log.error("%d: Got exception after %s seconds"
                            .formatted(id, (System.currentTimeMillis() - start) / 1000.0), caught);
                    latch.countDown();
                    exceptions.add(caught);
                }

                @Override
                public void onSuccess(Double result) {
                    log.info("%d: Got response %s after %s seconds"
                            .formatted(id, result, (System.currentTimeMillis() - start) / 1000.0));
                    latch.countDown();
                    assertThat(result).isNotNull();
                    assertThat(result).isEqualTo(13.0);
                }
            });
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptions).isEmpty();
    }


    protected String getModuleBaseURL() {
        return wm.baseUrl() + "/" + MODULE_RELATIVE_PATH;
    }

    private ValueTypesTestServiceAsync getService() {
        ValueTypesTestServiceAsync service = SyncProxy.create(ValueTypesTestService.class);
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "valuetypes");
        return service;
    }
}
