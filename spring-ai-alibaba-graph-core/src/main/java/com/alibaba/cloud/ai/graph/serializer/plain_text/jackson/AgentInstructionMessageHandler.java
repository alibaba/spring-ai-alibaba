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

import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

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

public interface AgentInstructionMessageHandler {

	enum Field {

		TEXT("text"),
		RENDERED("rendered");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<AgentInstructionMessage> {

		public Serializer() {
			super(AgentInstructionMessage.class);
		}

		@Override
		public void serialize(AgentInstructionMessage msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStartObject();
			serializeFields(msg, gen, provider);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(AgentInstructionMessage msg, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer) throws tools.jackson.core.JacksonException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
					typeSer.typeId(msg, JsonToken.START_OBJECT));
			serializeFields(msg, gen, provider);
			typeSer.writeTypeSuffix(gen, provider, typeIdDef);
		}

		private void serializeFields(AgentInstructionMessage msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStringProperty(Field.TEXT.name, msg.getText());
			gen.writeBooleanProperty(Field.RENDERED.name, msg.isRendered());
			serializeMetadata(gen, provider, msg.getMetadata());
		}
	}

	class Deserializer extends StdDeserializer<AgentInstructionMessage> {

		public Deserializer() {
			super(AgentInstructionMessage.class);
		}

		@Override
		public AgentInstructionMessage deserialize(JsonParser jsonParser, DeserializationContext ctx) throws tools.jackson.core.JacksonException {
			ObjectReadContext readContext = jsonParser.objectReadContext();
			ObjectNode node = (ObjectNode) ctx.readTree(jsonParser);

			var text = node.findValue(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(readContext, node);
			var renderedNode = node.findValue(Field.RENDERED.name);
			var rendered = renderedNode != null && renderedNode.asBoolean();

			return AgentInstructionMessage.builder().text(text).metadata(metadata).rendered(rendered).build();

		}

	}

}
