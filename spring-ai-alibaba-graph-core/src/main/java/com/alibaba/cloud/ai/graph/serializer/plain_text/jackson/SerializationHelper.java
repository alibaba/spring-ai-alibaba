package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SerializationHelper {

	static final String METADATA_FIELD = "metadata";

	static Map<String, Object> deserializeMetadata(ObjectMapper mapper, JsonNode parentNode)
			throws JsonProcessingException {
		var node = parentNode.findValue(METADATA_FIELD);

		if (node.isNull() || node.isEmpty()) {
			return Map.of();
		}
		if (!node.isObject()) {
			throw new IllegalStateException("Metadata must be an object");
		}
		return mapper.treeToValue(node, new TypeReference<>() {
		});
	}

	static void serializeMetadata(JsonGenerator gen, Map<String, Object> metadata) throws IOException {
		gen.writeObjectField(METADATA_FIELD, metadata);
	}

}
