/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.rpc;

import com.github.antoniomacri.reactivegwt.proxy.SyncProxy;
import com.ibm.icu.util.TimeZone;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for RPC tests.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
@ExtendWith(ArquillianExtension.class)
public abstract class RpcTestBase<T extends RemoteService, TAsync> {

    protected static WebArchive buildTestArchive(Class<? extends RemoteService> klass, String serviceRelativePath, String... resources) {
        var archive = ShrinkWrap.create(WebArchive.class, "client-test.war")
                .addClass(klass)
                .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class)
                        .version("3.0")
                        .createServlet()
                        .servletClass(klass.getName())
                        .servletName("servlet-name").up()
                        .createServletMapping()
                        .servletName("servlet-name")
                        .urlPattern("/AppRoot/AppModule/" + serviceRelativePath).up()
                        .exportAsString()));

        for (String resource : resources) {
            archive = archive.addAsWebResource(serviceRelativePath + "/" + resource, "AppRoot/AppModule/" + resource);
        }

        return archive;
    }


    private final Class<T> serviceClass;
    private CountDownLatch latch;

    protected TAsync service;
    protected int timeoutSeconds = 1;

    @ArquillianResource
    @SuppressWarnings("unused")
    private URL base;


    protected RpcTestBase(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @BeforeEach
    public void setUp() {
        SyncProxy.setBaseURL(base + "AppRoot/AppModule/");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        this.latch = new CountDownLatch(1);
        this.service = SyncProxy.create(serviceClass);
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        if (latch != null) {
            assertThat(latch.await(timeoutSeconds, TimeUnit.SECONDS)).isTrue();
        }
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
}
