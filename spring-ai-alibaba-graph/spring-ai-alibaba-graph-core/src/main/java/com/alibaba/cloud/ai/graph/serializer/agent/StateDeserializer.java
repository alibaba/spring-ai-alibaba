package com.alibaba.cloud.ai.graph.serializer.agent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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
		if (dataNode.has(NodeState.OUTPUT)) {
			JsonNode outputNode = dataNode.get(NodeState.OUTPUT);
			if (outputNode.isTextual())
			{
				data.put(NodeState.OUTPUT, outputNode.asText());
			}
			else
			{
				if (!outputNode.isNull()) {
					var agentOutcome = ctx.readValue(outputNode.traverse(parser.getCodec()), AgentOutcome.class);
					data.put("agent_outcome", agentOutcome);
				}
			}
		}
		if (dataNode.has(NodeState.RESUME_INPUT)){
			JsonNode resumeNode = dataNode.get(NodeState.RESUME_INPUT);
			if (resumeNode.isTextual())
			{
				data.put(NodeState.RESUME_INPUT, resumeNode.asText());
			}
		}
		if (dataNode.has(NodeState.SUB_GRAPH)) {
			JsonNode outputNode = dataNode.get(NodeState.SUB_GRAPH);
			var agentOutcome = ctx.readValue(outputNode.traverse(parser.getCodec()),
					CompiledGraph.AsyncNodeGenerator.class);
			data.put(NodeState.SUB_GRAPH, agentOutcome);
		}

		return new NodeState(data);
	}

}
