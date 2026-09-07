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

import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;

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
		public void serialize(Document document, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStartObject();
			serializeFields(document, gen, provider);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(Document document, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer) throws tools.jackson.core.JacksonException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
					typeSer.typeId(document, JsonToken.START_OBJECT));
			serializeFields(document, gen, provider);
			typeSer.writeTypeSuffix(gen, provider, typeIdDef);
		}

		private void serializeFields(Document document, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
			gen.writeStringProperty(Field.ID.name, document.getId());
			gen.writeStringProperty(Field.TEXT.name, document.getText());

			// Serialize media field
			if (document.getMedia() != null) {
				provider.defaultSerializeProperty(Field.MEDIA.name, document.getMedia(), gen);
			}

			// Serialize score field
			if (document.getScore() != null) {
				gen.writeNumberProperty(Field.SCORE.name, document.getScore());
			}
			serializeMetadata(gen, provider, document.getMetadata());
		}
	}

	class Deserializer extends StdDeserializer<Document> {

		protected Deserializer() {
			super(Document.class);
		}

		@Override
		public Document deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws tools.jackson.core.JacksonException {
			ObjectReadContext readContext = jsonParser.objectReadContext();
			ObjectNode node = (ObjectNode) ctxt.readTree(jsonParser);

			var id = node.get(Field.ID.name).asText();
			var text = node.get(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(readContext, node);

			// Deserialize media field
			Media media = null;
			if (node.has(Field.MEDIA.name) && !node.get(Field.MEDIA.name).isNull()) {
				media = readContext.readValue(readContext.treeAsTokens(node.get(Field.MEDIA.name)), Media.class);
			}

			// Deserialize score field
			Double score = null;
			if (node.has(Field.SCORE.name) && !node.get(Field.SCORE.name).isNull()) {
				score = node.get(Field.SCORE.name).asDouble();
			}
			return Document.builder().id(id).text(text).media(media).metadata(metadata).score(score).build();
		}

	}

}
