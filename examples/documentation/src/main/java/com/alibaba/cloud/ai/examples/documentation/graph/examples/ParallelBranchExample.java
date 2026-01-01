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
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 创建并行节点执行分支示例
 * 演示如何创建并行分支以加速图执行
 */
public class ParallelBranchExample {

	/**
	 * 创建节点的辅助方�?
	 */
	private static AsyncNodeAction makeNode(String message) {
		return node_async(state -> Map.of("messages", List.of(message)));
	}

	/**
	 * 定义带并行分支的 Graph
	 */
	public static CompiledGraph createParallelBranchGraph() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			return keyStrategyHashMap;
		};

		// 构建并行 Graph
		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("A", makeNode("A"))
				.addNode("A1", makeNode("A1"))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", makeNode("A3"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge("A", "A1")    // A �?A1
				.addEdge("A", "A2")    // A �?A2（并行）
				.addEdge("A", "A3")    // A �?A3（并行）
				.addEdge("A1", "B")    // A1 汇聚�?B
				.addEdge("A2", "B")    // A2 汇聚�?B
				.addEdge("A3", "B")    // A3 汇聚�?B
				.addEdge("B", "C")
				.addEdge(START, "A")
				.addEdge("C", END);

		return workflow.compile();
	}

	/**
	 * 执行并行 Graph
	 */
	public static void executeParallelGraph(CompiledGraph compiledGraph) {
		// 执行 Graph
		compiledGraph.stream(Map.of())
				.doOnNext(step -> System.out.println(step))
				.doOnError(error -> System.err.println("流错�? " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完�?))
				.blockLast();
	}

	/**
	 * 使用编译的子图作为并行节�?
	 */
	public static CompiledGraph useCompiledSubgraphAsParallelNode() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			return keyStrategyHashMap;
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
				.addNode("A1", node_async(state -> subgraphA1.invoke(state.data()).orElseThrow().data()))
				.addNode("A2", makeNode("A2"))
				.addNode("A3", node_async(state -> subgraphA3.invoke(state.data()).orElseThrow().data()))
				.addNode("B", makeNode("B"))
				.addEdge("A", "A1")
				.addEdge("A", "A2")
				.addEdge("A", "A3")
				.addEdge("A1", "B")
				.addEdge("A2", "B")
				.addEdge("A3", "B")
				.addEdge(START, "A")
				.addEdge("B", END);

		return workflow.compile();
	}

	public static void main(String[] args) {
		System.out.println("=== 创建并行节点执行分支示例 ===\n");

		try {
			// 示例 1: 定义带并行分支的 Graph
			System.out.println("示例 1: 定义带并行分支的 Graph");
			CompiledGraph graph = createParallelBranchGraph();
			System.out.println("并行分支图创建完�?);
			System.out.println();

			// 示例 2: 执行并行 Graph
			System.out.println("示例 2: 执行并行 Graph");
			executeParallelGraph(graph);
			System.out.println();

			// 示例 3: 使用编译的子图作为并行节�?
			System.out.println("示例 3: 使用编译的子图作为并行节�?);
			CompiledGraph subgraphGraph = useCompiledSubgraphAsParallelNode();
			System.out.println("子图作为并行节点示例创建完成");
			System.out.println();

			System.out.println("所有示例执行完�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

