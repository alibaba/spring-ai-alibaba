package dev.ai.alibaba.samples.executor.std;

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;
import dev.ai.alibaba.samples.executor.AgentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

@Slf4j
public class AgentStateSerializer extends ObjectStreamStateSerializer<AgentExecutor.State> {

    public AgentStateSerializer() {
        super(AgentExecutor.State::new);

        mapper().register( AgentExecutor.Outcome.class, new OutcomeSerializer() );
        mapper().register( AgentExecutor.Finish.class, new FinishSerializer() );
        mapper().register( AgentExecutor.Action.class, new ActionSerializer() );
        mapper().register( AgentExecutor.Step.class, new StepSerializer() );

    }

    static class FinishSerializer implements Serializer<AgentExecutor.Finish> {

        @Override
        public void write(AgentExecutor.Finish object, ObjectOutput out) throws IOException {
            out.writeObject(object.returnValues());
            //out.writeUTF(object.log());
        }

        @Override
        public AgentExecutor.Finish read(ObjectInput in) throws IOException, ClassNotFoundException {
            Map<String, Object> returnValues = (Map<String, Object>)in.readObject();
            // String log = in.readUTF();
            return new AgentExecutor.Finish(returnValues);
        }

    }

    static class ActionSerializer implements Serializer<AgentExecutor.Action> {

        @Override
        public void write(AgentExecutor.Action action, ObjectOutput out) throws IOException {
            var ter =  action.toolCall();
            out.writeUTF( ter.id() );
            out.writeUTF( ter.type() );
            out.writeUTF( ter.name() );
            out.writeUTF( ter.arguments() );
            out.writeUTF( action.log() );

        }

        @Override
        public AgentExecutor.Action read(ObjectInput in) throws IOException, ClassNotFoundException {
            var toolCall = new AssistantMessage.ToolCall( in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF() );

            return  new AgentExecutor.Action(  toolCall, in.readUTF() );

        }
    }

    static class OutcomeSerializer implements NullableObjectSerializer<AgentExecutor.Outcome> {
        @Override
        public void write(AgentExecutor.Outcome object, ObjectOutput out) throws IOException {
            writeNullableObject(object.action(),out);
            writeNullableObject(object.finish(), out);
        }

        @Override
        public AgentExecutor.Outcome read(ObjectInput in) throws IOException, ClassNotFoundException {

            var action = readNullableObject(in).map(AgentExecutor.Action.class::cast).orElse(null);;
            var finish = readNullableObject(in).map(AgentExecutor.Finish.class::cast).orElse(null);

            return new AgentExecutor.Outcome(action, finish);
        }
    }

    static class StepSerializer implements Serializer<AgentExecutor.Step> {
        @Override
        public void write(AgentExecutor.Step object, ObjectOutput out) throws IOException {
            out.writeUTF(object.observation());
            out.writeObject(object.action());
        }

        @Override
        public AgentExecutor.Step read(ObjectInput in) throws IOException, ClassNotFoundException {
            String observation = in.readUTF();
            var action = (AgentExecutor.Action)in.readObject();
            return new AgentExecutor.Step(action, observation);
        }
    }


}
