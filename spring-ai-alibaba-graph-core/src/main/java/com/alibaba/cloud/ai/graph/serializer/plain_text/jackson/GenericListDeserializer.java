package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

class GenericListDeserializer extends StdDeserializer<List<Object>> {

	final TypeMapper typeMapper;

	public GenericListDeserializer(TypeMapper typeMapper) {
		super(List.class);
		this.typeMapper = Objects.requireNonNull(typeMapper, "typeMapper cannot be null");
	}

	@Override
	public List<Object> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) p.getCodec();
		final ArrayNode node = mapper.readTree(p);

		final List<Object> result = new LinkedList<>();

		for (JsonNode valueNode : node) {

			result.add(JacksonDeserializer.valueFromNode(valueNode, mapper, typeMapper));
		}

		return result;
	}

}
