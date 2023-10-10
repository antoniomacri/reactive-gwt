/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.server.rpc.MixedSerializableEchoServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests run-time serialization errors for GWT RPC.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class RunTimeSerializationErrorsTest extends RpcAsyncTestBase<MixedSerializableEchoService, MixedSerializableEchoServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(MixedSerializableEchoService.class, MixedSerializableEchoServiceImpl.class, "mixed");
    }


    public RunTimeSerializationErrorsTest() {
        super(MixedSerializableEchoService.class, "mixed");
    }


    @Test
    public void testBadSerialization1() {
        service.echoVoid(new MixedSerializable.NonSerializableSub(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(MixedSerializable result) {
                fail("RPC request should have failed");
            }
        }));
    }

    @Test
    @Tag("NotSupported")
    public void testBadSerialization2() {
        final boolean[] callbackFired = new boolean[]{false};

        Request req = service.echoRequest(new MixedSerializable.NonSerializableSub(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                callbackFired[0] = true;
            }

            @Override
            public void onSuccess(MixedSerializable result) {
                fail("RPC request should have failed");
            }
        }));

        assertTrue(callbackFired[0]); // should have happened synchronously
        assertFalse(req.isPending());
        req.cancel();
    }

    @Test
    @Tag("NotSupported")
    public void testBadSerialization3() throws RequestException {
        final boolean[] callbackFired = new boolean[]{false};

        RequestBuilder rb = service.echoRequestBuilder(new MixedSerializable.NonSerializableSub(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                assertFalse(callbackFired[0], "callback fired twice");
                callbackFired[0] = true;
            }

            @Override
            public void onSuccess(MixedSerializable result) {
                fail("RPC request should have failed");
            }
        }));

        assertFalse(callbackFired[0]); // should fail when send() is called
        rb.send();
        assertTrue(callbackFired[0]); // should have happened now
    }

    @Test
    public void testGoodSerialization1() {
        service.echoVoid(new MixedSerializable.SerializableSub(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail(caught.toString());
            }

            @Override
            public void onSuccess(MixedSerializable result) {
            }
        }));
    }

    @Test
    public void testGoodSerialization2() {
        service.echoRequest(new MixedSerializable.SerializableSub(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail(caught.toString());
            }

            @Override
            public void onSuccess(MixedSerializable result) {
            }
        }));
    }

    @Test
    public void testGoodSerialization3() {
        service.echoVoid(new MixedSerializable.SerializableSub(), createCallback(new AsyncCallback<>() {
            @Override
            public void onFailure(Throwable caught) {
                fail(caught.toString());
            }

            @Override
            public void onSuccess(MixedSerializable result) {
            }
        }));
    }
}