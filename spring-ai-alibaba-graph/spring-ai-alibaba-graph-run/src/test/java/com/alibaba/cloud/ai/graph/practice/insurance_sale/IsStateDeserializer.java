package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.alibaba.cloud.ai.graph.state.NodeState;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import dev.ai.alibaba.samples.executor.AgentOutcome;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class IsStateDeserializer extends JsonDeserializer<NodeState> {

	@Override
	public NodeState deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);

		Map<String, Object> data = new HashMap<>();

		var dataNode = node.has("data") ? node.get("data") : node;
		data.put("input", dataNode.get("input").asText());

		var agentOutcomeNode = dataNode.get(NodeState.AGENT_OUTCOME);
		if (agentOutcomeNode != null && !agentOutcomeNode.isNull()) { // GUARD
			var agentOutcome = ctx.readValue(agentOutcomeNode.traverse(parser.getCodec()), AgentOutcome.class);
			data.put("agent_outcome", agentOutcome);
		}
		return new NodeState(data);
	}

}
