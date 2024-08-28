/*
 * Copyright www.gdevelop.com.
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
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RpcToken;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;


/**
 * @see com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter
 * @see com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader
 * @see com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter
 * @see com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader
 */
public class SyncClientSerializationStreamWriter extends AbstractSerializationStreamWriter {
    private static final Logger log = LoggerFactory.getLogger(SyncClientSerializationStreamWriter.class);

    /**
     * Map of {@link Class} objects to {@link ValueWriter}s.
     */
    private static final Map<Class<?>, ValueWriter> CLASS_TO_VALUE_WRITER = new IdentityHashMap<>(Map.of(
            boolean.class, SyncClientSerializationStreamWriter.ValueWriter.BOOLEAN,
            byte.class, SyncClientSerializationStreamWriter.ValueWriter.BYTE,
            char.class, SyncClientSerializationStreamWriter.ValueWriter.CHAR,
            double.class, SyncClientSerializationStreamWriter.ValueWriter.DOUBLE,
            float.class, SyncClientSerializationStreamWriter.ValueWriter.FLOAT,
            int.class, SyncClientSerializationStreamWriter.ValueWriter.INT,
            long.class, SyncClientSerializationStreamWriter.ValueWriter.LONG,
            Object.class, SyncClientSerializationStreamWriter.ValueWriter.OBJECT,
            short.class, SyncClientSerializationStreamWriter.ValueWriter.SHORT,
            String.class, SyncClientSerializationStreamWriter.ValueWriter.STRING
    ));

    /**
     * Map of {@link Class} vector objects to {@link VectorWriter}s.
     */
    private static final Map<Class<?>, VectorWriter> CLASS_TO_VECTOR_WRITER = new IdentityHashMap<>(Map.of(
            boolean[].class, SyncClientSerializationStreamWriter.VectorWriter.BOOLEAN_VECTOR,
            byte[].class, SyncClientSerializationStreamWriter.VectorWriter.BYTE_VECTOR,
            char[].class, SyncClientSerializationStreamWriter.VectorWriter.CHAR_VECTOR,
            double[].class, SyncClientSerializationStreamWriter.VectorWriter.DOUBLE_VECTOR,
            float[].class, SyncClientSerializationStreamWriter.VectorWriter.FLOAT_VECTOR,
            int[].class, SyncClientSerializationStreamWriter.VectorWriter.INT_VECTOR,
            long[].class, SyncClientSerializationStreamWriter.VectorWriter.LONG_VECTOR,
            Object[].class, SyncClientSerializationStreamWriter.VectorWriter.OBJECT_VECTOR,
            short[].class, SyncClientSerializationStreamWriter.VectorWriter.SHORT_VECTOR,
            String[].class, SyncClientSerializationStreamWriter.VectorWriter.STRING_VECTOR
    ));

    private final String moduleBaseURL;
    private final String serializationPolicyStrongName;
    private final SerializationPolicy serializationPolicy;
    private final RpcToken rpcToken;
    private StringBuffer encodeBuffer;


    public SyncClientSerializationStreamWriter(String moduleBaseURL, String serializationPolicyStrongName, SerializationPolicy serializationPolicy, RpcToken rpcToken, int version) {
        this.moduleBaseURL = moduleBaseURL;
        this.serializationPolicyStrongName = serializationPolicyStrongName;
        this.serializationPolicy = serializationPolicy;
        this.rpcToken = rpcToken;
        if (rpcToken != null) {
            addFlags(FLAG_RPC_TOKEN_INCLUDED);
        }
        super.setVersion(version);
    }

    @Override
    protected void append(String token) {
        append(this.encodeBuffer, token);
    }

    @Override
    protected String getObjectTypeSignature(Object o) {
        Class<?> clazz = o.getClass();

        if (o instanceof Enum<?> e) {
            clazz = e.getDeclaringClass();
        }

        String typeName = null;
        // By using the typeName from SerializabilityUtil, a request to the server may fail
        // ("Invalid type signature for java.util.ArrayList") since the local type signature is
        // different from the remote one, e.g. a local typeName "java.util.ArrayList/4159755760"
        // vs a remote "java.util.ArrayList/3821976829".
        // However, many collections have custom field serializers (for instance look at
        // com.google.gwt.user.client.rpc.core.java.util.ArrayList_CustomFieldSerializer),
        // which basically all work in the same way: serialize the size and then every
        // single element.
        // In those cases it should be safe to ignore the local typeSignature and use the
        // remote as written in the serialization policy.
        if (getVersion() == 5 && Collection.class.isAssignableFrom(clazz)) {
            if (serializationPolicy instanceof StandardSerializationPolicy std) {
                try {
                    typeName = std.getTypeIdForClass(clazz);
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
                log.warn("Using typeName={} from serializationPolicy={} for type={}",
                        typeName, serializationPolicyStrongName, clazz);
            }
        }
        if (typeName == null) {
            typeName = clazz.getName();
            String serializationSignature = SerializabilityUtil.getSerializationSignature(clazz, this.serializationPolicy);
            if (serializationSignature != null) {
                typeName += "/" + serializationSignature;
            }
        }

        return typeName;
    }

    @Override
    public void prepareToWrite() {
        super.prepareToWrite();
        this.encodeBuffer = new StringBuffer();

        // Write serialization policy info
        writeString(this.moduleBaseURL);
        writeString(this.serializationPolicyStrongName);
        if (hasFlags(FLAG_RPC_TOKEN_INCLUDED)) {
            try {
                serializeValue(this.rpcToken, this.rpcToken.getClass());
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void serialize(Object instance, String typeSignature) throws SerializationException {
        assert instance != null;

        Class<?> clazz = getClassForSerialization(instance);
        log.trace("Serialize instance={} signature={} as class={}", instance, typeSignature, clazz.getName());

        this.serializationPolicy.validateSerialize(clazz);

        serializeImpl(instance, clazz);
    }

    /**
     * Serialize an instance that is an array. Will default to serializing the
     * instance as an Object vector if the instance is not a vector of
     * primitives, Strings or Object.
     */
    private void serializeArray(Class<?> instanceClass, Object instance) throws SerializationException {
        assert instanceClass.isArray();

        VectorWriter instanceWriter = CLASS_TO_VECTOR_WRITER.get(instanceClass);
        Objects.requireNonNullElse(instanceWriter, VectorWriter.OBJECT_VECTOR).write(this, instance);
    }

    private void serializeClass(Object instance, Class<?> instanceClass) throws SerializationException {
        assert instance != null;

        Field[] serializableFields = SerializabilityUtil.applyFieldSerializationPolicy(instanceClass, this.serializationPolicy);

        // Serialize a null String as the server-only blob for enhanced classes. We don't actually care
        // about the value: see also the {@link SyncClientSerializationStreamReader#deserializeClass}
        // and Issue 36 of the original syncproxy project.
        if (this.serializationPolicy.getClientFieldNamesForEnhancedClass(instanceClass) != null) {
            serializeValue(null, String.class);
        }

        for (Field declField : serializableFields) {
            assert declField != null;

            boolean isAccessible = declField.isAccessible();
            boolean needsAccessOverride = !isAccessible && !Modifier.isPublic(declField.getModifiers());
            if (needsAccessOverride) {
                // Override the access restrictions
                declField.setAccessible(true);
            }

            Object value;
            try {
                value = declField.get(instance);
                serializeValue(value, declField.getType());
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new SerializationException(e);
            }
        }

        Class<?> superClass = instanceClass.getSuperclass();
        if (this.serializationPolicy.shouldSerializeFields(superClass)) {
            serializeImpl(instance, superClass);
        }
    }

    /**
     * @see com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter#serializeImpl
     */
    private void serializeImpl(Object instance, Class<?> instanceClass) throws SerializationException {
        assert instance != null;

        Class<?> customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instanceClass);
        if (customSerializer != null) {
            // Use custom field serializer
            serializeWithCustomSerializer(customSerializer, instance, instanceClass);
        } else if (instanceClass.isArray()) {
            serializeArray(instanceClass, instance);
        } else if (instanceClass.isEnum()) {
            writeInt(((Enum<?>) instance).ordinal());
        } else if (Exception.class.isAssignableFrom(instanceClass)) {
            // See SerializabilityUtil.fieldQualifiesForSerialization()
            writeString(((Exception) instance).getMessage());
        } else {
            // Regular class instance
            serializeClass(instance, instanceClass);
        }
    }

    public void serializeValue(Object value, Class<?> type) throws SerializationException {
        ValueWriter valueWriter = CLASS_TO_VALUE_WRITER.get(type);
        // Arrays of primitive or reference types need to go through writeObject.
        Objects.requireNonNullElse(valueWriter, ValueWriter.OBJECT).write(this, value);
    }

    private void serializeWithCustomSerializer(Class<?> customSerializer, Object instance, Class<?> instanceClass) throws SerializationException {
        log.trace("Serializing type={} with customSerializer={}", instanceClass.getName(), customSerializer.getName());
        try {
            assert !instanceClass.isArray();

            for (Method method : customSerializer.getMethods()) {
                if ("serialize".equals(method.getName())) {
                    method.invoke(null, this, instance);
                    return;
                }
            }
            throw new NoSuchMethodException("serialize");
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        writeHeader(buffer);
        writeStringTable(buffer);
        writePayload(buffer);
        return buffer.toString();
    }

    private void writeHeader(StringBuffer buffer) {
        append(buffer, String.valueOf(getVersion()));
        append(buffer, String.valueOf(getFlags()));
    }

    /**
     * @see ClientSerializationStreamWriter#writeLong
     */
    @Override
    public void writeLong(long fieldValue) {
        if (getVersion() == SERIALIZATION_STREAM_MIN_VERSION) {
            double[] parts;
            parts = makeLongComponents((int) (fieldValue >> 32), (int) fieldValue);
            assert parts.length == 2;
            writeDouble(parts[0]);
            writeDouble(parts[1]);
        } else {
            append(Utils.toBase64(fieldValue));
        }
    }

    private void writePayload(StringBuffer buffer) {
        buffer.append(this.encodeBuffer.toString());
    }

    private void writeStringTable(StringBuffer buffer) {
        List<String> stringTable = getStringTable();
        append(buffer, String.valueOf(stringTable.size()));
        for (String s : stringTable) {
            append(buffer, quoteString(s));
        }
    }

    private static void append(StringBuffer sb, String token) {
        assert token != null;
        sb.append(token);
        sb.append(RPC_SEPARATOR_CHAR);
    }

    /**
     * Returns the {@link Class} instance to use for serialization. Enumerations
     * are serialized as their declaring class while all others are serialized
     * using their true class instance.
     */
    private static Class<?> getClassForSerialization(Object instance) {
        assert instance != null;
        // Attempt to compensate for EnumMap
        /*
         * if (instance instanceof Class<?>) { return (Class<?>) instance; }
         * else
         */
        if (instance instanceof Enum<?> e) {
            return e.getDeclaringClass();
        } else {
            return instance.getClass();
        }
    }

    private static String quoteString(String str) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case 0 -> buffer.append("\\0");
                case '|' -> buffer.append("\\!");
                case '\\' -> buffer.append("\\\\");
                default -> {
                    // buffer.append(ch);
                    if (ch >= ' ' && ch <= 127) {
                        buffer.append(ch);
                    } else {
                        String hex = Integer.toHexString(ch);
                        buffer.append("\\u0000", 0, 6 - hex.length()).append(hex);
                    }
                }
            }
        }

        return buffer.toString();
    }


    @FunctionalInterface
    interface Writer {
        void write(SyncClientSerializationStreamWriter stream, Object instance) throws SerializationException;
    }

    /**
     * Enumeration used to provided typed instance writers.
     */
    private enum ValueWriter {
        BOOLEAN((stream, instance) -> stream.writeBoolean((Boolean) instance)),
        BYTE((stream, instance) -> stream.writeByte((Byte) instance)),
        CHAR((stream, instance) -> stream.writeChar((Character) instance)),
        DOUBLE((stream, instance) -> stream.writeDouble((Double) instance)),
        FLOAT((stream, instance) -> stream.writeFloat((Float) instance)),
        INT((stream, instance) -> stream.writeInt((Integer) instance)),
        LONG((stream, instance) -> stream.writeLong((Long) instance)),
        OBJECT(AbstractSerializationStreamWriter::writeObject),
        SHORT((stream, instance) -> stream.writeShort((Short) instance)),
        STRING((stream, instance) -> stream.writeString((String) instance));


        private final Writer writer;

        ValueWriter(Writer writer) {
            this.writer = writer;
        }

        void write(SyncClientSerializationStreamWriter stream, Object instance) throws SerializationException {
            writer.write(stream, instance);
        }
    }

    /**
     * Enumeration used to provided typed vector writers.
     */
    private enum VectorWriter {
        BOOLEAN_VECTOR((stream, instance) -> {
            boolean[] vector = (boolean[]) instance;
            stream.writeInt(vector.length);
            for (boolean element : vector) {
                stream.writeBoolean(element);
            }
        }),
        BYTE_VECTOR((stream, instance) -> {
            byte[] vector = (byte[]) instance;
            stream.writeInt(vector.length);
            for (byte element : vector) {
                stream.writeByte(element);
            }
        }),
        CHAR_VECTOR((stream, instance) -> {
            char[] vector = (char[]) instance;
            stream.writeInt(vector.length);
            for (char element : vector) {
                stream.writeChar(element);
            }
        }),
        DOUBLE_VECTOR((stream, instance) -> {
            double[] vector = (double[]) instance;
            stream.writeInt(vector.length);
            for (double element : vector) {
                stream.writeDouble(element);
            }
        }),
        FLOAT_VECTOR((stream, instance) -> {
            float[] vector = (float[]) instance;
            stream.writeInt(vector.length);
            for (float element : vector) {
                stream.writeFloat(element);
            }
        }),
        INT_VECTOR((stream, instance) -> {
            int[] vector = (int[]) instance;
            stream.writeInt(vector.length);
            for (int element : vector) {
                stream.writeInt(element);
            }
        }),
        LONG_VECTOR((stream, instance) -> {
            long[] vector = (long[]) instance;
            stream.writeInt(vector.length);
            for (long element : vector) {
                stream.writeLong(element);
            }
        }),
        OBJECT_VECTOR((stream, instance) -> {
            Object[] vector = (Object[]) instance;
            stream.writeInt(vector.length);
            for (Object element : vector) {
                stream.writeObject(element);
            }
        }),
        SHORT_VECTOR((stream, instance) -> {
            short[] vector = (short[]) instance;
            stream.writeInt(vector.length);
            for (short element : vector) {
                stream.writeShort(element);
            }
        }),
        STRING_VECTOR((stream, instance) -> {
            String[] vector = (String[]) instance;
            stream.writeInt(vector.length);
            for (String element : vector) {
                stream.writeString(element);
            }
        });


        private final Writer writer;

        VectorWriter(Writer writer) {
            this.writer = writer;
        }

        void write(SyncClientSerializationStreamWriter stream, Object instance) throws SerializationException {
            writer.write(stream, instance);
        }
    }
}
