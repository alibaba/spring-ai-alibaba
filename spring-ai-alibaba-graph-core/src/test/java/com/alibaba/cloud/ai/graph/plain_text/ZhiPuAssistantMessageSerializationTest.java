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
package com.alibaba.cloud.ai.graph.plain_text;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test case for ZhiPuAiAssistantMessage serialization/deserialization.
 * Verifies that ZhiPuAiAssistantMessage survives a serialize→deserialize round-trip
 * through SpringAIJacksonStateSerializer without losing type information or data.
 *
 * <p>This test reproduces and validates the fix for issue #4503:
 * ZhiPuAiAssistantMessage was not registered in the serializer, causing it to
 * deserialize as LinkedHashMap after cloneState(), which led to
 * "No AssistantMessage found in 'messages' state" error in ReactAgent.
 *
 * <p>No API keys or environment variables are required — ZhiPuAiAssistantMessage
 * is already on the classpath as a compile dependency.
 */
class ZhiPuAssistantMessageSerializationTest {

	private SpringAIJacksonStateSerializer serializer;

	@BeforeEach
	void setUp() {
		AgentStateFactory<OverAllState> stateFactory = OverAllState::new;
		serializer = new SpringAIJacksonStateSerializer(stateFactory);
	}

	private ZhiPuAiAssistantMessage createZhiPuMessage(String text, String reasoningContent,
			Map<String, Object> metadata, List<AssistantMessage.ToolCall> toolCalls) {
		return new ZhiPuAiAssistantMessage.Builder()
				.content(text)
				.reasoningContent(reasoningContent)
				.properties(metadata)
				.toolCalls(toolCalls)
				.build();
	}

	@Test
	@DisplayName("Basic round-trip: text content and exact type preserved")
	void testBasicRoundTrip() throws Exception {
		ZhiPuAiAssistantMessage original = createZhiPuMessage("Hello from ZhiPu", null, new HashMap<>(), List.of());

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		Object deserializedMessage = deserializedData.get("message");
		assertNotNull(deserializedMessage);
		assertTrue(deserializedMessage instanceof ZhiPuAiAssistantMessage,
				"Restored message should be ZhiPuAiAssistantMessage, but was: "
						+ deserializedMessage.getClass().getName());
		assertTrue(deserializedMessage instanceof AssistantMessage,
				"Restored message should also be an AssistantMessage (super-type)");

		ZhiPuAiAssistantMessage msg = (ZhiPuAiAssistantMessage) deserializedMessage;
		assertEquals("Hello from ZhiPu", msg.getText());
	}

	@Test
	@DisplayName("Round-trip with tool calls preserves ToolCall data")
	void testWithToolCalls() throws Exception {
		List<AssistantMessage.ToolCall> toolCalls = List.of(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{\"a\": 1, \"b\": 2}"),
				new AssistantMessage.ToolCall("call_2", "function", "weather", "{\"city\": \"Beijing\"}"));

		ZhiPuAiAssistantMessage original = createZhiPuMessage(
				"Let me calculate that for you.",
				"I need to use the calculator tool to add 1 and 2.",
				new HashMap<>(),
				toolCalls);

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		Object deserializedMessage = deserializedData.get("message");
		assertTrue(deserializedMessage instanceof ZhiPuAiAssistantMessage);

		ZhiPuAiAssistantMessage msg = (ZhiPuAiAssistantMessage) deserializedMessage;
		assertEquals("Let me calculate that for you.", msg.getText());
		assertEquals("I need to use the calculator tool to add 1 and 2.", msg.getReasoningContent());
		assertNotNull(msg.getToolCalls());
		assertEquals(2, msg.getToolCalls().size());

		AssistantMessage.ToolCall tc1 = msg.getToolCalls().get(0);
		assertEquals("call_1", tc1.id());
		assertEquals("function", tc1.type());
		assertEquals("calculator", tc1.name());
		assertEquals("{\"a\": 1, \"b\": 2}", tc1.arguments());

		AssistantMessage.ToolCall tc2 = msg.getToolCalls().get(1);
		assertEquals("call_2", tc2.id());
		assertEquals("weather", tc2.name());
		assertEquals("{\"city\": \"Beijing\"}", tc2.arguments());
	}

	@Test
	@DisplayName("Round-trip with reasoning content preserves the field")
	void testWithReasoningContent() throws Exception {
		ZhiPuAiAssistantMessage original = createZhiPuMessage(
				"The answer is 42",
				"Let me think about this step by step...",
				new HashMap<>(),
				List.of());

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		ZhiPuAiAssistantMessage msg = (ZhiPuAiAssistantMessage) deserializedData.get("message");
		assertEquals("The answer is 42", msg.getText());
		assertEquals("Let me think about this step by step...", msg.getReasoningContent());
	}

	@Test
	@DisplayName("Round-trip with null reasoning content handles null correctly")
	void testWithNullReasoningContent() throws Exception {
		ZhiPuAiAssistantMessage original = createZhiPuMessage(
				"Message without reasoning",
				null,
				new HashMap<>(),
				List.of());

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		ZhiPuAiAssistantMessage msg = (ZhiPuAiAssistantMessage) deserializedData.get("message");
		assertEquals("Message without reasoning", msg.getText());
		assertNull(msg.getReasoningContent());
	}

	@Test
	@DisplayName("Round-trip with metadata preserves properties")
	void testWithMetadata() throws Exception {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", "glm-4");
		metadata.put("temperature", 0.7);
		metadata.put("finishReason", "stop");

		ZhiPuAiAssistantMessage original = createZhiPuMessage(
				"Response with metadata",
				null,
				metadata,
				List.of());

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		ZhiPuAiAssistantMessage msg = (ZhiPuAiAssistantMessage) deserializedData.get("message");
		assertEquals("Response with metadata", msg.getText());
		Map<String, Object> deserializedMetadata = msg.getMetadata();
		assertNotNull(deserializedMetadata);
		assertEquals("glm-4", deserializedMetadata.get("model"));
		assertEquals(0.7, deserializedMetadata.get("temperature"));
		assertEquals("stop", deserializedMetadata.get("finishReason"));
	}

	@Test
	@DisplayName("Multiple ZhiPuAiAssistantMessage in list all preserve type")
	void testMultipleMessagesInList() throws Exception {
		ZhiPuAiAssistantMessage msg1 = createZhiPuMessage("First message", null, new HashMap<>(), List.of());

		List<AssistantMessage.ToolCall> toolCalls = List.of(
				new AssistantMessage.ToolCall("call_1", "function", "search", "{\"q\": \"test\"}"));
		ZhiPuAiAssistantMessage msg2 = createZhiPuMessage(
				"Second message with tools",
				"Thinking about search...",
				Map.of("model", "glm-4"),
				toolCalls);

		Map<String, Object> data = new HashMap<>();
		data.put("messages", List.of(msg1, msg2));

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertNotNull(deserializedMessages);
		assertEquals(2, deserializedMessages.size());

		assertTrue(deserializedMessages.get(0) instanceof ZhiPuAiAssistantMessage);
		assertTrue(deserializedMessages.get(1) instanceof ZhiPuAiAssistantMessage);

		ZhiPuAiAssistantMessage restored1 = (ZhiPuAiAssistantMessage) deserializedMessages.get(0);
		ZhiPuAiAssistantMessage restored2 = (ZhiPuAiAssistantMessage) deserializedMessages.get(1);

		assertEquals("First message", restored1.getText());
		assertEquals("Second message with tools", restored2.getText());
		assertEquals("Thinking about search...", restored2.getReasoningContent());
		assertEquals(1, restored2.getToolCalls().size());
		assertEquals("search", restored2.getToolCalls().get(0).name());
	}

	@Test
	@DisplayName("Mixed message types: plain AssistantMessage + ZhiPuAiAssistantMessage")
	void testMixedMessageTypes() throws Exception {
		AssistantMessage plainAssistant = new AssistantMessage("plain assistant response");
		ZhiPuAiAssistantMessage zhipuMsg = createZhiPuMessage("zhipu response", "reasoning...", new HashMap<>(), List.of());

		Map<String, Object> data = new HashMap<>();
		data.put("messages", List.of(plainAssistant, zhipuMsg));

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertEquals(2, deserializedMessages.size());

		// First should be plain AssistantMessage
		Object r0 = deserializedMessages.get(0);
		assertTrue(r0 instanceof AssistantMessage,
				"First message should be AssistantMessage, was: " + r0.getClass().getName());

		// Second should be ZhiPuAiAssistantMessage
		Object r1 = deserializedMessages.get(1);
		assertTrue(r1 instanceof ZhiPuAiAssistantMessage,
				"Second message should be ZhiPuAiAssistantMessage, was: " + r1.getClass().getName());

		assertEquals("plain assistant response", ((AssistantMessage) r0).getText());
		assertEquals("zhipu response", ((ZhiPuAiAssistantMessage) r1).getText());
		assertEquals("reasoning...", ((ZhiPuAiAssistantMessage) r1).getReasoningContent());
	}

	@Test
	@DisplayName("ZhiPuAiAssistantMessage in nested structures preserves type")
	void testInNestedStructures() throws Exception {
		ZhiPuAiAssistantMessage original = createZhiPuMessage(
				"Nested structure test",
				"Testing nested serialization",
				Map.of("test", "nested"),
				List.of());

		Map<String, Object> innerMap = new HashMap<>();
		innerMap.put("deepSeekMessage", original);
		innerMap.put("otherData", "test");

		Map<String, Object> outerMap = new HashMap<>();
		outerMap.put("inner", innerMap);
		outerMap.put("messages", List.of(original));

		Map<String, Object> deserializedData = serializeAndDeserialize(outerMap);

		// Verify from inner map
		@SuppressWarnings("unchecked")
		Map<String, Object> deserializedInner = (Map<String, Object>) deserializedData.get("inner");
		assertNotNull(deserializedInner);

		Object deserializedMessage = deserializedInner.get("deepSeekMessage");
		assertTrue(deserializedMessage instanceof ZhiPuAiAssistantMessage);
		assertEquals("Nested structure test", ((ZhiPuAiAssistantMessage) deserializedMessage).getText());

		// Verify from messages list
		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertEquals(1, deserializedMessages.size());
		assertTrue(deserializedMessages.get(0) instanceof ZhiPuAiAssistantMessage);
	}

	@Test
	@DisplayName("Full OverAllState cloneObject round-trip preserves ZhiPuAiAssistantMessage type")
	void testFullStateCloneRoundTrip() throws Exception {
		List<AssistantMessage.ToolCall> toolCalls = List.of(
				new AssistantMessage.ToolCall("call_1", "function", "get_weather", "{\"city\": \"Shanghai\"}"));

		ZhiPuAiAssistantMessage msg = createZhiPuMessage(
				"state-level test",
				"I should check the weather",
				Map.of("model", "glm-4"),
				toolCalls);

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("messages", List.of(msg)));

		SpringAIJacksonStateSerializer stateSerializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = stateSerializer.cloneObject(originalState);

		Object messagesObj = restoredState.value("messages").orElse(null);
		assertNotNull(messagesObj, "messages should be present in restored state");
		assertTrue(messagesObj instanceof List);

		@SuppressWarnings("unchecked")
		List<Object> messages = (List<Object>) messagesObj;
		assertEquals(1, messages.size());

		Object restoredMsg = messages.get(0);
		assertTrue(restoredMsg instanceof ZhiPuAiAssistantMessage,
				"After full state clone, message should be ZhiPuAiAssistantMessage, was: "
						+ (restoredMsg != null ? restoredMsg.getClass().getName() : "null"));

		ZhiPuAiAssistantMessage typed = (ZhiPuAiAssistantMessage) restoredMsg;
		assertEquals("state-level test", typed.getText());
		assertEquals("I should check the weather", typed.getReasoningContent());
		assertEquals(1, typed.getToolCalls().size());
		assertEquals("get_weather", typed.getToolCalls().get(0).name());
		assertEquals("{\"city\": \"Shanghai\"}", typed.getToolCalls().get(0).arguments());
	}

	@Test
	@DisplayName("Serialized JSON contains @class property for ZhiPuAiAssistantMessage")
	void testSerializedJsonContainsClassProperty() throws Exception {
		ZhiPuAiAssistantMessage original = createZhiPuMessage("test content", null, new HashMap<>(), List.of());

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		// Serialize to JSON string via ObjectMapper for inspection
		String json = serializer.objectMapper().writeValueAsString(data);
		assertNotNull(json);

		// Verify @class property exists and points to ZhiPuAiAssistantMessage
		assertTrue(json.contains("@class"),
				"JSON should contain @class property for type info. JSON: " + json);
		assertTrue(json.contains("org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage"),
				"JSON @class should reference ZhiPuAiAssistantMessage. JSON: " + json);
	}

	private Map<String, Object> serializeAndDeserialize(Map<String, Object> data) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return serializer.readData(ois);
	}

}
