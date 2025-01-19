/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.reader.email.msg;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A unified data model for MSG file processing.
 * This class implements a flexible and extensible data model for handling MSG file contents.
 * It uses the Builder pattern for constructing field definitions and provides type-safe data access.
 *
 * Key features:
 * - Flexible data reading through DataReader interface
 * - Built-in readers for common data types
 * - Type-safe data access with generics
 * - Builder pattern for field definitions
 * - Immutable field definitions
 *
 * Usage example:
 * <pre>
 * DataModel model = new DataModel();
 * FieldDefinition[] fields = new FieldDefinitionBuilder()
 *     .addField("id", DataReaders.INT, true)
 *     .addField("name", DataReaders.STRING, true)
 *     .addField("timestamp", DataReaders.TIME, false)
 *     .build();
 * model.read(byteBuffer, fields);
 * </pre>
 *
 * @author xiadong
 * @since 2024
 */
public class DataModel {
    
    /**
     * Interface for reading data from a ByteBuffer.
     * Implementations should handle specific data types and their reading logic.
     */
    @FunctionalInterface
    public interface DataReader {
        /**
         * Read data from the ByteBuffer
         *
         * @param bb The ByteBuffer to read from
         * @return The read data
         * @throws BufferUnderflowException if there are not enough bytes remaining
         */
        Object read(ByteBuffer bb);
    }
    
    /**
     * Built-in data readers for common types.
     * Provides a set of predefined readers for handling standard data types.
     */
    public static class DataReaders {
        private DataReaders() {
            // Prevent instantiation
        }
        
        /**
         * Reader for 16-bit integers
         */
        public static final DataReader SHORT = ByteBuffer::getShort;
        
        /**
         * Reader for 32-bit integers
         */
        public static final DataReader INT = ByteBuffer::getInt;
        
        /**
         * Reader for 64-bit integers
         */
        public static final DataReader LONG = ByteBuffer::getLong;
        
        /**
         * Reader for bytes
         */
        public static final DataReader BYTE = ByteBuffer::get;
        
        /**
         * Reader for null-terminated strings
         */
        public static final DataReader STRING = bb -> {
            StringBuilder sb = new StringBuilder();
            byte b;
            while ((b = bb.get()) != 0) {
                sb.append((char)b);
            }
            return sb.toString();
        };
        
        /**
         * Reader for GUIDs (16 bytes)
         */
        public static final DataReader GUID = bb -> {
            byte[] bytes = new byte[16];
            bb.get(bytes);
            return new GUID(bytes);
        };
        
        /**
         * Reader for file time values (converts to Date)
         */
        public static final DataReader TIME = bb -> {
            long fileTime = bb.getLong();
            return new Date(fileTime);
        };
    }
    
    /**
     * Builder class for creating field definitions.
     * Provides a fluent API for defining fields.
     */
    public static class FieldDefinitionBuilder {
        private final List<FieldDefinition> fields = new ArrayList<>();
        
        /**
         * Add a new field definition
         *
         * @param name The field name
         * @param reader The data reader
         * @param required Whether the field is required
         * @return this builder
         */
        public FieldDefinitionBuilder addField(String name, DataReader reader, boolean required) {
            fields.add(new FieldDefinition(name, reader, required));
            return this;
        }
        
        /**
         * Build the field definitions array
         *
         * @return Array of field definitions
         */
        public FieldDefinition[] build() {
            return fields.toArray(new FieldDefinition[0]);
        }
    }
    
    /**
     * Immutable definition of a data field
     */
    public static class FieldDefinition {
        private final String name;
        private final DataReader reader;
        private final boolean required;
        
        private FieldDefinition(String name, DataReader reader, boolean required) {
            this.name = Objects.requireNonNull(name, "Field name cannot be null");
            this.reader = Objects.requireNonNull(reader, "Data reader cannot be null");
            this.required = required;
        }
        
        public String getName() { return name; }
        public DataReader getReader() { return reader; }
        public boolean isRequired() { return required; }
    }
    
    private final Map<String, Object> data;
    
    /**
     * Creates a new data model with an empty data map
     */
    public DataModel() {
        this.data = new HashMap<>();
    }
    
    /**
     * Reads data from a ByteBuffer according to the field definitions.
     * Each field is read using its associated DataReader.
     *
     * @param bb The ByteBuffer to read from
     * @param fields The field definitions
     * @throws IllegalStateException if a required field is missing
     * @throws BufferUnderflowException if there are not enough bytes remaining
     */
    public void read(ByteBuffer bb, FieldDefinition[] fields) {
        Objects.requireNonNull(bb, "ByteBuffer cannot be null");
        Objects.requireNonNull(fields, "Field definitions cannot be null");
        
        for (FieldDefinition field : fields) {
            try {
                Object value = field.getReader().read(bb);
                if (field.isRequired() && value == null) {
                    throw new IllegalStateException("Required field " + field.getName() + " is missing");
                }
                data.put(field.getName(), value);
            } catch (BufferUnderflowException e) {
                throw new IllegalStateException("Not enough data for field " + field.getName(), e);
            }
        }
    }
    
    /**
     * Gets a value from the data model
     *
     * @param name The field name
     * @return The value, or null if not found
     */
    public Object get(String name) {
        return data.get(name);
    }
    
    /**
     * Gets a typed value from the data model
     *
     * @param name The field name
     * @param type The expected type
     * @return The value cast to the specified type
     * @throws ClassCastException if the value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        Object value = data.get(name);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("Value for field " + name + " cannot be cast to " + type.getName());
        }
        return (T)value;
    }
    
    /**
     * Sets a value in the data model
     *
     * @param name The field name
     * @param value The value to set
     * @throws NullPointerException if name is null
     */
    public void set(String name, Object value) {
        Objects.requireNonNull(name, "Field name cannot be null");
        data.put(name, value);
    }
    
    /**
     * Checks if the data model contains a field
     *
     * @param name The field name
     * @return true if the field exists, false otherwise
     */
    public boolean contains(String name) {
        return data.containsKey(name);
    }
    
    /**
     * Gets all field names in the data model
     *
     * @return An unmodifiable set of field names
     */
    public Set<String> getFieldNames() {
        return Collections.unmodifiableSet(data.keySet());
    }
    
    /**
     * Gets all values in the data model
     *
     * @return An unmodifiable collection of values
     */
    public Collection<Object> getValues() {
        return Collections.unmodifiableCollection(data.values());
    }
    
    /**
     * Gets all entries in the data model
     *
     * @return An unmodifiable set of entries
     */
    public Set<Map.Entry<String, Object>> getEntries() {
        return Collections.unmodifiableSet(data.entrySet());
    }
    
    @Override
    public String toString() {
        return "DataModel{" +
                "fields=" + data.keySet() +
                ", values=" + data.values() +
                '}';
    }
} 