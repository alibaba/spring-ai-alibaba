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
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAIJacksonStateSerializerTest {

	private SpringAIJacksonStateSerializer serializer;

	@BeforeEach
	void setUp() {
		AgentStateFactory<OverAllState> stateFactory = OverAllState::new;
		serializer = new SpringAIJacksonStateSerializer(stateFactory);
	}

	@Test
	void testSystemMessageSerialization() throws Exception {
		// 创建测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", "test");
		metadata.put("priority", 1);

		SystemMessage original = SystemMessage.builder().text("You are a helpful assistant").metadata(metadata).build();

		// 创建包含SystemMessage的状态数据
		Map<String, Object> data = new HashMap<>();
		data.put("systemMessage", original);

		// 序列化
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		// 反序列化
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);

		// 验证
		assertNotNull(deserializedData);
		assertTrue(deserializedData.containsKey("systemMessage"));

		SystemMessage deserialized = (SystemMessage) deserializedData.get("systemMessage");
		assertNotNull(deserialized);
		assertEquals(original.getText(), deserialized.getText());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
		assertEquals(MessageType.SYSTEM, deserialized.getMessageType());
	}

	@Test
	void testUserMessageSerialization() throws Exception {
		// 创建测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("user_id", "12345");
		metadata.put("session_id", "session_001");

		UserMessage original = UserMessage.builder().text("Hello, how can I help you?").metadata(metadata).build();

		// 直接序列化和反序列化UserMessage对象
		UserMessage deserialized = serializeAndDeserialize(original);

		// 验证
		assertNotNull(deserialized);
		assertEquals(original.getText(), deserialized.getText());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
		assertEquals(MessageType.USER, deserialized.getMessageType());
	}

	@Test
	void testAssistantMessageSerialization() throws Exception {
		// 创建测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", "gpt-3.5-turbo");
		metadata.put("temperature", 0.7);

		AssistantMessage original = new AssistantMessage("I'm here to help you!", metadata);

		// 直接序列化和反序列化AssistantMessage对象
		AssistantMessage deserialized = serializeAndDeserialize(original);

		// 验证
		assertNotNull(deserialized);
		assertEquals(original.getText(), deserialized.getText());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
		assertEquals(MessageType.ASSISTANT, deserialized.getMessageType());
	}

	@Test
	void testToolResponseMessageSerialization() throws Exception {
		// 创建测试数据 - 使用正确的ToolResponse结构
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("tool_execution_id", "exec_123");
		metadata.put("execution_time", 150);

		// 创建ToolResponse对象列表 - 根据实际ToolResponse的构造方式
		List<ToolResponseMessage.ToolResponse> responses = List.of(
				new ToolResponseMessage.ToolResponse("tool_call_1", "calculator", "{\"result\": 42}"),
				new ToolResponseMessage.ToolResponse("tool_call_2", "weather", "{\"temperature\": 25}"));

		ToolResponseMessage original = new ToolResponseMessage(responses, metadata);

		// 直接序列化和反序列化ToolResponseMessage对象
		ToolResponseMessage deserialized = serializeAndDeserialize(original);

		// 验证
		assertNotNull(deserialized);
		assertEquals(original.getResponses().size(), deserialized.getResponses().size());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
		assertEquals(MessageType.TOOL, deserialized.getMessageType());

		List<ToolResponseMessage.ToolResponse> originalResponses = original.getResponses();
		List<ToolResponseMessage.ToolResponse> deserializedResponses = deserialized.getResponses();

		for (int i = 0; i < originalResponses.size(); i++) {
			ToolResponseMessage.ToolResponse orig = originalResponses.get(i);
			ToolResponseMessage.ToolResponse deser = deserializedResponses.get(i);

			assertEquals(orig.id(), deser.id());
			assertEquals(orig.name(), deser.name());
			assertEquals(orig.responseData(), deser.responseData());
		}
	}

	@Test
	void testDocumentSerialization() throws Exception {
		// 创建测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", "file.pdf");
		metadata.put("page", 1);
		metadata.put("author", "John Doe");

		Document original = Document.builder()
			.id("doc_123")
			.text("This is a test document content.")
			.metadata(metadata)
			.score(0.95)
			.build();

		// 直接序列化和反序列化Document对象
		Document deserialized = serializeAndDeserialize(original);

		// 验证
		assertNotNull(deserialized);
		assertEquals(original.getId(), deserialized.getId());
		assertEquals(original.getText(), deserialized.getText());
		assertEquals(original.getScore(), deserialized.getScore());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
	}

	@Test
	void testDocumentWithoutOptionalFields() throws Exception {
		// 测试没有可选字段的 Document
		Document original = Document.builder()
			.id("minimal_doc")
			.text("Minimal document")
			.metadata(new HashMap<>())
			.build();

		// 直接序列化和反序列化Document对象
		Document deserialized = serializeAndDeserialize(original);

		// 验证
		assertEquals(original.getId(), deserialized.getId());
		assertEquals(original.getText(), deserialized.getText());
		assertNull(deserialized.getScore());
		assertNull(deserialized.getMedia());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
	}

	@Test
	void testMultipleMessagesSerialization() throws Exception {
		// 测试多种消息类型的混合序列化
		SystemMessage systemMessage = SystemMessage.builder()
			.text("You are a helpful assistant")
			.metadata(Map.of("role", "system"))
			.build();

		UserMessage userMessage = UserMessage.builder().text("Hello!").metadata(Map.of("user_id", "123")).build();

		Document document = Document.builder()
			.id("doc_001")
			.text("Sample document")
			.metadata(Map.of("type", "text"))
			.score(0.8)
			.build();

		// 创建包含多种类型的状态数据
		Map<String, Object> data = new HashMap<>();
		data.put("system", systemMessage);
		data.put("user", userMessage);
		data.put("doc", document);
		data.put("messages", List.of(systemMessage, userMessage));

		// 序列化和反序列化
		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		// 验证各个对象
		SystemMessage deserializedSystem = (SystemMessage) deserializedData.get("system");
		assertEquals(systemMessage.getText(), deserializedSystem.getText());

		UserMessage deserializedUser = (UserMessage) deserializedData.get("user");
		assertEquals(userMessage.getText(), deserializedUser.getText());

		Document deserializedDoc = (Document) deserializedData.get("doc");
		assertEquals(document.getId(), deserializedDoc.getId());
		assertEquals(document.getScore(), deserializedDoc.getScore());

		@SuppressWarnings("unchecked")
		List<Object> deserializedMessages = (List<Object>) deserializedData.get("messages");
		assertEquals(2, deserializedMessages.size());
		assertTrue(deserializedMessages.get(0) instanceof SystemMessage);
		assertTrue(deserializedMessages.get(1) instanceof UserMessage);
	}

	@Test
	void testComplexMetadataSerialization() throws Exception {
		// 测试复杂元数据的序列化
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("string_field", "test_value");
		metadata.put("number_field", 42);
		metadata.put("boolean_field", true);
		metadata.put("list_field", List.of("item1", "item2", "item3"));

		Map<String, Object> nestedMap = new HashMap<>();
		nestedMap.put("nested_key", "nested_value");
		metadata.put("nested_object", nestedMap);

		UserMessage original = UserMessage.builder().text("Message with complex metadata").metadata(metadata).build();

		// 直接序列化和反序列化UserMessage对象
		UserMessage deserialized = serializeAndDeserialize(original);

		// 验证
		assertNotNull(deserialized);
		assertEquals(original.getText(), deserialized.getText());

		Map<String, Object> deserializedMetadata = deserialized.getMetadata();
		assertEquals("test_value", deserializedMetadata.get("string_field"));
		assertEquals(42, deserializedMetadata.get("number_field"));
		assertEquals(true, deserializedMetadata.get("boolean_field"));

		@SuppressWarnings("unchecked")
		List<String> deserializedList = (List<String>) deserializedMetadata.get("list_field");
		assertEquals(3, deserializedList.size());
		assertEquals("item1", deserializedList.get(0));

		@SuppressWarnings("unchecked")
		Map<String, Object> deserializedNestedMap = (Map<String, Object>) deserializedMetadata.get("nested_object");
		assertEquals("nested_value", deserializedNestedMap.get("nested_key"));
	}

	private <T> T serializeAndDeserialize(T object) throws IOException, ClassNotFoundException {
		// 将对象包装在Map中进行序列化
		Map<String, Object> data = new HashMap<>();
		data.put("object", object);

		// 序列化
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		// 反序列化
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);

		// 返回反序列化的对象
		@SuppressWarnings("unchecked")
		T result = (T) deserializedData.get("object");
		return result;
	}

}
