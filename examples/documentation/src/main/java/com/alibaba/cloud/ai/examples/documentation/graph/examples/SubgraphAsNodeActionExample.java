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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 子图作为节点操作示例
 * 演示如何将子图作�?NodeAction 在父图中使用
 */
public class SubgraphAsNodeActionExample {

	/**
	 * 定义子图
	 */
	public static CompiledGraph createSubGraph(KeyStrategyFactory keyStrategyFactory) throws GraphStateException {
		StateGraph subGraph = new StateGraph(keyStrategyFactory)
				.addNode("substep1", node_async(state -> {
					String input = (String) state.value("input").orElse("");
					return Map.of("result", "SubStep1:" + input);
				}))
				.addNode("substep2", node_async(state -> {
					String prev = (String) state.value("result").orElse("");
					return Map.of("result", prev + "->SubStep2");
				}))
				.addEdge(START, "substep1")
				.addEdge("substep1", "substep2")
				.addEdge("substep2", END);

		return subGraph.compile();
	}

	/**
	 * 在父图中使用
	 */
	public static CompiledGraph useInParentGraph(KeyStrategyFactory keyStrategyFactory, CompiledGraph subGraph) throws GraphStateException {
		SubGraphNode subGraphNode = new SubGraphNode(subGraph);

		StateGraph parentGraph = new StateGraph(keyStrategyFactory)
				.addNode("prepare", node_async(state -> {
					return Map.of("data", "Input Data");
				}))
				.addNode("process", node_async(subGraphNode))  // 使用子图作为节点
				.addNode("finalize", node_async(state -> {
					String processed = (String) state.value("processed").orElse("");
					return Map.of("final", "Final:" + processed);
				}))
				.addEdge(START, "prepare")
				.addEdge("prepare", "process")
				.addEdge("process", "finalize")
				.addEdge("finalize", END);

		return parentGraph.compile();
	}

	public static void main(String[] args) throws GraphStateException {
		System.out.println("=== 子图作为节点操作示例 ===");
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("data", new ReplaceStrategy());
			strategies.put("input", new ReplaceStrategy());
			strategies.put("result", new ReplaceStrategy());
			strategies.put("processed", new ReplaceStrategy());
			strategies.put("final", new ReplaceStrategy());
			return strategies;
		};

		CompiledGraph subGraph = createSubGraph(keyStrategyFactory);
		CompiledGraph parentGraph = useInParentGraph(keyStrategyFactory, subGraph);
		System.out.println("所有示例执行完�?);
	}

	/**
	 * 将子图包装为 NodeAction
	 */
	public static class SubGraphNode implements NodeAction {

		private final CompiledGraph subGraph;

		public SubGraphNode(CompiledGraph subGraph) {
			this.subGraph = subGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			// 从父状态提取子图需要的数据
			String input = (String) state.value("data").orElse("");

			// 执行子图
			Map<String, Object> subInput = Map.of("input", input);
			Optional<OverAllState> subResult = subGraph.invoke(subInput);

			// 返回结果给父�?
			String result = (String) subResult.get().value("result").orElse("");
			return Map.of("processed", result);
		}
	}

	/**
	 * 可配置的子图节点
	 */
	public static class ConfigurableSubGraphNode implements NodeAction {

		private final CompiledGraph subGraph;
		private final String inputKey;
		private final String outputKey;

		public ConfigurableSubGraphNode(
				CompiledGraph subGraph,
				String inputKey,
				String outputKey
		) {
			this.subGraph = subGraph;
			this.inputKey = inputKey;
			this.outputKey = outputKey;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			// 从父状态读取指定键的数�?
			Object input = state.value(inputKey).orElse(null);

			// 执行子图
			OverAllState subResult = subGraph.invoke(Map.of("input", input)).orElseThrow();

			// 将结果写入指定键
			Object output = subResult.value("result").orElse(null);
			return Map.of(outputKey, output);
		}
	}
}

