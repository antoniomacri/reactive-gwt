package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class TestUtils {
    public static void serveStaticFiles(WireMockServer wm, String resourceFolder, String moduleRelativePath) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (var stream = classLoader.getResourceAsStream(resourceFolder)) {
            if (stream != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
                bufferedReader.lines().forEach(fileName -> {
                    String url = "/" + moduleRelativePath + fileName;
                    String resourcePath = resourceFolder + "/" + fileName;
                    try (InputStream inputStream = TestUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
                        wm.stubFor(get(url).willReturn(aResponse().withBody(Objects.requireNonNull(inputStream).readAllBytes())));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
