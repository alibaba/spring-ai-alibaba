/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContextKey Tests")
class ContextKeyTest {

	@Test
	@DisplayName("Should create ContextKey with valid name and type")
	void testCreateContextKey() {
		ContextKey<String> key = ContextKey.of("test.key", String.class);

		assertNotNull(key);
		assertEquals("test.key", key.getName());
		assertEquals(String.class, key.getType());
	}

	@Test
	@DisplayName("Should create generic ContextKey for parameterized types")
	void testCreateGenericContextKey() {
		ContextKey<List<String>> listKey = ContextKey.ofGeneric("list.key", List.class);
		ContextKey<Map<String, Object>> mapKey = ContextKey.ofGeneric("map.key", Map.class);

		assertNotNull(listKey);
		assertEquals("list.key", listKey.getName());
		assertEquals(List.class, listKey.getType());

		assertNotNull(mapKey);
		assertEquals("map.key", mapKey.getName());
		assertEquals(Map.class, mapKey.getType());
	}

	@Test
	@DisplayName("Should throw NullPointerException for null parameters")
	void testNullParameterValidation() {
		assertThrows(NullPointerException.class, () -> ContextKey.of(null, String.class));

		assertThrows(NullPointerException.class, () -> ContextKey.of("test.key", null));

		assertThrows(NullPointerException.class, () -> ContextKey.ofGeneric(null, List.class));

		assertThrows(NullPointerException.class, () -> ContextKey.ofGeneric("test.key", null));
	}

	@Test
	@DisplayName("Should check type compatibility correctly")
	void testTypeCompatibility() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		// Compatible types
		assertTrue(stringKey.isCompatibleType("hello"));
		assertTrue(stringKey.isCompatibleType(null));
		assertTrue(intKey.isCompatibleType(42));
		assertTrue(intKey.isCompatibleType(null));

		// Incompatible types
		assertFalse(stringKey.isCompatibleType(42));
		assertFalse(intKey.isCompatibleType("hello"));
	}

	@Test
	@DisplayName("Should cast values safely")
	void testValueCasting() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		// Successful casts
		assertEquals("hello", stringKey.cast("hello"));
		assertEquals(Integer.valueOf(42), intKey.cast(42));
		assertNull(stringKey.cast(null));
		assertNull(intKey.cast(null));
	}

	@Test
	@DisplayName("Should throw ClassCastException for invalid casts")
	void testInvalidCasting() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		assertThrows(ClassCastException.class, () -> stringKey.cast(42));

		assertThrows(ClassCastException.class, () -> intKey.cast("hello"));
	}

	@Test
	@DisplayName("Should implement equality and hashCode correctly")
	void testEqualityAndHashCode() {
		ContextKey<String> key1 = ContextKey.of("test.key", String.class);
		ContextKey<String> key2 = ContextKey.of("test.key", String.class);
		ContextKey<String> key3 = ContextKey.of("other.key", String.class);
		ContextKey<Integer> key4 = ContextKey.of("test.key", Integer.class);

		// Equality tests
		assertEquals(key1, key2);
		assertEquals(key2, key1);
		assertNotEquals(key1, key3);
		assertNotEquals(key1, key4);
		assertNotEquals(key1, null);
		assertNotEquals(key1, "not a context key");

		// HashCode tests
		assertEquals(key1.hashCode(), key2.hashCode());
		// Note: We can't guarantee different hash codes, but they should be consistent
		assertEquals(key1.hashCode(), key1.hashCode());
	}

	@Test
	@DisplayName("Should provide meaningful toString representation")
	void testToString() {
		ContextKey<String> key = ContextKey.of("test.key", String.class);
		String toString = key.toString();

		assertNotNull(toString);
		assertTrue(toString.contains("test.key"));
		assertTrue(toString.contains("String"));
	}

	@Test
	@DisplayName("Should handle generic types in toString")
	void testGenericTypeToString() {
		ContextKey<List<String>> listKey = ContextKey.ofGeneric("list.key", List.class);
		String toString = listKey.toString();

		assertNotNull(toString);
		assertTrue(toString.contains("list.key"));
		assertTrue(toString.contains("List"));
	}

	@Test
	@DisplayName("Should work with inheritance and polymorphism")
	void testInheritanceCompatibility() {
		ContextKey<Object> objectKey = ContextKey.of("object.key", Object.class);

		// Any object should be compatible with Object key
		assertTrue(objectKey.isCompatibleType("string"));
		assertTrue(objectKey.isCompatibleType(42));
		assertTrue(objectKey.isCompatibleType(new Object()));

		// Should cast to Object successfully
		assertEquals("string", objectKey.cast("string"));
		assertEquals(Integer.valueOf(42), objectKey.cast(42));
	}

	@Test
	@DisplayName("Should handle edge cases in error messages")
	void testErrorMessages() {
		ContextKey<String> stringKey = ContextKey.of("test.key", String.class);

		ClassCastException exception = assertThrows(ClassCastException.class, () -> stringKey.cast(42));

		String message = exception.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("test.key"));
		assertTrue(message.contains("String"));
		assertTrue(message.contains("Integer"));
	}

	@Test
	@DisplayName("Should be immutable")
	void testImmutability() {
		ContextKey<String> key = ContextKey.of("test.key", String.class);

		// Verify that key properties cannot be changed externally
		// (This is ensured by design - all fields are final and no setters are provided)

		String originalName = key.getName();
		Class<String> originalType = key.getType();

		// After multiple operations, key should remain unchanged
		key.isCompatibleType("test");
		key.cast("test");
		key.toString();

		assertEquals(originalName, key.getName());
		assertEquals(originalType, key.getType());
	}

	@Test
	@DisplayName("Should handle concurrent access safely")
	void testConcurrentAccess() throws InterruptedException {
		ContextKey<String> key = ContextKey.of("concurrent.key", String.class);

		// Create multiple threads that use the key simultaneously
		Thread[] threads = new Thread[10];
		Throwable[] exceptions = new Throwable[threads.length];

		for (int i = 0; i < threads.length; i++) {
			final int threadIndex = i;
			threads[i] = new Thread(() -> {
				try {
					for (int j = 0; j < 1000; j++) {
						key.getName();
						key.getType();
						key.isCompatibleType("test" + j);
						key.cast("value" + j);
					}
				}
				catch (Throwable t) {
					exceptions[threadIndex] = t;
				}
			});
		}

		// Start all threads
		for (Thread thread : threads) {
			thread.start();
		}

		// Wait for completion
		for (Thread thread : threads) {
			thread.join();
		}

		// Check for exceptions
		for (Throwable exception : exceptions) {
			if (exception != null) {
				fail("Concurrent access caused exception: " + exception.getMessage());
			}
		}
	}

}
