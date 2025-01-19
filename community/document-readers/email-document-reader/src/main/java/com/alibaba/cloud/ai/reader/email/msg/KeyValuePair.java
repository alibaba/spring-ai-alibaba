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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A generic key-value pair class that also provides list functionality.
 * This class combines the functionality of KVPArray and KVPEntry.
 * 
 * @author xiadong
 * @since 0.8.0
 *
 * @param <K> The type of the key
 * @param <V> The type of the value
 */
public class KeyValuePair<K, V> {
    
    /**
     * The key of this key-value pair
     */
    private final K key;
    
    /**
     * The value of this key-value pair
     */
    private final V value;
    
    /**
     * Creates a new key-value pair with the specified key and value
     *
     * @param key The key
     * @param value The value
     */
    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }
    
    /**
     * Gets the key of this pair
     *
     * @return The key
     */
    public K getKey() {
        return key;
    }
    
    /**
     * Gets the value of this pair
     *
     * @return The value
     */
    public V getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("%s=%s", key, value);
    }
    
    /**
     * A list implementation for managing collections of key-value pairs.
     */
    public static class KeyValuePairList<K, V> implements Iterable<KeyValuePair<K, V>> {
        
        private final List<KeyValuePair<K, V>> pairs;
        
        /**
         * Creates a new empty list of key-value pairs.
         */
        public KeyValuePairList() {
            this.pairs = new ArrayList<>();
        }
        
        /**
         * Adds a new key-value pair to the list.
         *
         * @param key The key
         * @param value The value
         */
        public void add(K key, V value) {
            pairs.add(new KeyValuePair<>(key, value));
        }
        
        /**
         * Adds an existing key-value pair to the list.
         *
         * @param pair The key-value pair to add
         */
        public void add(KeyValuePair<K, V> pair) {
            pairs.add(pair);
        }
        
        /**
         * Gets the key-value pair at the specified index.
         *
         * @param index The index
         * @return The key-value pair at the specified index
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        public KeyValuePair<K, V> get(int index) {
            return pairs.get(index);
        }
        
        /**
         * Gets the number of key-value pairs in the list.
         *
         * @return The size of the list
         */
        public int size() {
            return pairs.size();
        }
        
        /**
         * Checks if the list is empty.
         *
         * @return true if the list contains no key-value pairs
         */
        public boolean isEmpty() {
            return pairs.isEmpty();
        }
        
        /**
         * Clears all key-value pairs from the list.
         */
        public void clear() {
            pairs.clear();
        }
        
        @Override
        public Iterator<KeyValuePair<K, V>> iterator() {
            return pairs.iterator();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < pairs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(pairs.get(i));
            }
            sb.append("]");
            return sb.toString();
        }
    }
} 