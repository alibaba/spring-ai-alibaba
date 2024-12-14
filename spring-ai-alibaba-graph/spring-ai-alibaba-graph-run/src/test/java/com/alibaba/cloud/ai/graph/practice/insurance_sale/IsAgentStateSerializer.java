package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

@Slf4j
public class IsAgentStateSerializer extends ObjectStreamStateSerializer<IsExecutor.State> {

	public IsAgentStateSerializer() {
		super(IsExecutor.State::new);

		mapper().register(IsExecutor.Outcome.class, new OutcomeSerializer());
		mapper().register(IsExecutor.Finish.class, new FinishSerializer());
		mapper().register(IsExecutor.Action.class, new ActionSerializer());
		mapper().register(IsExecutor.Step.class, new StepSerializer());

	}

	static class FinishSerializer implements Serializer<IsExecutor.Finish> {

		@Override
		public void write(IsExecutor.Finish object, ObjectOutput out) throws IOException {
			out.writeObject(object.returnValues());
			out.writeUTF(object.log());
		}

		@Override
		public IsExecutor.Finish read(ObjectInput in) throws IOException, ClassNotFoundException {
			Map<String, Object> returnValues = (Map<String, Object>) in.readObject();
			String log = in.readUTF();
			return new IsExecutor.Finish(returnValues, log);
		}

	}

	static class ActionSerializer implements Serializer<IsExecutor.Action> {

		@Override
		public void write(IsExecutor.Action action, ObjectOutput out) throws IOException {
			var ter = action.toolCall();
			out.writeUTF(ter.id());
			out.writeUTF(ter.type());
			out.writeUTF(ter.name());
			out.writeUTF(ter.arguments());
			out.writeUTF(action.log());

		}

		@Override
		public IsExecutor.Action read(ObjectInput in) throws IOException, ClassNotFoundException {
			var toolCall = new AssistantMessage.ToolCall(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF());

			return new IsExecutor.Action(toolCall, in.readUTF());

		}

	}

	static class OutcomeSerializer implements NullableObjectSerializer<IsExecutor.Outcome> {

		@Override
		public void write(IsExecutor.Outcome object, ObjectOutput out) throws IOException {
			writeNullableObject(object.action(), out);
			writeNullableObject(object.finish(), out);
		}

		@Override
		public IsExecutor.Outcome read(ObjectInput in) throws IOException, ClassNotFoundException {

			var action = readNullableObject(in).map(IsExecutor.Action.class::cast).orElse(null);
			;
			var finish = readNullableObject(in).map(IsExecutor.Finish.class::cast).orElse(null);

			return new IsExecutor.Outcome(action, finish);
		}

	}

	static class StepSerializer implements Serializer<IsExecutor.Step> {

		@Override
		public void write(IsExecutor.Step object, ObjectOutput out) throws IOException {
			out.writeUTF(object.observation());
			out.writeObject(object.action());
		}

		@Override
		public IsExecutor.Step read(ObjectInput in) throws IOException, ClassNotFoundException {
			String observation = in.readUTF();
			var action = (IsExecutor.Action) in.readObject();
			return new IsExecutor.Step(action, observation);
		}

	}

}
