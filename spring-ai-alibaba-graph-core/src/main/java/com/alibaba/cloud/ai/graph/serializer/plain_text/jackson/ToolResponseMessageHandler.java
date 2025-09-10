package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
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
		public void serialize(ToolResponseMessage msg, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			gen.writeStartObject();

			gen.writeStringField("@type", msg.getMessageType().name());
			gen.writeObjectField(Field.RESPONSES.name, msg.getResponses());

			// gen.writeArrayFieldStart( Field.RESPONSES.name );
			// for( var response : msg.getResponses() ) {
			// gen.writeStartObject();
			// gen.writeStringField("id", response.id());
			// gen.writeStringField("name", response.name());
			// gen.writeStringField("responseData", response.responseData());
			// gen.writeEndObject();
			// }
			// gen.writeEndArray();

			serializeMetadata(gen, msg.getMetadata());

			gen.writeEndObject();
		}

	}

	class Deserializer extends StdDeserializer<ToolResponseMessage> {

		public Deserializer() {
			super(ToolResponseMessage.class);
		}

		@Override
		public ToolResponseMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
				throws IOException, JacksonException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var responsesNode = node.findValue(Field.RESPONSES.name);
			var metadata = deserializeMetadata(mapper, node);

			if (responsesNode.isNull() || responsesNode.isEmpty()) {
				return new ToolResponseMessage(List.of(), metadata);
			}

			var responses = new ArrayList<ToolResponseMessage.ToolResponse>(responsesNode.size());
			for (var responseNode : responsesNode) {
				responses.add(mapper.treeToValue(responseNode, ToolResponseMessage.ToolResponse.class));
			}

			return new ToolResponseMessage(responses, metadata);
		}

	}

}
