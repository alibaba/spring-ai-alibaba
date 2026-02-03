/*
 * Copyright 2024-2026 the original author or authors.
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

import org.springframework.ai.chat.metadata.Usage;

import java.util.List;
import java.util.Objects;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;

/**
 * Represents the output of a node in a graph.
 *
 */
public class NodeOutput {

	/**
	 * Build NodeOutput with chat response for after node processing
	 */
	public static NodeOutput of(String node, String agentName, OverAllState state, Usage tokenUsage) {
		return new NodeOutput(node, agentName, tokenUsage, state);
	}

	/**
	 * Build NodeOutput with calculated next nodes information
	 */
	public static NodeOutput of(String node, String agentName, OverAllState state, Usage tokenUsage, String nextNode, List<String> allNextNodes) {
		return new NodeOutput(node, agentName, tokenUsage, state, nextNode, allNextNodes);
	}

	/**
	 * The identifier of the node.
	 */
	protected final String node;

	protected String agent;

	protected Usage tokenUsage;
	/**
	 * The state associated with the node.
	 */
	protected final OverAllState state;

	/**
	 * The next execution node.
	 */
	protected final String nextNode;

	/**
	 * All possible next execution nodes.
	 */
	protected final List<String> allNextNodes;

	protected boolean subGraph = false;

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

	public void setTokenUsage(Usage tokenUsage) {
		this.tokenUsage = tokenUsage;
	}

	public String node() {
		return node;
	}

	public String agent() {
		return agent;
	}

	public Usage tokenUsage() {
		return tokenUsage;
	}

	public OverAllState state() {
		return state;
	}

	/**
	 * Get the next execution node based on the graph structure.
	 * @return the next node identifier, or null if there's no next node
	 */
	public String getNextNode() {
		return this.nextNode;
	}

	/**
	 * Get all possible next execution nodes.
	 * @return list of all next node identifiers, empty list if there are no next nodes
	 */
	public List<String> getAllNextNodes() {
		if (this.allNextNodes == null) {
			return List.of();
		}
		return this.allNextNodes;
	}

	protected NodeOutput(String node, String agentName, OverAllState state) {
		this.node = node;
		this.agent = agentName;
		this.state = state;
		this.nextNode = null;
		this.allNextNodes = null;
	}

	protected NodeOutput(String node, String agentName, Usage tokenUsage, OverAllState state) {
		this.node = node;
		this.agent = agentName;
		this.state = state;
		this.tokenUsage = tokenUsage;
		this.nextNode = null;
		this.allNextNodes = null;
	}

	protected NodeOutput(String node, String agentName, Usage tokenUsage, OverAllState state, String nextNode, List<String> allNextNodes) {
		this.node = node;
		this.agent = agentName;
		this.state = state;
		this.tokenUsage = tokenUsage;
		this.nextNode = nextNode;
		this.allNextNodes = allNextNodes;
	}

	protected NodeOutput(String node, OverAllState state) {
		this.node = node;
		this.state = state;
		this.nextNode = null;
		this.allNextNodes = null;
	}

	@Override
	public String toString() {
		return format("NodeOutput{node=%s, agent=%s, tokenUsage=%s, state=%s, nextNode=%s, allNextNodes=%s, subGraph=%s}",
				node(), agent(), tokenUsage(), state(), nextNode, allNextNodes, isSubGraph());
	}

}
