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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 人类反馈（Human-in-the-Loop）示例
 * 
 * 在实际业务场景中，经常会遇到人类介入的场景，人类的不同操作将影响工作流不同的走向。
 * Spring AI Alibaba Graph 提供了两种方式来实现人类反馈：
 * 
 * 1. InterruptionMetadata 模式：可以在任意节点随时中断，通过实现 InterruptableAction 接口来控制中断时机
 * 2. interruptBefore 模式：需要提前在编译配置中定义中断点，在指定节点执行前中断
 */
public class HumanInTheLoopExample {

	// ==================== 模式一：InterruptionMetadata 模式 ====================

	/**
	 * 定义带中断的 Graph（InterruptionMetadata 模式）
	 * 使用 InterruptableAction 实现中断，不需要 interruptBefore 配置
	 */
	public static CompiledGraph createGraphWithInterruptableAction() throws GraphStateException {
		// 定义普通节点
		var step1 = node_async(state -> {
			return Map.of("messages", "Step 1");
		});

		// 定义可中断节点（实现 InterruptableAction）
		var humanFeedback = new InterruptableNodeAction("human_feedback", "等待用户输入");

		var step3 = node_async(state -> {
			return Map.of("messages", "Step 3");
		});

		// 定义条件边：根据 human_feedback 的值决定路由
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
				.addNode("human_feedback", humanFeedback)  // 使用可中断节点
				.addNode("step_3", step3)
				.addEdge(START, "step_1")
				.addEdge("step_1", "human_feedback")
				.addConditionalEdges("human_feedback", evalHumanFeedback,
						Map.of("back", "step_1", "next", "step_3", "unknown", "human_feedback"))
				.addEdge("step_3", END);

		// 配置内存保存器（用于状态持久化）
		var saver = new MemorySaver();

		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(saver)
						.build())
				// 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
				.build();

		return builder.compile(compileConfig);
	}

	/**
	 * 执行 Graph 直到中断（InterruptionMetadata 模式）
	 * 检查流式输出中的 InterruptionMetadata
	 */
	public static InterruptionMetadata executeUntilInterruptWithMetadata(CompiledGraph graph) {
		// 初始输入
		Map<String, Object> initialInput = Map.of("messages", "Step 0");

		// 配置线程 ID
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 用于保存最后一个输出
		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();

		// 运行 Graph 直到第一个中断点
		graph.stream(initialInput, invokeConfig)
				.doOnNext(event -> {
					System.out.println("节点输出: " + event);
					lastOutputRef.set(event);
				})
				.doOnError(error -> System.err.println("流错误: " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完成"))
				.blockLast();

		// 检查最后一个输出是否是 InterruptionMetadata
		NodeOutput lastOutput = lastOutputRef.get();
		if (lastOutput instanceof InterruptionMetadata) {
			System.out.println("\n检测到中断: " + lastOutput);
			return (InterruptionMetadata) lastOutput;
		}

		return null;
	}

	/**
	 * 等待用户输入并更新状态（InterruptionMetadata 模式）
	 */
	public static RunnableConfig waitUserInputAndUpdateStateWithMetadata(CompiledGraph graph, InterruptionMetadata interruption) throws Exception {
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 检查当前状态
		System.out.printf("\n--State before update--\n%s\n", graph.getState(invokeConfig));

		// 模拟用户输入
		var userInput = "back"; // "back" 表示返回上一个节点
		System.out.printf("\n--User Input--\n用户选择: '%s'\n\n", userInput);

		// 更新状态：添加 human_feedback
		// 使用 updateState 更新状态，传入中断时的节点 ID
		var updatedConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), interruption.node());

		// 检查更新后的状态
		System.out.printf("--State after update--\n%s\n", graph.getState(updatedConfig));

		return updatedConfig;
	}

	/**
	 * 继续执行 Graph（InterruptionMetadata 模式）
	 * 使用 HUMAN_FEEDBACK_METADATA_KEY 来恢复执行
	 */
	public static void continueExecutionWithMetadata(CompiledGraph graph, RunnableConfig updatedConfig) {
		// 创建恢复配置，添加 HUMAN_FEEDBACK_METADATA_KEY
		RunnableConfig resumeConfig = RunnableConfig.builder(updatedConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();

		System.out.println("\n--继续执行 Graph--");

		// 继续执行 Graph（input 为 null，使用之前的状态）
		graph.stream(null, resumeConfig)
				.doOnNext(event -> System.out.println("节点输出: " + event))
				.doOnError(error -> System.err.println("流错误: " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完成"))
				.blockLast();
	}

	// ==================== 模式二：interruptBefore 模式 ====================

	/**
	 * 定义带中断的 Graph（interruptBefore 模式）
	 * 使用 interruptBefore 配置在指定节点前中断
	 */
	public static CompiledGraph createGraphWithInterruptBefore() throws GraphStateException {
		// 定义节点
		var step1 = node_async(state -> {
			return Map.of("messages", "Step 1");
		});

		var humanFeedback = node_async(state -> {
			return Map.of(); // 等待用户输入，不修改状态
		});

		var step3 = node_async(state -> {
			return Map.of("messages", "Step 3");
		});

		// 定义条件边
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

		// 配置内存保存器和中断点
		var saver = new MemorySaver();

		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(saver)
						.build())
				.interruptBefore("human_feedback") // 在 human_feedback 节点前中断
				.build();

		return builder.compile(compileConfig);
	}

	/**
	 * 执行 Graph 直到中断（interruptBefore 模式）
	 */
	public static void executeUntilInterruptWithInterruptBefore(CompiledGraph graph) {
		// 初始输入
		Map<String, Object> initialInput = Map.of("messages", "Step 0");

		// 配置线程 ID
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 运行 Graph 直到第一个中断点
		graph.stream(initialInput, invokeConfig)
				.doOnNext(event -> System.out.println(event))
				.doOnError(error -> System.err.println("流错误: " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完成"))
				.blockLast();
	}

	/**
	 * 等待用户输入并更新状态（interruptBefore 模式）
	 */
	public static RunnableConfig waitUserInputAndUpdateStateWithInterruptBefore(CompiledGraph graph) throws Exception {
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();

		// 检查当前状态
		System.out.printf("--State before update--\n%s\n", graph.getState(invokeConfig));

		// 模拟用户输入
		var userInput = "back"; // "back" 表示返回上一个节点
		System.out.printf("\n--User Input--\n用户选择: '%s'\n\n", userInput);

		// 更新状态（模拟 human_feedback 节点的输出）
		// 注意：interruptBefore 模式下，传入 null 作为节点 ID
		var updateConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), null);

		// 检查更新后的状态
		System.out.printf("--State after update--\n%s\n", graph.getState(updateConfig));

		return updateConfig;
	}

	/**
	 * 继续执行 Graph（interruptBefore 模式）
	 */
	public static void continueExecutionWithInterruptBefore(CompiledGraph graph, RunnableConfig updateConfig) {
		// 添加恢复执行的元数据标记
		RunnableConfig resumeConfig = RunnableConfig.builder(updateConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();

		// 继续执行 Graph（input 为 null，使用之前的状态）
		graph.stream(null, resumeConfig)
				.doOnNext(event -> System.out.println(event))
				.doOnError(error -> System.err.println("流错误: " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完成"))
				.blockLast();
	}

	/**
	 * 第二次等待用户输入（interruptBefore 模式）
	 */
	public static RunnableConfig waitUserInputSecondTime(CompiledGraph graph, RunnableConfig invokeConfig) throws Exception {
		var userInput = "next"; // "next" 表示继续下一个节点
		System.out.printf("\n--User Input--\n用户选择: '%s'\n", userInput);

		// 更新状态
		var updateConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), null);

		System.out.printf("\ngetNext()\n\twith invokeConfig:[%s]\n\twith updateConfig:[%s]\n",
				graph.getState(invokeConfig).next(),
				graph.getState(updateConfig).next());

		return updateConfig;
	}

	/**
	 * 继续执行直到完成（interruptBefore 模式）
	 */
	public static void continueExecutionUntilComplete(CompiledGraph graph, RunnableConfig updateConfig) {
		// 添加恢复执行的元数据标记
		RunnableConfig resumeConfig = RunnableConfig.builder(updateConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();

		// 继续执行 Graph
		graph.stream(null, resumeConfig)
				.doOnNext(event -> System.out.println(event))
				.doOnError(error -> System.err.println("流错误: " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完成"))
				.blockLast();
	}

	// ==================== 可中断的节点动作类 ====================

	/**
	 * 可中断的节点动作
	 * 实现 InterruptableAction 接口，可以在任意节点中断执行
	 */
	public static class InterruptableNodeAction implements AsyncNodeActionWithConfig, InterruptableAction {
		private final String nodeId;
		private final String message;

		public InterruptableNodeAction(String nodeId, String message) {
			this.nodeId = nodeId;
			this.message = message;
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			// 正常节点逻辑：更新状态
			return CompletableFuture.completedFuture(Map.of("messages", message));
		}

		@Override
		public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
			// 检查是否需要中断
			// 如果状态中没有 human_feedback，则中断等待用户输入
			Optional<Object> humanFeedback = state.value("human_feedback");

			if (humanFeedback.isEmpty()) {
				// 返回 InterruptionMetadata 来中断执行
				InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
						.addMetadata("message", "等待用户输入...")
						.addMetadata("node", nodeId)
						// 如果要做工具确认的话，可以在这里添加 toolFeedbacks，具体可参考 HumanInTheLoopHook 实现
						//.toolFeedbacks(List.of(InterruptionMetadata.ToolFeedback.builder().description("").build()))
						.build();

				return Optional.of(interruption);
			}

			// 如果已经有 human_feedback，继续执行
			return Optional.empty();
		}
	}

	// ==================== 主方法 ====================

	public static void main(String[] args) throws Exception {
		System.out.println("========================================");
		System.out.println("人类反馈（Human-in-the-Loop）示例");
		System.out.println("========================================\n");

		// ========== 模式一：InterruptionMetadata 模式 ==========
		System.out.println("=== 模式一：InterruptionMetadata 模式 ===");
		System.out.println("演示如何在任意节点实现 InterruptableAction，通过返回 InterruptionMetadata 实现中断\n");

		CompiledGraph graph1 = createGraphWithInterruptableAction();

		// 执行直到中断
		InterruptionMetadata interruption = executeUntilInterruptWithMetadata(graph1);

		if (interruption != null) {
			// 等待用户输入并更新状态
			RunnableConfig updatedConfig = waitUserInputAndUpdateStateWithMetadata(graph1, interruption);

			// 继续执行
			continueExecutionWithMetadata(graph1, updatedConfig);
		}

		System.out.println("\n模式一示例执行完成\n");

		// ========== 模式二：interruptBefore 模式 ==========
		System.out.println("=== 模式二：interruptBefore 模式 ===");
		System.out.println("演示如何使用 interruptBefore 配置在指定节点前中断\n");

		CompiledGraph graph2 = createGraphWithInterruptBefore();

		// 执行直到中断
		executeUntilInterruptWithInterruptBefore(graph2);

		// 等待用户输入并更新状态
		RunnableConfig updateConfig1 = waitUserInputAndUpdateStateWithInterruptBefore(graph2);

		// 继续执行
		continueExecutionWithInterruptBefore(graph2, updateConfig1);

		// 第二次等待用户输入
		var invokeConfig = RunnableConfig.builder()
				.threadId("Thread1")
				.build();
		RunnableConfig updateConfig2 = waitUserInputSecondTime(graph2, invokeConfig);

		// 继续执行直到完成
		continueExecutionUntilComplete(graph2, updateConfig2);

		System.out.println("\n模式二示例执行完成");
		System.out.println("\n========================================");
		System.out.println("所有示例执行完成");
		System.out.println("========================================");
	}
}
