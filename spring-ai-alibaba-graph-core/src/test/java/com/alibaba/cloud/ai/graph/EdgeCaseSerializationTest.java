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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test edge cases for serialization
 */
public class EdgeCaseSerializationTest {

	@Test
	void testPrimitiveArraySerialization() throws Exception {
		// Test that primitive arrays are handled correctly
		OverAllState originalState = new OverAllState();
		int[] primitiveArray = {1, 2, 3};
		originalState.updateState(Map.of("numbers", primitiveArray));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		// This should not throw ClassCastException
		assertDoesNotThrow(() -> {
			serializer.cloneObject(originalState);
		}, "Primitive arrays should be handled gracefully");
	}

	@Test
	void testCancelledFutureSerialization() throws Exception {
		// Test that cancelled CompletableFuture is handled correctly
		OverAllState originalState = new OverAllState();
		CompletableFuture<String> cancelledFuture = new CompletableFuture<>();
		cancelledFuture.cancel(true);
		originalState.updateState(Map.of("cancelled", cancelledFuture));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		assertDoesNotThrow(() -> {
			OverAllState restoredState = serializer.cloneObject(originalState);
			assertNotNull(restoredState);
		}, "Cancelled CompletableFuture should be handled gracefully");
	}

	@Test
	void testEmptyGraphResponseSerialization() throws Exception {
		// Test GraphResponse with null values
		OverAllState originalState = new OverAllState();
		GraphResponse<?> emptyResponse = GraphResponse.done(null);
		originalState.updateState(Map.of("empty", emptyResponse));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		Object restoredValue = restoredState.value("empty").orElse(null);
		assertNotNull(restoredValue, "Empty GraphResponse should be preserved");
		assertTrue(restoredValue instanceof GraphResponse, "Type should be preserved");
		
		@SuppressWarnings("unchecked")
		GraphResponse<Object> restoredResponse = (GraphResponse<Object>) restoredValue;
		assertTrue(restoredResponse.resultValue().isEmpty(), "Null result should be preserved");
	}

	@Test
	void testLargeStateSerialization() throws Exception {
		// Test performance with large state
		OverAllState originalState = new OverAllState();
		Map<String, Object> largeMap = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			largeMap.put("key" + i, "value" + i);
		}
		originalState.updateState(largeMap);

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		long startTime = System.currentTimeMillis();
		OverAllState restoredState = serializer.cloneObject(originalState);
		long duration = System.currentTimeMillis() - startTime;

		assertNotNull(restoredState);
		assertTrue(duration < 5000, "Serialization should complete in reasonable time (was " + duration + "ms)");
	}

	@Test
	void testNestedGraphResponseSerialization() throws Exception {
		// Test deeply nested GraphResponse
		OverAllState originalState = new OverAllState();
		
		GraphResponse<?> inner = GraphResponse.of("inner value");
		Map<String, Object> middleMap = Map.of("inner_response", inner);
		GraphResponse<?> outer = GraphResponse.done(middleMap);
		
		originalState.updateState(Map.of("nested", outer));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		Object restoredValue = restoredState.value("nested").orElse(null);
		assertNotNull(restoredValue);
		assertTrue(restoredValue instanceof GraphResponse, "Outer GraphResponse should be preserved");
	}
}

