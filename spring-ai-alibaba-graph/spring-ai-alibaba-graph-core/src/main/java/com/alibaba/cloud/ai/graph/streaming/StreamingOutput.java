package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

public class StreamingOutput<State extends AgentState> extends NodeOutput<State> {

	private final String chunk; // null

	public StreamingOutput(String chunk, String node, State state) {
		super(node, state);

		this.chunk = chunk;
	}

	public String chunk() {
		return chunk;
	}

	@Override
	public String toString() {
		if (node() == null) {
			return format("StreamingOutput{chunk=%s}", chunk());
		}
		return format("StreamingOutput{node=%s, state=%s, chunk=%s}", node(), state(), chunk());
	}

}
