package com.alibaba.cloud.ai.graph.serializer.agent;

import com.alibaba.cloud.ai.graph.state.NodeState;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class StateDeserializer extends JsonDeserializer<NodeState> {

	@Override
	public NodeState deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);

		Map<String, Object> data = new HashMap<>();

		var dataNode = node.has("data") ? node.get("data") : node;
		if (dataNode.has(NodeState.INPUT) && StringUtils.hasText(dataNode.get(NodeState.INPUT).asText())) {
			data.put(NodeState.INPUT, dataNode.get(NodeState.INPUT).asText());
		}
		if (dataNode.has(NodeState.OUTPUT) && StringUtils.hasText(dataNode.get(NodeState.OUTPUT).asText())) {
			data.put(NodeState.OUTPUT, dataNode.get(NodeState.OUTPUT).asText());
		}

		return new NodeState(data);
	}

}
