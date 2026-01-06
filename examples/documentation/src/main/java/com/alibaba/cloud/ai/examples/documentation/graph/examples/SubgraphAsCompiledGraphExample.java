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

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 子图作为 CompiledGraph 示例
 * 演示如何使用已编译的 Graph 作为子图
 */
public class SubgraphAsCompiledGraphExample {

	/**
	 * 创建并编译子图
	 */
	public static CompiledGraph createAndCompileSubGraph() throws GraphStateException {
		KeyStrategyFactory subKeyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("output", new ReplaceStrategy());
			return strategies;
		};

		// 定义并编译子图
		StateGraph subGraphDef = new StateGraph(subKeyFactory)
				.addNode("process", node_async(state -> {
					String input = (String) state.value("input").orElse("");
					String output = "Processed: " + input.toUpperCase();
					return Map.of("output", output);
				}))
				.addEdge(START, "process")
				.addEdge("process", END);

		// 编译子图
		return subGraphDef.compile();
	}

	/**
	 * 在父图中使用
	 */
	public static CompiledGraph useInParentGraph(CompiledGraph compiledSubGraph) throws GraphStateException {
		KeyStrategyFactory parentKeyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("data", new ReplaceStrategy());
			strategies.put("result", new ReplaceStrategy());
			return strategies;
		};

		StateGraph parentGraph = new StateGraph(parentKeyFactory)
				.addNode("prepare", node_async(state ->
						Map.of("data", "hello world")))
				.addNode("subgraph", compiledSubGraph)
				.addNode("finalize", node_async(state -> {
					String result = (String) state.value("result").orElse("");
					return Map.of("final", "Done: " + result);
				}))
				.addEdge(START, "prepare")
				.addEdge("prepare", "subgraph")
				.addEdge("subgraph", "finalize")
				.addEdge("finalize", END);

		return parentGraph.compile();
	}

	/**
	 * 多个子图复用
	 */
	public static CompiledGraph reuseMultipleSubGraphs(CompiledGraph dataProcessor) throws GraphStateException {
		KeyStrategyFactory keyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("data", new ReplaceStrategy());
			strategies.put("result", new ReplaceStrategy());
			return strategies;
		};

		// 在多个节点中复用
		StateGraph mainGraph = new StateGraph(keyFactory)
				.addNode("process1", dataProcessor)
				.addNode("process2", dataProcessor)
				.addNode("process3", dataProcessor)
				.addEdge(START, "process1")
				.addEdge("process1", "process2")
				.addEdge("process2", "process3")
				.addEdge("process3", END);

		return mainGraph.compile();
	}

	public static void main(String[] args) {
		System.out.println("=== 子图作为 CompiledGraph 示例 ===\n");

		try {
			// 示例 1: 创建并编译子图
			System.out.println("示例 1: 创建并编译子图");
			CompiledGraph subGraph = createAndCompileSubGraph();
			System.out.println("子图创建完成");
			System.out.println();

			// 示例 2: 在父图中使用
			System.out.println("示例 2: 在父图中使用");
			CompiledGraph parentGraph = useInParentGraph(subGraph);
			System.out.println("父图创建完成");
			System.out.println();

			// 示例 3: 多个子图复用
			System.out.println("示例 3: 多个子图复用");
			CompiledGraph reusedGraph = reuseMultipleSubGraphs(subGraph);
			System.out.println("多子图复用示例创建完成");
			System.out.println();

			System.out.println("所有示例执行完成");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 在节点中使用 CompiledGraph
	 */
	public static class CompiledSubGraphNode implements NodeAction {

		private final CompiledGraph compiledGraph;

		public CompiledSubGraphNode(CompiledGraph compiledGraph) {
			this.compiledGraph = compiledGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			// 从父状态提取输入
			String input = (String) state.value("data").orElse("");

			// 执行编译好的子图
			Map<String, Object> subInput = Map.of("input", input);
			OverAllState subResult = compiledGraph.invoke(subInput).orElseThrow();

			// 提取子图输出
			String output = (String) subResult.value("output").orElse("");
			return Map.of("result", output);
		}
	}
}

