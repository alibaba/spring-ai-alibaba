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

import org.springframework.ai.chat.messages.SystemMessage;

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

public interface SystemMessageHandler {

	enum Field {

		TEXT("text");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<SystemMessage> {

		public Serializer() {
			super(SystemMessage.class);
		}

		@Override
		public void serialize(SystemMessage msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStartObject();
			serializeFields(msg, gen, provider);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(SystemMessage msg, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer) throws tools.jackson.core.JacksonException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
					typeSer.typeId(msg, JsonToken.START_OBJECT));
			serializeFields(msg, gen, provider);
			typeSer.writeTypeSuffix(gen, provider, typeIdDef);
		}

		private void serializeFields(SystemMessage msg, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStringProperty(Field.TEXT.name, msg.getText());
			serializeMetadata(gen, provider, msg.getMetadata());
		}
	}

	class Deserializer extends StdDeserializer<SystemMessage> {

		protected Deserializer() {
			super(SystemMessage.class);
		}

		@Override
		public SystemMessage deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws tools.jackson.core.JacksonException {
			ObjectReadContext readContext = jsonParser.objectReadContext();
			ObjectNode node = (ObjectNode) ctxt.readTree(jsonParser);

			var text = node.get(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(readContext, node);

			return SystemMessage.builder().text(text).metadata(metadata).build();
		}

	}

}
