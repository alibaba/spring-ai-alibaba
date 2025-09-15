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
package com.alibaba.cloud.ai.graph;

import java.util.Objects;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;

/**
 * Represents the output of a node in a graph.
 *
 */
public class NodeOutput {

	public static NodeOutput of(String node, OverAllState state) {
		return new NodeOutput(node, state);
	}

	/**
	 * The identifier of the node.
	 */
	private final String node;

	/**
	 * The state associated with the node.
	 */
	private final OverAllState state;

	private boolean subGraph = false;

	/**
	 * Checks if the current node refers to the start of the graph processing.
	 * @return {@code true} if the current node refers to the start of the graph
	 * processing
	 */
	public boolean isSTART() {
		return Objects.equals(node(), START);
	}

	/**
	 * Checks if the current node refers to the end of the graph processing. useful to
	 * understand if the workflow has been interrupted.
	 * @return {@code true} if the current node refers to the end of the graph processing
	 */
	public boolean isEND() {
		return Objects.equals(node(), END);
	}

	public boolean isSubGraph() {
		return subGraph;
	}

	public NodeOutput setSubGraph(boolean subGraph) {
		this.subGraph = subGraph;
		return this;
	}

	public String node() {
		return node;
	}

	public OverAllState state() {
		return state;
	}

	protected NodeOutput(String node, OverAllState state) {
		this.node = node;
		this.state = state;
	}

	@Override
	public String toString() {
		return format("NodeOutput{node=%s, state=%s}", node(), state());
	}

}
