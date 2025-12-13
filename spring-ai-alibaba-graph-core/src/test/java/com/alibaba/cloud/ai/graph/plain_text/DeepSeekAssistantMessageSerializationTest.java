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
package com.alibaba.cloud.ai.graph.plain_text;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for DeepSeekAssistantMessage serialization/deserialization bug fix.
 * This test reproduces issue #3186 where DeepSeekAssistantMessage fails to deserialize
 * when it appears in arrays or nested structures.
 */
@EnabledIfEnvironmentVariable(named = "AI_DEEPSEEK_API_KEY", matches = ".+")
class DeepSeekAssistantMessageSerializationTest {

	private SpringAIJacksonStateSerializer serializer;

	@BeforeEach
	void setUp() {
		AgentStateFactory<OverAllState> stateFactory = OverAllState::new;
		serializer = new SpringAIJacksonStateSerializer(stateFactory);
	}

	/**
	 * Check if DeepSeekAssistantMessage is available on the classpath.
	 */
	private static boolean isDeepSeekAvailable() {
		try {
			Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Create a DeepSeekAssistantMessage instance.
	 */
	private DeepSeekAssistantMessage createDeepSeekMessage(String text, String reasoningContent, 
			Map<String, Object> metadata, List<AssistantMessage.ToolCall> toolCalls) {
		return new DeepSeekAssistantMessage.Builder()
				.content(text)
				.reasoningContent(reasoningContent)
				.properties(metadata)
				.toolCalls(toolCalls)
				.build();
	}

	@Test
	@EnabledIf("isDeepSeekAvailable")
	void testDeepSeekAssistantMessageInArray() throws Exception {
		// This test reproduces the bug reported in issue #3186
		// The bug occurs when DeepSeekAssistantMessage is in an array/list
		
		// Create a DeepSeekAssistantMessage with tool calls
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", "deepseek-chat");
		metadata.put("temperature", 0.7);
		
		List<AssistantMessage.ToolCall> toolCalls = List.of(
			new AssistantMessage.ToolCall("call_1", "function", "calculator", "{\"a\": 1, \"b\": 2}"),
			new AssistantMessage.ToolCall("call_2", "function", "weather", "{\"city\": \"Beijing\"}")
		);
		
		DeepSeekAssistantMessage originalMessage1 = createDeepSeekMessage(
			"Let me calculate that for you.",
			"I need to use the calculator tool to add 1 and 2.",
			metadata,
			toolCalls
		);
		
		DeepSeekAssistantMessage originalMessage2 = createDeepSeekMessage(
			"The weather in Beijing is sunny.",
			"I used the weather tool to get the current weather.",
			new HashMap<>(),
			List.of()
		);
		
		// Create a list containing DeepSeekAssistantMessage objects
		// This is the scenario that triggers the bug
		List<Object> messages = List.of(originalMessage1, originalMessage2);
		
		// Wrap in a map for serialization
		Map<String, Object> data = new HashMap<>();
		data.put("messages", messages);
		
		// Serialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();
		
		// Deserialize - this should work after the fix
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);
		
		// Verify deserialization succeeded
		assertNotNull(deserializedData);
		assertTrue(deserializedData.containsKey("messages"));
		
		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertNotNull(deserializedMessages);
		assertEquals(2, deserializedMessages.size());
		
		// Verify first message
		Object deserializedMessage1 = deserializedMessages.get(0);
		assertNotNull(deserializedMessage1);
		assertTrue(deserializedMessage1 instanceof DeepSeekAssistantMessage);
		DeepSeekAssistantMessage msg1 = (DeepSeekAssistantMessage) deserializedMessage1;
		assertEquals("Let me calculate that for you.", msg1.getText());
		assertEquals("I need to use the calculator tool to add 1 and 2.", msg1.getReasoningContent());
		
		// Verify metadata - messageType may be automatically added during deserialization
		Map<String, Object> deserializedMetadata1 = msg1.getMetadata();
		assertNotNull(deserializedMetadata1);
		assertEquals("deepseek-chat", deserializedMetadata1.get("model"));
		assertEquals(0.7, deserializedMetadata1.get("temperature"));
		// messageType may be present in deserialized metadata
		
		assertEquals(2, msg1.getToolCalls().size());
		
		// Verify second message
		Object deserializedMessage2 = deserializedMessages.get(1);
		assertNotNull(deserializedMessage2);
		assertTrue(deserializedMessage2 instanceof DeepSeekAssistantMessage);
		DeepSeekAssistantMessage msg2 = (DeepSeekAssistantMessage) deserializedMessage2;
		assertEquals("The weather in Beijing is sunny.", msg2.getText());
		assertEquals("I used the weather tool to get the current weather.", msg2.getReasoningContent());
		
		// Verify metadata - messageType may be automatically added during deserialization
		Map<String, Object> deserializedMetadata2 = msg2.getMetadata();
		assertNotNull(deserializedMetadata2);
		// Original metadata was empty, but messageType may be added during deserialization
		// So we only check that our original fields are not present
		assertNull(deserializedMetadata2.get("model"));
		assertNull(deserializedMetadata2.get("temperature"));
		
		assertTrue(msg2.getToolCalls().isEmpty());
	}

	@Test
	@EnabledIf("isDeepSeekAvailable")
	void testDeepSeekAssistantMessageInNestedStructure() throws Exception {
		// Test DeepSeekAssistantMessage in nested structures (Map containing List)
		
		DeepSeekAssistantMessage originalMessage = createDeepSeekMessage(
			"Nested structure test",
			"Testing nested serialization",
			Map.of("test", "nested"),
			List.of()
		);
		
		// Create nested structure
		Map<String, Object> innerMap = new HashMap<>();
		innerMap.put("deepSeekMessage", originalMessage);
		innerMap.put("otherData", "test");
		
		Map<String, Object> outerMap = new HashMap<>();
		outerMap.put("inner", innerMap);
		outerMap.put("messages", List.of(originalMessage));
		
		// Serialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(outerMap, oos);
		oos.flush();
		
		// Deserialize
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);
		
		// Verify
		assertNotNull(deserializedData);
		
		@SuppressWarnings("unchecked")
		Map<String, Object> deserializedInner = (Map<String, Object>) deserializedData.get("inner");
		assertNotNull(deserializedInner);
		
		Object deserializedMessage = deserializedInner.get("deepSeekMessage");
		assertNotNull(deserializedMessage);
		assertTrue(deserializedMessage instanceof DeepSeekAssistantMessage);
		DeepSeekAssistantMessage msg = (DeepSeekAssistantMessage) deserializedMessage;
		assertEquals("Nested structure test", msg.getText());
		
		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertNotNull(deserializedMessages);
		assertEquals(1, deserializedMessages.size());
		assertTrue(deserializedMessages.get(0) instanceof DeepSeekAssistantMessage);
	}

	@Test
	@EnabledIf("isDeepSeekAvailable")
	void testDeepSeekAssistantMessageAsTopLevelObject() throws Exception {
		// Test DeepSeekAssistantMessage as a top-level object (should work even before fix)
		
		DeepSeekAssistantMessage originalMessage = createDeepSeekMessage(
			"Top level message",
			"Reasoning content",
			Map.of("key", "value"),
			List.of(new AssistantMessage.ToolCall("id", "function", "name", "args"))
		);
		
		Map<String, Object> data = new HashMap<>();
		data.put("message", originalMessage);
		
		// Serialize and deserialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);
		
		// Verify
		assertNotNull(deserializedData);
		Object deserializedMessage = deserializedData.get("message");
		assertNotNull(deserializedMessage);
		assertTrue(deserializedMessage instanceof DeepSeekAssistantMessage);
		DeepSeekAssistantMessage msg = (DeepSeekAssistantMessage) deserializedMessage;
		assertEquals("Top level message", msg.getText());
		assertEquals("Reasoning content", msg.getReasoningContent());
		assertEquals(1, msg.getToolCalls().size());
	}

	@Test
	@EnabledIf("isDeepSeekAvailable")
	void testDeepSeekAssistantMessageWithNullReasoningContent() throws Exception {
		// Test DeepSeekAssistantMessage with null reasoning content
		
		DeepSeekAssistantMessage originalMessage = createDeepSeekMessage(
			"Message without reasoning",
			null, // null reasoning content
			new HashMap<>(),
			List.of()
		);
		
		List<Object> messages = List.of(originalMessage);
		Map<String, Object> data = new HashMap<>();
		data.put("messages", messages);
		
		// Serialize and deserialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);
		
		// Verify
		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertNotNull(deserializedMessages);
		assertEquals(1, deserializedMessages.size());
		
		Object deserializedMessage = deserializedMessages.get(0);
		assertNotNull(deserializedMessage);
		assertTrue(deserializedMessage instanceof DeepSeekAssistantMessage);
		DeepSeekAssistantMessage msg = (DeepSeekAssistantMessage) deserializedMessage;
		assertEquals("Message without reasoning", msg.getText());
		assertEquals(null, msg.getReasoningContent());
	}
}

