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
 *
 * This is a project specific patch contributed by Jarrod Ribble.
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;
import com.google.gwt.user.server.rpc.impl.TypeNameObfuscator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.*;

/**
 * API for loading a {@link SerializationPolicy}.
 * <p>
 * This is an exact copy of the GWT version, but the policies are switched for
 * serialization and deserialization.
 */
public final class SerializationPolicyLoader {

    /**
     * Keyword for listing the serializable fields of an enchanced class that
     * are visible to client code.
     */
    public static final String CLIENT_FIELDS_KEYWORD = "@ClientFields";

    private static final String FORMAT_ERROR_MESSAGE = "Expected: className, "
                                                       + "[true | false], [true | false], [true | false], [true | false], typeId, signature";

    /**
     * Returns the serialization policy file name from the serialization policy
     * strong name.
     *
     * @param serializationPolicyStrongName the serialization policy strong name
     * @return the serialization policy file name from the serialization policy
     * strong name
     */
    public static String getSerializationPolicyFileName(String serializationPolicyStrongName) {
        return serializationPolicyStrongName + ".gwt.rpc";
    }

    /**
     * Loads a SerializationPolicy from an input stream and optionally record
     * any {@link ClassNotFoundException}s.
     *
     * @param reader                  reader to load the SerializationPolicy from.
     * @param classNotFoundExceptions if not <code>null</code>, all of the
     *                                {@link ClassNotFoundException}s thrown while loading this
     *                                serialization policy will be added to this list
     * @return a {@link SerializationPolicy} loaded from the input stream.
     * @throws IOException    if an error occurs while reading the stream
     * @throws ParseException if the input stream is not properly formatted
     */
    public static StandardSerializationPolicy load(Reader reader, List<ClassNotFoundException> classNotFoundExceptions) throws IOException, ParseException {
        Map<Class<?>, Boolean> whitelistSer = new HashMap<>();
        Map<Class<?>, Boolean> whitelistDeser = new HashMap<>();
        Map<Class<?>, String> typeIds = new HashMap<>();
        Map<Class<?>, Set<String>> clientFields = new HashMap<>();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        BufferedReader br = new BufferedReader(reader);

        String line = br.readLine();
        int lineNum = 1;
        while (line != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                String[] components = line.split(",");

                if (components[0].equals(CLIENT_FIELDS_KEYWORD)) {
                    /*
                     * Lines starting with '@ClientFields' list potentially
                     * serializable fields known to client code for classes that
                     * may be enhanced with additional fields on the server. If
                     * additional server fields are found, they will be
                     * serizalized separately from the normal RPC process and
                     * transmitted to the client as an opaque blob of data
                     * stored in a WeakMapping associated with the object
                     * instance.
                     */
                    String binaryTypeName = components[1].trim();
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(binaryTypeName, false, contextClassLoader);
                        HashSet<String> fieldNames = new HashSet<>(Arrays.asList(components).subList(2, components.length));
                        clientFields.put(clazz, fieldNames);
                    } catch (ClassNotFoundException ex) {
                        // Ignore the error, but add it to the list of errors if
                        // one was provided.
                        if (classNotFoundExceptions != null) {
                            classNotFoundExceptions.add(ex);
                        }
                    }
                } else {
                    if (components.length != 2 && components.length != 7) {
                        throw new ParseException(FORMAT_ERROR_MESSAGE, lineNum);
                    }

                    for (int i = 0; i < components.length; i++) {
                        components[i] = components[i].trim();
                        if (components[i].isEmpty()) {
                            throw new ParseException(FORMAT_ERROR_MESSAGE, lineNum);
                        }
                    }

                    String binaryTypeName = components[0].trim();
                    boolean fieldSer;
                    boolean instantSer;
                    boolean fieldDeser;
                    boolean instantDeser;
                    String typeId;

                    if (components.length == 2) {
                        fieldSer = fieldDeser = true;
                        instantSer = instantDeser = Boolean.parseBoolean(components[1]);
                        typeId = binaryTypeName;
                    } else {
                        int idx = 1;
                        // TODO: Validate the instantiable string better.
                        fieldSer = Boolean.parseBoolean(components[idx++]);
                        instantSer = Boolean.parseBoolean(components[idx++]);
                        fieldDeser = Boolean.parseBoolean(components[idx++]);
                        instantDeser = Boolean.parseBoolean(components[idx++]);
                        typeId = components[idx++];

                        if (!fieldSer && !fieldDeser && !TypeNameObfuscator.SERVICE_INTERFACE_ID.equals(typeId)) {
                            throw new ParseException(
                                    "Type "
                                    + binaryTypeName
                                    + " is neither field serializable, field deserializable "
                                    + "nor the service interface",
                                    lineNum);
                        }
                    }

                    try {
                        Class<?> clazz = Class.forName(binaryTypeName, false, contextClassLoader);
                        if (fieldSer) {
                            whitelistSer.put(clazz, instantSer);
                        }
                        if (fieldDeser) {
                            whitelistDeser.put(clazz, instantDeser);
                        }
                        typeIds.put(clazz, typeId);
                    } catch (ClassNotFoundException ex) {
                        // Ignore the error, but add it to the list of errors if
                        // one was provided.
                        if (classNotFoundExceptions != null) {
                            classNotFoundExceptions.add(ex);
                        }
                    }
                }
            }

            line = br.readLine();
            lineNum++;
        }
        /*
         * HERE'S THE CHANGE FROM THE ORIGINAL Deser and Ser are swapped because
         * we are the client side
         */
        return new StandardSerializationPolicy(whitelistDeser, whitelistSer, typeIds, clientFields);
    }

    private SerializationPolicyLoader() {
    }
}
