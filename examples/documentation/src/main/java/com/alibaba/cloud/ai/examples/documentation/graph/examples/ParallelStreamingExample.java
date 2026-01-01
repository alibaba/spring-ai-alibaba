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
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * 并行流式输出示例
 * 演示如何在并行分支中使用 Flux 实现流式输出
 * 每个并行节点可以独立产生流式输出，并保持各自的节�?ID
 */
public class ParallelStreamingExample {

	/**
	 * 示例 1: 并行节点流式输出 - 每个节点保持独立的节�?ID
	 *
	 * 演示如何创建多个并行节点，每个节点返�?Flux 流式输出
	 * 流式输出会保持各自的节点 ID，便于区分不同节点的输出
	 */
	public static void parallelStreamingWithNodeIdPreservation() throws GraphStateException {
		// 定义状态策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("parallel_results", new AppendStrategy());
			return keyStrategyMap;
		};

		// 并行节点 1 - 返回 Flux 流式输出
		AsyncNodeAction node1 = state -> {
			System.out.println("Node1 executing on thread: " + Thread.currentThread().getName());

			// 创建流式数据
			Flux<String> stream1 = Flux.just("节点1-�?", "节点1-�?", "节点1-�?")
					.delayElements(Duration.ofMillis(50))
					.doOnNext(chunk ->
							System.out.println("Node1 streaming emitting on thread: " + Thread.currentThread().getName())
					);

			return CompletableFuture.completedFuture(Map.of("stream1", stream1));
		};

		// 并行节点 2 - 返回 Flux 流式输出
		AsyncNodeAction node2 = state -> {
			System.out.println("Node2 executing on thread: " + Thread.currentThread().getName());

			// 创建流式数据（延迟时间不同，模拟不同的处理速度�?
			Flux<String> stream2 = Flux.just("节点2-�?", "节点2-�?", "节点2-�?")
					.delayElements(Duration.ofMillis(75))
					.doOnNext(chunk ->
							System.out.println("Node2 streaming emitting on thread: " + Thread.currentThread().getName())
					);

			return CompletableFuture.completedFuture(Map.of("stream2", stream2));
		};

		// 合并节点 - 接收并行节点的结�?
		AsyncNodeAction mergeNode = state -> {
			System.out.println("\n合并节点接收到状�? " + state.data());
			return CompletableFuture.completedFuture(
					Map.of("messages", "所有并行节点已完成，结果已合并")
			);
		};

		// 构建图：两个并行节点�?START 开始，都汇聚到 merge 节点
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("node1", node1)
				.addNode("node2", node2)
				.addNode("merge", mergeNode)
				.addEdge(START, "node1")      // 并行分支 1
				.addEdge(START, "node2")      // 并行分支 2
				.addEdge("node1", "merge")    // 汇聚到合并节�?
				.addEdge("node2", "merge")    // 汇聚到合并节�?
				.addEdge("merge", END);

		// 编译�?
		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.build()
		);

		// 创建配置
		RunnableConfig config = RunnableConfig.builder()
				.threadId("parallel_streaming_thread")
				.build();

		// 跟踪每个节点产生的流式输出数�?
		Map<String, Integer> nodeStreamCounts = new HashMap<>();
		AtomicInteger totalChunks = new AtomicInteger(0);

		System.out.println("开始并行流式输�?..\n");

		// 执行流式图并处理输出
		graph.stream(Map.of("input", "test"), config)
				.doOnNext(output -> {
					if (output instanceof StreamingOutput<?> streamingOutput) {
						// 处理流式输出
						String nodeId = streamingOutput.node();
						String chunk = streamingOutput.chunk();

						// 统计每个节点的流式输�?
						nodeStreamCounts.merge(nodeId, 1, Integer::sum);
						totalChunks.incrementAndGet();

						// 实时打印流式内容，显示节�?ID
						System.out.println("[流式输出] 节点: " + nodeId +
								", 内容: " + chunk);
					}
					else {
						// 处理普通节点输�?
						String nodeId = output.node();
						Map<String, Object> state = output.state().data();
						System.out.println("\n[节点完成] " + nodeId +
								", 状�? " + state);
					}
				})
				.doOnComplete(() -> {
					System.out.println("\n=== 并行流式输出完成 ===");
					System.out.println("总流式块�? " + totalChunks.get());
					System.out.println("各节点流式输出统�? " + nodeStreamCounts);
				})
				.doOnError(error -> {
					System.err.println("流式输出错误: " + error.getMessage());
					error.printStackTrace();
				})
				.blockLast(); // 阻塞等待流完�?
	}

	/**
	 * 示例 2: 单个节点的流式输�?
	 *
	 * 演示单个节点使用 Flux 产生流式输出
	 */
	public static void singleNodeStreaming() throws GraphStateException {
		// 定义状态策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("stream_result", new AppendStrategy());
			return keyStrategyMap;
		};

		// 单个流式节点
		AsyncNodeAction streamingNode = state -> {
			// 创建流式数据
			Flux<String> dataStream = Flux.just("�?", "�?", "�?", "�?", "�?")
					.delayElements(Duration.ofMillis(100));


			return CompletableFuture.completedFuture(Map.of("stream_output", dataStream));
		};

		// 构建�?
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("streaming_node", streamingNode)
				.addEdge(START, "streaming_node")
				.addEdge("streaming_node", END);

		// 编译�?
		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.build()
		);

		// 创建配置
		RunnableConfig config = RunnableConfig.builder()
				.threadId("single_streaming_thread")
				.build();

		System.out.println("开始单节点流式输出...\n");

		AtomicInteger streamCount = new AtomicInteger(0);
		String[] lastNodeId = new String[1];

		// 执行流式�?
		graph.stream(Map.of("input", "test"), config)
				.filter(output -> output instanceof StreamingOutput)
				.map(output -> (StreamingOutput<?>) output)
				.doOnNext(streamingOutput -> {
					streamCount.incrementAndGet();
					lastNodeId[0] = streamingOutput.node();
					System.out.println("[流式输出] 节点: " + streamingOutput.node() +
							", 内容: " + streamingOutput.chunk());
				})
				.doOnComplete(() -> {
					System.out.println("\n=== 单节点流式输出完�?===");
					System.out.println("节点 ID: " + lastNodeId[0]);
					System.out.println("流式块数: " + streamCount.get());
				})
				.doOnError(error -> {
					System.err.println("流式输出错误: " + error.getMessage());
				})
				.blockLast();
	}

	public static void main(String[] args) {
		System.out.println("=== 并行流式输出示例 ===\n");

		try {
			// 示例 1: 并行节点流式输出
//			System.out.println("示例 1: 并行节点流式输出（保持节�?ID�?);
//			parallelStreamingWithNodeIdPreservation();
//			System.out.println();

			// 示例 2: 单个节点流式输出
			System.out.println("示例 2: 单个节点流式输出");
			singleNodeStreaming();
			System.out.println();

			System.out.println("所有示例执行完�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

