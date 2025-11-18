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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 持久化执行示例
 * 演示长时间运行任务的持久化执行
 */
public class LongTimeRunningTaskExample {

	/**
	 * 示例: 长时间运行的数据处理任务
	 */
	public static void longRunningDataProcessingTask() throws GraphStateException {
		// 定义状态
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("items", new ReplaceStrategy());
			keyStrategyMap.put("processedCount", new ReplaceStrategy());
			keyStrategyMap.put("results", new AppendStrategy());
			return keyStrategyMap;
		};

		// 处理数据的节点
		var processData = node_async(state -> {
			List<String> items = (List<String>) state.value("items").orElse(List.of());
			int processedCount = (int) state.value("processedCount").orElse(0);

			// 批量处理（例如每次处理 100 个）
			int batchSize = 100;
			int start = processedCount;
			int end = Math.min(start + batchSize, items.size());

			List<String> batch = items.subList(start, end);
			List<String> processedResults = batch.stream()
					.map(item -> "Processed: " + item)
					.collect(Collectors.toList());

			return Map.of(
					"processedCount", end,
					"results", processedResults
			);
		});

		// 检查是否完成
		var checkComplete = edge_async(state -> {
			int processedCount = (int) state.value("processedCount").orElse(0);
			List<String> items = (List<String>) state.value("items").orElse(List.of());

			return processedCount >= items.size() ? END : "process_data";
		});

		// 创建图
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("process_data", processData)
				.addEdge(START, "process_data")
				.addConditionalEdges("process_data", checkComplete,
						Map.of(END, END, "process_data", "process_data"));

		// 配置持久化
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();

		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.saverConfig(saverConfig)
						.build()
		);

		// 执行长时间运行的任务
		RunnableConfig config = RunnableConfig.builder()
				.threadId("long-running-task-" + UUID.randomUUID())
				.build();

		// 创建大量数据
		List<String> largeDataSet = IntStream.range(0, 10000)
				.mapToObj(i -> "Item-" + i)
				.collect(Collectors.toList());

		// 执行（可能会被中断，但可以恢复）
		graph.invoke(Map.of(
				"items", largeDataSet,
				"processedCount", 0
		), config);

		System.out.println("Long-running task example executed");
	}

	/**
	 * 示例: 从错误中恢复
	 */
	public static void errorRecoveryExample(CompiledGraph graph) {
		String threadId = "error-recovery-thread";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		try {
			// 第一次执行可能会失败
			graph.invoke(Map.of("data", "test"), config);
		}
		catch (Exception e) {
			System.err.println("第一次执行失败，准备重试: " + e.getMessage());

			// 使用相同的 threadId 重新执行，将从检查点恢复
			// 传入 null 作为输入，表示从上次状态继续
			graph.invoke(Map.of(), config);
		}
	}

	public static void main(String[] args) {
		System.out.println("=== 持久化执行示例 ===\n");

		try {
			// 示例 1: 长时间运行的数据处理任务
			System.out.println("示例 1: 长时间运行的数据处理任务");
			longRunningDataProcessingTask();
			System.out.println();

			// 示例 2: 从错误中恢复（需要 CompiledGraph）
			System.out.println("示例 2: 从错误中恢复");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// errorRecoveryExample(graph);
			System.out.println();

			System.out.println("所有示例执行完成");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

