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

import java.util.LinkedList;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;

import org.springframework.ai.chat.messages.AssistantMessage;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.deserializeMetadata;
import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.serializeMetadata;

public interface AssistantMessageHandler {

	enum Field {

		TEXT("text"), TOOL_CALLS("toolCalls"), MEDIA("media");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<AssistantMessage> {

		public Serializer() {
			super(AssistantMessage.class);
		}

	@Override
	public void serialize(AssistantMessage msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
		gen.writeStartObject();
		serializeFields(msg, gen, provider);
		gen.writeEndObject();
	}

	@Override
	public void serializeWithType(AssistantMessage msg, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer) throws tools.jackson.core.JacksonException {
		WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
				typeSer.typeId(msg, JsonToken.START_OBJECT));
		serializeFields(msg, gen, provider);
		typeSer.writeTypeSuffix(gen, provider, typeIdDef);
	}

	private void serializeFields(AssistantMessage msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
		gen.writeStringProperty(Field.TEXT.name, msg.getText());

		gen.writeArrayPropertyStart(Field.TOOL_CALLS.name);
		for (var toolCall : msg.getToolCalls()) {
			gen.writeStartObject();
			gen.writeStringProperty("id", toolCall.id());
			gen.writeStringProperty("name", toolCall.name());
			gen.writeStringProperty("type", toolCall.type());
			gen.writeStringProperty("arguments", toolCall.arguments());
			gen.writeEndObject();
		}
		gen.writeEndArray();

		serializeMetadata(gen, provider, msg.getMetadata());

		// gen.writeArrayFieldStart( Property.MEDIA.field);
		// for (var media : msg.getMedia()) {
		// gen.writeObject(media);
		// }
		// gen.writeEndArray();
	}
}

	class Deserializer extends StdDeserializer<AssistantMessage> {

		protected Deserializer() {
			super(AssistantMessage.class);
		}

		@Override
		public AssistantMessage deserialize(JsonParser jsonParser, DeserializationContext ctx) throws tools.jackson.core.JacksonException {
			ObjectReadContext readContext = jsonParser.objectReadContext();
			ObjectNode node = (ObjectNode) ctx.readTree(jsonParser);

			var text = node.findValue(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(readContext, node);
			var requestsNode = node.findValue(Field.TOOL_CALLS.name);

			if (requestsNode.isNull() || requestsNode.isEmpty()) {
				return AssistantMessage.builder()
						.content(text)
						.properties(metadata)
						.build();
			}

			var requests = new LinkedList<AssistantMessage.ToolCall>();

			for (JsonNode requestNode : requestsNode) {
				var request = readContext.readValue(readContext.treeAsTokens(requestNode),
						new TypeReference<AssistantMessage.ToolCall>() {
				});

				requests.add(request);
			}

			return AssistantMessage.builder()
					.content(text)
					.properties(metadata)
					.toolCalls(requests)
					.build();
		}

	}

}
