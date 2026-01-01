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
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 子图作为 StateGraph 示例
 * 演示如何�?StateGraph 组合使用
 */
public class SubgraphAsStateGraphExample {

	/**
	 * 定义子图
	 */
	public static StateGraph createProcessingSubGraph() throws GraphStateException {
		KeyStrategyFactory keyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("output", new ReplaceStrategy());
			strategies.put("valid", new ReplaceStrategy());
			return strategies;
		};

		return new StateGraph(keyFactory)
				.addNode("validate", node_async(state -> {
					String input = (String) state.value("input").orElse("");
					boolean isValid = input != null && !input.isEmpty();
					return Map.of("valid", isValid);
				}))
				.addNode("transform", node_async(state -> {
					String input = (String) state.value("input").orElse("");
					String transformed = input.toUpperCase();
					return Map.of("output", transformed);
				}))
				.addEdge(START, "validate")
				.addConditionalEdges("validate",
						edge_async(state -> {
							Boolean valid = (Boolean) state.value("valid").orElse(false);
							return valid ? "valid" : "invalid";
						}),
						Map.of(
								"valid", "transform",
								"invalid", END
						))
				.addEdge("transform", END);
	}

	/**
	 * 在父图中集成子图 - 方式 1: 直接嵌入
	 */
	public static StateGraph createParentGraphWithDirectEmbedding() throws GraphStateException {
		KeyStrategyFactory keyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("data", new ReplaceStrategy());
			strategies.put("output", new ReplaceStrategy());
			strategies.put("result", new ReplaceStrategy());
			return strategies;
		};

		StateGraph subGraph = createProcessingSubGraph();

		return new StateGraph(keyFactory)
				.addNode("prepare", node_async(state -> {
					return Map.of("data", "hello world");
				}))
				// 将子图作为节点添�?
				.addNode("process", subGraph)
				.addNode("finalize", node_async(state -> {
					String output = (String) state.value("output").orElse("");
					return Map.of("result", "Final: " + output);
				}))
				.addEdge(START, "prepare")
				.addEdge("prepare", "process")
				.addEdge("process", "finalize")
				.addEdge("finalize", END);
	}

	/**
	 * 在父图中集成子图 - 方式 2: 使用编译后的子图
	 */
	public static StateGraph createParentGraphWithCompiledSubGraph() throws GraphStateException {
		KeyStrategyFactory keyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("data", new ReplaceStrategy());
			strategies.put("output", new ReplaceStrategy());
			strategies.put("processed", new ReplaceStrategy());
			return strategies;
		};

		// 先编译子�?
		CompiledGraph compiledSubGraph = createProcessingSubGraph().compile();

		// 在父图中使用
		return new StateGraph(keyFactory)
				.addNode("prepare", node_async(state -> {
					return Map.of("data", "input");
				}))
				.addNode("process", node_async(state -> {
					// 手动调用子图
					Map<String, Object> subInput = Map.of(
							"input", state.value("data").orElse("")
					);
					OverAllState subResult = compiledSubGraph.invoke(subInput).orElseThrow();
					return Map.of("processed", subResult.value("output").orElse(""));
				}))
				.addEdge(START, "prepare")
				.addEdge("prepare", "process")
				.addEdge("process", END);
	}

	public static void main(String[] args) throws GraphStateException {
		System.out.println("=== 子图作为 StateGraph 示例 ===");
		StateGraph parentGraph1 = createParentGraphWithDirectEmbedding();
		StateGraph parentGraph2 = createParentGraphWithCompiledSubGraph();
		System.out.println("所有示例执行完�?);
	}

	/**
	 * 状态隔离示�?
	 */
	public static class IsolatedSubGraphNode implements NodeAction {
		private final CompiledGraph subGraph;

		public IsolatedSubGraphNode(StateGraph subGraphDef) throws GraphStateException {
			this.subGraph = subGraphDef.compile();
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState) {
			// 提取父状态数�?
			String input = (String) parentState.value("input").orElse("");

			// 创建子图独立状�?
			Map<String, Object> subState = Map.of("subInput", input);

			// 执行子图
			Optional<OverAllState> subResult = subGraph.invoke(subState);

			// 将子图结果映射回父状�?
			String output = (String) subResult.get().value("subOutput").orElse("");
			return Map.of("output", output);
		}
	}
}

