package com.alibaba.cloud.ai.graph;

import lombok.Getter;

import java.util.*;

import com.alibaba.cloud.ai.graph.action.NodeAction;

/**
 * NodeActionDescriptor is used to describe the behavior of #{@link NodeAction}, and it's
 * optional.
 */
@Getter
public class NodeActionDescriptor {

	public static final NodeActionDescriptor EMPTY = new NodeActionDescriptor(List.of(), List.of());

	/**
	 * list of input key, reduces the global state into a partial state
	 */
	private List<String> inputSchema;

	/**
	 * list of output key, format the output to the global state
	 */
	private List<String> outputSchema;

	public NodeActionDescriptor() {
		this.inputSchema = new ArrayList<>();
		this.outputSchema = new ArrayList<>();
	}

	public NodeActionDescriptor(List<String> inputSchema, List<String> outputSchema) {
		this.inputSchema = inputSchema;
		this.outputSchema = outputSchema;
	}

	public void addInputKey(String... inputKey) {
		Collections.addAll(inputSchema, inputKey);
	}

	public void addOutputKey(String... outputKey) {
		Collections.addAll(outputSchema, outputKey);
	}

}
