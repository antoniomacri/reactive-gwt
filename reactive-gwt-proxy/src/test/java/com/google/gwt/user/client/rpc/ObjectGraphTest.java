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

import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializablePrivateNoArg;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableWithTwoArrays;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.ObjectGraphTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO: document me.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class ObjectGraphTest extends RpcAsyncTestBase<ObjectGraphTestService, ObjectGraphTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(ObjectGraphTestService.class, ObjectGraphTestServiceImpl.class, "objectgraphs");
    }


    public ObjectGraphTest() {
        super(ObjectGraphTestService.class, "objectgraphs");
    }


    @Test
    public void testAcyclicGraph() {
        service.echo_AcyclicGraph(TestSetFactory.createAcyclicGraph(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidAcyclicGraph((SerializableDoublyLinkedNode) result));
        }));
    }

    @Test
    public void testComplexCyclicGraph() {
        service.echo_ComplexCyclicGraph(TestSetFactory.createComplexCyclicGraph(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidComplexCyclicGraph((SerializableDoublyLinkedNode) result));
        }));
    }

    @Test
    public void testComplexCyclicGraphWithCFS() {
        service.echo_ComplexCyclicGraphWithCFS(TestSetFactory.createComplexCyclicGraphWithCFS(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidComplexCyclicGraphWithCFS(result));
        }));
    }

    @Test
    public void testComplexCyclicGraph2() {
        final SerializableDoublyLinkedNode node = TestSetFactory.createComplexCyclicGraph();
        service.echo_ComplexCyclicGraph(node, node, createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidComplexCyclicGraph((SerializableDoublyLinkedNode) result));
        }));
    }

    @Test
    public void testDoublyReferencedArray() {
        final SerializableWithTwoArrays node = TestSetFactory.createDoublyReferencedArray();
        service.echo_SerializableWithTwoArrays(node, createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid((SerializableWithTwoArrays) result));
        }));
    }

    @Test
    public void testElision() throws SerializationException {
        SerializationStreamWriter writer = ((SerializationStreamFactory) service).createStreamWriter();
        AbstractSerializationStream stream = (AbstractSerializationStream) writer;
        assertThat(stream.hasFlags(AbstractSerializationStream.FLAG_ELIDE_TYPE_NAMES))
                .as("Missing flag")
                .isEqualTo(expectedObfuscationState());

        SerializableDoublyLinkedNode node = new SerializableDoublyLinkedNode();
        writer.writeObject(node);
        String s = writer.toString();

        // Don't use class.getName() due to conflict with removal of type names
        assertThat(!s.contains("SerializableDoublyLinkedNode"))
                .as("Checking for SerializableDoublyLinkedNode")
                .isEqualTo(expectedObfuscationState());
    }

    @Test
    public void testPrivateNoArg() {
        final SerializablePrivateNoArg node = TestSetFactory.createPrivateNoArg();
        service.echo_PrivateNoArg(node, createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid((SerializablePrivateNoArg) result));
        }));
    }

    @Test
    public void testTrivialCyclicGraph() {
        service.echo_TrivialCyclicGraph(TestSetFactory.createTrivialCyclicGraph(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidTrivialCyclicGraph((SerializableDoublyLinkedNode) result));
        }));
    }


    protected boolean expectedObfuscationState() {
        return false;
    }
}
