package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


public class RpcPolicyFinderTest {
    protected static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";

    WireMockServer wm;


    @BeforeEach
    public void setUp() {
        wm = new WireMockServer(wireMockConfig().dynamicPort());
        wm.start();
    }

    @AfterEach
    public final void afterEach() {
        wm.shutdownServer();
    }

    private void serveFolder(String resourceFolder) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (var stream = classLoader.getResourceAsStream(resourceFolder)) {
            if (stream != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
                bufferedReader.lines().forEach(resource ->
                        serveStaticFile("/" + MODULE_RELATIVE_PATH + resource, resourceFolder + "/" + resource)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void serveStaticFile(String url, String fileName) {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
            wm.stubFor(get(url).willReturn(aResponse()
                    .withBody(Objects.requireNonNull(inputStream).readAllBytes())
                    .withFixedDelay(10)));  // Simulate some delay to check waits
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getModuleBaseURL() {
        return wm.baseUrl() + "/" + MODULE_RELATIVE_PATH;
    }


    @Test
    void shouldFetchPolicyNameFromGWT282Service() {
        serveFolder("orders");

        RpcPolicyFinder policyFinder = new RpcPolicyFinder(getModuleBaseURL());
        String policyName = policyFinder.getOrFetchPolicyName(OrderService.class.getName());
        assertThat(policyName).isEqualTo("49500BE2C20C979AD4B0A80E6F04FA03");
    }

    @Test
    void shouldFetchPolicyNameFromGWT270Service() {
        serveFolder("orders-270");

        RpcPolicyFinder policyFinder = new RpcPolicyFinder(getModuleBaseURL());
        String policyName = policyFinder.getOrFetchPolicyName(OrderService.class.getName());
        assertThat(policyName).isEqualTo("49500BE2C20C979AD4B0A80E6F04FA03");
    }

    @Test
    void shouldFetchPolicyNameFromGWT203Service() {
        serveFolder("orders-203");

        RpcPolicyFinder policyFinder = new RpcPolicyFinder(getModuleBaseURL());
        String policyName = policyFinder.getOrFetchPolicyName(OrderService.class.getName());
        assertThat(policyName).isEqualTo("3639CB2AF30F48928BE6AA30F1CD8E92");
    }
}