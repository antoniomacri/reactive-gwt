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

import com.github.antoniomacri.reactivegwt.proxy.SyncProxy;
import com.google.gwt.user.server.rpc.AnnotatedRpcTokenTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;

/**
 * Tests RpcToken functionality.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class AnnotatedRpcTokenTest extends RpcAsyncTestBase<AnnotatedRpcTokenTestService, AnnotatedRpcTokenTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(AnnotatedRpcTokenTestService.class, AnnotatedRpcTokenTestServiceImpl.class);
    }

    public AnnotatedRpcTokenTest() {
        super(AnnotatedRpcTokenTestService.class);
    }


    @BeforeEach
    public void setUp() {
        SyncProxy.suppressRelativePathWarning(true);
    }


    @Test
    public void testRpcTokenAnnotationDifferentFromActualType() {
        // service is annotated to use AnotherTestRpcToken and not TestRpcToken,
        // generated proxy should catch this error
        final RpcTokenTest.TestRpcToken token = new RpcTokenTest.TestRpcToken();
        token.tokenValue = "Drink kumys!";
        try {
            ((HasRpcToken) service).setRpcToken(token);
            fail("Should have thrown an RpcTokenException");
        } catch (RpcTokenException e) {
            // Expected
        }
    }
}
