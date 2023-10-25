/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.user.server.rpc.RpcTokenServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests RpcToken functionality.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class RpcTokenTest extends RpcAsyncTestBase<RpcTokenTestService, RpcTokenTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(RpcTokenTestService.class, RpcTokenServiceImpl.class, "rpctokentest");
    }


    public RpcTokenTest() {
        super(RpcTokenTestService.class, "rpctokentest");
    }


    /**
     * First RpcToken implementation.
     */
    public static class TestRpcToken implements RpcToken {
        String tokenValue;
    }

    /**
     * Second RpcToken implementation.
     */
    public static class AnotherTestRpcToken implements RpcToken {
        int token;
    }


    @Test
    public void testRpcTokenMissing() {
        service.getRpcTokenFromRequest(waitedCallback(token -> {
            assertNull(token);
        }));
    }

    @Test
    public void testRpcTokenPresent() {
        final TestRpcToken token = new TestRpcToken();
        token.tokenValue = "Drink kumys!";
        ((HasRpcToken) service).setRpcToken(token);

        service.getRpcTokenFromRequest(waitedCallback(rpcToken -> {
            assertNotNull(rpcToken);
            assertTrue(rpcToken instanceof TestRpcToken);
            assertEquals(token.tokenValue, ((TestRpcToken) rpcToken).tokenValue);
        }));
    }

    /**
     * Modified by Antonio Macrì to remove dependence on {@link GWT#getModuleBaseURL()}
     */
    @Test
    public void testRpcTokenExceptionHandler() {
        ((ServiceDefTarget) service).setServiceEntryPoint(getModuleBaseURL() + "rpctokentest?throw=true");
        ((HasRpcToken) service).setRpcTokenExceptionHandler(Assertions::assertNotNull);

        service.getRpcTokenFromRequest(waitedCallback(rpcToken -> {
            fail("Should've called RpcTokenExceptionHandler");
        }));
    }

    @Test
    public void testRpcTokenAnnotation() {
        final AnotherTestRpcToken token = new AnotherTestRpcToken();
        token.token = 1337;
        ((HasRpcToken) service).setRpcToken(token);

        service.getRpcTokenFromRequest(waitedCallback(result -> {
            assertNotNull(result);
            assertTrue(result instanceof AnotherTestRpcToken);
            assertEquals(token.token, ((AnotherTestRpcToken) result).token);
        }));
    }
}
