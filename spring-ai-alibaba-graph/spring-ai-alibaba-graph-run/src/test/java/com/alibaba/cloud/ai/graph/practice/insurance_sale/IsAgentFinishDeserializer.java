package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import dev.ai.alibaba.samples.executor.AgentFinish;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class IsAgentFinishDeserializer extends JsonDeserializer<AgentFinish> {

	@Override
	public AgentFinish deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
		JsonNode node = parser.getCodec().readTree(parser);
		var log = node.get("log").asText();

		var returnValuesNode = node.get("returnValues");

		if (returnValuesNode == null || returnValuesNode.isNull()) {
			return new AgentFinish(null, log);
		}

		if (returnValuesNode.isObject()) { // GUARD
			Map<String, Object> returnValues = new HashMap<>();
			for (var entries = returnValuesNode.fields(); entries.hasNext();) {
				var entry = entries.next();
				returnValues.put(entry.getKey(), entry.getValue());
			}
			return new AgentFinish(returnValues, log);
		}
		throw new IOException("Unsupported return values Node: " + returnValuesNode.getNodeType());
	}

}
