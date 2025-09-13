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

import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;

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

public interface DocumentHandler {

	enum Field {

		ID("id"), TEXT("text"), MEDIA("media"), SCORE("score");

		final String name;

		Field(String name) {
			this.name = name;
		}

	}

	class Serializer extends StdSerializer<Document> {

		public Serializer() {
			super(Document.class);
		}

		@Override
		public void serialize(Document document, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("@type", "DOCUMENT");
			gen.writeStringField(Field.ID.name, document.getId());
			gen.writeStringField(Field.TEXT.name, document.getText());

			// 序列化 media 字段
			if (document.getMedia() != null) {
				gen.writeObjectField(Field.MEDIA.name, document.getMedia());
			}

			// 序列化 score 字段
			if (document.getScore() != null) {
				gen.writeNumberField(Field.SCORE.name, document.getScore());
			}

			serializeMetadata(gen, document.getMetadata());
			gen.writeEndObject();
		}

	}

	class Deserializer extends StdDeserializer<Document> {

		protected Deserializer() {
			super(Document.class);
		}

		@Override
		public Document deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var id = node.get(Field.ID.name).asText();
			var text = node.get(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(mapper, node);

			// 反序列化 media 字段
			Media media = null;
			if (node.has(Field.MEDIA.name) && !node.get(Field.MEDIA.name).isNull()) {
				media = mapper.treeToValue(node.get(Field.MEDIA.name), Media.class);
			}

			// 反序列化 score 字段
			Double score = null;
			if (node.has(Field.SCORE.name) && !node.get(Field.SCORE.name).isNull()) {
				score = node.get(Field.SCORE.name).asDouble();
			}

			return Document.builder().id(id).text(text).media(media).metadata(metadata).score(score).build();
		}

	}

}
