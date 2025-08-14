package com.alibaba.cloud.ai.studio.runtime.domain.chat;

import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

/**
 * Custom deserializer for chat message content. Handles different types of content:
 * arrays, text, and other JSON nodes.
 */
public class ChatMessageContentDeserializer extends JsonDeserializer<Object> {

	/**
	 * Deserializes JSON content into appropriate object type.
	 * @param p JSON parser
	 * @param ctxt Deserialization context
	 * @return Deserialized object (List<MultimodalContent>, String, or JsonNode)
	 * @throws IOException if deserialization fails
	 */
	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonNode node = p.getCodec().readTree(p);

		if (node.isArray()) {
			return JsonUtils.getObjectMapper().convertValue(node, new TypeReference<List<MultimodalContent>>() {
			});
		}
		else if (node.isTextual()) {
			return node.asText();
		}
		else {
			return node;
		}
	}

}
