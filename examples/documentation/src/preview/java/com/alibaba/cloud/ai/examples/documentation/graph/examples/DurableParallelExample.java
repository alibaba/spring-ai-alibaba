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
import com.alibaba.cloud.ai.graph.NodeExecutionPolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.Send;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction.node_async;

public class DurableParallelExample {

	public static void resumeDispatch(Path directory) throws Exception {
		var dispatchCalls = new AtomicInteger();
		var leftCalls = new AtomicInteger();
		var rightCalls = new AtomicInteger();
		var definition = new StateGraph(() -> Map.of("results", new AppendStrategy()))
				.addNode("dispatch", node_async((state, config) -> {
					dispatchCalls.incrementAndGet();
					List<?> items = state.value("items", List.class).orElseThrow();
					var sends = items.stream().map(item -> new Send("worker", Map.of("item", item))).toList();
					return MultiCommand.dispatch(sends, Map.of());
				}), Map.of("worker", "worker"))
				.addNode("worker", (state, config) -> {
					var item = state.value("item", String.class).orElseThrow();
					if ("left".equals(item)) {
						leftCalls.incrementAndGet();
					}
					else if (rightCalls.incrementAndGet() == 1) {
						throw new WorkerUnavailableException();
					}
					return CompletableFuture.completedFuture(Map.of("results", List.of(item)));
				})
				.addEdge(START, "dispatch").addEdge("worker", END);

		var executor = Executors.newFixedThreadPool(2);
		try {
			var run = RunnableConfig.builder().threadId("dispatch-" + UUID.randomUUID())
					.defaultParallelExecutor(executor).build();
			try {
				definition.compile(durableConfig(directory)).stream(Map.of("items", List.of("left", "right")), run)
						.blockLast(Duration.ofSeconds(5));
			}
			catch (WorkerUnavailableException expected) {
				System.out.println("Right worker needs another attempt");
			}
			var result = definition.compile(durableConfig(directory)).stream(Map.of(), run.withResume())
					.blockLast(Duration.ofSeconds(5));
			System.out.println("Results: " + result.state().value("results").orElseThrow());
			System.out.println("Dispatches: " + dispatchCalls.get());
			System.out.printf("Calls: left=%d, right=%d%n", leftCalls.get(), rightCalls.get());
		}
		finally {
			executor.shutdown();
		}
	}

	private static CompileConfig durableConfig(Path directory) {
		var saver = FileSystemSaver.builder().targetFolder(directory).build();
		return CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build())
				.taskCheckpoints(true).build();
	}

	public static void retryNode() throws Exception {
		var taskIds = new ArrayList<String>();
		var policy = new NodeExecutionPolicy(Duration.ofSeconds(2), 2, Duration.ofMillis(25),
				WorkerUnavailableException.class::isInstance, false);
		var graph = new StateGraph()
				.addNode("work", (state, config) -> {
					taskIds.add(config.taskId().orElseThrow());
					if (taskIds.size() == 1) {
						return CompletableFuture.failedFuture(new WorkerUnavailableException());
					}
					return CompletableFuture.completedFuture(Map.of("complete", true));
				})
				.addEdge(START, "work").addEdge("work", END)
				.compile(CompileConfig.builder().nodePolicy("work", policy).build());

		graph.stream(Map.of()).blockLast(Duration.ofSeconds(5));
		System.out.printf("Attempts: %d, same task ID: %s%n", taskIds.size(), taskIds.get(0).equals(taskIds.get(1)));
	}

	public static void main(String[] args) throws Exception {
		var directory = args.length == 0 ? Files.createTempDirectory("graph-dispatch-") : Path.of(args[0]);
		resumeDispatch(directory);
		retryNode();
	}

	private static class WorkerUnavailableException extends RuntimeException {

	}

}
