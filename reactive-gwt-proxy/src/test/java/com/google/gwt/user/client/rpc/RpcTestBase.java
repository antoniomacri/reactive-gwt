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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.EventListener;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for RPC tests.
 * <p>
 *
 * @author Antonio Macrì
 */
@ExtendWith(ArquillianExtension.class)
public abstract class RpcTestBase<T extends RemoteService, TAsync> {
    private static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";


    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Class<? extends RemoteServiceServlet> serviceImplClass
    ) {
        return buildTestArchive(serviceInterfaceClass, serviceImplClass, null);
    }


    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Class<? extends RemoteServiceServlet> serviceImplClass,
            Class<? extends EventListener> listenerClass
    ) {
        var serviceRelativePathAnnotation = serviceInterfaceClass.getDeclaredAnnotation(RemoteServiceRelativePath.class);
        if (serviceRelativePathAnnotation == null) {
            throw new RuntimeException("Class %s is not annotated with @RemoteServiceRelativePath".formatted(serviceInterfaceClass));
        }

        var archive = ShrinkWrap.create(WebArchive.class, "client-test.war");
        var webAppDescriptor = Descriptors.create(WebAppDescriptor.class).version("3.0");

        addServletWithResources(archive, webAppDescriptor, serviceImplClass, serviceRelativePathAnnotation.value());
        if (listenerClass != null) {
            webAppDescriptor.createListener().listenerClass(listenerClass.getName());
        }
        archive.setWebXML(new StringAsset(webAppDescriptor.exportAsString()));

        return archive;
    }

    private static void addServletWithResources(WebArchive archive, WebAppDescriptor webAppDescriptor, Class<? extends RemoteServiceServlet> serviceImplClass, String serviceRelativePath) {
        webAppDescriptor.createServlet()
                .servletClass(serviceImplClass.getName())
                .servletName("servlet-" + serviceImplClass.getName())
                .up()
                .createServletMapping()
                .servletName("servlet-" + serviceImplClass.getName())
                .urlPattern(MODULE_RELATIVE_PATH + serviceRelativePath);

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (var stream = classLoader.getResourceAsStream(serviceRelativePath)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
            bufferedReader.lines().forEach(resource ->
                    archive.addAsWebResource(serviceRelativePath + "/" + resource, MODULE_RELATIVE_PATH + resource)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        archive.addClass(serviceImplClass);
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
