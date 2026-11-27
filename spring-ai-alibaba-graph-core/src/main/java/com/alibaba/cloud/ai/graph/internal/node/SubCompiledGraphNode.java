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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;

public class SubCompiledGraphNode extends Node implements SubGraphNode {
	private final CompiledGraph subGraph;
	private final String outputKey;
	private final Set<String> outputKeys;

	public SubCompiledGraphNode(String id, CompiledGraph subGraph, Function<OverAllState, Map<String, Object>> inputs,
			UnaryOperator<Map<String, Object>> outputs) {
		super(Objects.requireNonNull(id, "id cannot be null"),
				config -> new SubCompiledGraphNodeAction(id, config, subGraph, UnaryOperator.identity(), null, null,
						Objects.requireNonNull(inputs), Objects.requireNonNull(outputs)));
		this.subGraph = subGraph;
		this.outputKey = outputKeyToParent(id);
		this.outputKeys = Set.of();
	}

	public SubCompiledGraphNode(String id, CompiledGraph subGraph) {
		this(id, subGraph, UnaryOperator.identity());
	}

	public SubCompiledGraphNode(String id, CompiledGraph subGraph, UnaryOperator<RunnableConfig> configure) {
		this(id, subGraph, configure, null, null);
	}

	public SubCompiledGraphNode(String id, CompiledGraph subGraph, Set<String> inputKeys, Set<String> outputKeys) {
		this(id, subGraph, UnaryOperator.identity(), Set.copyOf(inputKeys), Set.copyOf(outputKeys));
	}

	private SubCompiledGraphNode(String id, CompiledGraph subGraph, UnaryOperator<RunnableConfig> configure,
			Set<String> inputKeys, Set<String> outputKeys) {
		super(Objects.requireNonNull(id, "id cannot be null"),
				(config) -> new SubCompiledGraphNodeAction(id, config, subGraph, configure, inputKeys, outputKeys));
		this.subGraph = subGraph;
		this.outputKey = outputKeyToParent(id);
		this.outputKeys = outputKeys;
	}

	private SubCompiledGraphNode(String id, SubCompiledGraphNode node) {
		super(id, node.actionFactory());
		this.subGraph = node.subGraph;
		this.outputKey = node.outputKey;
		this.outputKeys = node.outputKeys;
	}

	@Override
	public SubCompiledGraphNode withIdUpdated(Function<String, String> newId) {
		return new SubCompiledGraphNode(newId.apply(id()), this);
	}

	public StateGraph subGraph() {
		return subGraph.stateGraph;
	}

	@Override
	public Map<String, KeyStrategy> keyStrategies() {
		var strategies = new HashMap<>(subGraph.getKeyStrategyMap());
		if (outputKeys != null) {
			strategies.keySet().retainAll(outputKeys);
		}
		strategies.put(outputKey, new ReplaceStrategy());
		return strategies;
	}

}
