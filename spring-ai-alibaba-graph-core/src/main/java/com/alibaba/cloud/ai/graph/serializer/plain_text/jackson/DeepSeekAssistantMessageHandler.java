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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.deserializeMetadata;
import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.serializeMetadata;

public interface DeepSeekAssistantMessageHandler {

	enum Field {

		TEXT("text"), TOOL_CALLS("toolCalls"), REASONING_CONTENT("reasoningContent"), MEDIA("media");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<Object> {

		private final Class<?> deepSeekClass;

		public Serializer() {
			super(Object.class);
			try {
				this.deepSeekClass = Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("DeepSeekAssistantMessage class not found", e);
			}
		}

		@Override
		public void serialize(Object msg, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			if (!deepSeekClass.isInstance(msg)) {
				throw new IllegalArgumentException("Expected DeepSeekAssistantMessage instance");
			}

			try {
				gen.writeStartObject();
				gen.writeStringField("@class", msg.getClass().getName());

				// Use reflection to call getText()
				Method getTextMethod = deepSeekClass.getMethod("getText");
				String text = (String) getTextMethod.invoke(msg);
				gen.writeStringField(Field.TEXT.name, text);

				// Use reflection to call getToolCalls()
				Method getToolCallsMethod = deepSeekClass.getMethod("getToolCalls");
				@SuppressWarnings("unchecked")
				java.util.List<AssistantMessage.ToolCall> toolCalls = (java.util.List<AssistantMessage.ToolCall>) getToolCallsMethod.invoke(msg);

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

				// Use reflection to call getReasoningContent()
				Method getReasoningContentMethod = deepSeekClass.getMethod("getReasoningContent");
				String reasoningContent = (String) getReasoningContentMethod.invoke(msg);

				if (reasoningContent != null) {
					gen.writeStringField(Field.REASONING_CONTENT.name, reasoningContent);
				}
				else {
					gen.writeNullField(Field.REASONING_CONTENT.name);
				}

				// Use reflection to call getMetadata()
				Method getMetadataMethod = deepSeekClass.getMethod("getMetadata");
				@SuppressWarnings("unchecked")
				java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) getMetadataMethod.invoke(msg);
				serializeMetadata(gen, metadata);

				gen.writeEndObject();
			}
			catch (Exception e) {
				if (e instanceof IOException) {
					throw (IOException) e;
				}
				throw new IOException("Failed to serialize DeepSeekAssistantMessage", e);
			}
		}

		@Override
		public void serializeWithType(Object value, JsonGenerator gen,
				SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
			serialize(value, gen, serializers);
		}

	}

	class Deserializer extends StdDeserializer<Object> {

		private final Class<?> deepSeekClass;
		private final Constructor<?> constructor;

		protected Deserializer() {
			super(Object.class);
			try {
				this.deepSeekClass = Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
				// Find constructor: DeepSeekAssistantMessage(String text, String reasoningContent, Map<String, Object> metadata, List<ToolCall> toolCalls)
				this.constructor = deepSeekClass.getConstructor(String.class, String.class, 
					java.util.Map.class, java.util.List.class);
			}
			catch (ClassNotFoundException | NoSuchMethodException e) {
				throw new IllegalStateException("DeepSeekAssistantMessage class or constructor not found", e);
			}
		}

		@Override
		public Object deserialize(JsonParser jsonParser, DeserializationContext ctx)
				throws IOException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var text = node.findValue(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(mapper, node);
			var requestsNode = node.findValue(Field.TOOL_CALLS.name);

			var reasoningContentNode = node.findValue(Field.REASONING_CONTENT.name);
			var reasoningContent = (reasoningContentNode != null && !reasoningContentNode.isNull())
					? reasoningContentNode.asText() : null;

			var requests = new LinkedList<AssistantMessage.ToolCall>();

			if (requestsNode != null && !requestsNode.isNull() && !requestsNode.isEmpty()) {
				for (JsonNode requestNode : requestsNode) {
					var request = mapper.treeToValue(requestNode, new TypeReference<AssistantMessage.ToolCall>() {
					});
					requests.add(request);
				}
			}

			try {
				return constructor.newInstance(text, reasoningContent, metadata, requests);
			}
			catch (Exception e) {
				throw new IOException("Failed to deserialize DeepSeekAssistantMessage", e);
			}
		}

	}

}

