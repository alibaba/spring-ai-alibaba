package com.alibaba.cloud.ai.graph.serializer.agent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;

class AgentActionDeserializer extends JsonDeserializer<AgentAction> {

	@Override
	public AgentAction deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);

		var toolExecutionRequestNode = node.get("toolExecutionRequest");
		var toolExecutionRequest = ctx.readValue(toolExecutionRequestNode.traverse(parser.getCodec()),
				AssistantMessage.ToolCall.class);

		return new AgentAction(toolExecutionRequest, node.get("log").asText());
	}

}
