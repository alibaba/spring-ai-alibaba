package dev.ai.alibaba.samples.executor.std.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import dev.ai.alibaba.samples.executor.AgentAction;
import dev.ai.alibaba.samples.executor.IntermediateStep;

import java.io.IOException;

class IntermediateStepDeserializer extends JsonDeserializer<IntermediateStep> {

	@Override
	public IntermediateStep deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException, JacksonException {
		JsonNode node = parser.getCodec().readTree(parser);
		var actionNode = node.get("action");
		var action = (actionNode != null && !actionNode.isNull())
				? ctx.readValue(actionNode.traverse(parser.getCodec()), AgentAction.class) : null;

		return new IntermediateStep(action, node.get("observation").asText());
	}

}
