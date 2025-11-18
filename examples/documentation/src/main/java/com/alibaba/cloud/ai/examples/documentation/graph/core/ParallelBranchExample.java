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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 并行节点定义示例
 * 演示如何定义并行节点以加速图执行
 */
public class ParallelBranchExample {

	/**
	 * 创建节点的辅助方法
	 */
	private static AsyncNodeAction makeNode(String name) {
		return node_async(state -> {
			return Map.of("messages", List.of(name));
		});
	}

	/**
	 * 示例 1: 定义并行节点
	 */
	public static void defineParallelNodes() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};

		// 构建并行图
		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")   // A 到 A1
				.addEdge("A", "A2")   // A 到 A2 (并行)
				.addEdge("A", "A3")   // A 到 A3 (并行)
				.addEdge("A1", "B")   // A1 到 B
				.addEdge("A2", "B")   // A2 到 B (汇聚)
				.addEdge("A3", "B")   // A3 到 B (汇聚)
				.addEdge("B", "C")
				.addEdge(START, "A")
				.addEdge("C", END);

		CompiledGraph graph = workflow.compile();
		System.out.println("Parallel nodes graph compiled successfully");
	}

	/**
	 * 示例 2: 条件返回到并行节点
	 */
	public static void conditionalReturnToParallelNodes() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				// 条件边：根据状态决定是继续还是返回
				.addConditionalEdges("B",
						edge_async(state -> {
							// 检查上一个节点
							List<String> messages = (List<String>) state.value("messages").orElse(List.of());
							String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1);
							return lastMessage.equals("A3") ? "continue" : "back";
						}),
						Map.of(
								"back", "A1",
								"continue", "C"
						)
				)
				.addEdge(START, "A")
				.addEdge("C", END);

		CompiledGraph graph = workflow.compile();
		System.out.println("Conditional return to parallel nodes graph compiled successfully");
	}

	/**
	 * 示例 3: 使用编译的子图作为并行节点
	 */
	public static void useCompiledSubgraphAsParallelNode() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};

		// 创建子图 A3
		StateGraph subgraphA3Builder = new StateGraph(keyStrategyFactory)
				.addNode("A3.1", makeNode("A3.1"))
				.addNode("A3.2", makeNode("A3.2"))
				.addEdge(START, "A3.1")
				.addEdge("A3.1", "A3.2")
				.addEdge("A3.2", END);

		CompiledGraph subgraphA3 = subgraphA3Builder.compile();

		// 创建子图 A1
		StateGraph subgraphA1Builder = new StateGraph(keyStrategyFactory)
				.addNode("A1.1", makeNode("A1.1"))
				.addNode("A1.2", makeNode("A1.2"))
				.addEdge(START, "A1.1")
				.addEdge("A1.1", "A1.2")
				.addEdge("A1.2", END);

		CompiledGraph subgraphA1 = subgraphA1Builder.compile();

		// 主图：混合使用节点和子图
		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("A", makeNode("A"))
				.addNode("A1", node_async(state -> {
					// 调用子图
					return subgraphA1.invoke(state.data(),
									RunnableConfig.builder().build())
							.orElseThrow()
							.data();
				}))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", node_async(state -> {
					// 调用子图
					return subgraphA3.invoke(state.data(),
									RunnableConfig.builder().build())
							.orElseThrow()
							.data();
				}))
				.addNode("B", makeNode("B"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge(START, "A")
				.addEdge("B", END);

		CompiledGraph graph = workflow.compile();
		System.out.println("Compiled subgraph as parallel node example created");
	}

	/**
	 * 示例 4: 完整示例 - 并行数据处理
	 */
	public static void parallelDataProcessing() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("data", new ReplaceStrategy());
			keyStrategyMap.put("results", new AppendStrategy());
			return keyStrategyMap;
		};

		// 数据处理节点
		var processTypeA = node_async(state -> {
			String data = (String) state.value("data").orElse("");
			String result = "Type A processed: " + data;
			return Map.of("results", List.of(result));
		});

		var processTypeB = node_async(state -> {
			String data = (String) state.value("data").orElse("");
			String result = "Type B processed: " + data;
			return Map.of("results", List.of(result));
		});

		var processTypeC = node_async(state -> {
			String data = (String) state.value("data").orElse("");
			String result = "Type C processed: " + data;
			return Map.of("results", List.of(result));
		});

		// 聚合结果
		var aggregateResults = node_async(state -> {
			List<String> results = (List<String>) state.value("results").orElse(List.of());
			String aggregated = String.join(", ", results);
			return Map.of("final_result", aggregated);
		});

		// 构建图
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("process_a", processTypeA)
				.addNode("process_b", processTypeB)
				.addNode("process_c", processTypeC)
				.addNode("aggregate", aggregateResults)
				.addEdge(START, "process_a")
				.addEdge(START, "process_b")
				.addEdge(START, "process_c")
				.addEdge("process_a", "aggregate")
				.addEdge("process_b", "aggregate")
				.addEdge("process_c", "aggregate")
				.addEdge("aggregate", END);

		CompiledGraph graph = stateGraph.compile();

		// 配置并行执行器
		RunnableConfig config = RunnableConfig.builder()
				.addParallelNodeExecutor("process_a", ForkJoinPool.commonPool())
				.addParallelNodeExecutor("process_b", ForkJoinPool.commonPool())
				.addParallelNodeExecutor("process_c", ForkJoinPool.commonPool())
				.build();

		System.out.println("Parallel data processing graph compiled successfully");
	}

	public static void main(String[] args) {
		System.out.println("=== 并行节点定义示例 ===\n");

		try {
			// 示例 1: 定义并行节点
			System.out.println("示例 1: 定义并行节点");
			defineParallelNodes();
			System.out.println();

			// 示例 2: 条件返回到并行节点
			System.out.println("示例 2: 条件返回到并行节点");
			conditionalReturnToParallelNodes();
			System.out.println();

			// 示例 3: 使用编译的子图作为并行节点
			System.out.println("示例 3: 使用编译的子图作为并行节点");
			useCompiledSubgraphAsParallelNode();
			System.out.println();

			// 示例 4: 完整示例 - 并行数据处理
			System.out.println("示例 4: 完整示例 - 并行数据处理");
			parallelDataProcessing();
			System.out.println();

			System.out.println("所有示例执行完成");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

