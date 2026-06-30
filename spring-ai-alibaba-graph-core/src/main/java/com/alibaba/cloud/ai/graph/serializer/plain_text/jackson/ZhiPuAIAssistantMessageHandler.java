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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.deserializeMetadata;
import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.serializeMetadata;

public interface ZhiPuAIAssistantMessageHandler {

	String MESSAGE_CLASS_NAME = "org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage";

	String BUILDER_CLASS_NAME = MESSAGE_CLASS_NAME + "$Builder";

	enum Field {

		TEXT("text"), TOOL_CALLS("toolCalls"), REASONING_CONTENT("reasoningContent"), MEDIA("media");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	@SuppressWarnings("unchecked")
	static void registerTo(SimpleModule module, Class<?> zhiPuAIClass) {
		Class<Object> messageClass = (Class<Object>) zhiPuAIClass;
		module.addSerializer(messageClass, new Serializer(messageClass));
		module.addDeserializer(messageClass, new Deserializer(messageClass));
	}

	class Serializer extends StdSerializer<Object> {

		Serializer(Class<Object> zhiPuAIClass) {
			super(zhiPuAIClass);
		}

		@Override
		public void serialize(Object msg, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			serializeFields(msg, gen);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(Object msg, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
				throws IOException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(msg, JsonToken.START_OBJECT));
			serializeFields(msg, gen);
			typeSer.writeTypeSuffix(gen, typeIdDef);
		}

		@SuppressWarnings("unchecked")
		private void serializeFields(Object msg, JsonGenerator gen) throws IOException {
			String text = (String) invoke(msg, "getText");
			gen.writeStringField(Field.TEXT.name, text);

			List<AssistantMessage.ToolCall> toolCalls = (List<AssistantMessage.ToolCall>) invoke(msg, "getToolCalls");
			gen.writeArrayFieldStart(Field.TOOL_CALLS.name);
			for (var toolCall : toolCalls) {
				gen.writeStartObject();
				gen.writeStringField("id", toolCall.id());
				gen.writeStringField("name", toolCall.name());
				gen.writeStringField("type", toolCall.type());
				gen.writeStringField("arguments", toolCall.arguments());
				gen.writeEndObject();
			}
			gen.writeEndArray();

			String reasoningContent = (String) invoke(msg, "getReasoningContent");
			if (reasoningContent != null) {
				gen.writeStringField(Field.REASONING_CONTENT.name, reasoningContent);
			}
			else {
				gen.writeNullField(Field.REASONING_CONTENT.name);
			}

			Map<String, Object> metadata = (Map<String, Object>) invoke(msg, "getMetadata");
			serializeMetadata(gen, metadata);
		}

	}

	class Deserializer extends StdDeserializer<Object> {

		private final Class<?> zhiPuAIClass;

		Deserializer(Class<?> zhiPuAIClass) {
			super(zhiPuAIClass);
			this.zhiPuAIClass = zhiPuAIClass;
		}

		@Override
		public Object deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var textNode = node.get(Field.TEXT.name);
			var text = (textNode != null && !textNode.isNull()) ? textNode.asText() : "";
			var metadata = deserializeMetadata(mapper, node);
			var requestsNode = node.get(Field.TOOL_CALLS.name);

			var reasoningContentNode = node.get(Field.REASONING_CONTENT.name);
			var reasoningContent = (reasoningContentNode != null && !reasoningContentNode.isNull())
					? reasoningContentNode.asText() : null;

			var requests = new LinkedList<AssistantMessage.ToolCall>();
			if (requestsNode != null && !requestsNode.isNull() && !requestsNode.isEmpty()) {
				for (JsonNode requestNode : requestsNode) {
					var request = mapper.treeToValue(requestNode, AssistantMessage.ToolCall.class);
					requests.add(request);
				}
			}

			return buildMessage(this.zhiPuAIClass, text, metadata, requests, reasoningContent);
		}

	}

	private static Object buildMessage(Class<?> zhiPuAIClass, String text, Map<String, Object> metadata,
			List<AssistantMessage.ToolCall> toolCalls, String reasoningContent) throws IOException {
		try {
			Class<?> builderClass = Class.forName(BUILDER_CLASS_NAME, true, zhiPuAIClass.getClassLoader());
			Object builder = builderClass.getDeclaredConstructor().newInstance();
			invokeBuilder(builderClass, builder, "content", String.class, text);
			invokeBuilder(builderClass, builder, "reasoningContent", String.class, reasoningContent);
			invokeBuilder(builderClass, builder, "properties", Map.class, metadata);
			invokeBuilder(builderClass, builder, "toolCalls", List.class, toolCalls);
			return builderClass.getMethod("build").invoke(builder);
		}
		catch (ReflectiveOperationException e) {
			throw new IOException("Failed to construct ZhiPuAiAssistantMessage", e);
		}
	}

	private static Object invoke(Object target, String methodName) throws IOException {
		try {
			return target.getClass().getMethod(methodName).invoke(target);
		}
		catch (IllegalAccessException | NoSuchMethodException e) {
			throw new IOException("Failed to read ZhiPuAiAssistantMessage." + methodName + "()", e);
		}
		catch (InvocationTargetException e) {
			throw new IOException("Failed to read ZhiPuAiAssistantMessage." + methodName + "()", e.getCause());
		}
	}

	private static void invokeBuilder(Class<?> builderClass, Object builder, String methodName, Class<?> parameterType,
			Object value) throws ReflectiveOperationException {
		builderClass.getMethod(methodName, parameterType).invoke(builder, value);
	}

}
