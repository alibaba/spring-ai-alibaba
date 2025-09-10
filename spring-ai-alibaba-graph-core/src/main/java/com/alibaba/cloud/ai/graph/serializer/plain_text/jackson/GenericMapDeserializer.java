package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

class GenericMapDeserializer extends StdDeserializer<Map<String, Object>> {

	final TypeMapper typeMapper;

	public GenericMapDeserializer(TypeMapper mapper) {
		super(Map.class);
		this.typeMapper = mapper;
	}

	@Override
	public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		var mapper = (ObjectMapper) p.getCodec();
		final ObjectNode node = mapper.readTree(p);

		final Map<String, Object> result = new HashMap<>();

		final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

		while (fields.hasNext()) {
			final var entry = fields.next();

			result.put(entry.getKey(), JacksonDeserializer.valueFromNode(entry.getValue(), mapper, typeMapper));
		}

		return result;
	}

}
