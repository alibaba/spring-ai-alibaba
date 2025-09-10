package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.util.LinkedList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

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
		public void serialize(AssistantMessage msg, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("@type", msg.getMessageType().name());
			gen.writeStringField(Field.TEXT.name, msg.getText());
			gen.writeObjectField(Field.TOOL_CALLS.name, msg.getToolCalls());

			// gen.writeArrayFieldStart(Field.TOOL_CALLS.name);
			// for( var toolCall : msg.getToolCalls() )
			// gen.writeObject( toolCall );
			// gen.writeEndArray();

			serializeMetadata(gen, msg.getMetadata());

			// gen.writeArrayFieldStart( Property.MEDIA.field);
			// for (var media : msg.getMedia()) {
			// gen.writeObject(media);
			// }
			// gen.writeEndArray();

			gen.writeEndObject();
		}

	}

	class Deserializer extends StdDeserializer<AssistantMessage> {

		protected Deserializer() {
			super(AssistantMessage.class);
		}

		@Override
		public AssistantMessage deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
			var mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			var text = node.findValue(Field.TEXT.name).asText();
			var metadata = deserializeMetadata(mapper, node);
			var requestsNode = node.findValue(Field.TOOL_CALLS.name);

			if (requestsNode.isNull() || requestsNode.isEmpty()) {
				return new AssistantMessage(text, metadata);
			}

			var requests = new LinkedList<AssistantMessage.ToolCall>();

			for (JsonNode requestNode : requestsNode) {
				var request = mapper.treeToValue(requestNode, new TypeReference<AssistantMessage.ToolCall>() {
				});

				requests.add(request);
			}

			return new AssistantMessage(text, metadata, requests);
		}

	}

}
