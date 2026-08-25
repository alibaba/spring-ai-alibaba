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

import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

		AssistantMessage original = AssistantMessage.builder()
			.content("I'm here to help you!")
			.properties(metadata)
			.build();

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

		ToolResponseMessage original = ToolResponseMessage.builder()
			.responses(responses)
			.metadata(metadata)
			.build();

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
	void shouldPreserveTypedListElementsInStateValue() throws Exception {
		Plan original = new Plan(List.of(new PlanStep("search"), new PlanStep("write")));

		Plan deserialized = serializeAndDeserialize(original);

		assertNotNull(deserialized);
		assertEquals(2, deserialized.steps().size());
		assertTrue(deserialized.steps().get(0) instanceof PlanStep);
		assertEquals("search", deserialized.steps().get(0).title());

		Map<String, Object> data = new HashMap<>();
		data.put("plan", deserialized);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();
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

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldSerializeDashScopeSearchInfoContainingMapResults() throws Exception {
		List rawSearchResults = List.of(Map.of(
				"site_name", "example",
				"icon", "",
				"index", 0,
				"title", "Example",
				"url", "https://example.com"));
		DashScopeApiSpec.SearchInfo searchInfo = new DashScopeApiSpec.SearchInfo(rawSearchResults, List.of());
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("search_info", searchInfo))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		assertTrue(deserialized.getMetadata().get("search_info") instanceof Map);
		Map<String, Object> deserializedSearchInfo =
				(Map<String, Object>) deserialized.getMetadata().get("search_info");
		assertTrue(deserializedSearchInfo.get("search_results") instanceof List);
		List<?> deserializedSearchResults = (List<?>) deserializedSearchInfo.get("search_results");
		assertEquals("example", ((Map<?, ?>) deserializedSearchResults.get(0)).get("site_name"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldSerializeDashScopeSearchInfoWithGlobalInclusion() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawSearchResults = List.of(Map.of(
				"site_name", "example",
				"icon", "",
				"index", 0,
				"title", "Example",
				"url", "https://example.com"));
		DashScopeApiSpec.SearchInfo searchInfo = new DashScopeApiSpec.SearchInfo(rawSearchResults, List.of());
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("search_info", searchInfo))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> deserializedSearchInfo =
				(Map<String, Object>) deserialized.getMetadata().get("search_info");
		List<?> deserializedSearchResults = (List<?>) deserializedSearchInfo.get("search_results");
		assertEquals("example", ((Map<?, ?>) deserializedSearchResults.get(0)).get("site_name"));
	}

	@Test
	void shouldSerializePrivateRecordUsingJacksonPropertyRules() throws Exception {
		PrivateMetadata metadata = new PrivateMetadata("visible", "ignored", "secret");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("private_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		PrivateMetadata record =
				(PrivateMetadata) deserialized.getMetadata().get("private_record");
		assertEquals("visible", record.visible());
		assertNull(record.ignored());
		assertNull(record.secret());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizePrivateRecordWithIncompatibleCollectionElements() throws Exception {
		List rawResults = List.of(Map.of("name", "visible"));
		PrivateSearchMetadata metadata = new PrivateSearchMetadata(rawResults, "ignored", "secret");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("private_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("private_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertEquals(1, record.size());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizePrivateRecordWithIncompatibleMapValues() throws Exception {
		Map rawResults = Map.of("result", Map.of("name", "visible"));
		PrivateMapMetadata metadata = new PrivateMapMetadata(rawResults);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("private_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("private_record");
		assertEquals(Map.of("result", Map.of("name", "visible")), record.get("results"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldHonorNonAbsentForJacksonReferenceValues() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		ReferenceMetadata metadata = new ReferenceMetadata(rawResults, new AtomicReference<>());
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("private_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("private_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertEquals(1, record.size());
	}

	@Test
	void shouldPreserveJacksonByteArraySerialization() throws Exception {
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("payload", new byte[] { 1, 2, 3 }))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		// Primitive-array metadata must retain its binary type. The previous
		// List["[B", "AQID"] assertion described the serialization defect fixed by #4860.
		Object payload = deserialized.getMetadata().get("payload");
		assertInstanceOf(byte[].class, payload);
		assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) payload);
	}

	@Test
	void shouldPreserveListOfByteArraysRoundTrip() throws Exception {
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("payload", List.of(new byte[] { 1, 2, 3 }, new byte[] { 4, 5 })))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		// Nested primitive arrays keep their element type too: the serializer
		// restores every WRAPPER_ARRAY-encoded entry, so a List<byte[]> must not
		// degrade into a List<String> of "[B" descriptors.
		List<?> list = assertInstanceOf(List.class, deserialized.getMetadata().get("payload"));
		assertInstanceOf(byte[].class, list.get(0));
		assertInstanceOf(byte[].class, list.get(1));
		assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) list.get(0));
		assertArrayEquals(new byte[] { 4, 5 }, (byte[]) list.get(1));
	}

	@Test
	void shouldPreserveOrdinaryObjectArrayType() throws Exception {
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("values", new String[] { "first", "second" }))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		assertTrue(deserialized.getMetadata().get("values") instanceof String[]);
		assertArrayEquals(new String[] { "first", "second" },
				(String[]) deserialized.getMetadata().get("values"));
	}

	@Test
	void shouldPreservePerTypeContainerFormat() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configOverride(FormatList.class)
			.setFormat(JsonFormat.Value.empty()
				.withFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED));
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		FormatList values = new FormatList();
		values.add("only");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("values", values))
			.build();

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			customSerializer.writeData(Map.of("object", message), output);
		}

		assertTrue(new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1)
			.contains(FormatList.class.getName()));
	}

	@Test
	void shouldUseConfiguredRecordSerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(RedactedMetadata.class, new JsonSerializer<>() {
			@Override
			public void serialize(RedactedMetadata value, JsonGenerator gen, SerializerProvider serializers)
					throws IOException {
				gen.writeString("redacted");
			}

			@Override
			public void serializeWithType(RedactedMetadata value, JsonGenerator gen, SerializerProvider serializers,
					TypeSerializer typeSerializer) throws IOException {
				serialize(value, gen, serializers);
			}
		});
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_record", new RedactedMetadata("secret")))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		assertEquals("redacted", deserialized.getMetadata().get("custom_record"));
	}

	@Test
	void shouldUseConfiguredRecordComponentSerializer() throws Exception {
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_component", new ComponentMetadata("secret", List.of("visible"))))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		ComponentMetadata record =
				(ComponentMetadata) deserialized.getMetadata().get("custom_component");
		assertEquals("redacted", record.value());
		assertEquals(List.of("visible"), record.items());
	}

	@Test
	void shouldUseConfiguredMetadataMapSerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(SecretMap.class, new RedactingSerializer<>());
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		SecretMap secretMap = new SecretMap();
		secretMap.put("password", "secret");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_map", secretMap))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		assertEquals("redacted", deserialized.getMetadata().get("custom_map"));
	}

	@Test
	void shouldUseConfiguredMetadataCollectionSerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(SecretList.class, new RedactingSerializer<>());
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		SecretList secretList = new SecretList();
		secretList.add("secret");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_list", secretList))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		assertEquals("redacted", deserialized.getMetadata().get("custom_list"));
	}

	@Test
	void shouldUseConfiguredMetadataObjectArraySerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(SecretValue[].class, new RedactingSerializer<>());
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_array", new SecretValue[] { new SecretValue("secret") }))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		assertEquals("redacted", deserialized.getMetadata().get("custom_array"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldUseAnnotatedContainerContentSerializers() throws Exception {
		SecretAnnotatedMap secretMap = new SecretAnnotatedMap();
		secretMap.put("password", "secret");
		SecretAnnotatedList secretList = new SecretAnnotatedList();
		secretList.add("secret");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_map", secretMap, "custom_list", secretList))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> deserializedMap =
				(Map<String, Object>) deserialized.getMetadata().get("custom_map");
		assertEquals("redacted", deserializedMap.get("password"));
		assertEquals(List.of(SecretAnnotatedList.class.getName(), List.of("redacted")),
				deserialized.getMetadata().get("custom_list"));
	}

	@Test
	void shouldUseRecordPropertyInclusionRules() throws Exception {
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("included_record", new IncludedMetadata("visible", List.of())))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		IncludedMetadata record =
				(IncludedMetadata) deserialized.getMetadata().get("included_record");
		assertEquals("visible", record.visible());
		assertNull(record.emptyItems());
	}

	@Test
	void shouldUseConfiguredRecordFilter() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setFilterProvider(new SimpleFilterProvider().addFilter("metadataFilter",
				SimpleBeanPropertyFilter.filterOutAllExcept("visible")));
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("filtered_record", new FilteredMetadata("visible", "secret")))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		FilteredMetadata record =
				(FilteredMetadata) deserialized.getMetadata().get("filtered_record");
		assertEquals("visible", record.visible());
		assertNull(record.secret());
	}

	@Test
	void shouldUseMapperRecordInclusionRules() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("included_record", new MapperIncludedMetadata("visible", List.of())))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		MapperIncludedMetadata record =
				(MapperIncludedMetadata) deserialized.getMetadata().get("included_record");
		assertEquals("visible", record.visible());
		assertNull(record.emptyItems());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldUseAnnotatedMapKeySerializer() throws Exception {
		SecretKeyMap secretMap = new SecretKeyMap();
		secretMap.put("secret-key", "visible");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_map", secretMap))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> deserializedMap =
				(Map<String, Object>) deserialized.getMetadata().get("custom_map");
		assertEquals(Map.of("redacted-key", "visible"), deserializedMap);
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldUsePerTypeMapInclusion() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configOverride(OverrideMap.class)
			.setInclude(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));
		assertTrue(!objectMapper.getSerializationConfig()
			.getDefaultPropertyInclusion()
			.equals(objectMapper.getSerializationConfig().getDefaultPropertyInclusion(OverrideMap.class)));
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		OverrideMap overrideMap = new OverrideMap();
		overrideMap.put("empty", null);
		overrideMap.put("visible", "value");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_map", overrideMap))
			.build();

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			customSerializer.writeData(Map.of("object", message), output);
		}
		assertTrue(new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1)
			.contains(OverrideMap.class.getName()));
		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> deserializedMap =
				(Map<String, Object>) deserialized.getMetadata().get("custom_map");
		assertEquals("value", deserializedMap.get("visible"));
		assertTrue(deserializedMap.containsKey("empty"));
	}

	@Test
	void shouldPreserveContainerMixInSerializationRules() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.addMixIn(MixInMap.class, TypedContainerMixIn.class);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		MixInMap mixInMap = new MixInMap();
		mixInMap.put("visible", "value");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("custom_map", mixInMap))
			.build();

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			customSerializer.writeData(Map.of("object", message), output);
		}

		assertTrue(new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1)
			.contains(MixInMap.class.getName()));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreserveModifierAssignedJacksonPropertySerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new BeanSerializerModifier() {
			@Override
			public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
					BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
				if (beanDesc.getBeanClass() == ModifiedMetadata.class) {
					beanProperties.stream()
						.filter(property -> property.getName().equals("code"))
						.forEach(property -> property.assignSerializer(ToStringSerializer.instance));
				}
				return beanProperties;
			}
		});
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		ModifiedMetadata metadata = new ModifiedMetadata(42, rawResults);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("modified_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("modified_record");
		assertEquals("42", record.get("code"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldApplyRecordContentInclusionDuringNormalization() throws Exception {
		Map rawResults = new HashMap();
		rawResults.put("visible", Map.of("name", "visible"));
		rawResults.put("empty", null);
		ContentIncludedMetadata metadata = new ContentIncludedMetadata(rawResults);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("included_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("included_record");
		assertEquals(Map.of("visible", Map.of("name", "visible")), record.get("results"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreserveCollectionPositionsWithRecordContentInclusion() throws Exception {
		List rawResults = new ArrayList();
		rawResults.add(Map.of("name", "first"));
		rawResults.add(null);
		rawResults.add(Map.of("name", "third"));
		CollectionContentIncludedMetadata metadata = new CollectionContentIncludedMetadata(rawResults);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("included_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("included_record");
		List<?> results = (List<?>) record.get("results");
		assertEquals(3, results.size());
		assertNull(results.get(1));
		assertEquals(Map.of("name", "third"), results.get(2));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreserveModifierAssignedNullSerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new BeanSerializerModifier() {
			@Override
			public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
					BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
				if (beanDesc.getBeanClass() == NullModifiedMetadata.class) {
					beanProperties.stream()
						.filter(property -> property.getName().equals("nullable"))
						.forEach(property -> property.assignNullSerializer(new RedactingSerializer<>()));
				}
				return beanProperties;
			}
		});
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		NullModifiedMetadata metadata = new NullModifiedMetadata(null, rawResults);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("modified_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("modified_record");
		assertEquals("redacted", record.get("nullable"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizeRecordWhilePreservingComponentRules() throws Exception {
		List rawResults = List.of(Map.of("name", "visible"));
		CombinedRuleMetadata metadata = new CombinedRuleMetadata(rawResults, "secret", null);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("combined_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("combined_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertEquals("redacted", record.get("secret"));
		assertTrue(!record.containsKey("optional"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizeNestedIncompatibleContainers() throws Exception {
		List rawResults = List.of(List.of(Map.of("name", "visible")));
		NestedMetadata metadata = new NestedMetadata(rawResults);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("nested_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("nested_record");
		assertEquals(List.of(List.of(Map.of("name", "visible"))), record.get("results"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizeRecordWithSupportedMixInRules() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.addMixIn(MixInRuleMetadata.class, NonNullRecordMixIn.class);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		MixInRuleMetadata metadata = new MixInRuleMetadata(rawResults, null);
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("mix_in_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("mix_in_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertTrue(!record.containsKey("optional"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldHonorPropertiesRemovedBySerializerModifier() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new BeanSerializerModifier() {
			@Override
			public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
					BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
				if (beanDesc.getBeanClass() == RemovedPropertyMetadata.class) {
					beanProperties.removeIf(property -> property.getName().equals("secret"));
				}
				return beanProperties;
			}
		});
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		RemovedPropertyMetadata metadata = new RemovedPropertyMetadata(rawResults, "secret");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("modified_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("modified_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertTrue(!record.containsKey("secret"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreserveUnwrappedWriterFieldsDuringNormalization() throws Exception {
		List rawResults = List.of(Map.of("name", "visible"));
		UnwrappedMetadata metadata =
				new UnwrappedMetadata(rawResults, new UnwrappedDetails("detail"));
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("unwrapped_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("unwrapped_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertEquals("detail", record.get("detail"));
		assertTrue(!record.containsKey("details"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizeChildrenOfOverriddenContainers() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.addMixIn(NestedOverrideMap.class, TypedContainerMixIn.class);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		NestedOverrideMap values = new NestedOverrideMap();
		values.put("record", new PrivateSearchMetadata(rawResults, "ignored", "secret"));
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("values", values))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> map = (Map<String, Object>) deserialized.getMetadata().get("values");
		assertTrue(map.get("record") instanceof Map);
		assertEquals(List.of(Map.of("name", "visible")),
				((Map<String, Object>) map.get("record")).get("results"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreservePropertiesRenamedBySerializerModifier() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new BeanSerializerModifier() {
			@Override
			public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
					BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
				if (beanDesc.getBeanClass() == RenamedPropertyMetadata.class) {
					return beanProperties.stream()
						.map(property -> property.getName().equals("secret")
								? property.rename(new NameTransformer() {
									@Override
									public String transform(String name) {
										return "redacted";
									}

									@Override
									public String reverse(String transformed) {
										return "redacted".equals(transformed) ? "secret" : null;
									}
								})
								: property)
						.toList();
				}
				return beanProperties;
			}
		});
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		RenamedPropertyMetadata metadata = new RenamedPropertyMetadata(rawResults, "value");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("renamed_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("renamed_record");
		assertEquals("value", record.get("redacted"));
		assertTrue(!record.containsKey("secret"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldKeepNormalizedChildrenWhenOverriddenContainerCannotBeCopied() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.addMixIn(ConstructorMap.class, TypedContainerMixIn.class);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		ConstructorMap values = new ConstructorMap("required");
		values.put("record", new PrivateSearchMetadata(rawResults, "ignored", "secret"));
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("values", values))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> map = (Map<String, Object>) deserialized.getMetadata().get("values");
		assertTrue(map.get("record") instanceof Map);
		assertEquals(List.of(Map.of("name", "visible")),
				((Map<String, Object>) map.get("record")).get("results"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizeChildrenOfOverriddenObjectArrays() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.addMixIn(PrivateSearchMetadata[].class, TypedContainerMixIn.class);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		PrivateSearchMetadata[] values =
				{ new PrivateSearchMetadata(rawResults, "ignored", "secret") };
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("values", values))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		List<?> array = (List<?>) deserialized.getMetadata().get("values");
		assertTrue(array.get(0) instanceof Map);
		assertEquals(List.of(Map.of("name", "visible")),
				((Map<String, Object>) array.get(0)).get("results"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreserveAnyGetterFieldsDuringRecordNormalization() throws Exception {
		List rawResults = List.of(Map.of("name", "visible"));
		AnyGetterMetadata metadata =
				new AnyGetterMetadata(rawResults, Map.of("dynamic", "value"));
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("any_getter_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("any_getter_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertEquals("value", record.get("dynamic"));
		assertTrue(!record.containsKey("additional"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreservePropertyTypeInfoDuringRecordNormalization() throws Exception {
		List rawResults = List.of(Map.of("name", "visible"));
		TypeInfoMetadata metadata =
				new TypeInfoMetadata(rawResults, new SecretValue("value"));
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("type_info_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("type_info_record");
		SecretValue typedValue = (SecretValue) record.get("typedValue");
		assertEquals("value", typedValue.value());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldApplyComponentCustomAndDefaultInclusionDuringNormalization() throws Exception {
		List rawResults = List.of(Map.of("name", "visible"));
		ComponentIncludedMetadata metadata =
				new ComponentIncludedMetadata(rawResults, 0, "hidden");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("included_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("included_record");
		assertEquals(List.of(Map.of("name", "visible")), record.get("results"));
		assertTrue(!record.containsKey("defaultCode"));
		assertTrue(!record.containsKey("filteredValue"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldPreservePropertiesAddedBySerializerModifier() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(new BeanSerializerModifier() {
			@Override
			public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
					BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
				if (beanDesc.getBeanClass() == RenamedPropertyMetadata.class) {
					BeanPropertyWriter source = beanProperties.stream()
						.filter(property -> property.getName().equals("secret"))
						.findFirst()
						.orElseThrow();
					beanProperties.add(source.rename(new NameTransformer() {
						@Override
						public String transform(String name) {
							return "computed";
						}

						@Override
						public String reverse(String transformed) {
							return "computed".equals(transformed) ? "secret" : null;
						}
					}));
				}
				return beanProperties;
			}
		});
		objectMapper.registerModule(module);
		SpringAIJacksonStateSerializer customSerializer =
				new SpringAIJacksonStateSerializer(OverAllState::new, objectMapper);
		List rawResults = List.of(Map.of("name", "visible"));
		RenamedPropertyMetadata metadata = new RenamedPropertyMetadata(rawResults, "value");
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("modified_record", metadata))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message, customSerializer);

		Map<String, Object> record =
				(Map<String, Object>) deserialized.getMetadata().get("modified_record");
		assertEquals("value", record.get("secret"));
		assertEquals("value", record.get("computed"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void shouldNormalizeGenericStandardContainerSubclasses() throws Exception {
		SearchResultList results = new SearchResultList();
		((List) results).add(Map.of("name", "visible"));
		AssistantMessage message = AssistantMessage.builder()
			.content("result")
			.properties(Map.of("generic_results", results))
			.build();

		AssistantMessage deserialized = serializeAndDeserialize(message);

		assertEquals(List.of(Map.of("name", "visible")),
				deserialized.getMetadata().get("generic_results"));
	}

	private record PrivateMetadata(@JsonProperty("visible_name") String visible,
			@JsonIgnore String ignored,
			@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String secret) {
	}

	private record PrivateSearchMetadata(@JsonProperty("results") List<SearchResultMetadata> results,
			@JsonIgnore String ignored,
			@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String secret) {
	}

	private record SearchResultMetadata(String name) {
	}

	private record PrivateMapMetadata(Map<String, SearchResultMetadata> results) {
	}

	private record ReferenceMetadata(List<SearchResultMetadata> results, AtomicReference<String> reference) {
	}

	private record ModifiedMetadata(Integer code, List<SearchResultMetadata> results) {
	}

	@JsonInclude(content = JsonInclude.Include.NON_NULL)
	private record ContentIncludedMetadata(Map<String, SearchResultMetadata> results) {
	}

	@JsonInclude(content = JsonInclude.Include.NON_NULL)
	private record CollectionContentIncludedMetadata(List<SearchResultMetadata> results) {
	}

	private record NullModifiedMetadata(String nullable, List<SearchResultMetadata> results) {
	}

	private record CombinedRuleMetadata(List<SearchResultMetadata> results,
			@JsonSerialize(using = RedactingSerializer.class) String secret,
			@JsonInclude(JsonInclude.Include.NON_NULL) String optional) {
	}

	private record NestedMetadata(List<List<SearchResultMetadata>> results) {
	}

	private record MixInRuleMetadata(List<SearchResultMetadata> results, String optional) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private abstract static class NonNullRecordMixIn {
	}

	private record RemovedPropertyMetadata(List<SearchResultMetadata> results, String secret) {
	}

	private record RenamedPropertyMetadata(List<SearchResultMetadata> results, String secret) {
	}

	private record UnwrappedMetadata(List<SearchResultMetadata> results,
			@JsonUnwrapped UnwrappedDetails details) {
	}

	private record UnwrappedDetails(String detail) {
	}

	private record AnyGetterMetadata(List<SearchResultMetadata> results,
			@JsonAnyGetter Map<String, Object> additional) {
	}

	private record TypeInfoMetadata(List<SearchResultMetadata> results,
			@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) SecretValue typedValue) {
	}

	private record ComponentIncludedMetadata(List<SearchResultMetadata> results,
			@JsonInclude(JsonInclude.Include.NON_DEFAULT) int defaultCode,
			@JsonInclude(value = JsonInclude.Include.CUSTOM,
					valueFilter = HiddenValueFilter.class) String filteredValue) {
	}

	private record RedactedMetadata(String value) {
	}

	private record ComponentMetadata(
			@JsonSerialize(using = RedactingSerializer.class) String value, List<String> items) {
	}

	private record SecretValue(String value) {
	}

	private record IncludedMetadata(String visible,
			@JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> emptyItems) {
	}

	@JsonFilter("metadataFilter")
	private record FilteredMetadata(String visible, String secret) {
	}

	private record MapperIncludedMetadata(String visible, List<String> emptyItems) {
	}

	private static final class SecretMap extends HashMap<String, Object> {
	}

	private static final class SecretList extends ArrayList<String> {
	}

	@JsonSerialize(contentUsing = RedactingSerializer.class)
	private static final class SecretAnnotatedMap extends HashMap<String, Object> {
	}

	@JsonSerialize(contentUsing = RedactingSerializer.class)
	private static final class SecretAnnotatedList extends ArrayList<String> {
	}

	@JsonSerialize(keyUsing = RedactingKeySerializer.class)
	private static final class SecretKeyMap extends HashMap<String, Object> {
	}

	private static final class OverrideMap extends HashMap<String, Object> {
	}

	private static final class MixInMap extends HashMap<String, Object> {
	}

	private static final class FormatList extends ArrayList<String> {
	}

	private static final class NestedOverrideMap extends HashMap<String, Object> {
	}

	private static final class ConstructorMap extends HashMap<String, Object> {

		private ConstructorMap(String required) {
		}

	}

	private static final class SearchResultList extends ArrayList<SearchResultMetadata> {
	}

	private static final class HiddenValueFilter {

		@Override
		public boolean equals(Object value) {
			return "hidden".equals(value);
		}

	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
	private abstract static class TypedContainerMixIn {
	}

	private static final class RedactingSerializer<T> extends JsonSerializer<T> {

		@Override
		public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeString("redacted");
		}

		@Override
		public void serializeWithType(T value, JsonGenerator gen, SerializerProvider serializers,
				TypeSerializer typeSerializer) throws IOException {
			serialize(value, gen, serializers);
		}

	}

	private static final class RedactingKeySerializer extends JsonSerializer<Object> {

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeFieldName("redacted-key");
		}

	}

	private <T> T serializeAndDeserialize(T object) throws IOException, ClassNotFoundException {
		return serializeAndDeserialize(object, this.serializer);
	}

	private <T> T serializeAndDeserialize(T object, SpringAIJacksonStateSerializer stateSerializer)
			throws IOException, ClassNotFoundException {
		// 将对象包装在Map中进行序列化
		Map<String, Object> data = new HashMap<>();
		data.put("object", object);

		// 序列化
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		stateSerializer.writeData(data, oos);
		oos.flush();

		// 反序列化
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = stateSerializer.readData(ois);

		// 返回反序列化的对象
		@SuppressWarnings("unchecked")
		T result = (T) deserializedData.get("object");
		return result;
	}


	@Test
	void testNoDoubleClassField() throws Exception {

		AssistantMessage assistantMsg = AssistantMessage.builder()
			.content("test response")
			.properties(Map.of("key", "value"))
			.build();
		verifyNoDuplicateClassField(assistantMsg, "AssistantMessage", 1);

		SystemMessage systemMsg = SystemMessage.builder()
			.text("system prompt")
			.metadata(Map.of("source", "test"))
			.build();
		verifyNoDuplicateClassField(systemMsg, "SystemMessage", 1);

		UserMessage userMsg = UserMessage.builder()
			.text("user query")
			.metadata(Map.of("user_id", "123"))
			.build();
		verifyNoDuplicateClassField(userMsg, "UserMessage", 1);

		Document doc = Document.builder()
			.id("doc_001")
			.text("document content")
			.metadata(Map.of("type", "pdf"))
			.score(0.95)
			.build();
		verifyNoDuplicateClassField(doc, "Document", 1);

		List<ToolResponseMessage.ToolResponse> responses = List.of(
			new ToolResponseMessage.ToolResponse("call_1", "tool1", "{\"result\": 1}")
		);
		ToolResponseMessage toolMsg = ToolResponseMessage.builder()
			.responses(responses)
			.metadata(Map.of("tool", "test"))
			.build();
		verifyNoDuplicateClassField(toolMsg, "ToolResponseMessage", 1);
	}


	private void verifyNoDuplicateClassField(Object object, String objectType, int expectedClassCount) throws IOException {
		Map<String, Object> data = new HashMap<>();
		data.put("object", object);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		String serializedContent = baos.toString();
		int actualClassCount = countOccurrences(serializedContent, "\"@class\"");
		assertEquals(expectedClassCount, actualClassCount,
			String.format("%s serialization should contain exactly %d @class field(s), but found: %d. " +
				"If the actual count is double the expected count, this indicates Bug #3895 (duplicate @class fields). " +
				"If the count is less than expected, the serialization may have issues with type information.\n" +
				"Serialized content:\n%s",
				objectType, expectedClassCount, actualClassCount, serializedContent));

		verifyNoIdenticalClassValues(serializedContent, objectType);
	}


	private void verifyNoIdenticalClassValues(String serializedContent, String objectType) {
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"@class\":\"([^\"]+)\"");
		java.util.regex.Matcher matcher = pattern.matcher(serializedContent);

		java.util.Map<String, Integer> classValueCounts = new java.util.HashMap<>();
		while (matcher.find()) {
			String className = matcher.group(1);
			classValueCounts.put(className, classValueCounts.getOrDefault(className, 0) + 1);
		}
		for (java.util.Map.Entry<String, Integer> entry : classValueCounts.entrySet()) {
			if (!entry.getKey().contains("java.util") && entry.getValue() > 1) {
				fail(String.format(
					"%s serialization contains duplicate @class value: \"%s\" appears %d times. " +
					"This indicates .\nClass value counts: %s\nSerialized content:\n%s",
					objectType, entry.getKey(), entry.getValue(), classValueCounts, serializedContent));
			}
		}
	}


	private int countOccurrences(String text, String substring) {
		int count = 0;
		int index = 0;
		while ((index = text.indexOf(substring, index)) != -1) {
			count++;
			index += substring.length();
		}
		return count;
	}

	public static class Plan {

		private List<PlanStep> steps;

		@SuppressWarnings("unused")
		public Plan() {
		}

		public Plan(List<PlanStep> steps) {
			this.steps = steps;
		}

		public List<PlanStep> steps() {
			return steps;
		}

	}

	public static final class PlanStep {

		private String title;

		@SuppressWarnings("unused")
		public PlanStep() {
		}

		public PlanStep(String title) {
			this.title = title;
		}

		public String title() {
			return title;
		}

	}

}
