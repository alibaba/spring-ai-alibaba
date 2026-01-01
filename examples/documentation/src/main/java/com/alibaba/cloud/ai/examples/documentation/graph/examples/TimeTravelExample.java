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
 * 时光旅行示例
 * 演示如何查看和恢�?Graph 执行的历史状�?
 */
public class TimeTravelExample {

	/**
	 * 配置 Checkpoint
	 */
	public static CompiledGraph configureCheckpoint(StateGraph stateGraph) throws GraphStateException {
		// 创建 Checkpointer
		var checkpointer = new MemorySaver();

		// 配置持久�?
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(checkpointer)
						.build())
				.build();

		return stateGraph.compile(compileConfig);
	}

	/**
	 * 执行 Graph 并生成历�?
	 */
	public static void executeGraphAndGenerateHistory(CompiledGraph graph) {
		// 配置线程 ID
		var config = RunnableConfig.builder()
				.threadId("conversation-1")
				.build();

		// 执行 Graph
		Map<String, Object> input = Map.of("query", "Hello");
		graph.invoke(input, config);

		// 再次执行
		graph.invoke(Map.of("query", "Follow-up question"), config);
	}

	/**
	 * 查看状态历�?
	 */
	public static void viewStateHistory(CompiledGraph graph) {
		var config = RunnableConfig.builder()
				.threadId("conversation-1")
				.build();

		// 获取所有历史状�?
		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		System.out.println("State history:");
		for (int i = 0; i < history.size(); i++) {
			StateSnapshot snapshot = history.get(i);
			System.out.printf("Step %d: %s\n", i, snapshot.state());
			System.out.printf("  Checkpoint ID: %s\n", snapshot.config().checkPointId().orElse("N/A"));
			System.out.printf("  Node: %s\n", snapshot.node());
		}
	}

	/**
	 * 回溯到历史状�?
	 */
	public static void travelBackToHistory(CompiledGraph graph) {
		var config = RunnableConfig.builder()
				.threadId("conversation-1")
				.build();

		// 获取所有历史状�?
		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		// 获取特定的历史状�?
		StateSnapshot historicalSnapshot = history.get(1);

		// 使用历史状态的 checkpoint ID 创建新配�?
		var historicalConfig = RunnableConfig.builder()
				.threadId("conversation-1")
				.checkPointId(historicalSnapshot.config().checkPointId().orElse(null))
				.build();

		// 从历史状态继续执�?
		graph.invoke(
				Map.of("query", "New question from historical state"),
				historicalConfig
		);
	}

	/**
	 * 分支创建
	 */
	public static void createBranch(CompiledGraph graph) {
		var config = RunnableConfig.builder()
				.threadId("conversation-1")
				.build();

		// 获取所有历史状�?
		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		// 获取特定的历史状�?
		StateSnapshot historicalSnapshot = history.get(1);

		// 从历史状态创建新分支
		var branchConfig = RunnableConfig.builder()
				.threadId("conversation-1-branch")  // 新的线程 ID
				.checkPointId(historicalSnapshot.config().checkPointId().orElse(null))
				.build();

		// 在新分支上执�?
		graph.invoke(
				Map.of("query", "Alternative path"),
				branchConfig
		);
	}

	/**
	 * 完整示例
	 */
	public static void completeExample() throws GraphStateException {
		// 构建 Graph
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			strategies.put("step", new ReplaceStrategy());
			return strategies;
		};

		StateGraph builder = new StateGraph(keyStrategyFactory)
				.addNode("step1", node_async(state ->
						Map.of("messages", "Step 1", "step", 1)))
				.addNode("step2", node_async(state ->
						Map.of("messages", "Step 2", "step", 2)))
				.addNode("step3", node_async(state ->
						Map.of("messages", "Step 3", "step", 3)))
				.addEdge(START, "step1")
				.addEdge("step1", "step2")
				.addEdge("step2", "step3")
				.addEdge("step3", END);

		// 配置持久�?
		var checkpointer = new MemorySaver();
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(checkpointer)
						.build())
				.build();

		CompiledGraph graph = builder.compile(compileConfig);

		// 执行
		var config = RunnableConfig.builder()
				.threadId("demo")
				.build();

		graph.invoke(Map.of(), config);

		// 查看历史
		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
		history.forEach(snapshot -> {
			System.out.println("State: " + snapshot.state());
			System.out.println("Node: " + snapshot.node());
			System.out.println("---");
		});

		// 回溯�?step1
		StateSnapshot step1Snapshot = history.stream()
				.filter(s -> "step1".equals(s.node()))
				.findFirst()
				.orElseThrow();

		var replayConfig = RunnableConfig.builder()
				.threadId("demo")
				.checkPointId(step1Snapshot.config().checkPointId().orElse(null))
				.build();

		// �?step1 重新执行
		graph.invoke(Map.of(), replayConfig);
	}

	public static void main(String[] args) {
		System.out.println("=== 时光旅行示例 ===\n");

		try {
			// 示例 1: 配置 Checkpoint
			System.out.println("示例 1: 配置 Checkpoint");
			KeyStrategyFactory keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				strategies.put("step", new ReplaceStrategy());
				return strategies;
			};

			StateGraph builder = new StateGraph(keyStrategyFactory)
					.addNode("step1", node_async(state -> Map.of("messages", "Step 1", "step", 1)))
					.addNode("step2", node_async(state -> Map.of("messages", "Step 2", "step", 2)))
					.addNode("step3", node_async(state -> Map.of("messages", "Step 3", "step", 3)))
					.addEdge(START, "step1")
					.addEdge("step1", "step2")
					.addEdge("step2", "step3")
					.addEdge("step3", END);

			CompiledGraph graph = configureCheckpoint(builder);
			System.out.println("Checkpoint 配置完成");
			System.out.println();

			// 示例 2: 执行 Graph 并生成历�?
			System.out.println("示例 2: 执行 Graph 并生成历�?);
			executeGraphAndGenerateHistory(graph);
			System.out.println();

			// 示例 3: 查看状态历�?
			System.out.println("示例 3: 查看状态历�?);
			viewStateHistory(graph);
			System.out.println();

			// 示例 4: 回溯到历史状�?
			System.out.println("示例 4: 回溯到历史状�?);
			System.out.println("注意: 此示例需要有效的历史状态，跳过执行");
			// travelBackToHistory(graph);
			System.out.println();

			// 示例 5: 分支创建
			System.out.println("示例 5: 分支创建");
			System.out.println("注意: 此示例需要有效的历史状态，跳过执行");
			// createBranch(graph);
			System.out.println();

			// 示例 6: 完整示例
			System.out.println("示例 6: 完整示例");
			completeExample();
			System.out.println();

			System.out.println("所有示例执行完�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

