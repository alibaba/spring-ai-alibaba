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

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.deserializeMetadata;
import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SerializationHelper.serializeMetadata;

/**
 * Jackson handlers for user messages, including media with String or byte[] data. Spring
 * AI normalizes URI and Resource media to these types. Other media data types remain
 * omitted during serialization.
 */
public interface UserMessageHandler {

	enum Field {

		TEXT("text"), MEDIA("media"), MIME_TYPE("mimeType"), DATA_TYPE("dataType"), DATA("data"), ID("id"),
		NAME("name");

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
			serializeFields(msg, gen, provider);
			gen.writeEndObject();
		}

		@Override
		public void serializeWithType(UserMessage msg, JsonGenerator gen, SerializerProvider provider,
				TypeSerializer typeSer) throws IOException {
			WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(msg, JsonToken.START_OBJECT));
			serializeFields(msg, gen, provider);
			typeSer.writeTypeSuffix(gen, typeIdDef);
		}

		private void serializeFields(UserMessage msg, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			gen.writeStringField(Field.TEXT.name, msg.getText());
			serializeMetadata(gen, provider, msg.getMetadata());

			List<Media> supportedMedia = msg.getMedia()
				.stream()
				.filter(media -> media.getData() instanceof String || media.getData() instanceof byte[])
				.toList();
			if (!supportedMedia.isEmpty()) {
				gen.writeArrayFieldStart(Field.MEDIA.name);
				for (Media media : supportedMedia) {
					serializeMedia(media, gen);
				}
				gen.writeEndArray();
			}
		}

		private void serializeMedia(Media media, JsonGenerator gen) throws IOException {
			// Binary data is encoded as text in JSON, so retain its type separately.
			gen.writeStartObject();
			gen.writeStringField(Field.MIME_TYPE.name, media.getMimeType().toString());
			gen.writeStringField(Field.ID.name, media.getId());
			gen.writeStringField(Field.NAME.name, media.getName());
			if (media.getData() instanceof byte[] bytes) {
				gen.writeStringField(Field.DATA_TYPE.name, "bytes");
				gen.writeBinaryField(Field.DATA.name, bytes);
			}
			else {
				gen.writeStringField(Field.DATA_TYPE.name, "string");
				gen.writeStringField(Field.DATA.name, (String) media.getData());
			}
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

			List<Media> media = deserializeMedia(node.get(Field.MEDIA.name), ctx);
			return UserMessage.builder().text(text).metadata(metadata).media(media).build();

		}

		private List<Media> deserializeMedia(JsonNode node, DeserializationContext ctx) throws IOException {
			if (node == null || node.isNull()) {
				return List.of();
			}
			if (!node.isArray()) {
				return ctx.reportInputMismatch(UserMessage.class, "User message media must be an array");
			}
			List<Media> media = new ArrayList<>();
			for (JsonNode item : node) {
				JsonNode data = item.path(Field.DATA.name);
				if (!data.isTextual()) {
					return ctx.reportInputMismatch(UserMessage.class, "User message media data must be a string");
				}
				Object value = switch (item.path(Field.DATA_TYPE.name).asText("string")) {
					case "bytes" -> data.binaryValue();
					case "string" -> data.asText();
					default -> ctx.reportInputMismatch(UserMessage.class, "Unsupported user message media data type");
				};
				media.add(Media.builder()
					.mimeType(MimeTypeUtils.parseMimeType(item.path(Field.MIME_TYPE.name).asText()))
					.data(value)
					.id(item.path(Field.ID.name).asText(null))
					.name(item.path(Field.NAME.name).asText(null))
					.build());
			}
			return media;
		}

	}

}
