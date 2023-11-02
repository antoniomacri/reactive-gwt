package com.github.antoniomacri.reactivegwt.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;


public class ExceptionThrowingServiceTest {
    private static final Logger log = LoggerFactory.getLogger(ExceptionThrowingServiceTest.class);
    protected static final String MODULE_RELATIVE_PATH = "AppRoot/AppModule/";
    protected static final String RESOURCE_FOLDER = "throws";

    WireMockServer wm;


    @BeforeEach
    public void setUp() {
        wm = new WireMockServer(wireMockConfig().dynamicPort());
        wm.start();

        TestUtils.serveStaticFiles(wm, RESOURCE_FOLDER, MODULE_RELATIVE_PATH);

        wm.stubFor(post("/" + MODULE_RELATIVE_PATH + "throws").willReturn(aResponse()
                .withBody("//EX[2,1,[\"java.lang.Exception/1920171873\",\"EJB Exception: : java.lang.NullPointerException\\n\\tat com.example.test.ejb.facade.AlertFacadeBean.delete(AlertFacadeBean.java:310)\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\\n\\tat com.truncated.stacktrace.SampleImpl.invoke(SampleImpl.java:62)\\n\\tat weblogic.work.LivePartitionUtility.doRunWorkUnderContext(LivePartitionUtility.java:57)\\n\\tat weblogic.work.PartitionUtility.runWorkUnderContext(PartitionUtility.java:41)\\n\\tat weblogic.work.SelfTuningWorkManagerImpl.runWorkUnderContext(SelfTuningWorkManagerImpl.java:655)\\n\\tat weblogic.work.ExecuteThread.execute(ExecuteThread.java:420)\\n\\tat weblogic.work.ExecuteThread.run(ExecuteThread.java:360)\\n\"],0,7]")
                .withHeader("content-type", "application/json; charset=utf-8")
                .withStatus(200)
        ));
    }

    @AfterEach
    public final void afterEach() {
        wm.shutdownServer();
    }


    @Test
    public void throwCheckedException() throws InterruptedException {
        ExceptionThrowingServiceAsync service = getService();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> exceptionReference = new AtomicReference<>();
        AtomicReference<Error> unexpectedExceptionReference = new AtomicReference<>();

        service.throwCheckedException(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                log.error("Got exception", caught);
                exceptionReference.set(caught);
                latch.countDown();
            }

            @Override
            public void onSuccess(Void result) {
                log.info(String.format("Got response %s", result));
                try {
                    fail("Unexpected onSuccess");
                } catch (Error e) {
                    unexpectedExceptionReference.set(e);
                }
                latch.countDown();
            }
        });

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

        if (unexpectedExceptionReference.get() != null) {
            throw unexpectedExceptionReference.get();
        }

        assertThat(exceptionReference).hasValueSatisfying(exception ->
                assertThat(exception)
                        .isNotNull()
                        .isInstanceOf(Exception.class)
                        .hasMessage("EJB Exception: : java.lang.NullPointerException\n\tat com.example.test.ejb.facade.AlertFacadeBean.delete(AlertFacadeBean.java:310)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat com.truncated.stacktrace.SampleImpl.invoke(SampleImpl.java:62)\n\tat weblogic.work.LivePartitionUtility.doRunWorkUnderContext(LivePartitionUtility.java:57)\n\tat weblogic.work.PartitionUtility.runWorkUnderContext(PartitionUtility.java:41)\n\tat weblogic.work.SelfTuningWorkManagerImpl.runWorkUnderContext(SelfTuningWorkManagerImpl.java:655)\n\tat weblogic.work.ExecuteThread.execute(ExecuteThread.java:420)\n\tat weblogic.work.ExecuteThread.run(ExecuteThread.java:360)\n")
                        .hasNoCause()
        );
    }


    protected String getModuleBaseURL() {
        return wm.baseUrl() + "/" + MODULE_RELATIVE_PATH;
    }

    private ExceptionThrowingServiceAsync getService() {
        ExceptionThrowingServiceAsync service = ReactiveGWT.create(ExceptionThrowingService.class, getModuleBaseURL());
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "throws");
        return service;
    }
}
