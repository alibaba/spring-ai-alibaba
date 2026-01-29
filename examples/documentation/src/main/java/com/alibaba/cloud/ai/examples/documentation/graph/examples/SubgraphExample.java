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
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 子图示例
 * 演示如何在 Spring AI Alibaba Graph 中使用子图
 */
public class SubgraphExample {

	/**
	 * 示例 1: 添加编译的子图作为节点
	 */
	public static void addCompiledSubgraphAsNode() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("sharedData", new ReplaceStrategy());
			keyStrategyMap.put("results", new AppendStrategy());
			return keyStrategyMap;
		};

		// 创建子图
		var childNode1 = node_async(state -> {
			String data = (String) state.value("sharedData").orElse("");
			return Map.of("results", List.of("Child processed: " + data));
		});

		StateGraph childGraph = new StateGraph(keyStrategyFactory)
				.addNode("child_node1", childNode1)
				.addEdge(START, "child_node1")
				.addEdge("child_node1", END);

		CompiledGraph compiledChild = childGraph.compile();

		// 创建父图
		var parentNode1 = node_async(state -> {
			return Map.of("sharedData", "Parent data");
		});

		StateGraph parentGraph = new StateGraph(keyStrategyFactory)
				.addNode("parent_node1", parentNode1)
				.addNode("call_child", node_async(state -> {
					return compiledChild.invoke(state.data(),
									RunnableConfig.builder().build())
							.orElseThrow()
							.data();
				}))
				.addEdge(START, "parent_node1")
				.addEdge("parent_node1", "call_child")
				.addEdge("call_child", END);

		CompiledGraph compiledParent = parentGraph.compile();
		System.out.println("Compiled subgraph as node example created");
	}

	/**
	 * 示例 2: 在节点操作中调用子图
	 */
	public static void callSubgraphInNodeAction() throws GraphStateException {
		// 父图状态
		KeyStrategyFactory parentKeyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("parentData", new ReplaceStrategy());
			keyStrategyMap.put("processedResult", new ReplaceStrategy());
			return keyStrategyMap;
		};

		// 子图状态（完全不同）
		KeyStrategyFactory childKeyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("childInput", new ReplaceStrategy());
			keyStrategyMap.put("childOutput", new ReplaceStrategy());
			return keyStrategyMap;
		};

		// 创建子图
		var childProcessor = node_async(state -> {
			String input = (String) state.value("childInput").orElse("");
			String output = "Processed: " + input;
			return Map.of("childOutput", output);
		});

		StateGraph childGraph = new StateGraph(childKeyStrategyFactory)
				.addNode("processor", childProcessor)
				.addEdge(START, "processor")
				.addEdge("processor", END);

		CompiledGraph compiledChild = childGraph.compile();

		// 父图中的转换节点
		var transformAndCallChild = node_async(state -> {
			// 1. 从父状态提取数据
			String parentData = (String) state.value("parentData").orElse("");

			// 2. 转换为子图输入
			Map<String, Object> childInput = Map.of("childInput", parentData);

			// 3. 调用子图
			OverAllState childResult = compiledChild.invoke(
					childInput,
					RunnableConfig.builder().build()
			).orElseThrow();

			// 4. 转换子图输出回父状态
			String childOutput = (String) childResult.value("childOutput").orElse("");
			return Map.of("processedResult", childOutput);
		});

		// 创建父图
		StateGraph parentGraph = new StateGraph(parentKeyStrategyFactory)
				.addNode("call_child_with_transform", transformAndCallChild)
				.addEdge(START, "call_child_with_transform")
				.addEdge("call_child_with_transform", END);

		CompiledGraph compiledParent = parentGraph.compile();
		System.out.println("Call subgraph in node action example created");
	}

	/**
	 * 示例 3: 可视化子图
	 */
	public static void visualizeSubgraph() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("data", new ReplaceStrategy());
			return keyStrategyMap;
		};

		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("node1", node_async(state -> Map.of("data", "processed")))
				.addNode("node2", node_async(state -> Map.of("data", "finalized")))
				.addEdge(START, "node1")
				.addEdge("node1", "node2")
				.addEdge("node2", END);

		// 获取 PlantUML 表示
		GraphRepresentation representation = stateGraph.getGraph(
				GraphRepresentation.Type.PLANTUML,
				"My Graph"
		);

		System.out.println("PlantUML representation:");
		System.out.println(representation.content());
	}

	/**
	 * 示例 4: 流式处理子图
	 */
	public static void streamSubgraph() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("data", new ReplaceStrategy());
			return keyStrategyMap;
		};

		StateGraph childGraph = new StateGraph(keyStrategyFactory)
				.addNode("process", node_async(state -> Map.of("data", "processed")))
				.addEdge(START, "process")
				.addEdge("process", END);

		CompiledGraph compiledChild = childGraph.compile();

		// 执行父图并获取流式输出
		Flux<NodeOutput> stream = compiledChild.stream(
				Map.of("data", "input"),
				RunnableConfig.builder().threadId("parent-thread").build()
		);

		// 处理流式输出
		stream.subscribe(output -> {
			System.out.println("Subgraph output: " + output);
		});
	}

	public static void main(String[] args) {
		System.out.println("=== 子图示例 ===\n");

		try {
			// 示例 1: 添加编译的子图作为节点
			System.out.println("示例 1: 添加编译的子图作为节点");
			addCompiledSubgraphAsNode();
			System.out.println();

			// 示例 2: 在节点操作中调用子图
			System.out.println("示例 2: 在节点操作中调用子图");
			callSubgraphInNodeAction();
			System.out.println();

			// 示例 3: 可视化子图
			System.out.println("示例 3: 可视化子图");
			visualizeSubgraph();
			System.out.println();

			// 示例 4: 流式处理子图
			System.out.println("示例 4: 流式处理子图");
			streamSubgraph();
			System.out.println();

			System.out.println("所有示例执行完成");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

