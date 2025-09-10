package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import org.springframework.ai.chat.messages.SystemMessage;

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
		public void serialize(SystemMessage msg, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("@type", msg.getMessageType().name());
			gen.writeStringField(Field.TEXT.name, msg.getText());
			serializeMetadata(gen, msg.getMetadata());
			gen.writeEndObject();
		}

	}

	class Deserializer extends StdDeserializer<SystemMessage> {

		protected Deserializer() {
			super(SystemMessage.class);
		}

		@Override
		public SystemMessage deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var text = node.get(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(mapper, node);

			return SystemMessage.builder().text(text).metadata(metadata).build();
		}

	}

}
