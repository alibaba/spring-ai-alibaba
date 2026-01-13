/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tool;

import com.alibaba.cloud.ai.graph.KeyStrategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ToolStateCollector parallel state management.
 *
 * <p>
 * Covers the discardToolUpdateMap functionality which is crucial for Issue 1 fix (async
 * timeout state discard).
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("ToolStateCollector Tests")
class ToolStateCollectorTest {

	@Nested
	@DisplayName("Basic Functionality Tests")
	class BasicFunctionalityTests {

		@Test
		@DisplayName("should create isolated update maps for each tool")
		void shouldCreateIsolatedUpdateMaps() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			Map<String, Object> map0 = collector.createToolUpdateMap(0);
			Map<String, Object> map1 = collector.createToolUpdateMap(1);
			Map<String, Object> map2 = collector.createToolUpdateMap(2);

			// Each map should be independent
			map0.put("key", "value0");
			map1.put("key", "value1");
			map2.put("key", "value2");

			assertEquals("value0", map0.get("key"));
			assertEquals("value1", map1.get("key"));
			assertEquals("value2", map2.get("key"));
		}

		@Test
		@DisplayName("should merge updates in index order")
		void shouldMergeUpdatesInIndexOrder() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			// Create maps out of order
			Map<String, Object> map2 = collector.createToolUpdateMap(2);
			Map<String, Object> map0 = collector.createToolUpdateMap(0);
			Map<String, Object> map1 = collector.createToolUpdateMap(1);

			// With default REPLACE strategy, later indices win
			map0.put("result", "from-tool-0");
			map1.put("result", "from-tool-1");
			map2.put("result", "from-tool-2");

			Map<String, Object> merged = collector.mergeAll();

			// Tool 2 (last in order) should win
			assertEquals("from-tool-2", merged.get("result"));
		}

		@Test
		@DisplayName("should handle empty update maps")
		void shouldHandleEmptyUpdateMaps() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			collector.createToolUpdateMap(0);
			Map<String, Object> map1 = collector.createToolUpdateMap(1);
			collector.createToolUpdateMap(2);

			map1.put("onlyKey", "onlyValue");

			Map<String, Object> merged = collector.mergeAll();

			assertEquals(1, merged.size());
			assertEquals("onlyValue", merged.get("onlyKey"));
		}

		@Test
		@DisplayName("should track completed count")
		void shouldTrackCompletedCount() {
			ToolStateCollector collector = new ToolStateCollector(5, null);

			assertEquals(0, collector.getCompletedCount());

			collector.createToolUpdateMap(0);
			assertEquals(1, collector.getCompletedCount());

			collector.createToolUpdateMap(2);
			assertEquals(2, collector.getCompletedCount());

			collector.createToolUpdateMap(4);
			assertEquals(3, collector.getCompletedCount());
		}

	}

	@Nested
	@DisplayName("Discard Functionality Tests")
	class DiscardFunctionalityTests {

		@Test
		@DisplayName("discardToolUpdateMap should remove tool updates from merge")
		void discardToolUpdateMap_removesFromMerge() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			Map<String, Object> map0 = collector.createToolUpdateMap(0);
			Map<String, Object> map1 = collector.createToolUpdateMap(1);
			Map<String, Object> map2 = collector.createToolUpdateMap(2);

			map0.put("result", "tool0-result");
			map1.put("result", "tool1-result");
			map2.put("result", "tool2-result");

			// Discard tool 1 (simulating timeout)
			collector.discardToolUpdateMap(1);

			Map<String, Object> merged = collector.mergeAll();

			// Tool 2 should win (tool 1 was discarded)
			assertEquals("tool2-result", merged.get("result"));
		}

		@Test
		@DisplayName("discardToolUpdateMap should prevent late writes from being merged")
		void discardToolUpdateMap_preventsLateWrites() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			Map<String, Object> map0 = collector.createToolUpdateMap(0);
			Map<String, Object> map1 = collector.createToolUpdateMap(1);

			map0.put("key", "value0");

			// Discard map1 before any writes
			collector.discardToolUpdateMap(1);

			// Late write after discard - this write goes to the original map
			// but since it's discarded, it won't be merged
			map1.put("key", "value1-late");

			Map<String, Object> merged = collector.mergeAll();

			// Only tool 0's value should be present
			assertEquals("value0", merged.get("key"));
		}

		@Test
		@DisplayName("discardToolUpdateMap should reduce completed count")
		void discardToolUpdateMap_reducesCompletedCount() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			collector.createToolUpdateMap(0);
			collector.createToolUpdateMap(1);
			collector.createToolUpdateMap(2);

			assertEquals(3, collector.getCompletedCount());

			collector.discardToolUpdateMap(1);

			assertEquals(2, collector.getCompletedCount());
		}

		@Test
		@DisplayName("discardToolUpdateMap should be idempotent")
		void discardToolUpdateMap_isIdempotent() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			collector.createToolUpdateMap(0);
			collector.createToolUpdateMap(1);

			// Discard same index multiple times
			collector.discardToolUpdateMap(1);
			collector.discardToolUpdateMap(1);
			collector.discardToolUpdateMap(1);

			assertEquals(1, collector.getCompletedCount());
		}

		@Test
		@DisplayName("discardToolUpdateMap should handle non-existent index gracefully")
		void discardToolUpdateMap_handlesNonExistentIndex() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			collector.createToolUpdateMap(0);

			// Discard index that was never created - should not throw
			collector.discardToolUpdateMap(1);
			collector.discardToolUpdateMap(99);

			assertEquals(1, collector.getCompletedCount());
		}

	}

	@Nested
	@DisplayName("Merge Once Contract Tests")
	class MergeOnceContractTests {

		@Test
		@DisplayName("mergeAll should only be callable once")
		void mergeAll_onlyCallableOnce() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			collector.createToolUpdateMap(0).put("key", "value");
			collector.mergeAll();

			assertThrows(IllegalStateException.class, collector::mergeAll);
		}

		@Test
		@DisplayName("createToolUpdateMap should throw after mergeAll")
		void createToolUpdateMap_throwsAfterMergeAll() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			collector.createToolUpdateMap(0);
			collector.mergeAll();

			assertThrows(IllegalStateException.class, () -> collector.createToolUpdateMap(1));
		}

		@Test
		@DisplayName("isMerged should return correct state")
		void isMerged_returnsCorrectState() {
			ToolStateCollector collector = new ToolStateCollector(1, null);

			assertFalse(collector.isMerged());

			collector.createToolUpdateMap(0);
			assertFalse(collector.isMerged());

			collector.mergeAll();
			assertTrue(collector.isMerged());
		}

	}

	@Nested
	@DisplayName("Key Strategy Tests")
	class KeyStrategyTests {

		@Test
		@DisplayName("should apply REPLACE strategy by default")
		void shouldApplyReplaceStrategyByDefault() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			collector.createToolUpdateMap(0).put("key", "first");
			collector.createToolUpdateMap(1).put("key", "second");

			Map<String, Object> merged = collector.mergeAll();
			assertEquals("second", merged.get("key"));
		}

		@Test
		@DisplayName("should apply APPEND strategy for specified keys")
		void shouldApplyAppendStrategy() {
			Map<String, KeyStrategy> strategies = Map.of("items", KeyStrategy.APPEND);

			ToolStateCollector collector = new ToolStateCollector(2, strategies);

			collector.createToolUpdateMap(0).put("items", java.util.List.of("a", "b"));
			collector.createToolUpdateMap(1).put("items", java.util.List.of("c", "d"));

			Map<String, Object> merged = collector.mergeAll();

			@SuppressWarnings("unchecked")
			java.util.List<String> items = (java.util.List<String>) merged.get("items");
			assertEquals(4, items.size());
			assertTrue(items.containsAll(java.util.List.of("a", "b", "c", "d")));
		}

		@Test
		@DisplayName("should handle null keyStrategies")
		void shouldHandleNullKeyStrategies() {
			ToolStateCollector collector = new ToolStateCollector(2, null);

			collector.createToolUpdateMap(0).put("key", "value");

			// Should not throw
			Map<String, Object> merged = collector.mergeAll();
			assertNotNull(merged);
		}

	}

	@Nested
	@DisplayName("Thread Safety Tests")
	class ThreadSafetyTests {

		@Test
		@DisplayName("should handle concurrent writes to different tool maps")
		void shouldHandleConcurrentWritesToDifferentMaps() throws InterruptedException {
			ToolStateCollector collector = new ToolStateCollector(10, null);

			CountDownLatch latch = new CountDownLatch(10);
			ExecutorService executor = Executors.newFixedThreadPool(10);

			for (int i = 0; i < 10; i++) {
				final int index = i;
				executor.submit(() -> {
					try {
						Map<String, Object> map = collector.createToolUpdateMap(index);
						map.put("toolId", index);
						map.put("result", "result-" + index);
					}
					finally {
						latch.countDown();
					}
				});
			}

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			executor.shutdown();

			assertEquals(10, collector.getCompletedCount());

			Map<String, Object> merged = collector.mergeAll();
			// Last tool's values should be present (REPLACE strategy)
			assertNotNull(merged.get("toolId"));
			assertNotNull(merged.get("result"));
		}

		@Test
		@DisplayName("should handle concurrent writes within single tool map")
		void shouldHandleConcurrentWritesWithinSingleMap() throws InterruptedException {
			ToolStateCollector collector = new ToolStateCollector(1, null);
			Map<String, Object> map = collector.createToolUpdateMap(0);

			CountDownLatch latch = new CountDownLatch(100);
			ExecutorService executor = Executors.newFixedThreadPool(10);
			AtomicInteger counter = new AtomicInteger(0);

			for (int i = 0; i < 100; i++) {
				final int index = i;
				executor.submit(() -> {
					try {
						map.put("key-" + index, "value-" + index);
						counter.incrementAndGet();
					}
					finally {
						latch.countDown();
					}
				});
			}

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			executor.shutdown();

			assertEquals(100, counter.get());
			assertEquals(100, map.size());
		}

		@Test
		@DisplayName("should handle concurrent discard operations")
		void shouldHandleConcurrentDiscardOperations() throws InterruptedException {
			ToolStateCollector collector = new ToolStateCollector(10, null);

			// Create all maps first
			for (int i = 0; i < 10; i++) {
				collector.createToolUpdateMap(i).put("key", "value-" + i);
			}

			CountDownLatch latch = new CountDownLatch(5);
			ExecutorService executor = Executors.newFixedThreadPool(5);

			// Concurrently discard half of them
			for (int i = 0; i < 5; i++) {
				final int index = i * 2; // Discard even indices
				executor.submit(() -> {
					try {
						collector.discardToolUpdateMap(index);
					}
					finally {
						latch.countDown();
					}
				});
			}

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			executor.shutdown();

			assertEquals(5, collector.getCompletedCount());
		}

	}

	@Nested
	@DisplayName("Edge Case Tests")
	class EdgeCaseTests {

		@Test
		@DisplayName("should handle zero tools")
		void shouldHandleZeroTools() {
			ToolStateCollector collector = new ToolStateCollector(0, null);

			Map<String, Object> merged = collector.mergeAll();
			assertTrue(merged.isEmpty());
		}

		@Test
		@DisplayName("should handle all tools discarded")
		void shouldHandleAllToolsDiscarded() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			collector.createToolUpdateMap(0).put("key", "value0");
			collector.createToolUpdateMap(1).put("key", "value1");
			collector.createToolUpdateMap(2).put("key", "value2");

			// Discard all
			collector.discardToolUpdateMap(0);
			collector.discardToolUpdateMap(1);
			collector.discardToolUpdateMap(2);

			Map<String, Object> merged = collector.mergeAll();
			assertTrue(merged.isEmpty());
		}

		@Test
		@DisplayName("should handle sparse tool indices")
		void shouldHandleSparseToolIndices() {
			ToolStateCollector collector = new ToolStateCollector(100, null);

			// Only create maps for indices 0, 50, 99
			collector.createToolUpdateMap(0).put("result", "first");
			collector.createToolUpdateMap(50).put("result", "middle");
			collector.createToolUpdateMap(99).put("result", "last");

			Map<String, Object> merged = collector.mergeAll();

			// Last created (by index) should win
			assertEquals("last", merged.get("result"));
		}

		@Test
		@DisplayName("should preserve non-overlapping keys from all tools")
		void shouldPreserveNonOverlappingKeys() {
			ToolStateCollector collector = new ToolStateCollector(3, null);

			collector.createToolUpdateMap(0).put("unique0", "value0");
			collector.createToolUpdateMap(1).put("unique1", "value1");
			collector.createToolUpdateMap(2).put("unique2", "value2");

			Map<String, Object> merged = collector.mergeAll();

			assertEquals(3, merged.size());
			assertEquals("value0", merged.get("unique0"));
			assertEquals("value1", merged.get("unique1"));
			assertEquals("value2", merged.get("unique2"));
		}

	}

}
