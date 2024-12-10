package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import dev.ai.alibaba.samples.executor.AgentOutcome;
import dev.ai.alibaba.samples.executor.IntermediateStep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class IsStateDeserializer extends JsonDeserializer<IsExecutor.State> {

	@Override
	public IsExecutor.State deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);

		Map<String, Object> data = new HashMap<>();

		var dataNode = node.has("data") ? node.get("data") : node;
		data.put("input", dataNode.get("input").asText());

		var intermediateStepsNode = dataNode.get("intermediate_steps");

		if (intermediateStepsNode == null || intermediateStepsNode.isNull()) { // GUARD
			throw new IOException("intermediate_steps must not be null!");
		}
		if (!intermediateStepsNode.isArray()) { // GUARD
			throw new IOException("intermediate_steps must be an array!");
		}
		var intermediateStepList = new ArrayList<IntermediateStep>();
		for (JsonNode intermediateStepNode : intermediateStepsNode) {

			var intermediateStep = ctx.readValue(intermediateStepNode.traverse(parser.getCodec()),
					IntermediateStep.class);
			intermediateStepList.add(intermediateStep); // intermediateStepList
		}
		data.put("intermediate_steps", intermediateStepList);

		var agentOutcomeNode = dataNode.get(IsExecutor.State.AGENT_OUTCOME);
		if (agentOutcomeNode != null && !agentOutcomeNode.isNull()) { // GUARD
			var agentOutcome = ctx.readValue(agentOutcomeNode.traverse(parser.getCodec()), AgentOutcome.class);
			data.put("agent_outcome", agentOutcome);
		}
		return new IsExecutor.State(data);
	}

}
