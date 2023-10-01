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

import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArraysAsList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnum;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnumMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeVector;
import com.google.gwt.user.client.rpc.core.java.util.LinkedHashMap_CustomFieldSerializer;
import com.google.gwt.user.server.rpc.CollectionsTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests collections across RPC.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class CollectionsTest extends RpcAsyncTestBase<CollectionsTestService, CollectionsTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(CollectionsTestService.class, CollectionsTestServiceImpl.class);
    }


    public CollectionsTest() {
        super(CollectionsTestService.class);
    }

    @Test
    public void testArrayList() {
        service.echo(TestSetFactory.createArrayList(), createCallback(result -> {
            assertThat(result).isNotNull();
            assertTrue(TestSetValidator.isValid(result));
        }));
    }

    @Test
    public void testArrayListVoid() {
        service.echoArrayListVoid(TestSetFactory.createArrayListVoid(), createCallback(result -> {
            assertThat(result).isNotNull();
            assertTrue(TestSetValidator.isValidArrayListVoid(result));
        }));
    }

    @Test
    public void testArraysAsList() {
        final List<MarkerTypeArraysAsList> expected = TestSetFactory.createArraysAsList();

        service.echoArraysAsList(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(expected).isEqualTo(result);
        }));
    }

    @Test
    public void testBooleanArray() {
        final Boolean[] expected = TestSetFactory.createBooleanArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, false));
        }));
    }

    @Test
    public void testByteArray() {
        final Byte[] expected = TestSetFactory.createByteArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, (byte) 0));
        }));
    }

    @Test
    public void testCharArray() {
        final Character[] expected = TestSetFactory.createCharArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, '0'));
        }));
    }

    @Test
    public void testDateArray() {
        final Date[] expected = TestSetFactory.createDateArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, new Date()));
        }));
    }

    @Test
    public void testDoubleArray() {
        final Double[] expected = TestSetFactory.createDoubleArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, 0.0d));
        }));
    }

    @Test
    public void testEmptyEnumMap() {
        final EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected = TestSetFactory.createEmptyEnumMap();
        service.echoEmptyEnumMap(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testEmptyList() {
        service.echo(TestSetFactory.createEmptyList(), createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }));
    }

    @Test
    public void testEmptyMap() {
        service.echo(TestSetFactory.createEmptyMap(), createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }));
    }

    @Test
    public void testEmptySet() {
        service.echo(TestSetFactory.createEmptySet(), createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }));
    }

    @Test
    public void testEnumArray() {
        final Enum<?>[] expected = TestSetFactory.createEnumArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, MarkerTypeEnum.C));
        }));
    }

    @Test
    public void testEnumMap() {
        final EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected = TestSetFactory.createEnumMap();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testEnumMapEnumKey() {
        final EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected = TestSetFactory.createEnumMapEnumKey();
        service.echoEnumKey(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertTrue(TestSetValidator.isValidEnumKey(expected, result));
        }));
    }

    @Test
    public void testFloatArray() {
        final Float[] expected = TestSetFactory.createFloatArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type
            // checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, 0.0f));
        }));
    }

    @Test
    public void testHashMap() {
        final HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue> expected = TestSetFactory.createHashMap();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testHashSet() {
        final HashSet<MarkerTypeHashSet> expected = TestSetFactory.createHashSet();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testIdentityHashMap() {
        final IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> expected = TestSetFactory
                .createIdentityHashMap();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testIdentityHashMapEnumKey() {
        final IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> expected = TestSetFactory
                .createIdentityHashMapEnumKey();
        service.echoEnumKey(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertTrue(TestSetValidator.isValidEnumKey(expected, result));
        }));
    }

    @Test
    public void testIntegerArray() {
        final Integer[] expected = TestSetFactory.createIntegerArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, 0));
        }));
    }

    @Test
    public void testLinkedHashMap() {

        final LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> expected = TestSetFactory
                .createLinkedHashMap();
        assertFalse(LinkedHashMap_CustomFieldSerializer.getAccessOrderNoReflection(expected));

        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            expected.get("SerializableSet");
            result.get("SerializableSet");
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testLinkedHashMapLRU() {

        final LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> expected = TestSetFactory.createLRULinkedHashMap();
        assertTrue(LinkedHashMap_CustomFieldSerializer.getAccessOrderNoReflection(expected));

        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            expected.get("SerializableSet");
            result.get("SerializableSet");
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testLinkedHashSet() {
        final LinkedHashSet<MarkerTypeLinkedHashSet> expected = TestSetFactory.createLinkedHashSet();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testLinkedList() {
        final LinkedList<MarkerTypeLinkedList> expected = TestSetFactory.createLinkedList();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testLongArray() {
        final Long[] expected = TestSetFactory.createLongArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type
            // checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, 0L));
        }));
    }

    @Test
    public void testPrimitiveBooleanArray() {
        final boolean[] expected = TestSetFactory.createPrimitiveBooleanArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveByteArray() {
        final byte[] expected = TestSetFactory.createPrimitiveByteArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveCharArray() {
        final char[] expected = TestSetFactory.createPrimitiveCharArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveDoubleArray() {
        final double[] expected = TestSetFactory.createPrimitiveDoubleArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveFloatArray() {
        final float[] expected = TestSetFactory.createPrimitiveFloatArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveIntegerArray() {
        final int[] expected = TestSetFactory.createPrimitiveIntegerArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveLongArray() {
        final long[] expected = TestSetFactory.createPrimitiveLongArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testPrimitiveShortArray() {
        final short[] expected = TestSetFactory.createPrimitiveShortArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }

    @Test
    public void testShortArray() {
        final Short[] expected = TestSetFactory.createShortArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type
            // checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, (short) 0));
        }));
    }

    @Test
    public void testSingletonList() {
        service.echoSingletonList(TestSetFactory.createSingletonList(), createCallback(result -> {
            assertThat(result).isNotNull();
            assertTrue(TestSetValidator.isValidSingletonList(result));
        }));
    }

    @Test
    public void testSqlDateArray() {
        final java.sql.Date[] expected = TestSetFactory.createSqlDateArray();
        service.echo(expected, this.<java.sql.Date[]>createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, new java.sql.Date(0L)));
        }));
    }

    @Test
    public void testSqlTimeArray() {
        final java.sql.Time[] expected = TestSetFactory.createSqlTimeArray();
        service.echo(expected, this.<java.sql.Time[]>createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, new java.sql.Time(0L)));
        }));
    }

    @Test
    public void testSqlTimestampArray() {
        final java.sql.Timestamp[] expected = TestSetFactory.createSqlTimestampArray();
        service.echo(expected, this.<java.sql.Timestamp[]>createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, new java.sql.Timestamp(0L)));
        }));
    }

    @Test
    public void testStringArray() {
        final String[] expected = TestSetFactory.createStringArray();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, ""));
        }));
    }

    @Test
    public void testStringArrayArray() {
        final String[][] expected = new String[][]{new String[]{"hello"}, new String[]{"bye"}};
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();

            // ensure result preserves meta-data for array store type checking
            assertTrue(TestSetValidator.checkObjectArrayElementAssignment(result, 0, new String[4]));
        }));
    }

    @Test
    public void testTreeMap() {
        for (boolean option : new boolean[]{true, false}) {
            final TreeMap<String, MarkerTypeTreeMap> expected = TestSetFactory.createTreeMap(option);
            service.echo(expected, option, createCallback(result -> {
                assertThat(result).isNotNull();
                assertThat(result).isEqualTo(expected);
            }));
        }
    }

    @Test
    public void testTreeSet() {
        for (boolean option : new boolean[]{true, false}) {
            final TreeSet<MarkerTypeTreeSet> expected = TestSetFactory.createTreeSet(option);
            service.echo(expected, option, createCallback(result -> {
                assertThat(result).isNotNull();
                assertThat(result).isEqualTo(expected);
            }));
        }
    }

    @Test
    public void testVector() {
        final Vector<MarkerTypeVector> expected = TestSetFactory.createVector();
        service.echo(expected, createCallback(result -> {
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);
        }));
    }
}
