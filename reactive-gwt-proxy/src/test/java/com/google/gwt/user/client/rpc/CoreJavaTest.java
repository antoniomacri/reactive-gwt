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

import com.google.gwt.user.server.rpc.CoreJavaTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test cases for Java core emulation classes in GWT RPC.
 * <p>
 * Logging is tested in a separate suite because it requires logging enabled in
 * gwt.xml. Otherwise, only MathContext has non-trivial content for RPC.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class CoreJavaTest extends RpcAsyncTestBase<CoreJavaTestService, CoreJavaTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(CoreJavaTestService.class, CoreJavaTestServiceImpl.class);
    }

    private static MathContext createMathContext() {
        return new MathContext(5, RoundingMode.CEILING);
    }

    public static boolean isValid(MathContext value) {
        return createMathContext().equals(value);
    }


    public CoreJavaTest() {
        super(CoreJavaTestService.class);
    }


    @Test
    public void testMathContext() {
        final MathContext expected = createMathContext();
        service.echoMathContext(expected, waitedCallback(result -> {
            assertNotNull(result);
            assertTrue(isValid(result));
        }));
    }
}
