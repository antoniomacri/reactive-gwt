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

import com.google.gwt.user.server.rpc.ValueTypesTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test transfer of value types over RPC.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class ValueTypesTest extends RpcAsyncTestBase<ValueTypesTestService, ValueTypesTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(ValueTypesTestService.class, ValueTypesTestServiceImpl.class, "valuetypes");
    }


    public ValueTypesTest() {
        super(ValueTypesTestService.class, "valuetypes");
    }


    @Test
    public void bigDecimal_base() {
        assertEcho(new BigDecimal("42"));
    }

    @Test
    public void bigDecimal_exponential() {
        assertEcho(new BigDecimal("0.00000000000000000001"));
    }

    @Test
    public void bigDecimal_negative() {
        assertEcho(new BigDecimal("-42"));
    }

    @Test
    public void bigDecimal_zero() {
        assertEcho(new BigDecimal("0.0"));
    }

    @Test
    public void bigInteger_base() {
        assertEcho(new BigInteger("42"));
    }

    @Test
    public void bigInteger_exponential() {
        assertEcho(new BigInteger("100000000000000000000"));
    }

    @Test
    public void bigInteger_negative() {
        assertEcho(new BigInteger("-42"));
    }

    @Test
    public void bigInteger_zero() {
        assertEcho(new BigInteger("0"));
    }

    @Test
    public void boolean_FALSE() {
        service.echo_FALSE(false, createCallback(result -> {
            assertNotNull(result);
            assertFalse(result);
        }));
    }

    @Test
    public void boolean_TRUE() {
        service.echo_TRUE(true, createCallback(result -> {
            assertNotNull(result);
            assertTrue(result);
        }));
    }

    @Test
    public void testByte() {
        service.echo((byte) (Byte.MAX_VALUE / (byte) 2), this.<Byte>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Byte.MAX_VALUE / 2, result.byteValue());
        }));
    }

    @Test
    public void byte_MAX_VALUE() {
        service.echo_MAX_VALUE(Byte.MAX_VALUE, this.<Byte>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Byte.MAX_VALUE, result.byteValue());
        }));
    }

    @Test
    public void byte_MIN_VALUE() {
        service.echo_MIN_VALUE(Byte.MIN_VALUE, this.<Byte>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Byte.MIN_VALUE, result.byteValue());
        }));
    }

    @Test
    public void testChar() {
        final char value = (char) (Character.MAX_VALUE / (char) 2);
        service.echo(value, this.<Character>createCallback(result -> {
            assertNotNull(result);
            assertEquals(value, result.charValue());
        }));
    }

    @Test
    public void char_MAX_VALUE() {
        service.echo_MAX_VALUE(Character.MAX_VALUE, this.<Character>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Character.MAX_VALUE, result.charValue());
        }));
    }

    @Test
    public void char_MIN_VALUE() {
        service.echo_MIN_VALUE(Character.MIN_VALUE, this.<Character>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Character.MIN_VALUE, result.charValue());
        }));
    }

    @Test
    public void testDouble() {
        service.echo(Double.MAX_VALUE / 2, createCallback(result -> {
            assertNotNull(result);
            assertEquals(Double.MAX_VALUE / 2, result, 0.0);
        }));
    }

    @Test
    public void double_MAX_VALUE() {
        service.echo_MAX_VALUE(Double.MAX_VALUE, createCallback(result -> {
            assertNotNull(result);
            assertEquals(Double.MAX_VALUE, result, 0.0);
        }));
    }

    @Test
    public void double_MIN_VALUE() {
        service.echo_MIN_VALUE(Double.MIN_VALUE, createCallback(result -> {
            assertNotNull(result);
            assertEquals(Double.MIN_VALUE, result, 0.0);
        }));
    }

    /**
     * Validate that NaNs (not-a-number, such as 0/0) propagate properly via RPC.
     */
    @Test
    public void double_NaN() {
        service.echo(Double.NaN, createCallback(result -> {
            assertNotNull(result);
            assertTrue(Double.isNaN(result));
        }));
    }

    /**
     * Validate that negative infinity propagates properly via RPC.
     */
    @Test
    public void double_NegInfinity() {
        service.echo(Double.NEGATIVE_INFINITY, createCallback(result -> {
            assertNotNull(result);
            double doubleValue = result;
            assertTrue(Double.isInfinite(doubleValue) && doubleValue < 0);
        }));
    }

    /**
     * Validate that positive infinity propagates properly via RPC.
     */
    @Test
    public void double_PosInfinity() {
        service.echo(Double.POSITIVE_INFINITY, createCallback(result -> {
            assertNotNull(result);
            double doubleValue = result;
            assertTrue(Double.isInfinite(doubleValue) && doubleValue > 0);
        }));
    }

    @Test
    public void testFloat() {
        service.echo(Float.MAX_VALUE / 2, this.<Float>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Float.MAX_VALUE / 2, result, 0.0);
        }));
    }

    @Test
    public void float_MAX_VALUE() {
        service.echo_MAX_VALUE(Float.MAX_VALUE, this.<Float>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Float.MAX_VALUE, result, 0.0);
        }));
    }

    @Test
    public void float_MIN_VALUE() {
        service.echo_MIN_VALUE(Float.MIN_VALUE, this.<Float>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Float.MIN_VALUE, result, 0.0);
        }));
    }

    /**
     * Validate that NaNs (not-a-number, such as 0/0) propagate properly via RPC.
     */
    @Test
    public void float_NaN() {
        service.echo(Float.NaN, this.<Float>createCallback(result -> {
            assertNotNull(result);
            assertTrue(Float.isNaN(result));
        }));
    }

    /**
     * Validate that negative infinity propagates properly via RPC.
     */
    @Test
    public void float_NegInfinity() {
        service.echo(Float.NEGATIVE_INFINITY, this.<Float>createCallback(result -> {
            assertNotNull(result);
            float floatValue = result;
            assertTrue(Float.isInfinite(floatValue) && floatValue < 0);
        }));
    }

    /**
     * Validate that positive infinity propagates properly via RPC.
     */
    @Test
    public void float_PosInfinity() {
        service.echo(Float.POSITIVE_INFINITY, this.<Float>createCallback(result -> {
            assertNotNull(result);
            float floatValue = result;
            assertTrue(Float.isInfinite(floatValue) && floatValue > 0);
        }));
    }

    @Test
    public void testInteger() {
        service.echo(Integer.MAX_VALUE / 2, this.<Integer>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Integer.MAX_VALUE / 2, result.intValue());
        }));
    }

    @Test
    public void integer_MAX_VALUE() {
        service.echo_MAX_VALUE(Integer.MAX_VALUE, this.<Integer>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Integer.MAX_VALUE, result.intValue());
        }));
    }

    @Test
    public void integer_MIN_VALUE() {
        service.echo_MIN_VALUE(Integer.MIN_VALUE, this.<Integer>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Integer.MIN_VALUE, result.intValue());
        }));
    }

    @Test
    public void testLong() {
        service.echo(Long.MAX_VALUE / 2, this.<Long>createCallback(result -> {
            assertNotNull(result);
            long expected = Long.MAX_VALUE / 2;
            assertEquals(expected, result.longValue());
        }));
    }

    @Test
    public void long_MAX_VALUE() {
        service.echo_MAX_VALUE(Long.MAX_VALUE, this.<Long>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Long.MAX_VALUE, result.longValue());
        }));
    }

    @Test
    public void long_MIN_VALUE() {
        service.echo_MIN_VALUE(Long.MIN_VALUE, this.<Long>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Long.MIN_VALUE, result.longValue());
        }));
    }

    @Test
    public void testShort() {
        final short value = (short) (Short.MAX_VALUE / 2);
        service.echo(value, this.<Short>createCallback(result -> {
            assertNotNull(result);
            assertEquals(value, result.shortValue());
        }));
    }

    @Test
    public void short_MAX_VALUE() {
        service.echo_MAX_VALUE(Short.MAX_VALUE, this.<Short>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Short.MAX_VALUE, result.shortValue());
        }));
    }

    @Test
    public void short_MIN_VALUE() {
        service.echo_MIN_VALUE(Short.MIN_VALUE, this.<Short>createCallback(result -> {
            assertNotNull(result);
            assertEquals(Short.MIN_VALUE, result.shortValue());
        }));
    }

    @Test
    public void voidParameterizedType() {
        service.echo(new SerializableGenericWrapperType<>(), createCallback(result -> {
            assertNotNull(result);
            assertNull(result.getValue());
        }));
    }

    @Test
    public void string() {
        assertEcho("test");
    }

    @Test
    public void string_empty() {
        assertEcho("");
    }

    @Test
    public void string_over64KB() {
        // Test a string over 64KB of a-z characters repeated.
        StringBuilder testString = new StringBuilder();
        int totalChars = 0xFFFF + 0xFF;
        for (int i = 0; i < totalChars; i++) {
            testString.append((char) ('a' + (i % 26)));
        }
        assertEcho(testString.toString());
    }

    @Test
    public void string_over64KBWithUnicode() {
        // Test a string over64KB string that requires unicode escaping.
        StringBuilder testString = new StringBuilder();
        int totalChars = 0xFFFF + 0xFF;
        for (int i = 0; i < totalChars; i += 2) {
            testString.append('‑');
            testString.append((char) 0x08);
        }
        assertEcho(testString.toString());
    }

    private void assertEcho(final BigDecimal value) {
        service.echo(value, createCallback(result -> assertEquals(value, result)));
    }

    private void assertEcho(final BigInteger value) {
        service.echo(value, createCallback(result -> assertEquals(value, result)));
    }

    private void assertEcho(final String value) {
        service.echo(value, createCallback(result -> assertEquals(value, result)));
    }
}
