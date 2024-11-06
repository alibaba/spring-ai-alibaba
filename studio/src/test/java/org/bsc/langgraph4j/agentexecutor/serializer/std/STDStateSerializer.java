package org.bsc.langgraph4j.agentexecutor.serializer.std;

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.bsc.langgraph4j.agentexecutor.AgentAction;
import org.bsc.langgraph4j.agentexecutor.GraphAgentExecutor;
import org.bsc.langgraph4j.agentexecutor.AgentFinish;
import org.bsc.langgraph4j.agentexecutor.AgentOutcome;
import org.bsc.langgraph4j.agentexecutor.IntermediateStep;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;



public class STDStateSerializer extends ObjectStreamStateSerializer<GraphAgentExecutor.State> {

    public STDStateSerializer() {
        super(GraphAgentExecutor.State::new);

        mapper().register(ToolExecutionRequest.class, new ToolExecutionRequestSerializer());
        mapper().register(AgentAction.class, new AgentActionSerializer());
        mapper().register(AgentFinish.class, new AgentFinishSerializer());
        mapper().register(AgentOutcome.class, new AgentOutcomeSerializer());
        mapper().register(IntermediateStep.class, new IntermediateStepSerializer());
    }
}

class AgentActionSerializer implements Serializer<AgentAction> {

    @Override
    public void write(AgentAction action, ObjectOutput out) throws IOException {
        ToolExecutionRequest ter =  action.toolExecutionRequest();
        out.writeUTF( ter.id() );
        out.writeUTF( ter.name() );
        out.writeUTF( ter.arguments() );
        out.writeUTF( action.log() );

    }

    @Override
    public AgentAction read(ObjectInput in) throws IOException, ClassNotFoundException {
        ToolExecutionRequest ter = ToolExecutionRequest.builder()
                .id(in.readUTF())
                .name(in.readUTF())
                .arguments(in.readUTF())
                .build();

        return  new AgentAction(  ter, in.readUTF() );

    }
}

class AgentFinishSerializer implements Serializer<AgentFinish> {

    @Override
    public void write(AgentFinish object, ObjectOutput out) throws IOException {
        out.writeObject(object.returnValues());
        out.writeUTF(object.log());
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentFinish read(ObjectInput in) throws IOException, ClassNotFoundException {
        Map<String, Object> returnValues = (Map<String, Object>)in.readObject();
        String log = in.readUTF();
        return new AgentFinish(returnValues, log);
    }

}

class AgentOutcomeSerializer implements NullableObjectSerializer<AgentOutcome> {
    @Override
    public void write(AgentOutcome object, ObjectOutput out) throws IOException {
        writeNullableObject(object.action(), out);
        writeNullableObject(object.finish(), out);
    }

    @Override
    public AgentOutcome read(ObjectInput in) throws IOException, ClassNotFoundException {
        AgentAction action = readNullableObject( in ).map(AgentAction.class::cast).orElse(null);
        AgentFinish finish = readNullableObject( in ).map(AgentFinish.class::cast).orElse(null);
        return new AgentOutcome(action, finish);
    }
}

class IntermediateStepSerializer implements Serializer<IntermediateStep> {
    @Override
    public void write(IntermediateStep object, ObjectOutput out) throws IOException {
        out.writeUTF(object.observation());
        out.writeObject(object.action());
    }

    @Override
    public IntermediateStep read(ObjectInput in) throws IOException, ClassNotFoundException {
        String observation = in.readUTF();
        AgentAction action = (AgentAction)in.readObject();
        return new IntermediateStep(action, observation);
    }
}
