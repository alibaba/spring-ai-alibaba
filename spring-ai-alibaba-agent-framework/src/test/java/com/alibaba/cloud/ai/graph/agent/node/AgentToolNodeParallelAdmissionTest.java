/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(15)
class AgentToolNodeParallelAdmissionTest {

	@Test
	void timedOutWaiterShouldNotInvokeToolAfterPermitIsReleased() throws Exception {
		try (BlockedTools tools = new BlockedTools()) {
			ToolResponseMessage response = tools.result.get(5, TimeUnit.SECONDS);
			assertThat(response.getResponses()).extracting(ToolResponseMessage.ToolResponse::responseData)
					.containsExactly("Error: Tool execution timed out", "Error: Tool execution timed out");
			tools.releaseFirst.countDown();
			tools.workers.shutdown();
			assertThat(tools.workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
			assertThat(tools.secondToolCalls).hasValue(0);
		}
	}

	@Test
	void timedOutWaiterShouldReleaseExecutorWorkerWhileFirstToolIsStillBlocked() throws Exception {
		try (BlockedTools tools = new BlockedTools()) {
			tools.result.get(5, TimeUnit.SECONDS);

			assertThat(CompletableFuture.supplyAsync(() -> "worker available", tools.workers))
					.succeedsWithin(Duration.ofSeconds(2))
					.isEqualTo("worker available");
			assertThat(tools.releaseFirst.getCount()).isEqualTo(1);
			assertThat(tools.secondToolCalls).hasValue(0);
		}
	}

	@Test
	void waiterShouldRunWhenPermitIsReleasedBeforeTimeout() throws Exception {
		try (BlockedTools tools = new BlockedTools()) {
			tools.releaseFirst.countDown();
			ToolResponseMessage response = tools.result.get(5, TimeUnit.SECONDS);

			assertThat(response.getResponses()).extracting(ToolResponseMessage.ToolResponse::responseData)
					.containsExactly("first result", "second result");
			assertThat(tools.secondToolCalls).hasValue(1);
		}
	}

	private static final class BlockedTools implements AutoCloseable {

		private final ExecutorService workers = Executors.newFixedThreadPool(2);

		private final ExecutorService caller = Executors.newSingleThreadExecutor();

		private final CountDownLatch firstStarted = new CountDownLatch(1);

		private final CountDownLatch releaseFirst = new CountDownLatch(1);

		private final AtomicInteger secondToolCalls = new AtomicInteger();

		private final CompletableFuture<ToolResponseMessage> result;

		private BlockedTools() throws Exception {
			AtomicInteger submissions = new AtomicInteger();
			AtomicReference<Thread> waitingWorker = new AtomicReference<>();
			Executor orderedExecutor = command -> {
				int index = submissions.getAndIncrement();
				workers.execute(() -> {
					if (index == 1) {
						awaitLatch(firstStarted);
						waitingWorker.set(Thread.currentThread());
					}
					command.run();
				});
			};
			ToolCallback first = tool("first", () -> {
				firstStarted.countDown();
				awaitLatch(releaseFirst);
				return "first result";
			});
			ToolCallback second = tool("second", () -> {
				secondToolCalls.incrementAndGet();
				return "second result";
			});
			AgentToolNode node = AgentToolNode.builder().agentName("test-agent")
					.toolCallbacks(List.of(first, second)).parallelToolExecution(true).maxParallelTools(1)
					.toolExecutionTimeout(Duration.ofSeconds(2))
					.toolExecutionExceptionProcessor(
							DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build())
					.build();
			var message = AssistantMessage.builder().content("").toolCalls(List.of(
					new AssistantMessage.ToolCall("call-first", "function", "first", "{}"),
					new AssistantMessage.ToolCall("call-second", "function", "second", "{}"))).build();
			RunnableConfig config = RunnableConfig.builder().addParallelNodeExecutor("_AGENT_TOOL_", orderedExecutor)
					.build();
			result = CompletableFuture.supplyAsync(() -> {
				try {
					return (ToolResponseMessage) node.apply(new OverAllState(Map.of("messages", List.of(message))), config)
							.get("messages");
				}
				catch (Exception ex) {
					throw new java.util.concurrent.CompletionException(ex);
				}
			}, caller);
			try {
				assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();
				await().atMost(Duration.ofSeconds(1)).until(() -> waitingWorker.get() != null
						&& Arrays.stream(waitingWorker.get().getStackTrace())
								.anyMatch(frame -> frame.getClassName().equals("java.util.concurrent.Semaphore")));
			}
			catch (Throwable ex) {
				close();
				throw ex;
			}
		}

		@Override
		public void close() {
			releaseFirst.countDown();
			workers.shutdownNow();
			caller.shutdownNow();
		}

	}

	private static void awaitLatch(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Test latch was not released");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	private static ToolCallback tool(String name, Supplier<String> action) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name(name).description(name).inputSchema("{\"type\":\"object\"}").build();
			}

			@Override
			public String call(String input) {
				return action.get();
			}
		};
	}

}
