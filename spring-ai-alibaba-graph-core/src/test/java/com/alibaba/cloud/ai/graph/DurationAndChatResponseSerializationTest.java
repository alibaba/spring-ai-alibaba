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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Duration and ChatResponse serialization support in checkpoint.
 * 
 * These tests verify the fixes for:
 * 1. Duration serialization (JavaTimeModule support)
 * 2. ChatResponse serialization (normalization to Map, deserialization returns null)
 * 
 * Related to Issue #2702 (discovered during NPE fix testing).
 */
public class DurationAndChatResponseSerializationTest {

	@Test
	void testDurationSerialization() throws Exception {
		// Test that Duration can be serialized without exception
		OverAllState originalState = new OverAllState();
		Duration duration = Duration.ofSeconds(30);
		originalState.updateState(Map.of("duration", duration));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		assertDoesNotThrow(() -> {
			OverAllState restoredState = serializer.cloneObject(originalState);
			assertNotNull(restoredState, "Restored state should not be null");
			
			Object restoredValue = restoredState.value("duration").orElse(null);
			assertNotNull(restoredValue, "Duration should be preserved after serialization");
		}, "Duration serialization should work without 'Java 8 date/time type not supported' error");
	}

	@Test
	void testDurationInMetadata() throws Exception {
		// Test Duration nested in metadata Map
		OverAllState originalState = new OverAllState();
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("execution_time", Duration.ofMillis(1500));
		metadata.put("timeout", Duration.ofMinutes(5));
		originalState.updateState(Map.of("metadata", metadata));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		assertDoesNotThrow(() -> {
			OverAllState restoredState = serializer.cloneObject(originalState);
			assertNotNull(restoredState, "Restored state should not be null");
			
			@SuppressWarnings("unchecked")
			Map<String, Object> restoredMetadata = (Map<String, Object>) restoredState.value("metadata").orElse(null);
			assertNotNull(restoredMetadata, "Metadata should be preserved");
			assertNotNull(restoredMetadata.get("execution_time"), "Execution time should be preserved");
			assertNotNull(restoredMetadata.get("timeout"), "Timeout should be preserved");
		}, "Duration in metadata serialization should work without 'Java 8 date/time type not supported' error");
	}

	@Test
	void testChatResponseSerialization() throws Exception {
		// Test that ChatResponse can be serialized without exception
		AssistantMessage message = new AssistantMessage("Test response");
		Generation generation = new Generation(message);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("chatResponse", chatResponse));

		// Verify runtime type
		Object runtimeValue = originalState.value("chatResponse").orElse(null);
		assertTrue(runtimeValue instanceof ChatResponse, "Runtime value should be ChatResponse");

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		assertDoesNotThrow(() -> {
			OverAllState restoredState = serializer.cloneObject(originalState);
			
			// ChatResponse should return null after deserialization (cannot be reconstructed)
			Object restoredValue = restoredState.value("chatResponse").orElse(null);
			assertNull(restoredValue, 
				"ChatResponse should return null after deserialization (cannot be reconstructed)");
		}, "ChatResponse serialization should work without 'Cannot construct instance' error");
	}

	@Test
	void testChatResponseInNestedMap() throws Exception {
		// Test ChatResponse nested in Map structure
		AssistantMessage message = new AssistantMessage("Nested response");
		Generation generation = new Generation(message);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		Map<String, Object> container = new HashMap<>();
		container.put("response", chatResponse);
		container.put("other", "data");

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("container", container));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		@SuppressWarnings("unchecked")
		Map<String, Object> restoredContainer = (Map<String, Object>) restoredState.value("container").orElse(null);
		assertNotNull(restoredContainer, "Container should be preserved");
		
		// ChatResponse should be null after deserialization
		Object restoredResponse = restoredContainer.get("response");
		assertNull(restoredResponse, 
			"Nested ChatResponse should return null after deserialization");
		
		// Other values should be preserved
		assertEquals("data", restoredContainer.get("other"), "Other values should be preserved");
	}

	@Test
	void testChatResponseInList() throws Exception {
		// Test ChatResponse in List
		AssistantMessage message1 = new AssistantMessage("Response 1");
		AssistantMessage message2 = new AssistantMessage("Response 2");
		ChatResponse response1 = new ChatResponse(List.of(new Generation(message1)));
		ChatResponse response2 = new ChatResponse(List.of(new Generation(message2)));

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("responses", List.of(response1, response2, "string")));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		@SuppressWarnings("unchecked")
		List<Object> restoredList = (List<Object>) restoredState.value("responses").orElse(null);
		assertNotNull(restoredList, "List should be preserved");
		assertEquals(3, restoredList.size(), "List size should be preserved");
		
		// ChatResponse items should be null
		assertNull(restoredList.get(0), "First ChatResponse should be null");
		assertNull(restoredList.get(1), "Second ChatResponse should be null");
		
		// Other items should be preserved
		assertEquals("string", restoredList.get(2), "Non-ChatResponse items should be preserved");
	}

	@Test
	void testDurationAndChatResponseTogether() throws Exception {
		// Test both Duration and ChatResponse in the same state
		Duration duration = Duration.ofSeconds(60);
		AssistantMessage message = new AssistantMessage("Combined test");
		ChatResponse chatResponse = new ChatResponse(List.of(new Generation(message)));

		OverAllState originalState = new OverAllState();
		Map<String, Object> data = new HashMap<>();
		data.put("duration", duration);
		data.put("chatResponse", chatResponse);
		data.put("metadata", Map.of("timeout", Duration.ofMinutes(2)));
		originalState.updateState(data);

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		assertDoesNotThrow(() -> {
			OverAllState restoredState = serializer.cloneObject(originalState);
			assertNotNull(restoredState, "Restored state should not be null");

			// Verify Duration is preserved
			Object restoredDuration = restoredState.value("duration").orElse(null);
			assertNotNull(restoredDuration, "Duration should be preserved");

			// Verify ChatResponse returns null
			Object restoredChatResponse = restoredState.value("chatResponse").orElse(null);
			assertNull(restoredChatResponse, "ChatResponse should return null");

			// Verify nested Duration in metadata
			@SuppressWarnings("unchecked")
			Map<String, Object> restoredMetadata = (Map<String, Object>) restoredState.value("metadata").orElse(null);
			assertNotNull(restoredMetadata, "Metadata should be preserved");
			assertNotNull(restoredMetadata.get("timeout"), "Nested Duration should be preserved");
		}, "Combined Duration and ChatResponse serialization should work without errors");
	}

	@Test
	void testChatResponseWithDurationInMetadata() throws Exception {
		// Test ChatResponse that contains Duration in its metadata
		// Note: Since ChatResponse cannot be reconstructed, the Duration inside
		// its metadata is lost. This test verifies the serialization doesn't crash.
		AssistantMessage message = new AssistantMessage("Response with duration metadata");
		Generation generation = new Generation(message);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("response", chatResponse));

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// ChatResponse itself should be null
		Object restoredResponse = restoredState.value("response").orElse(null);
		assertNull(restoredResponse, "ChatResponse should return null");
	}

	@Test
	void testSerializationRoundTripWithRawData() throws Exception {
		// Test direct serialization/deserialization of data Map
		Duration duration = Duration.ofHours(1);
		AssistantMessage message = new AssistantMessage("Raw data test");
		ChatResponse chatResponse = new ChatResponse(List.of(new Generation(message)));

		Map<String, Object> data = new HashMap<>();
		data.put("duration", duration);
		data.put("chatResponse", chatResponse);
		data.put("string", "test");

		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		assertDoesNotThrow(() -> {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			serializer.writeData(data, oos);
			oos.flush();

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			Map<String, Object> deserializedData = serializer.readData(ois);

			// Verify Duration is preserved
			Object deserializedDuration = deserializedData.get("duration");
			assertNotNull(deserializedDuration, "Duration should be preserved");

			// Verify ChatResponse returns null
			Object deserializedChatResponse = deserializedData.get("chatResponse");
			assertNull(deserializedChatResponse, "ChatResponse should return null after deserialization");

			// Verify other data
			assertEquals("test", deserializedData.get("string"), "Other data should be preserved");
		}, "Raw data serialization round trip should work without errors");
	}

}
