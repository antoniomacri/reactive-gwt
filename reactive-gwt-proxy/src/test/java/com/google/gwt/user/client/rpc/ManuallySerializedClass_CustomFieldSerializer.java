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
 * This class is defined outside of the CustomFieldSerializerTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes. Once
 * we fix this bug we can move this class into the test set factory.
 */
public class ManuallySerializedClass_CustomFieldSerializer {
    public static void deserialize(SerializationStreamReader streamReader,
                                   ManuallySerializedClass instance) throws SerializationException {
        instance.setA(streamReader.readInt());
        instance.setB(streamReader.readInt());
        instance.setC(streamReader.readInt());
        instance.setString(streamReader.readString());
        instance.setStackTraceElement((StackTraceElement) streamReader.readObject());
    }

    public static void serialize(SerializationStreamWriter streamWriter,
                                 ManuallySerializedClass instance) throws SerializationException {
        streamWriter.writeInt(4);
        streamWriter.writeInt(5);
        streamWriter.writeInt(6);
        streamWriter.writeString("bye");
        streamWriter.writeObject(new StackTraceElement("HighClass",
                "highClassMethod", "HighClass.java", 5789));
    }
}