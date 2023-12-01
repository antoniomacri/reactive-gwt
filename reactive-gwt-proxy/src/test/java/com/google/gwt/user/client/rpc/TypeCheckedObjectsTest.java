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

import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedSuperClass;
import com.google.gwt.user.server.rpc.TypeCheckedObjectsTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for RPC serialization of type checked objects.
 * <p>
 * Type checked objects are those that are verified as being of the correct type
 * before they are deserialized, thus catching certain attacks that occur
 * through deserialization.
 * <p>
 * Test Cases: - Type checked generic class with a server-side custom serializer
 * that is NOT derived from ServerCustomFieldSerializer but which does define
 * instantiateChecked and deserializeChecked, to verify that such methods are
 * found and called. - Generic class that has no custom field serializer but
 * which does include fields that do have type checked serializers, to verify
 * that such serializers are still used. - Generic class that has no custom
 * field serializer but which does extend a class with type checked serializers,
 * to verify that such serializers are still used.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class TypeCheckedObjectsTest extends RpcAsyncTestBase<TypeCheckedObjectsTestService, TypeCheckedObjectsTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(TypeCheckedObjectsTestService.class, TypeCheckedObjectsTestServiceImpl.class, "typecheckedobjects");
    }


    public TypeCheckedObjectsTest() {
        super(TypeCheckedObjectsTestService.class, "typecheckedobjects");
    }


    @Test
    public void testInvalidCheckedFieldSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createInvalidCheckedFieldClass(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                // Expected in this case
                assertThat(caught).isInstanceOf(SerializationException.class);
            }

            @Override
            public void onSuccess(TypeCheckedFieldClass<Integer, String> result) {
                fail("testInvalidCheckedFieldSerializer is expected to throw an assertion");
            }
        }));
    }

    @Test
    public void testInvalidCheckedSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createInvalidCheckedGenericClass(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                // Expected in this case
                assertThat(caught).isInstanceOf(SerializationException.class);
            }

            @Override
            public void onSuccess(TypeCheckedGenericClass<Integer, String> result) {
                fail("testInvalidCheckedSerializer is expected to throw an assertion");
            }
        }));
    }

    @Test
    public void testInvalidCheckedSuperSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createInvalidCheckedSuperClass(), waitedCallback(new AsyncCallback<TypeCheckedSuperClass<Integer, String>>() {
            @Override
            public void onFailure(Throwable caught) {
                // Expected in this case
                assertThat(caught).isInstanceOf(SerializationException.class);
            }

            @Override
            public void onSuccess(TypeCheckedSuperClass<Integer, String> result) {
                fail("testInvalidCheckedSerializer is expected to throw an assertion");
            }
        }));
    }

    @Test
    public void testInvalidUncheckedSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createInvalidUncheckedGenericClass(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                // Expected in this case
                assertThat(caught).isInstanceOf(SerializationException.class);
            }

            @Override
            public void onSuccess(TypeUncheckedGenericClass<Integer, String> result) {
                fail("testInvalidUncheckedSerializer is expected to throw an assertion");
            }
        }));
    }

    @Test
    public void testTypeCheckedFieldSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedFieldClass(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail("Could not serialize/deserialize TypeCheckedFieldClass", caught);
            }

            @Override
            public void onSuccess(TypeCheckedFieldClass<Integer, String> result) {
                assertNotNull(result);
                assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            }
        }));
    }

    @Test
    public void testTypeCheckedNestedLists() {
        service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedNestedLists(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail("Could not serialize/deserialize TypeCheckedNestedLists", caught);
            }

            @Override
            public void onSuccess(TypeCheckedNestedLists result) {
                assertNotNull(result);
                assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            }
        }));
    }

    @Test
    public void testTypeCheckedSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedGenericClass(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail("Could not serialize/deserialize TypeCheckedGenericClass", caught);
            }

            @Override
            public void onSuccess(TypeCheckedGenericClass<Integer, String> result) {
                assertNotNull(result);
                assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            }
        }));
    }

    @Test
    public void testTypeCheckedSuperSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedSuperClass(), waitedCallback(new AsyncCallback<TypeCheckedSuperClass<Integer, String>>() {
            @Override
            public void onFailure(Throwable caught) {
                fail("Could not serialize/deserialize TypeCheckedGenericClass", caught);
            }

            @Override
            public void onSuccess(TypeCheckedSuperClass<Integer, String> result) {
                assertNotNull(result);
                assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            }
        }));
    }

    @Test
    public void testTypeUncheckedSerializer() {
        service.echo(TypeCheckedObjectsTestSetFactory.createTypeUncheckedGenericClass(), waitedCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail("Could not serialize/deserialize TypeUncheckedGenericClass", caught);
            }

            @Override
            public void onSuccess(TypeUncheckedGenericClass<Integer, String> result) {
                assertNotNull(result);
                assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            }
        }));
    }
}
