/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

/**
 * Defines the interface for a sub-graph node in an agent's state graph.
 */
public interface SubGraphNode {

	String PREFIX_FORMAT = "%s-%s";

	/**
	 * node identifier
	 * @return the unique identifier for the node.
	 */
	String id();

	/**
	 * Returns a subgraph of the current state graph containing all reachable states from
	 * the current state.
	 * @return {@code StateGraph<State>} representation of the subgraph.
	 */
	StateGraph subGraph();

	default String formatId(String nodeId) {
		return format(PREFIX_FORMAT, id(), nodeId);
	}

	/**
	 * Formats the given {@code subGraphNodeId} and {@code nodeId} into a single string
	 * using a predefined prefix.
	 * @param subGraphNodeId The ID of the sub-graph node.
	 * @param nodeId The ID of the node.
	 * @return A formatted string combining the prefix with the provided IDs.
	 */
	static String formatId(String subGraphNodeId, String nodeId) {
		return format(PREFIX_FORMAT, subGraphNodeId, nodeId);
	}

}