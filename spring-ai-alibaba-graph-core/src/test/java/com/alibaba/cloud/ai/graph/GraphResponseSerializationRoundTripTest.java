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

import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that GraphResponse and CompletableFuture can be serialized and deserialized
 * while preserving their type and data.
 */
public class GraphResponseSerializationRoundTripTest {

	@Test
	void graphResponseErrorShouldPreserveTypeAfterSerialization() throws Exception {
		// Create state with GraphResponse
		OverAllState originalState = new OverAllState();
		RuntimeException error = new RuntimeException("Test error");
		GraphResponse<?> errorResponse = GraphResponse.error(error, Map.of("key", "value"));
		originalState.updateState(Map.of("error_result", errorResponse));

		// Verify runtime type
		Object runtimeValue = originalState.value("error_result").orElse(null);
		assertTrue(runtimeValue instanceof GraphResponse, "Runtime value should be GraphResponse");

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify type is preserved
		Object restoredValue = restoredState.value("error_result").orElse(null);
		assertNotNull(restoredValue, "Restored value should not be null");
		assertTrue(restoredValue instanceof GraphResponse, 
			"Restored value should still be GraphResponse, but was: " + restoredValue.getClass().getName());

		// Verify data is preserved
		@SuppressWarnings("unchecked")
		GraphResponse<Object> restoredResponse = (GraphResponse<Object>) restoredValue;
		assertTrue(restoredResponse.isError(), "Should preserve error status");
		assertEquals("value", restoredResponse.getAllMetadata().get("key"), "Should preserve metadata");
	}

	@Test
	void graphResponseSuccessShouldPreserveTypeAfterSerialization() throws Exception {
		// Create state with successful GraphResponse
		OverAllState originalState = new OverAllState();
		GraphResponse<?> successResponse = GraphResponse.of("Success result", Map.of("status", "ok"));
		originalState.updateState(Map.of("success_result", successResponse));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify type is preserved
		Object restoredValue = restoredState.value("success_result").orElse(null);
		assertNotNull(restoredValue, "Restored value should not be null");
		assertTrue(restoredValue instanceof GraphResponse,
			"Restored value should still be GraphResponse");

		// Verify data is preserved
		@SuppressWarnings("unchecked")
		GraphResponse<Object> restoredResponse = (GraphResponse<Object>) restoredValue;
		assertFalse(restoredResponse.isError(), "Should preserve success status");
		assertEquals("Success result", restoredResponse.resultValue().orElse(null), "Should preserve result");
		assertEquals("ok", restoredResponse.getAllMetadata().get("status"), "Should preserve metadata");
	}

	@Test
	void completableFutureShouldPreserveTypeAfterSerialization() throws Exception {
		// Create state with CompletableFuture
		OverAllState originalState = new OverAllState();
		CompletableFuture<String> future = CompletableFuture.completedFuture("Async result");
		originalState.updateState(Map.of("async_result", future));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify type is preserved
		Object restoredValue = restoredState.value("async_result").orElse(null);
		assertNotNull(restoredValue, "Restored value should not be null");
		assertTrue(restoredValue instanceof CompletableFuture,
			"Restored value should still be CompletableFuture");

		// Verify data is preserved
		@SuppressWarnings("unchecked")
		CompletableFuture<String> restoredFuture = (CompletableFuture<String>) restoredValue;
		assertTrue(restoredFuture.isDone(), "Should preserve completion status");
		assertEquals("Async result", restoredFuture.getNow(null), "Should preserve result");
	}

	@Test
	void nestedGraphResponseInMapShouldPreserveType() throws Exception {
		// Create state with nested GraphResponse in Map
		OverAllState originalState = new OverAllState();
		GraphResponse<?> response = GraphResponse.of("Nested result", Map.of());
		Map<String, Object> container = Map.of("nested", response, "other", "data");
		originalState.updateState(Map.of("container", container));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify nested GraphResponse type is preserved
		@SuppressWarnings("unchecked")
		Map<String, Object> restoredContainer = (Map<String, Object>) restoredState.value("container").orElse(null);
		assertNotNull(restoredContainer, "Container should be restored");
		
		Object nestedValue = restoredContainer.get("nested");
		assertTrue(nestedValue instanceof GraphResponse,
			"Nested GraphResponse should preserve type");
		
		assertEquals("data", restoredContainer.get("other"), "Other values should be preserved");
	}
}

