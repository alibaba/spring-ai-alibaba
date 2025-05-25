package com.alibaba.cloud.ai.graph.serializer.check_point;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;

public class CheckPointSerializer implements NullableObjectSerializer<Checkpoint> {

	final StateSerializer stateSerializer;

	public CheckPointSerializer(StateSerializer stateSerializer) {
		this.stateSerializer = stateSerializer;
	}

	@Override
	public void write(Checkpoint object, ObjectOutput out) throws IOException {
		out.writeUTF(object.getId());
		writeNullableUTF(object.getNodeId(), out);
		writeNullableUTF(object.getNextNodeId(), out);
		OverAllState state = (OverAllState) stateSerializer.stateFactory().apply(object.getState());
		stateSerializer.write(state, out);
	}

	@Override
	public Checkpoint read(ObjectInput in) throws IOException, ClassNotFoundException {
		return Checkpoint.builder()
			.id(in.readUTF())
			.nextNodeId(readNullableUTF(in).orElse(null))
			.nodeId(readNullableUTF(in).orElse(null))
			.state((OverAllState) stateSerializer.read(in))
			.build();
	}

}
