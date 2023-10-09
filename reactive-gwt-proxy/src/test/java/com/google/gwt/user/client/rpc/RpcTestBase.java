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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.EventListener;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for RPC tests.
 * <p>
 *
 * @author Antonio Macrì
 */
@ExtendWith(ArquillianExtension.class)
public abstract class RpcTestBase {
    protected static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";


    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Class<? extends RemoteServiceServlet> serviceImplClass
    ) {
        return buildTestArchive(serviceInterfaceClass, serviceImplClass, null, null);
    }

    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Class<? extends RemoteServiceServlet> serviceImplClass,
            Class<? extends EventListener> listenerClass
    ) {
        return buildTestArchive(serviceInterfaceClass, serviceImplClass, null, listenerClass);
    }

    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Class<? extends RemoteServiceServlet> serviceImplClass,
            String remoteServiceRelativePath
    ) {
        return buildTestArchive(serviceInterfaceClass, serviceImplClass, remoteServiceRelativePath, null);
    }

    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Class<? extends RemoteServiceServlet> serviceImplClass,
            String remoteServiceRelativePath,
            Class<? extends EventListener> listenerClass
    ) {
        return buildTestArchive(serviceInterfaceClass, Map.of(
                serviceImplClass, Optional.ofNullable(remoteServiceRelativePath)
        ), listenerClass);
    }

    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Map<Class<? extends RemoteServiceServlet>, Optional<String>> serviceImplClasses
    ) {
        return buildTestArchive(serviceInterfaceClass, serviceImplClasses, null);
    }


    protected static WebArchive buildTestArchive(
            Class<? extends RemoteService> serviceInterfaceClass,
            Map<Class<? extends RemoteServiceServlet>, Optional<String>> serviceImplClasses,
            Class<? extends EventListener> listenerClass
    ) {
        var archive = ShrinkWrap.create(WebArchive.class, "client-test.war");
        var webAppDescriptor = Descriptors.create(WebAppDescriptor.class).version("3.0");

        serviceImplClasses.forEach((serviceImplClass, remoteServiceRelativePathArg) -> {
            String remoteServiceRelativePath = remoteServiceRelativePathArg.orElseGet(() -> {
                var serviceRelativePathAnnotation = serviceInterfaceClass.getDeclaredAnnotation(RemoteServiceRelativePath.class);
                if (serviceRelativePathAnnotation == null) {
                    throw new RuntimeException("Class %s is not annotated with @RemoteServiceRelativePath".formatted(serviceInterfaceClass));
                }
                return serviceRelativePathAnnotation.value();
            });
            addServletWithResources(archive, webAppDescriptor, serviceImplClass, remoteServiceRelativePath);
        });

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
            if (stream != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
                bufferedReader.lines().forEach(resource ->
                        archive.addAsWebResource(serviceRelativePath + "/" + resource, MODULE_RELATIVE_PATH + resource)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        archive.addClass(serviceImplClass);
    }


    @ArquillianResource
    @SuppressWarnings("unused")
    private URL base;


    @BeforeEach
    public final void setUpTestBase() {
        SyncProxy.setBaseURL(getModuleBaseURL());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    protected String getModuleBaseURL() {
        return base + MODULE_RELATIVE_PATH;
    }
}
