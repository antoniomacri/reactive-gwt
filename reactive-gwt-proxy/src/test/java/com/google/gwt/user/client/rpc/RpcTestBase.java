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
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for RPC tests.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
@ExtendWith(ArquillianExtension.class)
public abstract class RpcTestBase<T extends RemoteService, TAsync> {
    private static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";

    private static <T> Consumer<T> noop() {
        return ignored -> {
        };
    }

    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceKlass,
            Class<? extends RemoteServiceServlet> serviceImplKlass,
            String... resources
    ) {
        return buildTestArchive(serviceInterfaceKlass, serviceImplKlass, noop(), resources);
    }

    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceKlass,
            Class<? extends RemoteServiceServlet> serviceImplKlass,
            Consumer<WebAppDescriptor> webAppDescriptorConsumer,
            String... resources
    ) {
        RemoteServiceRelativePath serviceRelativePathAnnotation = serviceInterfaceKlass.getDeclaredAnnotation(RemoteServiceRelativePath.class);
        if (serviceRelativePathAnnotation == null) {
            throw new RuntimeException("Class %s is not annotated with @RemoteServiceRelativePath".formatted(serviceInterfaceKlass));
        }

        var webAppDescriptor = Descriptors.create(WebAppDescriptor.class)
                .version("3.0")
                .createServlet()
                .servletClass(serviceImplKlass.getName())
                .servletName("servlet-name").up()
                .createServletMapping()
                .servletName("servlet-name")
                .urlPattern(MODULE_RELATIVE_PATH + serviceRelativePathAnnotation.value())
                .up();

        webAppDescriptorConsumer.accept(webAppDescriptor);

        var archive = ShrinkWrap.create(WebArchive.class, "client-test.war")
                .addClass(serviceImplKlass)
                .setWebXML(new StringAsset(webAppDescriptor.exportAsString()));

        for (String resource : resources) {
            archive = archive.addAsWebResource(serviceRelativePathAnnotation.value() + "/" + resource, MODULE_RELATIVE_PATH + resource);
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
    public final void setUp() {
        SyncProxy.setBaseURL(getModuleBaseURL());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        this.latch = new CountDownLatch(1);
        this.service = getService();
    }

    protected String getModuleBaseURL() {
        return base + MODULE_RELATIVE_PATH;
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        if (latch != null) {
            assertThat(latch.await(timeoutSeconds, TimeUnit.SECONDS)).isTrue();
        }
    }


    protected TAsync getService() {
        return SyncProxy.create(serviceClass);
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

    protected <R> void waitForServiceCall(Consumer<AsyncCallback<R>> call) {
        var l = new CountDownLatch(1);
        call.accept(createCallback(ignored -> l.countDown()));
        try {
            l.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected <R> R getFromServiceCall(Consumer<AsyncCallback<R>> call) {
        var l = new CountDownLatch(1);
        AtomicReference<R> resultRef = new AtomicReference<>();
        call.accept(createCallback(result -> {
            resultRef.set(result);
            l.countDown();
        }));
        try {
            l.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return resultRef.get();
    }
}
