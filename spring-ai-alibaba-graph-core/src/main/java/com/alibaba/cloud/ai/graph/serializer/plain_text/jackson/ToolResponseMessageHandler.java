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

import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.deserializeMetadata;
import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.serializeMetadata;

public interface ToolResponseMessageHandler {

	enum Field {

		RESPONSES("responses");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<ToolResponseMessage> {

		public Serializer() {
			super(ToolResponseMessage.class);
		}

		@Override
		public void serialize(ToolResponseMessage msg, JsonGenerator gen, SerializationContext provider)
				throws JacksonException {
			gen.writeStartObject();
			serializeFields(msg, gen, provider);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(ToolResponseMessage msg, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer)
				throws JacksonException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
					typeSer.typeId(msg, JsonToken.START_OBJECT));
			serializeFields(msg, gen, provider);
			typeSer.writeTypeSuffix(gen, provider, typeIdDef);
		}

		private void serializeFields(ToolResponseMessage msg, JsonGenerator gen, SerializationContext provider) throws JacksonException {
			gen.writeStringProperty(AssistantMessageHandler.Field.TEXT.name, msg.getText());

			gen.writeArrayPropertyStart(Field.RESPONSES.name);
			for(var response : msg.getResponses()) {
				 gen.writeStartObject();
				 gen.writeStringProperty("id", response.id());
				 gen.writeStringProperty("name", response.name());
				 gen.writeStringProperty("responseData", response.responseData());
				 gen.writeEndObject();
			}
			gen.writeEndArray();

			serializeMetadata(gen, provider, msg.getMetadata());
		}
	}

	class Deserializer extends StdDeserializer<ToolResponseMessage> {

		public Deserializer() {
			super(ToolResponseMessage.class);
		}

		@Override
		public ToolResponseMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
				throws JacksonException {
			ObjectReadContext readContext = jsonParser.objectReadContext();
			ObjectNode node = (ObjectNode) deserializationContext.readTree(jsonParser);

			var responsesNode = node.findValue(Field.RESPONSES.name);
			var metadata = deserializeMetadata(readContext, node);

			if (responsesNode.isNull() || responsesNode.isEmpty()) {
				return ToolResponseMessage.builder()
						.metadata(metadata)
						.build();
			}

			var responses = new ArrayList<ToolResponseMessage.ToolResponse>(responsesNode.size());
			for (var responseNode : responsesNode) {
				responses.add(readContext.readValue(readContext.treeAsTokens(responseNode),
						ToolResponseMessage.ToolResponse.class));
			}

			return ToolResponseMessage.builder()
					.responses(responses)
					.metadata(metadata)
					.build();
		}

	}

}
