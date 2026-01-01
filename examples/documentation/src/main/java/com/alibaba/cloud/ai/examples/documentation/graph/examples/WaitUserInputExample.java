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
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 等待用户输入示例
 * 演示如何实现等待用户输入的交互式工作�?
 */
public class WaitUserInputExample {

	/**
	 * 定义带中断的 Graph
	 */
	public static CompiledGraph createGraphWithInterrupt() throws GraphStateException {
		// 定义节点
		var step1 = node_async(state -> {
			return Map.of("messages", "Step 1");
		});

		var humanFeedback = node_async(state -> {
			return Map.of(); // 等待用户输入，不修改状�?
		});

		var step3 = node_async(state -> {
			return Map.of("messages", "Step 3");
		});

		// 定义条件�?
		var evalHumanFeedback = edge_async(state -> {
			var feedback = (String) state.value("human_feedback").orElse("unknown");
			return (feedback.equals("next") || feedback.equals("back")) ? feedback : "unknown";
		});

		// 配置 KeyStrategyFactory
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		// 构建 Graph
		StateGraph builder = new StateGraph(keyStrategyFactory)
				.addNode("step_1", step1)
				.addNode("human_feedback", humanFeedback)
				.addNode("step_3", step3)
				.addEdge(START, "step_1")
				.addEdge("step_1", "human_feedback")
				.addConditionalEdges("human_feedback", evalHumanFeedback,
						Map.of("back", "step_1", "next", "step_3", "unknown", "human_feedback"))
				.addEdge("step_3", END);

		// 配置内存保存器和中断�?
		var saver = new MemorySaver();

		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(saver)
						.build())
				.interruptBefore("human_feedback") // �?human_feedback 节点前中�?
				.build();

		return builder.compile(compileConfig);
	}

	/**
	 * 执行 Graph 直到中断
	 */
	public static void executeUntilInterrupt(CompiledGraph graph) {
		// 初始输入
		Map<String, Object> initialInput = Map.of("messages", "Step 0");

		// 配置线程 ID
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 运行 Graph 直到第一个中断点
		graph.stream(initialInput, invokeConfig)
				.doOnNext(event -> System.out.println(event))
				.doOnError(error -> System.err.println("流错�? " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完�?))
				.blockLast();
	}

	/**
	 * 等待用户输入并更新状�?
	 */
	public static void waitUserInputAndUpdateState(CompiledGraph graph) throws Exception {
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 检查当前状�?
		System.out.printf("--State before update--\n%s\n", graph.getState(invokeConfig));

		// 模拟用户输入
		var userInput = "back"; // "back" 表示返回上一个节�?
		System.out.printf("\n--User Input--\n用户选择: '%s'\n\n", userInput);

		// 更新状态（模拟 human_feedback 节点的输出）
		var updateConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), null);

		// 检查更新后的状�?
		System.out.printf("--State after update--\n%s\n", graph.getState(invokeConfig));
	}

	/**
	 * 继续执行 Graph
	 */
	public static void continueExecution(CompiledGraph graph) {
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 继续执行 Graph
		graph.stream(null, invokeConfig)
				.doOnNext(event -> System.out.println(event))
				.doOnError(error -> System.err.println("流错�? " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完�?))
				.blockLast();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("=== 等待用户输入示例 ===");
		CompiledGraph graph = createGraphWithInterrupt();
		executeUntilInterrupt(graph);
		waitUserInputAndUpdateState(graph);
		continueExecution(graph);
		System.out.println("所有示例执行完�?);
	}
}

