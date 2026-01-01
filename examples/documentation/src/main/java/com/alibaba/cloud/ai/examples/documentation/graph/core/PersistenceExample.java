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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 持久化示�?
 * 演示如何使用 Checkpointer 实现工作流状态持久化
 */
public class PersistenceExample {

	/**
	 * 示例 1: 基本持久化配�?
	 */
	public static void basicPersistenceExample() throws GraphStateException {
		// 定义状态策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("foo", new ReplaceStrategy());
			keyStrategyMap.put("bar", new AppendStrategy());
			return keyStrategyMap;
		};

		// 定义节点操作
		var nodeA = node_async(state -> {
			return Map.of("foo", "a", "bar", List.of("a"));
		});

		var nodeB = node_async(state -> {
			return Map.of("foo", "b", "bar", List.of("b"));
		});

		// 创建�?
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("node_a", nodeA)
				.addNode("node_b", nodeB)
				.addEdge(START, "node_a")
				.addEdge("node_a", "node_b")
				.addEdge("node_b", END);

		// 配置检查点
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();

		// 编译�?
		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.saverConfig(saverConfig)
						.build()
		);

		// 运行�?
		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		Map<String, Object> input = new HashMap<>();
		input.put("foo", "");

		graph.invoke(input, config);
		System.out.println("Basic persistence example executed");
	}

	/**
	 * 示例 2: 获取状�?
	 */
	public static void getStateExample(CompiledGraph graph) {
		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		// 获取最新的状态快�?
		StateSnapshot stateSnapshot = graph.getState(config);
		System.out.println("Current state: " + stateSnapshot.state());
		System.out.println("Current node: " + stateSnapshot.node());

		// 获取特定 checkpoint_id 的状态快�?
		RunnableConfig configWithCheckpoint = RunnableConfig.builder()
				.threadId("1")
				.checkPointId("1ef663ba-28fe-6528-8002-5a559208592c")
				.build();
		StateSnapshot specificSnapshot = graph.getState(configWithCheckpoint);
		System.out.println("Specific checkpoint state: " + specificSnapshot.state());
	}

	/**
	 * 示例 3: 获取状态历�?
	 */
	public static void getStateHistoryExample(CompiledGraph graph) {
		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
		System.out.println("State history:");
		for (int i = 0; i < history.size(); i++) {
			StateSnapshot snapshot = history.get(i);
			System.out.printf("Step %d: %s\n", i, snapshot.state());
			System.out.printf("  Checkpoint ID: %s\n", snapshot.config().checkPointId());
			System.out.printf("  Node: %s\n", snapshot.node());
		}
	}

	/**
	 * 示例 4: 更新状�?
	 */
	public static void updateStateExample(CompiledGraph graph) throws Exception {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("foo", new ReplaceStrategy());  // 替换策略
			keyStrategyMap.put("bar", new AppendStrategy());   // 追加策略
			return keyStrategyMap;
		};

		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		Map<String, Object> updates = new HashMap<>();
		updates.put("foo", 2);
		updates.put("bar", List.of("b"));

		graph.updateState(config, updates, null);
		System.out.println("State updated successfully");
	}

	/**
	 * 示例 5: 重放（Replay�?
	 */
	public static void replayExample(CompiledGraph graph) {
		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.checkPointId("0c62ca34-ac19-445d-bbb0-5b4984975b2a")
				.build();

		graph.invoke(Map.of(), config);
		System.out.println("Replay executed");
	}

	public static void main(String[] args) {
		System.out.println("=== 持久化示�?===\n");

		try {
			// 示例 1: 基本持久化配�?
			System.out.println("示例 1: 基本持久化配�?);
			basicPersistenceExample();
			System.out.println();

			// 创建图用于后续示�?
			KeyStrategyFactory keyStrategyFactory = () -> {
				Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
				keyStrategyMap.put("foo", new ReplaceStrategy());
				keyStrategyMap.put("bar", new AppendStrategy());
				return keyStrategyMap;
			};

			StateGraph stateGraph = new StateGraph(keyStrategyFactory)
					.addNode("node_a", node_async(state -> Map.of("foo", "a", "bar", List.of("a"))))
					.addNode("node_b", node_async(state -> Map.of("foo", "b", "bar", List.of("b"))))
					.addEdge(START, "node_a")
					.addEdge("node_a", "node_b")
					.addEdge("node_b", END);

			SaverConfig saverConfig = SaverConfig.builder()
					.register(new MemorySaver())
					.build();

			CompiledGraph graph = stateGraph.compile(
					CompileConfig.builder()
							.saverConfig(saverConfig)
							.build()
			);

			RunnableConfig config = RunnableConfig.builder()
					.threadId("1")
					.build();

			Map<String, Object> input = new HashMap<>();
			input.put("foo", "");
			graph.invoke(input, config);

			// 示例 2: 获取状�?
			System.out.println("示例 2: 获取状�?);
			getStateExample(graph);
			System.out.println();

			// 示例 3: 获取状态历�?
			System.out.println("示例 3: 获取状态历�?);
			getStateHistoryExample(graph);
			System.out.println();

			// 示例 4: 更新状�?
			System.out.println("示例 4: 更新状�?);
			updateStateExample(graph);
			System.out.println();

			// 示例 5: 重放（需要有效的 checkpointId�?
			System.out.println("示例 5: 重放（需要有效的 checkpointId�?);
			System.out.println("注意: 此示例需要有效的 checkpointId，跳过执�?);
			// replayExample(graph);
			System.out.println();

			System.out.println("所有示例执行完�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

