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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated("Uses a Reactor scheduler factory snapshot")
class AgentToolNodeSchedulingTest {

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void delegatedAgentsProgressWhenThereAreMoreParentsThanSubscriptionWorkers(boolean legacyInterceptor)
			throws Exception {
		var shared = Schedulers.fromExecutorService(Executors.newFixedThreadPool(2));
		var snapshot = Schedulers.setFactoryWithSnapshot(new Schedulers.Factory() {
			@Override
			public Scheduler newBoundedElastic(int threads, int capacity, ThreadFactory factory, int ttlSeconds) {
				return shared;
			}

			@Override
			public Scheduler newThreadPerTaskBoundedElastic(int threads, int capacity, ThreadFactory factory) {
				return shared;
			}
		});
		var entered = new CountDownLatch(2);
		var release = new CountDownLatch(1);
		try {
			var child = ReactAgent.builder().name("child").description("Returns a fixed answer")
					.model(prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("child-ok")))))
					.build();
			var node = toolNode(AgentTool.create(child)).build();
			if (legacyInterceptor) {
				node.setToolInterceptors(List.of(new ToolInterceptor() {
					@Override
					public String getName() {
						return "test-interceptor";
					}

					@Override
					public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
						return handler.call(request);
					}
				}));
			}
			for (int i = 0; i < 2; i++) {
				Schedulers.boundedElastic().schedule(() -> {
					entered.countDown();
					await(release);
				});
			}
			await(entered);
			var results = new ArrayList<CompletableFuture<Map<String, Object>>>();
			for (int i = 0; i < 6; i++) {
				results.add(node.applyAsync(state("child", "{\"input\":\"probe\"}"), run("parent-" + i)));
			}
			release.countDown();
			for (var result : results) {
				assertTrue(response(result.get(5, TimeUnit.SECONDS)).contains("child-ok"));
			}
		}
		finally {
			release.countDown();
			Schedulers.resetFrom(snapshot);
			shared.dispose();
		}
	}

	@Test
	void fullBlockingExecutorRejectsAndCancelledQueuedToolNeverRuns() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var calls = new AtomicInteger();
		var executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
				task -> new Thread(task, "test-blocking-tool"));
		ToolCallback blocking = new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("blocking").description("Waits for a test signal")
						.inputSchema("{}").build();
			}

			@Override
			public String call(String input) {
				calls.incrementAndGet();
				entered.countDown();
				await(release);
				return Thread.currentThread().getName();
			}
		};
		try {
			var node = toolNode(blocking).toolExecutor(executor).build();
			var running = node.applyAsync(state("blocking", "{}"), run("running"));
			await(entered);
			var queued = node.applyAsync(state("blocking", "{}"), run("queued"));
			assertEquals(1, executor.getQueue().size());
			var rejected = node.applyAsync(state("blocking", "{}"), run("rejected"));
			assertEquals("Error: Tool executor is overloaded", response(rejected.get(5, TimeUnit.SECONDS)));
			assertTrue(queued.cancel(true));
			release.countDown();
			assertEquals("test-blocking-tool", response(running.get(5, TimeUnit.SECONDS)));
			executor.shutdown();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			assertEquals(1, calls.get());
		}
		finally {
			release.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void timingOutARunningBlockingToolInterruptsIt() throws Exception {
		var entered = new CountDownLatch(1);
		var interrupted = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var executor = Executors.newSingleThreadExecutor();
		ToolCallback tool = new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("blocking").description("Waits to be interrupted")
						.inputSchema("{}").build();
			}

			@Override
			public String call(String input) {
				entered.countDown();
				try {
					release.await(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException error) {
					Thread.currentThread().interrupt();
					interrupted.countDown();
				}
				return "completed";
			}
		};
		try {
			var node = toolNode(tool).toolExecutor(executor).wrapSyncToolsAsAsync(true)
					.toolExecutionTimeout(Duration.ofSeconds(1)).build();
			var result = node.applyAsync(state("blocking", "{}"), run("timeout"));
			await(entered);
			assertEquals("Error: Tool execution timed out", response(result.get(5, TimeUnit.SECONDS)));
			await(interrupted);
		}
		finally {
			release.countDown();
			executor.shutdownNow();
		}
	}

	private static AgentToolNode.Builder toolNode(ToolCallback callback) {
		return AgentToolNode.builder().agentName("parent").toolCallbacks(List.of(callback))
				.toolExecutionTimeout(Duration.ofSeconds(4)).toolExecutionExceptionProcessor(
						DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build());
	}

	private static OverAllState state(String tool, String arguments) {
		var call = new AssistantMessage.ToolCall("call", "function", tool, arguments);
		return new OverAllState(Map.of("messages", List.of(AssistantMessage.builder().toolCalls(List.of(call)).build())));
	}

	private static RunnableConfig run(String threadId) {
		return RunnableConfig.builder().threadId(threadId).build();
	}

	private static String response(Map<String, Object> updates) {
		return ((ToolResponseMessage) updates.get("messages")).getResponses().get(0).responseData();
	}

	private static void await(CountDownLatch latch) {
		try {
			assertTrue(latch.await(5, TimeUnit.SECONDS));
		}
		catch (InterruptedException error) {
			Thread.currentThread().interrupt();
			throw new AssertionError(error);
		}
	}

}
