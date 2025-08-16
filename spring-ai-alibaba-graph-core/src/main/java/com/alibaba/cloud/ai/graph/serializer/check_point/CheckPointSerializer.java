/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.serializer.check_point;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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
