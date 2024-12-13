package com.alibaba.cloud.ai.graph.serializer.agent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

class AgentOutcomeDeserializer extends JsonDeserializer<AgentOutcome> {

	@Override
	public AgentOutcome deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);

		var actionNode = node.get("action");
		var action = (actionNode != null && !actionNode.isNull())
				? ctx.readValue(actionNode.traverse(parser.getCodec()), AgentAction.class) : null;

		var finishNode = node.get("finish");
		var finish = (finishNode != null && !finishNode.isNull())
				? ctx.readValue(finishNode.traverse(parser.getCodec()), AgentFinish.class) : null;

		return new AgentOutcome(action, finish);
	}

}
