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

/**
 * A RemoteService for testing the details of the "over-HTTP" part of
 * RPC-over-HTTP.
 */
public interface RemoteServiceServletTestService extends RemoteService {
    void test();

    void testExpectCustomHeader();

    void testExpectPermutationStrongName(String expectedStrongName);

    void throwDeclaredRuntimeException() throws NullPointerException;

    void throwUnknownRuntimeException();
}
