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

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

/**
 * Service interface used by the
 * {@link CustomFieldSerializerTest CustomFieldSerializerTest}
 * unit test.
 */
public interface CustomFieldSerializerTestService extends RemoteService {
    ManuallySerializedClass echo(ManuallySerializedClass manuallySerializableClass);

    ManuallySerializedImmutableClass[] echo(ManuallySerializedImmutableClass[] manuallySerializableImmutables);

    SerializableSubclass echo(SerializableSubclass serializableClass);
}
