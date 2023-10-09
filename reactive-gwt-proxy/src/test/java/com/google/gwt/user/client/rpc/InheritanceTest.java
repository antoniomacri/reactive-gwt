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

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.Circle;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;
import com.google.gwt.user.server.rpc.InheritanceTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests RPC serialization of classes without custom serializers.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class InheritanceTest extends RpcAsyncTestBase<InheritanceTestServiceSubtype, InheritanceTestServiceSubtypeAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(InheritanceTestServiceSubtype.class, InheritanceTestServiceImpl.class, "inheritance");
    }


    public InheritanceTest() {
        super(InheritanceTestServiceSubtype.class, "inheritance");
    }


    /**
     * Test that anonymous classes are not serializable.
     */
    @Test
    public void testAnonymousClasses() {
        service.echo(() -> {
            // purposely empty
        }, createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Object result) {
                fail("Anonymous inner classes should not be serializable");
            }
        }));
    }

    /**
     * Tests that a shadowed field is properly serialized.
     * <p>
     * Checks for <a href="http://code.google.com/p/google-web-toolkit/issues/detail?id=161">BUG 161</a>
     */
    @Test
    public void testFieldShadowing() {
        service.echo(InheritanceTestSetFactory.createCircle(), createCallback(result -> {
            Circle circle = (Circle) result;
            assertNotNull(circle.getName());
        }));
    }

    /**
     * Tests that transient fields do not prevent serializability.
     */
    @Test
    public void testJavaSerializableClass() {
        service.echo(new InheritanceTestSetFactory.JavaSerializableClass(3),
                createCallback(Assertions::assertNotNull));
    }

    /**
     * Tests that a serialized type can be sent again on the wire.
     */
    @Test
    public void testResendJavaSerializableClass() {
        final InheritanceTestSetFactory.JavaSerializableClass first = new InheritanceTestSetFactory.JavaSerializableClass(3);
        AsyncCallback<Object> resendCallback = new AsyncCallback<>() {
            private boolean resend = true;

            @Override
            public void onFailure(Throwable caught) {
                TestSetValidator.rethrowException(caught);
            }

            @Override
            public void onSuccess(Object result) {
                assertEquals(first, result);
                if (resend) {
                    resend = false;
                    service.echo((InheritanceTestSetFactory.JavaSerializableClass) result, this);
                }
            }
        };
        service.echo(first, createCallback(resendCallback));
    }

    /**
     * Test that non-static inner classes are not serializable.
     */
    @Test
    public void testNonStaticInnerClass() {
        service.echo(InheritanceTestSetFactory.createNonStaticInnerClass(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Object result) {
                fail("Non-static inner classes should not be serializable");
            }
        }));
    }

    @Test
    public void testReturnOfUnserializableClassFromServer() {
        service.getUnserializableClass(createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Object result) {
                fail("Returning an unserializable class from the server should fail");
            }
        }));
    }

    /**
     * Test that a valid serializable class can be serialized.
     */
    @Test
    public void testSerializableClass() {
        service.echo(InheritanceTestSetFactory.createSerializableClass(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableClass) result));
        }));
    }

    /**
     * Test that IsSerializable is inherited, also test static inner classes.
     */
    @Test
    public void testSerializableSubclass() {
        service.echo(InheritanceTestSetFactory.createSerializableSubclass(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableSubclass) result));
        }));
    }

    @Test
    public void testSerializationExceptionPreventsCall() {
        AtomicBoolean serializationExceptionCaught = new AtomicBoolean();

        service.echo(() -> {
            // purposely empty
        }, createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertThat(caught)
                        .as("onFailure: got something other than a SerializationException (" + caught.getClass().getName() + ")")
                        .isInstanceOf(SerializationException.class);
                serializationExceptionCaught.set(true);
            }

            @Override
            public void onSuccess(Object result) {
                fail("onSuccess: call should not have succeeded");
            }
        }));

        assertThat(serializationExceptionCaught).as("serializationExceptionCaught was not true").isTrue();
    }

    /**
     * Tests that transient fields do not prevent serializability.
     */
    @Test
    public void testTransientFieldExclusion() {
        service.echo(InheritanceTestSetFactory.createSerializableClassWithTransientField(), createCallback(result -> {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableClassWithTransientField) result));
        }));
    }
}
