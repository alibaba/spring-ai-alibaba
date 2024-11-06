package dev.ai.alibaba.samples.executor.std.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import dev.ai.alibaba.samples.executor.AgentAction;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.io.IOException;

class AgentActionDeserializer extends  JsonDeserializer<AgentAction> {

    @Override
    public AgentAction deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
        JsonNode node = parser.getCodec().readTree(parser);

        var toolExecutionRequestNode = node.get("toolExecutionRequest");
        var toolExecutionRequest = ctx.readValue(toolExecutionRequestNode.traverse(parser.getCodec()), ToolExecutionRequest.class);

        return new AgentAction(
                toolExecutionRequest,
                node.get("log").asText()
        );
    }
}

