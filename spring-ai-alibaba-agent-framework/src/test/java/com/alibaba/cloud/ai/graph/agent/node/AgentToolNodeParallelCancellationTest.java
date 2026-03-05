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
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cancellation token propagation in parallel tool execution.
 *
 * <p>
 * This test verifies that when the outer timeout fires in parallel execution mode,
 * the cancellation token for CancellableAsyncToolCallback is properly cancelled
 * so that the tool can stop gracefully.
 *
 * @author disaster
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Parallel Cancellation Tests")
class AgentToolNodeParallelCancellationTest {

	private AgentToolNode.Builder baseBuilder;

	@BeforeEach
	void setUp() {
		baseBuilder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionExceptionProcessor(
					DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build());
	}

	@Nested
	@DisplayName("Outer Timeout Cancellation Propagation Tests")
	class OuterTimeoutCancellationTests {

		/**
		 * Tests that when the outer timeout fires in parallel execution mode,
		 * the cancellation token is properly cancelled.
		 *
		 * <p>
		 * This is a regression test for the bug where cancellation tokens were
		 * NOT being cancelled when the outer orTimeout() fired in parallel mode,
		 * causing cancellable tools to continue running in the background.
		 * </p>
		 *
		 * <p>
		 * Note: We need at least 2 tools for parallel execution mode to be used
		 * (see AgentToolNode line 162: parallelToolExecution && toolCalls.size() > 1).
		 * </p>
		 */
		@Test
		@Timeout(value = 15, unit = TimeUnit.SECONDS)
		@DisplayName("outer timeout should cancel the cancellation token in parallel mode")
		void outerTimeout_shouldCancel_cancellationToken_inParallelMode() throws Exception {
			ExecutorService executor = Executors.newFixedThreadPool(4);

			try {
				// Track the cancellation token state for the slow tool
				AtomicBoolean slowToolTokenCancelled = new AtomicBoolean(false);
				AtomicReference<CancellationToken> slowToolToken = new AtomicReference<>();
				CountDownLatch slowToolStarted = new CountDownLatch(1);
				CountDownLatch tokenCancelledLatch = new CountDownLatch(1);

				// Create a quick tool (needed to trigger parallel mode which requires > 1 tool)
				CancellableAsyncToolCallback quickTool = new CancellableAsyncToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return ToolDefinition.builder()
							.name("quickTool")
							.description("A tool that completes quickly")
							.inputSchema("{\"type\":\"object\",\"properties\":{}}")
							.build();
					}

					@Override
					public Duration getTimeout() {
						return Duration.ofSeconds(30);
					}

					@Override
					public CompletableFuture<String> callAsync(String arguments, ToolContext context,
							CancellationToken cancellationToken) {
						return CompletableFuture.completedFuture("quick-success");
					}

					@Override
					public String call(String toolInput) {
						return "quick-success";
					}
				};

				// Create a slow cancellable tool that takes longer than the outer timeout
				// and registers a callback to detect cancellation
				CancellableAsyncToolCallback slowTool = new CancellableAsyncToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return ToolDefinition.builder()
							.name("slowTool")
							.description("A tool that runs for a long time")
							.inputSchema("{\"type\":\"object\",\"properties\":{}}")
							.build();
					}

					@Override
					public Duration getTimeout() {
						// Per-tool timeout is longer than outer timeout
						return Duration.ofSeconds(30);
					}

					@Override
					public CompletableFuture<String> callAsync(String arguments, ToolContext context,
							CancellationToken cancellationToken) {
						slowToolToken.set(cancellationToken);

						// Register a callback to be notified when cancelled
						cancellationToken.onCancel(() -> {
							slowToolTokenCancelled.set(true);
							tokenCancelledLatch.countDown();
						});

						// Signal that slow tool has started BEFORE the async work
						slowToolStarted.countDown();

						return CompletableFuture.supplyAsync(() -> {
							// Simulate long-running work
							try {
								Thread.sleep(10000); // 10 seconds - longer than outer timeout
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return "interrupted";
							}
							return "completed";
						});
					}

					@Override
					public String call(String toolInput) {
						return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
					}
				};

				// Build AgentToolNode with short outer timeout and 2 tools for parallel mode
				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(quickTool, slowTool))
					.parallelToolExecution(true)
					.maxParallelTools(5)
					.toolExecutionTimeout(Duration.ofMillis(500)) // Short outer timeout
					.build();

				// Create state with TWO tool calls to trigger parallel execution mode
				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "quickTool", "{}"),
						createToolCall("call2", "slowTool", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", executor)
					.build();

				// Execute - this should timeout via the outer timeout for the slow tool
				toolNode.apply(state, config);

				// Wait for the slow tool to actually start
				assertThat(slowToolStarted.await(2, TimeUnit.SECONDS)).isTrue();

				// Wait for the cancellation token to be cancelled (should happen quickly after outer timeout)
				assertThat(tokenCancelledLatch.await(2, TimeUnit.SECONDS))
					.as("Cancellation token callback should be invoked when outer timeout fires")
					.isTrue();

				// Verify the cancellation token was cancelled by the outer timeout handler
				assertThat(slowToolToken.get()).isNotNull();
				assertThat(slowToolToken.get().isCancelled())
					.as("Cancellation token should be cancelled when outer timeout fires")
					.isTrue();
				assertThat(slowToolTokenCancelled.get())
					.as("Token cancellation callback should have been invoked")
					.isTrue();
			}
			finally {
				executor.shutdownNow();
			}
		}

		/**
		 * Tests that cancellation tokens are NOT cancelled when tools complete
		 * successfully in parallel mode.
		 *
		 * <p>
		 * Note: We need at least 2 tools for parallel execution mode to be used.
		 * </p>
		 */
		@Test
		@Timeout(value = 15, unit = TimeUnit.SECONDS)
		@DisplayName("cancellation token should NOT be cancelled on successful completion")
		void cancellationToken_shouldNotBeCancelled_onSuccess_inParallelMode() throws Exception {
			ExecutorService executor = Executors.newFixedThreadPool(4);

			try {
				AtomicReference<CancellationToken> capturedToken1 = new AtomicReference<>();
				AtomicReference<CancellationToken> capturedToken2 = new AtomicReference<>();

				// Create two quick cancellable tools
				CancellableAsyncToolCallback quickTool1 = new CancellableAsyncToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return ToolDefinition.builder()
							.name("quickTool1")
							.description("A tool that completes quickly")
							.inputSchema("{\"type\":\"object\",\"properties\":{}}")
							.build();
					}

					@Override
					public Duration getTimeout() {
						return Duration.ofSeconds(30);
					}

					@Override
					public CompletableFuture<String> callAsync(String arguments, ToolContext context,
							CancellationToken cancellationToken) {
						capturedToken1.set(cancellationToken);
						return CompletableFuture.completedFuture("success1");
					}

					@Override
					public String call(String toolInput) {
						return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
					}
				};

				CancellableAsyncToolCallback quickTool2 = new CancellableAsyncToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return ToolDefinition.builder()
							.name("quickTool2")
							.description("A tool that completes quickly")
							.inputSchema("{\"type\":\"object\",\"properties\":{}}")
							.build();
					}

					@Override
					public Duration getTimeout() {
						return Duration.ofSeconds(30);
					}

					@Override
					public CompletableFuture<String> callAsync(String arguments, ToolContext context,
							CancellationToken cancellationToken) {
						capturedToken2.set(cancellationToken);
						return CompletableFuture.completedFuture("success2");
					}

					@Override
					public String call(String toolInput) {
						return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
					}
				};

				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(quickTool1, quickTool2))
					.parallelToolExecution(true)
					.maxParallelTools(5)
					.toolExecutionTimeout(Duration.ofSeconds(30)) // Long outer timeout
					.build();

				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "quickTool1", "{}"),
						createToolCall("call2", "quickTool2", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", executor)
					.build();

				Map<String, Object> result = toolNode.apply(state, config);

				// Verify successful result
				ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
				assertThat(responseMessage.getResponses()).hasSize(2);
				assertThat(responseMessage.getResponses().get(0).responseData()).isEqualTo("success1");
				assertThat(responseMessage.getResponses().get(1).responseData()).isEqualTo("success2");

				// Verify the cancellation tokens were NOT cancelled
				assertThat(capturedToken1.get()).isNotNull();
				assertThat(capturedToken1.get().isCancelled())
					.as("Cancellation token 1 should NOT be cancelled on successful completion")
					.isFalse();

				assertThat(capturedToken2.get()).isNotNull();
				assertThat(capturedToken2.get().isCancelled())
					.as("Cancellation token 2 should NOT be cancelled on successful completion")
					.isFalse();
			}
			finally {
				executor.shutdownNow();
			}
		}

		/**
		 * Tests that multiple cancellable tools in parallel each get their own
		 * cancellation token, and only the timed-out ones get cancelled.
		 */
		@Test
		@Timeout(value = 15, unit = TimeUnit.SECONDS)
		@DisplayName("each tool should get its own cancellation token in parallel mode")
		void eachTool_shouldGetOwnCancellationToken_inParallelMode() throws Exception {
			ExecutorService executor = Executors.newFixedThreadPool(4);

			try {
				AtomicReference<CancellationToken> quickToolToken = new AtomicReference<>();
				AtomicReference<CancellationToken> slowToolToken = new AtomicReference<>();
				CountDownLatch slowToolStarted = new CountDownLatch(1);
				CountDownLatch tokenCancelledLatch = new CountDownLatch(1);

				// Create a quick tool
				CancellableAsyncToolCallback quickTool = new CancellableAsyncToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return ToolDefinition.builder()
							.name("quickTool")
							.description("Quick tool")
							.inputSchema("{\"type\":\"object\",\"properties\":{}}")
							.build();
					}

					@Override
					public Duration getTimeout() {
						return Duration.ofSeconds(30);
					}

					@Override
					public CompletableFuture<String> callAsync(String arguments, ToolContext context,
							CancellationToken cancellationToken) {
						quickToolToken.set(cancellationToken);
						return CompletableFuture.completedFuture("quick-success");
					}

					@Override
					public String call(String toolInput) {
						return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
					}
				};

				// Create a slow tool that will timeout
				CancellableAsyncToolCallback slowTool = new CancellableAsyncToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return ToolDefinition.builder()
							.name("slowTool")
							.description("Slow tool")
							.inputSchema("{\"type\":\"object\",\"properties\":{}}")
							.build();
					}

					@Override
					public Duration getTimeout() {
						return Duration.ofSeconds(30);
					}

					@Override
					public CompletableFuture<String> callAsync(String arguments, ToolContext context,
							CancellationToken cancellationToken) {
						slowToolToken.set(cancellationToken);
						// Signal that slow tool has started BEFORE the async work
						slowToolStarted.countDown();
						// Register callback to detect cancellation
						cancellationToken.onCancel(tokenCancelledLatch::countDown);
						return CompletableFuture.supplyAsync(() -> {
							try {
								Thread.sleep(10000); // Long sleep
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							return "slow-success";
						});
					}

					@Override
					public String call(String toolInput) {
						return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
					}
				};

				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(quickTool, slowTool))
					.parallelToolExecution(true)
					.maxParallelTools(5)
					.toolExecutionTimeout(Duration.ofMillis(500)) // Short outer timeout
					.build();

				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "quickTool", "{}"), createToolCall("call2", "slowTool", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", executor)
					.build();

				Map<String, Object> result = toolNode.apply(state, config);

				// Wait for the slow tool to start (should have happened before apply returns due to timeout)
				assertThat(slowToolStarted.await(2, TimeUnit.SECONDS)).isTrue();

				// Verify results
				ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
				assertThat(responseMessage.getResponses()).hasSize(2);

				// Quick tool should succeed
				assertThat(responseMessage.getResponses().get(0).responseData()).isEqualTo("quick-success");

				// Slow tool should timeout
				assertThat(responseMessage.getResponses().get(1).responseData()).contains("Error:");

				// Wait for the cancellation to propagate
				assertThat(tokenCancelledLatch.await(2, TimeUnit.SECONDS))
					.as("Cancellation token callback should be invoked when outer timeout fires")
					.isTrue();

				// Verify tokens: quick tool token should NOT be cancelled
				assertThat(quickToolToken.get()).isNotNull();
				assertThat(quickToolToken.get().isCancelled()).isFalse();

				// Slow tool token SHOULD be cancelled due to outer timeout
				assertThat(slowToolToken.get()).isNotNull();
				assertThat(slowToolToken.get().isCancelled())
					.as("Slow tool's cancellation token should be cancelled due to outer timeout")
					.isTrue();
			}
			finally {
				executor.shutdownNow();
			}
		}

	}

	// Helper methods

	private AssistantMessage.ToolCall createToolCall(String id, String name, String arguments) {
		return new AssistantMessage.ToolCall(id, "function", name, arguments);
	}

	private AssistantMessage createAssistantMessageWithToolCalls(AssistantMessage.ToolCall... toolCalls) {
		return AssistantMessage.builder().content("").toolCalls(List.of(toolCalls)).build();
	}

	private OverAllState createStateWithMessages(Message... messages) {
		Map<String, Object> stateData = new HashMap<>();
		stateData.put("messages", new ArrayList<>(List.of(messages)));
		return new OverAllState(stateData);
	}

}
