package com.alibaba.cloud.ai.graph.exception;

import java.util.Map;
import java.util.Optional;
import static java.lang.String.format;

public class SubGraphInterruptionException extends GraphRunnerException {

	final String parentNodeId;

	final String nodeId;

	final Map<String, Object> state;

	public SubGraphInterruptionException(String parentNodeId, String nodeId, Map<String, Object> state) {
		super(format("interruption in subgraph: %s on node: %s", parentNodeId, nodeId));
		this.parentNodeId = parentNodeId;
		this.nodeId = nodeId;
		this.state = state;
	}

	public String parentNodeId() {
		return parentNodeId;
	}

	public String nodeId() {
		return nodeId;
	}

	public Map<String, Object> state() {
		return state;
	}

	public static Optional<SubGraphInterruptionException> from(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SubGraphInterruptionException ex) {
				return Optional.of(ex);
			}
			current = current.getCause();
		}
		return Optional.empty();
	}

}
