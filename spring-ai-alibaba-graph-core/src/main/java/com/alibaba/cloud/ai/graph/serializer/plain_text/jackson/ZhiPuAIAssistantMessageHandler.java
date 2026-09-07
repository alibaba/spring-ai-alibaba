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

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.AssistantMessage;

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
		public void serialize(Object msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStartObject();
			serializeFields(msg, gen, provider);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(Object msg, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer)
				throws tools.jackson.core.JacksonException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
					typeSer.typeId(msg, JsonToken.START_OBJECT));
			serializeFields(msg, gen, provider);
			typeSer.writeTypeSuffix(gen, provider, typeIdDef);
		}

		@SuppressWarnings("unchecked")
		private void serializeFields(Object msg, JsonGenerator gen, SerializationContext provider)
				throws tools.jackson.core.JacksonException {
			String text = (String) invoke(msg, "getText");
			gen.writeStringProperty(Field.TEXT.name, text);

			List<AssistantMessage.ToolCall> toolCalls = (List<AssistantMessage.ToolCall>) invoke(msg, "getToolCalls");
			gen.writeArrayPropertyStart(Field.TOOL_CALLS.name);
			for (var toolCall : toolCalls) {
				gen.writeStartObject();
				gen.writeStringProperty("id", toolCall.id());
				gen.writeStringProperty("name", toolCall.name());
				gen.writeStringProperty("type", toolCall.type());
				gen.writeStringProperty("arguments", toolCall.arguments());
				gen.writeEndObject();
			}
			gen.writeEndArray();

			String reasoningContent = (String) invoke(msg, "getReasoningContent");
			if (reasoningContent != null) {
				gen.writeStringProperty(Field.REASONING_CONTENT.name, reasoningContent);
			}
			else {
				gen.writeNullProperty(Field.REASONING_CONTENT.name);
			}

			Map<String, Object> metadata = (Map<String, Object>) invoke(msg, "getMetadata");
			serializeMetadata(gen, provider, metadata);
		}

	}

	class Deserializer extends StdDeserializer<Object> {

		private final Class<?> zhiPuAIClass;

		Deserializer(Class<?> zhiPuAIClass) {
			super(zhiPuAIClass);
			this.zhiPuAIClass = zhiPuAIClass;
		}

		@Override
		public Object deserialize(JsonParser jsonParser, DeserializationContext ctx) throws tools.jackson.core.JacksonException {
			ObjectReadContext readContext = jsonParser.objectReadContext();
			ObjectNode node = (ObjectNode) ctx.readTree(jsonParser);

			var textNode = node.get(Field.TEXT.name);
			var text = (textNode != null && !textNode.isNull()) ? textNode.asText() : "";
			var metadata = deserializeMetadata(readContext, node);
			var requestsNode = node.get(Field.TOOL_CALLS.name);

			var reasoningContentNode = node.get(Field.REASONING_CONTENT.name);
			var reasoningContent = (reasoningContentNode != null && !reasoningContentNode.isNull())
					? reasoningContentNode.asText() : null;

			var requests = new LinkedList<AssistantMessage.ToolCall>();
			if (requestsNode != null && !requestsNode.isNull() && !requestsNode.isEmpty()) {
				for (JsonNode requestNode : requestsNode) {
					var request = readContext.readValue(readContext.treeAsTokens(requestNode), AssistantMessage.ToolCall.class);
					requests.add(request);
				}
			}

			return buildMessage(this.zhiPuAIClass, text, metadata, requests, reasoningContent);
		}

	}

	private static Object buildMessage(Class<?> zhiPuAIClass, String text, Map<String, Object> metadata,
			List<AssistantMessage.ToolCall> toolCalls, String reasoningContent) {
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
			throw new IllegalStateException("Failed to construct ZhiPuAiAssistantMessage", e);
		}
	}

	private static Object invoke(Object target, String methodName) {
		try {
			return target.getClass().getMethod(methodName).invoke(target);
		}
		catch (IllegalAccessException | NoSuchMethodException e) {
			throw new IllegalStateException("Failed to read ZhiPuAiAssistantMessage." + methodName + "()", e);
		}
		catch (InvocationTargetException e) {
			throw new IllegalStateException("Failed to read ZhiPuAiAssistantMessage." + methodName + "()", e.getCause());
		}
	}

	private static void invokeBuilder(Class<?> builderClass, Object builder, String methodName, Class<?> parameterType,
			Object value) throws ReflectiveOperationException {
		builderClass.getMethod(methodName, parameterType).invoke(builder, value);
	}

}
