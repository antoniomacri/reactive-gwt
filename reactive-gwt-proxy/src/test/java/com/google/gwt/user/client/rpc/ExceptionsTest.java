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

import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.server.rpc.ExceptionsTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests serialization of various GWT Exception classes for RPC.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class ExceptionsTest extends RpcAsyncTestBase<ExceptionsTestService, ExceptionsTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(ExceptionsTestService.class, ExceptionsTestServiceImpl.class);
    }


    public ExceptionsTest() {
        super(ExceptionsTestService.class);
    }

    @Test
    public void testSerializableThrowable() {
        SerializableThrowable expected = new SerializableThrowable(null, "msg");
        expected.setDesignatedType("x", true);
        expected.setStackTrace(new StackTraceElement[]{new StackTraceElement(
                "c", "m", "f", 42)});
        expected.initCause(new SerializableThrowable(null, "cause"));

        service.echo(expected, createCallback(result -> {
            assertThat(result)
                    .isNotNull()
                    .hasMessage("msg");
            assertEquals("x", result.getDesignatedType());
            assertTrue(result.isExactDesignatedTypeKnown());
            assertEquals("c.m(f:42)", result.getStackTrace()[0].toString());
            assertThat(result.getCause())
                    .isInstanceOf(SerializableThrowable.class)
                    .hasMessage("cause");
        }));
    }

    @Test
    public void testUmbrellaException() {
        final UmbrellaException expected = TestSetFactory.createUmbrellaException();
        service.echo(expected, createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(expected, result));
        }));
    }
}
