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

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;
import com.google.gwt.user.server.rpc.CustomFieldSerializerTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * Tests the following scenarios.
 * <ul>
 * <li>Manually serializable types use their custom field serializer</li>
 * <li>Subtypes of manually serializable types that are not auto-serializable
 * fail to be serialized</li>
 * <li>Automatically serializable subtypes of manually serialized types can be
 * serialized</li>
 * </ul>
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class CustomFieldSerializerTest extends RpcAsyncTestBase<CustomFieldSerializerTestService, CustomFieldSerializerTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(CustomFieldSerializerTestService.class, CustomFieldSerializerTestServiceImpl.class, "customfieldserializers");
    }


    public CustomFieldSerializerTest() {
        super(CustomFieldSerializerTestService.class, "customfieldserializers");
    }


    /**
     * Test that custom field serializers do not make their subclasses
     * serializable.
     */
    @Test
    public void testCustomFieldSerializabilityInheritance() {
        service.echo(CustomFieldSerializerTestSetFactory.createUnserializableSubclass(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertThat(caught).as("Should be a SerializationException")
                        .isInstanceOf(SerializationException.class);
            }

            @Override
            public void onSuccess(Object result) {
                fail("Class UnserializableSubclass should not be serializable");
            }
        }));
    }

    /**
     * Tests that the custom field serializers are actually called when the
     * custom field serializer does not derive from
     * {@link CustomFieldSerializer}
     */
    @Test
    public void testCustomFieldSerialization() {
        service.echo(CustomFieldSerializerTestSetFactory.createUnserializableClass(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertThat(caught).as("Class UnserializableClass should be serializable because it has a custom field serializer")
                        .doesNotThrowAnyException();
            }

            @Override
            public void onSuccess(Object result) {
                assertThat(result).isNotNull();
                assertThat(CustomFieldSerializerTestSetValidator.isValid((ManuallySerializedClass) result)).isTrue();
            }
        }));
    }

    /**
     * Test that custom serializers that call readObject() inside instantiate
     * (as is required for most immutable classes) work.
     * <p>
     * This also checks that custom <code>instantiate</code> works when the
     * custom serializer does not implement {@link CustomFieldSerializer}.
     */
    @Test
    public void testSerializableImmutables() {
        service.echo(CustomFieldSerializerTestSetFactory.createSerializableImmutablesArray(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertThat(caught).as("Could not serialize/deserialize immutable classes")
                        .doesNotThrowAnyException();
            }

            @Override
            public void onSuccess(Object result) {
                assertThat(result).isNotNull();
                assertThat(CustomFieldSerializerTestSetValidator.isValid((ManuallySerializedImmutableClass[]) result)).isTrue();
            }
        }));
    }

    /**
     * Test that serializable subclasses of classes that have custom field
     * serializers serialize and deserialize correctly.
     */
    @Test
    public void testSerializableSubclasses() {
        service.echo(CustomFieldSerializerTestSetFactory.createSerializableSubclass(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertThat(caught).as("Class SerializableSubclass should be serializable automatically")
                        .doesNotThrowAnyException();
            }

            @Override
            public void onSuccess(Object result) {
                assertThat(result).isNotNull();
                assertThat(CustomFieldSerializerTestSetValidator.isValid((SerializableSubclass) result)).isTrue();
            }
        }));
    }
}
