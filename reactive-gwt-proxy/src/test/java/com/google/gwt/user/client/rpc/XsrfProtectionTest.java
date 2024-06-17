/*
 * Copyright 2011 Google Inc.
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

import com.github.antoniomacri.reactivegwt.proxy.ReactiveGWT;
import com.google.gwt.user.server.rpc.MockXsrfTokenServiceImpl;
import com.google.gwt.user.server.rpc.XsrfProtectedServiceServlet;
import com.google.gwt.user.server.rpc.XsrfTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests XSRF protection provided by {@link XsrfProtectedServiceServlet} and
 * {@link XsrfTokenService}.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class XsrfProtectionTest extends RpcAsyncTestBase<XsrfTestService, XsrfTestServiceAsync> {

    public static final String SESSION_COOKIE_NAME = "MYSESSIONCOOKIE";


    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(XsrfTestService.class, Map.of(
                XsrfTestServiceImpl.class, Optional.of("xsrftestservice"),
                MockXsrfTokenServiceImpl.class, Optional.of("xsrfmock")
        ));
    }


    public XsrfProtectionTest() {
        super(XsrfTestService.class, "xsrftestservice");
    }


    @BeforeEach
    public void gwtSetUp() {
        ReactiveGWT.suppressRelativePathWarning(true);
        // MD5 test vector
        // TODO (AM): set cookie
//        Cookies.setCookie(SESSION_COOKIE_NAME, "abc");
    }

    @AfterEach
    public void gwtTearDown() {
        // TODO (AM): remove cookie
//        Cookies.removeCookie(SESSION_COOKIE_NAME);
    }


    protected XsrfTokenServiceAsync getAsyncXsrfService() {
        XsrfTokenServiceAsync service = ReactiveGWT.create(XsrfTokenService.class, getModuleBaseURL());
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "xsrfmock");
        return service;
    }


    @Test
    @Tag("IllegalAccess")
    public void testRpcWithoutXsrfTokenFails() {
        service.drink("kumys", waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                RpcTokenException e = (RpcTokenException) caught;
                assertTrue(e.getMessage().contains("XSRF token missing"));
                checkServerState("kumys", false);
            }

            @Override
            public void onSuccess(Void result) {
                fail("Should've failed without XSRF token");
            }
        }));
    }

    @Test
    public void testRpcWithBadXsrfTokenFails() {
        XsrfToken badToken = new XsrfToken("Invalid Token");
        ((HasRpcToken) service).setRpcToken(badToken);

        service.drink("maksym", waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                checkServerState("maksym", false);
            }

            @Override
            public void onSuccess(Void result) {
                fail("Should've failed with bad XSRF token");
            }
        }));
    }

    @Test
    @Tag("NotSupported")
    public void testXsrfTokenService() {
        XsrfTokenServiceAsync xsrfService = getAsyncXsrfService();

        xsrfService.getNewXsrfToken(waitedCallback(result -> {
            assertNotNull(result);
            assertNotNull(result.getToken());
            // MD5("abc")
            assertEquals("900150983CD24FB0D6963F7D28E17F72", result.getToken());
        }));
    }

    @Test
    @Tag("NotSupported")
    public void testRpcWithXsrfToken() {
        XsrfTokenServiceAsync xsrfService = getAsyncXsrfService();

        xsrfService.getNewXsrfToken(waitedCallback(result -> {
            ((HasRpcToken) service).setRpcToken(result);
            service.drink("airan", waitedCallback(ignored -> {
                checkServerState("airan", true);
            }));
        }));
    }

    @Test
    @Tag("NotSupported")
    public void testXsrfTokenWithDifferentSessionCookieFails() {
        XsrfTokenServiceAsync xsrfService = getAsyncXsrfService();

        xsrfService.getNewXsrfToken(waitedCallback(result -> {
            // Ensure it's MD5
            assertEquals(32, result.getToken().length());

            ((HasRpcToken) service).setRpcToken(result);

            // change cookie to ensure verification fails since
            // XSRF token was derived from previous cookie value
            // TODO (AM): change cookie
            //Cookies.setCookie(SESSION_COOKIE_NAME, "sometingrandom");

            service.drink("bozo", waitedCallback(new AsyncCallback<>() {
                @Override
                public void onFailure(Throwable caught) {
                    RpcTokenException e = (RpcTokenException) caught;
                    assertTrue(e.getMessage().contains("Invalid XSRF token"));
                    checkServerState("bozo", false);
                }

                @Override
                public void onSuccess(Void result) {
                    fail("Should've failed since session cookie has changed");
                }
            }));
        }));
    }

    private void checkServerState(String drink, final boolean stateShouldChange) {
        service.checkIfDrankDrink(drink, waitedCallback(result ->
                assertEquals(stateShouldChange, result)
        ));
    }
}
