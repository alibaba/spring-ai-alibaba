package org.bsc.langgraph4j.agentexecutor.serializer.json;

import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.NonNull;
import org.bsc.langgraph4j.agentexecutor.AgentAction;
import org.bsc.langgraph4j.agentexecutor.GraphAgentExecutor;
import org.bsc.langgraph4j.agentexecutor.AgentFinish;
import org.bsc.langgraph4j.agentexecutor.AgentOutcome;
import org.bsc.langgraph4j.agentexecutor.IntermediateStep;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JSONStateSerializer extends PlainTextStateSerializer<GraphAgentExecutor.State> {

    final ObjectMapper objectMapper;

    public JSONStateSerializer() {
        this( new ObjectMapper() );
    }
    public JSONStateSerializer(  @NonNull ObjectMapper objectMapper) {
        super( GraphAgentExecutor.State::new );
        this.objectMapper = objectMapper;
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        var module = new SimpleModule();
        module.addDeserializer(GraphAgentExecutor.State.class, new StateDeserializer());
        module.addDeserializer(AgentOutcome.class, new AgentOutcomeDeserializer());
        module.addDeserializer(AgentAction.class, new AgentActionDeserializer());
        module.addDeserializer(AgentFinish.class, new AgentFinishDeserializer());
        module.addDeserializer(ToolExecutionRequest.class, new ToolExecutionRequestDeserializer());
        module.addDeserializer(IntermediateStep.class, new IntermediateStepDeserializer());

        objectMapper.registerModule(module);
    }

    @Override
    public String mimeType() {
        return "application/json";
    }

    @Override
    public void write(GraphAgentExecutor.State object, ObjectOutput out) throws IOException {
        var json = objectMapper.writeValueAsString(object);
        out.writeUTF(json);
    }

    @Override
    public GraphAgentExecutor.State read(ObjectInput in) throws IOException, ClassNotFoundException {
        var json = in.readUTF();
        return objectMapper.readValue(json, GraphAgentExecutor.State.class);
    }

}

class IntermediateStepDeserializer extends  JsonDeserializer<IntermediateStep> {

    @Override
    public IntermediateStep deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
        JsonNode node = parser.getCodec().readTree(parser);
        var actionNode = node.get("action");
        var action = ( actionNode != null && !actionNode.isNull()) ?
                ctx.readValue(actionNode.traverse(parser.getCodec()), AgentAction.class) :
                null;

        return new IntermediateStep(action, node.get("observation").asText());
    }
}

class ToolExecutionRequestDeserializer extends  JsonDeserializer<ToolExecutionRequest> {

    @Override
    public ToolExecutionRequest deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
        JsonNode node = parser.getCodec().readTree(parser);
        return ToolExecutionRequest.builder()
                .id(node.get("id").asText())
                .name(node.get("name").asText())
                .arguments(node.get("arguments").asText())
                .build();
    }
}

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

class AgentFinishDeserializer extends  JsonDeserializer<AgentFinish> {

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
            for (var entries = returnValuesNode.fields(); entries.hasNext(); ) {
                var entry = entries.next();
                returnValues.put(entry.getKey(), entry.getValue());
            }
            return new AgentFinish(returnValues, log);
        }
        throw new IOException("Unsupported return values Node: " + returnValuesNode.getNodeType());
    }
}

class AgentOutcomeDeserializer extends  JsonDeserializer<AgentOutcome> {

    @Override
    public AgentOutcome deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
        JsonNode node = parser.getCodec().readTree(parser);

        var actionNode = node.get("action");
        var action = ( actionNode != null && !actionNode.isNull()) ?
                ctx.readValue(actionNode.traverse(parser.getCodec()), AgentAction.class) :
                null;

        var finishNode = node.get("finish");
        var finish = ( finishNode != null && !finishNode.isNull()) ?
                ctx.readValue(finishNode.traverse(parser.getCodec()), AgentFinish.class) :
                null;

        return new AgentOutcome( action, finish );
    }
}

class StateDeserializer extends JsonDeserializer<GraphAgentExecutor.State> {

    @Override
    public GraphAgentExecutor.State deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JsonProcessingException {
        JsonNode node = parser.getCodec().readTree(parser);

        Map<String,Object> data = new HashMap<>();

        var dataNode = node.has("data") ? node.get("data") : node;
        data.put( "input", dataNode.get("input").asText() );

        var intermediateStepsNode = dataNode.get("intermediate_steps");

        if( intermediateStepsNode == null || intermediateStepsNode.isNull() ) { // GUARD
            throw new IOException("intermediate_steps must not be null!");
        }
        if(  !intermediateStepsNode.isArray()) { // GUARD
            throw new IOException("intermediate_steps must be an array!");
        }
        var intermediateStepList = new ArrayList<IntermediateStep>();
        for (JsonNode intermediateStepNode : intermediateStepsNode) {

            var intermediateStep = ctx.readValue(intermediateStepNode.traverse(parser.getCodec()), IntermediateStep.class);
            intermediateStepList.add(intermediateStep); // intermediateStepList
        }
        data.put("intermediate_steps", intermediateStepList);

        var agentOutcomeNode = dataNode.get("agent_outcome");
        if( agentOutcomeNode != null && !agentOutcomeNode.isNull() ) { // GUARD
            var agentOutcome = ctx.readValue(agentOutcomeNode.traverse(parser.getCodec()), AgentOutcome.class);
            data.put("agent_outcome", agentOutcome);
        }
        return new GraphAgentExecutor.State( data );
    }
}
