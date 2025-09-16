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
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("JManus Execution Context Tests")
class JManusExecutionContextTest {

	private JManusExecutionContext context;

	@BeforeEach
	void setUp() {
		context = new JManusExecutionContext("test-plan-id");
	}

	@Test
	@DisplayName("Should create empty context")
	void testCreateEmptyContext() {
		assertNotNull(context);
		assertTrue(context.isEmpty());
		assertEquals(0, context.size());
	}

	@Test
	@DisplayName("Should put and get values with type safety")
	void testPutAndGet() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		context.put(stringKey, "hello");
		context.put(intKey, 42);

		assertEquals("hello", context.get(stringKey).orElse(null));
		assertEquals(Integer.valueOf(42), context.get(intKey).orElse(null));
		assertEquals(2, context.size());
		assertFalse(context.isEmpty());
	}

	@Test
	@DisplayName("Should handle null values")
	void testNullValues() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);

		// Putting null should work
		context.put(stringKey, null);
		assertTrue(context.containsKey(stringKey));
		assertFalse(context.get(stringKey).isPresent());

		// Getting non-existent key should return empty Optional
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);
		assertFalse(context.get(intKey).isPresent());
		assertFalse(context.containsKey(intKey));
	}

	@Test
	@DisplayName("Should check key existence")
	void testContainsKey() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		assertFalse(context.containsKey(stringKey));
		assertFalse(context.containsKey(intKey));

		context.put(stringKey, "hello");

		assertTrue(context.containsKey(stringKey));
		assertFalse(context.containsKey(intKey));
	}

	@Test
	@DisplayName("Should remove values")
	void testRemove() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		context.put(stringKey, "hello");
		context.put(intKey, 42);
		assertEquals(2, context.size());

		String removed = context.remove(stringKey);
		assertEquals("hello", removed);
		assertFalse(context.containsKey(stringKey));
		assertEquals(1, context.size());

		// Removing non-existent key should return null
		ContextKey<Boolean> boolKey = ContextKey.of("bool.key", Boolean.class);
		Boolean notFound = context.remove(boolKey);
		assertNull(notFound);
		assertEquals(1, context.size());
	}

	@Test
	@DisplayName("Should clear all values")
	void testClear() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		context.put(stringKey, "hello");
		context.put(intKey, 42);
		assertEquals(2, context.size());

		context.clear();

		assertEquals(0, context.size());
		assertTrue(context.isEmpty());
		assertFalse(context.containsKey(stringKey));
		assertFalse(context.containsKey(intKey));
	}

	@Test
	@DisplayName("Should get all keys")
	void testKeySet() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		Set<ContextKey<?>> emptyKeys = context.keySet();
		assertTrue(emptyKeys.isEmpty());

		context.put(stringKey, "hello");
		context.put(intKey, 42);

		Set<ContextKey<?>> keys = context.keySet();
		assertEquals(2, keys.size());
		assertTrue(keys.contains(stringKey));
		assertTrue(keys.contains(intKey));

		// Returned set should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> keys.add(ContextKey.of("new.key", String.class)));
	}

	@Test
	@DisplayName("Should manage metadata")
	void testMetadata() {
		String key = "test.metadata";
		String value = "test value";

		// Initially no metadata
		assertNull(context.getMetadata(key));
		assertTrue(context.getAllMetadata().isEmpty());

		// Set metadata
		context.putMetadata(key, value);
		assertEquals(value, context.getMetadata(key));
		assertEquals(1, context.getAllMetadata().size());

		// Update metadata
		String newValue = "updated value";
		context.putMetadata(key, newValue);
		assertEquals(newValue, context.getMetadata(key));

		// Note: JManusExecutionContext doesn't have removeMetadata method
		// So we'll test the available functionality
		assertEquals(1, context.getAllMetadata().size());
	}

	@Test
	@DisplayName("Should handle null metadata keys and values")
	void testNullMetadata() {
		assertThrows(IllegalArgumentException.class, () -> context.putMetadata(null, "value"));

		assertThrows(IllegalArgumentException.class, () -> context.getMetadata(null));

		assertThrows(IllegalArgumentException.class, () -> context.putMetadata("", "value"));

		// Null values should be handled gracefully
		context.putMetadata("test.key", null);
		assertNull(context.getMetadata("test.key"));
	}

	@Test
	@DisplayName("Should create immutable snapshot")
	void testCreateSnapshot() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		context.put(stringKey, "hello");
		context.put(intKey, 42);
		context.putMetadata("test.meta", "meta value");

		JManusExecutionContext.ContextSnapshot snapshot = context.createSnapshot();

		assertNotNull(snapshot);
		assertEquals("test-plan-id", snapshot.getPlanId());
		assertEquals(2, snapshot.getData().size());
		assertTrue(snapshot.getData().containsKey(stringKey));
		assertTrue(snapshot.getData().containsKey(intKey));

		// Snapshot data should be immutable
		assertThrows(UnsupportedOperationException.class,
				() -> snapshot.getData().put(ContextKey.of("new.key", String.class), "new value"));

		// Changes to context should not affect snapshot
		context.put(ContextKey.of("new.key", String.class), "new value");
		assertEquals(2, snapshot.getData().size()); // Snapshot unchanged
	}

	@Test
	@DisplayName("Should generate state string for visualization")
	void testGetStateString() {
		// Empty context
		String emptyState = context.getStateString(false);
		assertNotNull(emptyState);
		assertTrue(emptyState.contains("Context State"));
		assertTrue(emptyState.contains("test-plan-id"));

		// Context with data
		ContextKey<String> stringKey = ContextKey.of("task.result", String.class);
		ContextKey<Integer> intKey = ContextKey.of("processed.count", Integer.class);

		context.put(stringKey, "success");
		context.put(intKey, 100);
		context.putMetadata("execution.start", "2025-01-01T10:00:00Z");

		String stateStringNoMeta = context.getStateString(false);
		assertNotNull(stateStringNoMeta);
		assertTrue(stateStringNoMeta.contains("task.result"));
		assertTrue(stateStringNoMeta.contains("success"));
		assertFalse(stateStringNoMeta.contains("execution.start"));

		String stateStringWithMeta = context.getStateString(true);
		assertTrue(stateStringWithMeta.contains("execution.start"));
		assertTrue(stateStringWithMeta.contains("2025-01-01T10:00:00Z"));
	}

	@Test
	@DisplayName("Should handle complex data types")
	void testComplexDataTypes() {
		ContextKey<List<String>> listKey = ContextKey.ofGeneric("list.key", List.class);
		ContextKey<Map<String, Object>> mapKey = ContextKey.ofGeneric("map.key", Map.class);

		List<String> testList = Arrays.asList("item1", "item2", "item3");
		Map<String, Object> testMap = new HashMap<>();
		testMap.put("nested.key", "nested.value");
		testMap.put("number", 42);

		context.put(listKey, testList);
		context.put(mapKey, testMap);

		assertEquals(testList, context.get(listKey).orElse(null));
		assertEquals(testMap, context.get(mapKey).orElse(null));

		String stateString = context.getStateString(false);
		assertTrue(stateString.contains("list.key"));
		assertTrue(stateString.contains("map.key"));
	}

	@Test
	@DisplayName("Should handle null context keys")
	void testNullContextKeys() {
		assertThrows(NullPointerException.class, () -> context.put(null, "value"));

		assertThrows(NullPointerException.class, () -> context.get(null));

		assertThrows(NullPointerException.class, () -> context.remove(null));

		assertThrows(NullPointerException.class, () -> context.containsKey(null));
	}

	@Test
	@DisplayName("Should use getOrDefault correctly")
	void testGetOrDefault() {
		ContextKey<String> stringKey = ContextKey.of("string.key", String.class);
		ContextKey<Integer> intKey = ContextKey.of("int.key", Integer.class);

		// Non-existent keys should return defaults
		assertEquals("default", context.getOrDefault(stringKey, "default"));
		assertEquals(Integer.valueOf(0), context.getOrDefault(intKey, 0));

		// Existing keys should return stored values
		context.put(stringKey, "stored");
		context.put(intKey, 42);

		assertEquals("stored", context.getOrDefault(stringKey, "default"));
		assertEquals(Integer.valueOf(42), context.getOrDefault(intKey, 0));
	}

	@Test
	@DisplayName("Should be thread-safe for concurrent operations")
	void testThreadSafety() throws InterruptedException {
		final int threadCount = 5;
		final int operationsPerThread = 100;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					for (int j = 0; j < operationsPerThread; j++) {
						String keyName = "thread." + threadId + ".key." + j;
						String value = "value." + threadId + "." + j;

						ContextKey<String> key = ContextKey.of(keyName, String.class);

						// Put operation
						context.put(key, value);

						// Get operation
						Optional<String> retrieved = context.get(key);
						assertNotNull(retrieved);

						// Contains operation
						boolean contains = context.containsKey(key);
						assertTrue(contains);

						// Metadata operation
						context.putMetadata("meta." + keyName, "meta." + value);
					}
				}
				finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Verify final state consistency
		assertEquals(threadCount * operationsPerThread, context.size());

		// Verify all values are present and correct
		for (int i = 0; i < threadCount; i++) {
			for (int j = 0; j < operationsPerThread; j++) {
				String keyName = "thread." + i + ".key." + j;
				String expectedValue = "value." + i + "." + j;

				ContextKey<String> key = ContextKey.of(keyName, String.class);
				assertEquals(expectedValue, context.get(key).orElse(null), "Value mismatch for key: " + keyName);
			}
		}
	}

	@Test
	@DisplayName("Should maintain type safety under concurrent access")
	void testTypeSafetyUnderConcurrency() throws InterruptedException {
		ContextKey<String> stringKey = ContextKey.of("concurrent.string", String.class);
		ContextKey<Integer> intKey = ContextKey.of("concurrent.int", Integer.class);

		final int threadCount = 5;
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		// Initialize values
		context.put(stringKey, "initial");
		context.put(intKey, 0);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					for (int j = 0; j < 50; j++) {
						// Each thread updates with its own values
						context.put(stringKey, "thread-" + threadId + "-" + j);
						context.put(intKey, threadId * 1000 + j);

						// Verify type safety
						Optional<String> stringValue = context.get(stringKey);
						Optional<Integer> intValue = context.get(intKey);

						assertTrue(stringValue.isPresent());
						assertTrue(intValue.isPresent());
					}
				}
				finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// Final values should still be type-safe
		assertTrue(context.get(stringKey).isPresent());
		assertTrue(context.get(intKey).isPresent());
	}

	@Test
	@DisplayName("Should handle large number of keys efficiently")
	void testLargeNumberOfKeys() {
		final int keyCount = 1000;

		// Add many keys
		for (int i = 0; i < keyCount; i++) {
			ContextKey<String> key = ContextKey.of("key." + i, String.class);
			context.put(key, "value." + i);
		}

		assertEquals(keyCount, context.size());

		// Verify all keys are accessible
		for (int i = 0; i < keyCount; i++) {
			ContextKey<String> key = ContextKey.of("key." + i, String.class);
			assertEquals("value." + i, context.get(key).orElse(null));
		}

		// State string should still be generated
		String stateString = context.getStateString(false);
		assertNotNull(stateString);
		assertTrue(stateString.contains("Context State"));
	}

	@Test
	@DisplayName("Should provide meaningful toString")
	void testToString() {
		String toString = context.toString();
		assertNotNull(toString);
		assertTrue(toString.contains("test-plan-id"));
		assertTrue(toString.contains("size=0"));

		// Add some data and verify toString updates
		context.put(ContextKey.of("test.key", String.class), "test.value");

		toString = context.toString();
		assertTrue(toString.contains("size=1"));
	}

}
