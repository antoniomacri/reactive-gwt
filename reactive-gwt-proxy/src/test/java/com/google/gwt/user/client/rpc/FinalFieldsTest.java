/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.junit.client.WithProperties;
import com.google.gwt.junit.client.WithProperties.Property;
import com.google.gwt.user.client.rpc.FinalFieldsTestService.FinalFieldsNode;
import com.google.gwt.user.server.rpc.FinalFieldsTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test serializing final fields when rpc.XserializeFinalFields=true.
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class FinalFieldsTest extends RpcAsyncTestBase<FinalFieldsTestService, FinalFieldsTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(FinalFieldsTestService.class, FinalFieldsTestServiceImpl.class, "finalfields");
    }


    public FinalFieldsTest() {
        super(FinalFieldsTestService.class, "finalfields");
    }


    @Test
    @WithProperties(@Property(name = "rpc.XserializeFinalFields", value = "true"))
    public void testFinalFields() {
        FinalFieldsNode node = new FinalFieldsNode(4, "C", 9);

        service.transferObject(node, createCallback(result -> {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidFinalFieldsObjectFromServer(result));
        }));
    }
}
