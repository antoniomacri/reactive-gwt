/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.client.rpc.EnumsTestService.Basic;
import com.google.gwt.user.client.rpc.EnumsTestService.Complex;
import com.google.gwt.user.client.rpc.EnumsTestService.FieldEnum;
import com.google.gwt.user.client.rpc.EnumsTestService.FieldEnumWrapper;
import com.google.gwt.user.client.rpc.EnumsTestService.Subclassing;
import com.google.gwt.user.server.rpc.EnumsTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests enums over RPC.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class EnumsTest extends RpcAsyncTestBase<EnumsTestService, EnumsTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(EnumsTestService.class, EnumsTestServiceImpl.class, "enums");
    }


    public EnumsTest() {
        super(EnumsTestService.class, "enums");
    }


    /**
     * Test that basic enums can be used over RPC.
     */
    @Test
    public void testBasicEnums() {
        getService().echo(Basic.A, createCallback(result -> {
            assertThat(result).isNotNull();
            assertEquals(Basic.A, result);
        }));
    }

    /**
     * Test that complex enums with state and non-default constructors can be used
     * over RPC and that the client state does not change.
     */
    @Test
    public void testComplexEnums() {
        Complex a = Complex.A;
        a.value = "client";
        getService().echo(Complex.A, createCallback(result -> {
            assertThat(result).isNotNull();
            assertEquals(Complex.A, result);

            // Ensure that the server's changes did not impact us.
            assertEquals("client", result.value);
        }));
    }

    /**
     * Test that null can be used as an enumeration.
     */
    @Test
    public void testNull() {
        getService().echo((Basic) null, createCallback(result -> {
            assertThat(result).isNull();
        }));
    }

    /**
     * Test that enums with subclasses can be passed over RPC.
     */
    @Test
    public void testSubclassingEnums() {
        getService().echo(Subclassing.A, createCallback(result -> {
            assertThat(result).isNotNull();
            assertEquals(Subclassing.A, result);
        }));
    }

    /**
     * Test that enums as fields in a wrapper class can be passed over RPC.
     */
    @Test
    public void testFieldEnumWrapperClass() {
        FieldEnumWrapper wrapper = new FieldEnumWrapper();
        wrapper.setFieldEnum(FieldEnum.X);
        getService().echo(wrapper, createCallback(result -> {
            assertThat(result).isNotNull();
            FieldEnum fieldEnum = result.getFieldEnum();
            /*
             * Don't want to do assertEquals(FieldEnum.X, fieldEnum) here,
             * since it will force an implicit upcast on FieldEnum -> Object,
             * which will bias the test.  We want to assert that the
             * EnumOrdinalizer properly prevents ordinalization of FieldEnum.
             */
            assertTrue(FieldEnum.X == fieldEnum);
        }));
    }


    private static void rethrowException(Throwable caught) {
        if (caught instanceof RuntimeException) {
            throw (RuntimeException) caught;
        } else {
            throw new RuntimeException(caught);
        }
    }
}
