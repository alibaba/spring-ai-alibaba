package com.alibaba.cloud.ai.graph.serializer.plain_text;

import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.io.*;

public abstract class PlainTextStateSerializer<State extends NodeState> extends StateSerializer<State> {

	protected PlainTextStateSerializer(@NonNull AgentStateFactory<State> stateFactory) {
		super(stateFactory);
	}

	@Override
	public String mimeType() {
		return "plain/text";
	}

	public State read(String data) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();

		try (ObjectOutputStream out = new ObjectOutputStream(bytesStream)) {
			out.writeUTF(data);
			out.flush();
		}

		try (ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytesStream.toByteArray()))) {
			return read(in);
		}

	}

	public State read(Reader reader) throws IOException, ClassNotFoundException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
		}
		return read(sb.toString());
	}

}
