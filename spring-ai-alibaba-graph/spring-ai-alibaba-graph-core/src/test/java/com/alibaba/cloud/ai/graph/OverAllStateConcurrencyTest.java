/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OverAllStateConcurrencyTest {

	@Test
	void testConcurrentStateIsolation() throws InterruptedException {

		OverAllState state1 = new OverAllState();
		OverAllState state2 = new OverAllState();

		state1.registerKeyAndStrategy("testKey", new ReplaceStrategy());
		state2.registerKeyAndStrategy("testKey", new ReplaceStrategy());

		state1.input(Map.of("testKey", "request1_value"));
		state2.input(Map.of("testKey", "request2_value"));

		assertEquals("request1_value", state1.value("testKey").orElse(null));
		assertEquals("request2_value", state2.value("testKey").orElse(null));

		OverAllState snapshot1 = state1.snapShot().get();
		OverAllState snapshot2 = state2.snapShot().get();

		assertEquals("request1_value", snapshot1.value("testKey").orElse(null));
		assertEquals("request2_value", snapshot2.value("testKey").orElse(null));
	}

	@Test
	void testConcurrentUpdates() throws InterruptedException {
		final int threadCount = 10;
		final int updatesPerThread = 100;
		final OverAllState state = new OverAllState();
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		state.registerKeyAndStrategy("counter", new ReplaceStrategy());

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					for (int j = 0; j < updatesPerThread; j++) {
						String value = "thread_" + threadId + "_update_" + j;
						state.input(Map.of("counter", value));

						String currentValue = state.value("counter", "");
						if (currentValue.contains("thread_" + threadId)) {
							successCount.incrementAndGet();
						}
					}
				}
				finally {
					latch.countDown();
				}
			});
		}

		// Wait for all threads to complete
		assertTrue(latch.await(30, TimeUnit.SECONDS));
		executor.shutdown();

		String finalValue = state.value("counter", "");
		assertNotNull(finalValue);
		assertTrue(finalValue.startsWith("thread_"));
		assertTrue(successCount.get() > 0, "At least some thread updates should have been successful");
	}

	@Test
	void testCloneStateIsolation() {

		Map<String, KeyStrategy> keyStrategies = new ConcurrentHashMap<>();
		keyStrategies.put("testKey", new ReplaceStrategy());

		Map<String, Object> data1 = new ConcurrentHashMap<>();
		data1.put("testKey", "data1_value");

		Map<String, Object> data2 = new ConcurrentHashMap<>();
		data2.put("testKey", "data2_value");

		OverAllState clonedState1 = new OverAllState(new ConcurrentHashMap<>(data1),
				new ConcurrentHashMap<>(keyStrategies), false);
		OverAllState clonedState2 = new OverAllState(new ConcurrentHashMap<>(data2),
				new ConcurrentHashMap<>(keyStrategies), false);

		assertEquals("data1_value", clonedState1.value("testKey").orElse(null));
		assertEquals("data2_value", clonedState2.value("testKey").orElse(null));

		clonedState1.input(Map.of("testKey", "modified_data1"));

		assertEquals("modified_data1", clonedState1.value("testKey").orElse(null));
		assertEquals("data2_value", clonedState2.value("testKey").orElse(null));
	}

	@Test
	void testKeyStrategiesIsolation() {
		OverAllState state1 = new OverAllState();
		OverAllState state2 = new OverAllState();

		state1.registerKeyAndStrategy("customKey", new ReplaceStrategy());

		assertTrue(state1.containStrategy("customKey"));
		assertFalse(state2.containStrategy("customKey"));

		assertTrue(state1.containStrategy(OverAllState.DEFAULT_INPUT_KEY));
		assertTrue(state2.containStrategy(OverAllState.DEFAULT_INPUT_KEY));
	}

}
