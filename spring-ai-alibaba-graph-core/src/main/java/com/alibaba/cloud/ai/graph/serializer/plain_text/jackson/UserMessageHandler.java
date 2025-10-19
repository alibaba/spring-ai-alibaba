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

import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.deserializeMetadata;
import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.serializeMetadata;

public interface UserMessageHandler {

	enum Field {

		TEXT("text"), MEDIA("media");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<UserMessage> {

		public Serializer() {
			super(UserMessage.class);
		}

		@Override
		public void serialize(UserMessage msg, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("@type", msg.getMessageType().name());
			gen.writeStringField(Field.TEXT.name, msg.getText());
			serializeMetadata(gen, msg.getMetadata());

			// gen.writeArrayFieldStart( Property.MEDIA.field);
			// for (var media : msg.getMedia()) {
			// gen.writeObject(media);
			// }
			// gen.writeEndArray();

			gen.writeEndObject();
		}

	}

	class Deserializer extends StdDeserializer<UserMessage> {

		public Deserializer() {
			super(UserMessage.class);
		}

		@Override
		public UserMessage deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var text = node.findValue(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(mapper, node);

			return UserMessage.builder().text(text).metadata(metadata).build();

		}

	}

}
