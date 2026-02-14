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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for thread pool starvation bug when parallelToolExecution=true and
 * wrapSyncToolsAsAsync=true.
 *
 * <p>
 * The bug scenario:
 * <ol>
 * <li>Outer runAsync submits tasks to executor (occupies threads)</li>
 * <li>AsyncToolCallbackAdapter.callAsync() submits another task to the same executor</li>
 * <li>Outer thread calls join() waiting for inner task</li>
 * <li>Inner task can't get a thread → deadlock</li>
 * </ol>
 *
 * <p>
 * The fix: In parallel mode, skip wrapping sync tools because the outer runAsync already
 * provides concurrency. The {@code inParallelExecution} parameter is passed through to
 * prevent the wrapping.
 *
 * @author disaster
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Parallel Starvation Tests")
class AgentToolNodeParallelStarvationTest {

	private AgentToolNode.Builder baseBuilder;

	@BeforeEach
	void setUp() {
		baseBuilder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofSeconds(10))
			.toolExecutionExceptionProcessor(
					DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build());
	}

	@Nested
	@DisplayName("Thread Pool Starvation Prevention Tests")
	class ThreadPoolStarvationTests {

		/**
		 * Tests that parallel execution with wrapSyncToolsAsAsync=true does NOT cause
		 * deadlock when using a single-thread executor.
		 *
		 * <p>
		 * Before the fix, this test would timeout/deadlock because:
		 * <ul>
		 * <li>Single thread runs the outer CompletableFuture.runAsync</li>
		 * <li>Inside, AsyncToolCallbackAdapter.callAsync() tries to submit to same
		 * executor</li>
		 * <li>The single thread is blocked waiting for inner task → deadlock</li>
		 * </ul>
		 *
		 * <p>
		 * After the fix, sync tools execute directly in parallel mode (no wrapping), so
		 * no deadlock occurs.
		 */
		@Test
		@Timeout(value = 15, unit = TimeUnit.SECONDS)
		@DisplayName("parallel execution with wrapSyncToolsAsAsync should not deadlock with single-thread executor")
		void parallelExecutionWithWrapSyncToolsAsAsync_shouldNotDeadlock_withSingleThreadExecutor() throws Exception {
			// Use single-thread executor to expose the starvation bug
			ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

			try {
				AtomicInteger executionCount = new AtomicInteger(0);

				// Create two simple sync tools
				ToolCallback tool1 = createSyncTool("tool1", () -> {
					executionCount.incrementAndGet();
					return "result1";
				});

				ToolCallback tool2 = createSyncTool("tool2", () -> {
					executionCount.incrementAndGet();
					return "result2";
				});

				// Build AgentToolNode with both parallel AND wrapSyncToolsAsAsync enabled
				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2))
					.parallelToolExecution(true)
					.maxParallelTools(5)
					.wrapSyncToolsAsAsync(true) // This would cause deadlock before fix
					.build();

				// Create state with assistant message containing tool calls
				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "tool1", "{}"), createToolCall("call2", "tool2", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				// Create config with the single-thread executor
				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", singleThreadExecutor)
					.build();

				// Execute - before fix this would deadlock; after fix it completes
				Map<String, Object> result = toolNode.apply(state, config);

				// Verify both tools executed successfully
				assertThat(executionCount.get()).isEqualTo(2);
				assertThat(result).containsKey("messages");
				Object messages = result.get("messages");
				assertThat(messages).isInstanceOf(ToolResponseMessage.class);

				ToolResponseMessage responseMessage = (ToolResponseMessage) messages;
				assertThat(responseMessage.getResponses()).hasSize(2);
				assertThat(responseMessage.getResponses().get(0).responseData()).isEqualTo("result1");
				assertThat(responseMessage.getResponses().get(1).responseData()).isEqualTo("result2");
			}
			finally {
				singleThreadExecutor.shutdownNow();
			}
		}

		/**
		 * Tests that sequential execution with wrapSyncToolsAsAsync=true still wraps
		 * tools correctly.
		 *
		 * <p>
		 * This ensures the fix only affects parallel mode, not sequential mode where
		 * wrapping is safe and useful.
		 */
		@Test
		@Timeout(value = 15, unit = TimeUnit.SECONDS)
		@DisplayName("sequential execution with wrapSyncToolsAsAsync should still work")
		void sequentialExecutionWithWrapSyncToolsAsAsync_shouldStillWork() throws Exception {
			ExecutorService executor = Executors.newFixedThreadPool(2);

			try {
				AtomicInteger executionCount = new AtomicInteger(0);

				ToolCallback tool1 = createSyncTool("tool1", () -> {
					executionCount.incrementAndGet();
					return "result1";
				});

				ToolCallback tool2 = createSyncTool("tool2", () -> {
					executionCount.incrementAndGet();
					return "result2";
				});

				// Sequential mode with wrapSyncToolsAsAsync - should still work
				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2))
					.parallelToolExecution(false) // Sequential mode
					.wrapSyncToolsAsAsync(true)
					.build();

				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "tool1", "{}"), createToolCall("call2", "tool2", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", executor)
					.build();

				Map<String, Object> result = toolNode.apply(state, config);

				assertThat(executionCount.get()).isEqualTo(2);
				assertThat(result).containsKey("messages");
				ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
				assertThat(responseMessage.getResponses()).hasSize(2);
			}
			finally {
				executor.shutdownNow();
			}
		}

		/**
		 * Regression test for P1 semaphore deadlock bug.
		 *
		 * <p>Tests that when toolCalls > maxParallelTools, execution completes without
		 * deadlock. The old two-stage semaphore pattern would deadlock because:
		 * <ol>
		 * <li>Stage 1 tasks acquire permits and block executor threads</li>
		 * <li>Stage 2 tasks (which release permits) are queued but can't run</li>
		 * <li>Deadlock: Stage 1 waits for permits; Stage 2 waits for threads</li>
		 * </ol>
		 *
		 * <p>The fix uses a single-stage pattern where semaphore acquire and release
		 * happen within the same task, preventing thread starvation.</p>
		 */
		@Test
		@Timeout(value = 30, unit = TimeUnit.SECONDS)
		@DisplayName("should not deadlock when toolCalls > maxParallelTools with limited thread pool")
		void parallelExecution_shouldNotDeadlock_whenToolCallsExceedMaxParallelTools() throws Exception {
			// Critical setup: Use executor with fewer threads than tool calls
			// This setup would cause deadlock with the old two-stage semaphore pattern
			int threadPoolSize = 3;
			int maxParallelTools = 2;
			int toolCount = 10; // More tools than threads AND more than maxParallelTools

			ExecutorService limitedExecutor = Executors.newFixedThreadPool(threadPoolSize);

			try {
				AtomicInteger executionCount = new AtomicInteger(0);
				AtomicInteger maxConcurrent = new AtomicInteger(0);
				AtomicInteger currentConcurrent = new AtomicInteger(0);

				// Create tools that track execution
				List<ToolCallback> tools = new ArrayList<>();
				for (int i = 0; i < toolCount; i++) {
					final int toolIndex = i;
					tools.add(createSyncTool("tool" + i, () -> {
						int concurrent = currentConcurrent.incrementAndGet();
						maxConcurrent.updateAndGet(max -> Math.max(max, concurrent));
						try {
							// Simulate some work
							Thread.sleep(50);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						finally {
							currentConcurrent.decrementAndGet();
						}
						executionCount.incrementAndGet();
						return "result" + toolIndex;
					}));
				}

				AgentToolNode toolNode = baseBuilder.toolCallbacks(tools)
					.parallelToolExecution(true)
					.maxParallelTools(maxParallelTools)
					.wrapSyncToolsAsAsync(false)
					.build();

				// Create tool calls for all tools
				AssistantMessage.ToolCall[] toolCalls = new AssistantMessage.ToolCall[toolCount];
				for (int i = 0; i < toolCount; i++) {
					toolCalls[i] = createToolCall("call" + i, "tool" + i, "{}");
				}

				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(toolCalls);
				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", limitedExecutor)
					.build();

				// Execute - this would deadlock with old two-stage pattern
				// With single-stage pattern, it should complete successfully
				Map<String, Object> result = toolNode.apply(state, config);

				// Verify all tools executed
				assertThat(executionCount.get()).isEqualTo(toolCount);

				// Verify concurrency was limited by semaphore
				assertThat(maxConcurrent.get()).isLessThanOrEqualTo(maxParallelTools);

				// Verify response contains all tool results
				ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
				assertThat(responseMessage.getResponses()).hasSize(toolCount);
			}
			finally {
				limitedExecutor.shutdownNow();
			}
		}

	}

	@Nested
	@DisplayName("Parallel Concurrency Control Tests")
	class ParallelConcurrencyControlTests {

		/**
		 * Tests parallel execution with a limited thread pool to verify proper
		 * concurrency control.
		 */
		@Test
		@Timeout(value = 20, unit = TimeUnit.SECONDS)
		@DisplayName("parallel execution with limited thread pool should complete successfully")
		void parallelExecution_withLimitedThreadPool_shouldCompleteSucessfully() throws Exception {
			// Use 2 threads but 4 tools to verify semaphore limits work correctly
			ExecutorService limitedExecutor = Executors.newFixedThreadPool(2);

			try {
				AtomicInteger concurrentExecutions = new AtomicInteger(0);
				AtomicInteger maxConcurrent = new AtomicInteger(0);
				AtomicInteger totalExecutions = new AtomicInteger(0);

				Supplier<String> workSimulator = () -> {
					int current = concurrentExecutions.incrementAndGet();
					maxConcurrent.updateAndGet(max -> Math.max(max, current));
					try {
						Thread.sleep(100); // Simulate work
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						concurrentExecutions.decrementAndGet();
					}
					totalExecutions.incrementAndGet();
					return "result";
				};

				ToolCallback tool1 = createSyncTool("tool1", () -> workSimulator.get());
				ToolCallback tool2 = createSyncTool("tool2", () -> workSimulator.get());
				ToolCallback tool3 = createSyncTool("tool3", () -> workSimulator.get());
				ToolCallback tool4 = createSyncTool("tool4", () -> workSimulator.get());

				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2, tool3, tool4))
					.parallelToolExecution(true)
					.maxParallelTools(2) // Limit to 2 concurrent
					.wrapSyncToolsAsAsync(true)
					.build();

				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "tool1", "{}"), createToolCall("call2", "tool2", "{}"),
						createToolCall("call3", "tool3", "{}"), createToolCall("call4", "tool4", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", limitedExecutor)
					.build();

				Map<String, Object> result = toolNode.apply(state, config);

				// All 4 tools should have executed
				assertThat(totalExecutions.get()).isEqualTo(4);

				// Max concurrent should be limited to 2 (by semaphore)
				assertThat(maxConcurrent.get()).isLessThanOrEqualTo(2);

				ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
				assertThat(responseMessage.getResponses()).hasSize(4);
			}
			finally {
				limitedExecutor.shutdownNow();
			}
		}

		/**
		 * Tests that parallel mode without wrapSyncToolsAsAsync also works correctly.
		 */
		@Test
		@Timeout(value = 15, unit = TimeUnit.SECONDS)
		@DisplayName("parallel execution without wrapSyncToolsAsAsync should work")
		void parallelExecution_withoutWrapSyncToolsAsAsync_shouldWork() throws Exception {
			ExecutorService executor = Executors.newFixedThreadPool(4);

			try {
				AtomicInteger executionCount = new AtomicInteger(0);

				ToolCallback tool1 = createSyncTool("tool1", () -> {
					executionCount.incrementAndGet();
					return "result1";
				});

				ToolCallback tool2 = createSyncTool("tool2", () -> {
					executionCount.incrementAndGet();
					return "result2";
				});

				AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2))
					.parallelToolExecution(true)
					.maxParallelTools(5)
					.wrapSyncToolsAsAsync(false) // Disabled
					.build();

				AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
						createToolCall("call1", "tool1", "{}"), createToolCall("call2", "tool2", "{}"));

				OverAllState state = createStateWithMessages(assistantMessage);

				RunnableConfig config = RunnableConfig.builder()
					.threadId("test-thread")
					.addParallelNodeExecutor("_AGENT_TOOL_", executor)
					.build();

				Map<String, Object> result = toolNode.apply(state, config);

				assertThat(executionCount.get()).isEqualTo(2);
				ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
				assertThat(responseMessage.getResponses()).hasSize(2);
			}
			finally {
				executor.shutdownNow();
			}
		}

	}

	// Helper methods

	private ToolCallback createSyncTool(String name, Supplier<String> execution) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Test tool " + name)
					.inputSchema("{\"type\":\"object\",\"properties\":{}}")
					.build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				return execution.get();
			}

			@Override
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};
	}

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
