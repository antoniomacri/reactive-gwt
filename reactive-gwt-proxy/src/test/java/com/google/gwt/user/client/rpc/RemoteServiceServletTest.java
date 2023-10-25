/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.server.rpc.RemoteServiceServletTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test case is used to check that the RemoteServiceServlet walks the class
 * hierarchy looking for the service interface. Prior to this test the servlet
 * would only look into the concrete class but not in any of its super classes.
 * <p>
 * See <a href=
 * "http://code.google.com/p/google-web-toolkit/issues/detail?id=50&can=3&q="
 * >Bug 50</a> for more details.
 * <p>
 * This test works in conjunction with
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServletTestServiceImpl}.
 * </p>
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class RemoteServiceServletTest extends RpcAsyncTestBase<RemoteServiceServletTestService, RemoteServiceServletTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(RemoteServiceServletTestService.class, RemoteServiceServletTestServiceImpl.class, "rss");
    }


    public RemoteServiceServletTest() {
        super(RemoteServiceServletTestService.class, "rss");
    }


    private static class MyRpcRequestBuilder extends RpcRequestBuilder {
        private boolean doCreate;
        private boolean doFinish;
        private boolean doSetCallback;
        private boolean doSetContentType;
        private boolean doSetRequestData;
        private boolean doSetRequestId;

        public void check() {
            assertTrue(this.doCreate);
            assertTrue(this.doFinish);
            assertTrue(this.doSetCallback);
            assertTrue(this.doSetContentType);
            assertTrue(this.doSetRequestData);
            assertTrue(this.doSetRequestId);
        }

        @Override
        protected RequestBuilder doCreate(String serviceEntryPoint) {
            this.doCreate = true;
            return super.doCreate(serviceEntryPoint);
        }

        @Override
        protected void doFinish(RequestBuilder rb) {
            this.doFinish = true;
            super.doFinish(rb);
        }

        @Override
        protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
            this.doSetCallback = true;
            super.doSetCallback(rb, callback);
        }

        @Override
        protected void doSetContentType(RequestBuilder rb, String contentType) {
            this.doSetContentType = true;
            super.doSetContentType(rb, contentType);
        }

        @Override
        protected void doSetRequestData(RequestBuilder rb, String data) {
            this.doSetRequestData = true;
            super.doSetRequestData(rb, data);
        }

        @Override
        protected void doSetRequestId(RequestBuilder rb, int id) {
            this.doSetRequestId = true;
            super.doSetRequestId(rb, id);
        }
    }

    private Request req;


    /**
     * Modified by P.Prith to handle the {@link GWT#getModuleBaseURL()}
     * dependency
     */
    @Test
    public void testAlternateStatusCode() {
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "servlettest/404");

        service.test(waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                if (caught instanceof StatusCodeException) {
                    assertEquals(Response.SC_NOT_FOUND, ((StatusCodeException) caught).getStatusCode());
                    assertEquals("Not Found", ((StatusCodeException) caught).getStatusText());
                } else {
                    TestSetValidator.rethrowException(caught);
                }
            }

            @Override
            public void onSuccess(Void result) {
                fail("Should not have succeeded");
            }
        }));
    }

    /**
     * Verify behavior when the RPC method throws a RuntimeException declared on
     * the RemoteService interface.
     */
    @Test
    public void testDeclaredRuntimeException() {
        service.throwDeclaredRuntimeException(waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertTrue(caught instanceof NullPointerException);
                assertEquals("expected", caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                fail("Unexpected onSuccess");
            }
        }));
    }

    @Test
    @Tag("NotSupported")
    public void testManualSend() throws RequestException {
        RequestBuilder builder = service.testExpectCustomHeader(waitedCallback(result -> {
            assertFalse(req.isPending());
        }));

        builder.setHeader("X-Custom-Header", "true");
        this.req = builder.send();
        assertTrue(this.req.isPending());
    }

    /**
     * Modified by P.prith to remove dependence on GWT
     */
    @Test
    public void testPermutationStrongName() {
        String permutationStrongName = ((ServiceDefTarget) service).getSerializationPolicyName();
        // assertNotNull(GWT.getPermutationStrongName());
        assertNotNull(permutationStrongName);
        // service.testExpectPermutationStrongName(GWT.getPermutationStrongName(),
        service.testExpectPermutationStrongName(permutationStrongName, waitedCallback(ignored -> {
        }));
    }

    /**
     * Test that the policy strong name is available from browser-side Java code.
     */
    @Test
    public void testPolicyStrongName() {
        String policy = ((ServiceDefTarget) service).getSerializationPolicyName();
        assertNotNull(policy);
        assertFalse(policy.isEmpty());
    }

    /**
     * Send request without the permutation strong name and expect a
     * SecurityException. This tests
     * RemoteServiceServlet#checkPermutationStrongName.
     */
    @Test
    @Tag("NotSupported")
    public void testRequestWithoutStrongNameHeader() {
        ((ServiceDefTarget) service).setRpcRequestBuilder(new RpcRequestBuilder() {
            /**
             * Copied from base class.
             */
            @Override
            protected void doFinish(RequestBuilder rb) {
                // Don't set permutation strong name
                rb.setHeader(MODULE_BASE_HEADER, GWT.getModuleBaseURL());
            }
        });

        service.test(waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertTrue(caught instanceof StatusCodeException);
                assertEquals(500, ((StatusCodeException) caught).getStatusCode());
            }

            @Override
            public void onSuccess(Void result) {
                fail("Unexpected onSuccess");
            }
        }));
    }

    /**
     * Ensure that each doFoo method is called.
     */
    @Test
    @Tag("NotSupported")
    public void testRpcRequestBuilder() {
        final MyRpcRequestBuilder builder = new MyRpcRequestBuilder();
        ((ServiceDefTarget) service).setRpcRequestBuilder(builder);

        service.test(waitedCallback(result -> builder.check()));
    }

    @Test
    @Tag("NotSupported")
    public void testServiceInterfaceLocation() {
        this.req = service.test(waitedCallback(result -> {
            assertFalse(RemoteServiceServletTest.this.req.isPending());
        }));
        assertTrue(this.req.isPending());
    }

    /**
     * Verify behavior when the RPC method throws an unknown RuntimeException
     * (possibly one unknown to the client).
     */
    @Test
    public void testUnknownRuntimeException() {
        service.throwUnknownRuntimeException(waitedCallback(new AsyncCallback<>() {

            @Override
            public void onFailure(Throwable caught) {
                assertTrue(caught instanceof InvocationException);
            }

            @Override
            public void onSuccess(Void result) {
                fail("Unexpected onSuccess");
            }
        }));
    }
}
