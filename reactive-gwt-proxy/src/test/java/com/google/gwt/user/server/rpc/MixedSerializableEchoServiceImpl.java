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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.MixedSerializable;
import com.google.gwt.user.client.rpc.MixedSerializableEchoService;

/**
 * Servlet used by the
 * {@link com.google.gwt.user.client.rpc.RunTimeSerializationErrorsTest}.
 * <p>
 * Taken from GWT sources.
 */
public class MixedSerializableEchoServiceImpl extends RemoteServiceServlet
        implements MixedSerializableEchoService {
    @Override
    public MixedSerializable echoVoid(MixedSerializable mixed) {
        return mixed;
    }

    @Override
    public MixedSerializable echoRequest(MixedSerializable mixed) {
        return mixed;
    }

    @Override
    public MixedSerializable echoRequestBuilder(MixedSerializable mixed) {
        return mixed;
    }
}
