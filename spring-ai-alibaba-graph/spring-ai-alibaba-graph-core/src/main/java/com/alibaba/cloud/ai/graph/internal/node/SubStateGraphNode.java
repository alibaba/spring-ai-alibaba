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

package com.alibaba.cloud.ai.graph.internal.node;

import lombok.NonNull;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.AgentState;

public class SubStateGraphNode extends Node implements SubGraphNode {

	private final StateGraph subGraph;

	public SubStateGraphNode(@NonNull String id, @NonNull StateGraph subGraph) {
		super(id);
		this.subGraph = subGraph;
	}

	public StateGraph subGraph() {
		return subGraph;
	}

	public String formatId(String nodeId) {
		return SubGraphNode.formatId(id(), nodeId);
	}

}
