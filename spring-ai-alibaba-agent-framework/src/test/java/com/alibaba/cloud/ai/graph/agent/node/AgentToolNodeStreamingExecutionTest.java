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

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StreamingToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolResult;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.ToolStreamingOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AgentToolNode streaming execution methods.
 *
 * <p>
 * Covers executeToolCallsStreaming(), executeStreamingToolBlocking(),
 * and related streaming functionality.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Streaming Execution Tests")
class AgentToolNodeStreamingExecutionTest {

	private AgentToolNode.Builder baseBuilder;

	private OverAllState testState;

	@BeforeEach
	void setUp() {
		baseBuilder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofSeconds(10))
			.toolExecutionExceptionProcessor(
					DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build());

		testState = OverAllStateBuilder.builder()
			.withKeyStrategy("messages", new ReplaceStrategy())
			.build()
			.input(Map.of("messages", new ArrayList<>()));
	}

	@Nested
	@DisplayName("executeToolCallsStreaming Basic Tests")
	class ExecuteToolCallsStreamingBasicTests {

		@Test
		@DisplayName("should emit streaming chunks from StreamingToolCallback")
		void shouldEmitStreamingChunks_fromStreamingTool() {
			// Given
			StreamingToolCallback streamingTool = createStreamingTool("streamTool", 3);
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(streamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "streamTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Verify first output has correct tool call id
			Object first = results.get(0);
			assertInstanceOf(ToolStreamingOutput.class, first);
			ToolStreamingOutput<?> firstOutput = (ToolStreamingOutput<?>) first;
			assertEquals("call_1", firstOutput.getToolCallId());

			// Verify we have streaming and finished outputs
			long streamingCount = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_STREAMING)
				.count();
			assertTrue(streamingCount >= 2, "Should have at least 2 streaming chunks");

			// Verify last item is GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
			assertTrue(((GraphResponse<?>) lastItem).isDone());
		}

		@Test
		@DisplayName("should emit GraphResponse.done at stream end")
		void shouldEmitGraphResponseDone_atStreamEnd() {
			// Given
			ToolCallback syncTool = createSimpleTool("syncTool", args -> "result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(syncTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "syncTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then - should end with GraphResponse.done
			assertNotNull(results);
			assertFalse(results.isEmpty());

			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
			GraphResponse<?> graphResponse = (GraphResponse<?>) lastItem;
			assertTrue(graphResponse.isDone());
		}

		@Test
		@DisplayName("should handle sync ToolCallback in streaming mode")
		void shouldHandleSyncToolCallback_inStreamingMode() {
			// Given
			ToolCallback syncTool = createSimpleTool("syncTool", args -> "sync-result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(syncTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "syncTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the tool output
			ToolStreamingOutput<?> toolOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(toolOutput);
			assertEquals(OutputType.AGENT_TOOL_FINISHED, toolOutput.getOutputType());
			assertTrue(toolOutput.isFinalChunk());
		}

		@Test
		@DisplayName("should handle AsyncToolCallback in streaming mode")
		void shouldHandleAsyncToolCallback_inStreamingMode() {
			// Given
			AsyncToolCallback asyncTool = createAsyncTool("asyncTool", "async-result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(asyncTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "asyncTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the tool output
			ToolStreamingOutput<?> toolOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(toolOutput);
			assertEquals(OutputType.AGENT_TOOL_FINISHED, toolOutput.getOutputType());
		}

	}

	@Nested
	@DisplayName("executeToolCallsStreaming Multiple Tools Tests")
	class ExecuteToolCallsStreamingMultipleToolsTests {

		@Test
		@DisplayName("should handle multiple streaming tools")
		void shouldHandleMultipleStreamingTools() {
			// Given
			StreamingToolCallback tool1 = createStreamingTool("tool1", 2);
			StreamingToolCallback tool2 = createStreamingTool("tool2", 2);
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2))
				.parallelToolExecution(true)
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "tool1", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "tool2", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then
			assertNotNull(results);
			// Should have outputs from both tools plus final GraphResponse.done
			long toolOutputCount = results.stream().filter(obj -> obj instanceof ToolStreamingOutput<?>).count();
			assertTrue(toolOutputCount >= 4, "Should have outputs from both tools");

			// Last item should be GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

		@Test
		@DisplayName("should handle mixed tool types")
		void shouldHandleMixedToolTypes() {
			// Given
			StreamingToolCallback streamingTool = createStreamingTool("streamTool", 2);
			AsyncToolCallback asyncTool = createAsyncTool("asyncTool", "async-result");
			ToolCallback syncTool = createSimpleTool("syncTool", args -> "sync-result");

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(streamingTool, asyncTool, syncTool))
				.parallelToolExecution(true)
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "streamTool", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "asyncTool", "{}"),
					new AssistantMessage.ToolCall("call_3", "function", "syncTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then
			assertNotNull(results);
			// Should end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);

			// All tools should produce outputs
			long toolOutputCount = results.stream().filter(obj -> obj instanceof ToolStreamingOutput<?>).count();
			assertTrue(toolOutputCount >= 3, "Each tool should produce at least one output");
		}

	}

	@Nested
	@DisplayName("executeToolCallsStreaming Error Handling Tests")
	class ExecuteToolCallsStreamingErrorTests {

		@Test
		@DisplayName("should handle error in streaming tool")
		void shouldHandleError_inStreamingTool() {
			// Given
			StreamingToolCallback failingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("failingTool").description("Failing tool").inputSchema("{}").build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.error(new RuntimeException("Stream error"));
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(failingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "failingTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then - should emit error output, not terminate the stream
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the error output
			ToolStreamingOutput<?> errorOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(errorOutput);
			assertTrue(errorOutput.isFinalChunk());

			// Should contain error info
			if (errorOutput.getChunkData() instanceof ToolResult result) {
				assertTrue(result.getTextContent().contains("Error:"));
			}

			// Should end with GraphResponse
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

		@Test
		@DisplayName("should handle null tool callback")
		void shouldHandleNullToolCallback() {
			// Given - node with no tools
			AgentToolNode node = baseBuilder.toolCallbacks(List.of()).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "nonExistentTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then - should emit error output
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the error output
			ToolStreamingOutput<?> errorOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(errorOutput);

			// Should end with GraphResponse
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

		@Test
		@DisplayName("should handle partial failure in multiple tools")
		void shouldHandlePartialFailure_inMultipleTools() {
			// Given
			ToolCallback goodTool = createSimpleTool("goodTool", args -> "success");
			StreamingToolCallback failingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("failingTool").description("Failing tool").inputSchema("{}").build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.error(new RuntimeException("Failed"));
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(goodTool, failingTool))
				.parallelToolExecution(true)
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "goodTool", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "failingTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - both tools should produce outputs, stream should complete
			assertNotNull(results);
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

	}

	@Nested
	@DisplayName("executeToolCallsStreaming Concurrency Tests")
	class ExecuteToolCallsStreamingConcurrencyTests {

		@Test
		@DisplayName("should respect maxParallelTools limit")
		void shouldRespectMaxParallelToolsLimit() throws InterruptedException {
			// Given
			AtomicInteger concurrentCount = new AtomicInteger(0);
			AtomicInteger maxConcurrent = new AtomicInteger(0);
			CountDownLatch startLatch = new CountDownLatch(1);

			StreamingToolCallback slowTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("slowTool").description("Slow tool").inputSchema("{}").build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.create(sink -> {
						int current = concurrentCount.incrementAndGet();
						maxConcurrent.updateAndGet(max -> Math.max(max, current));

						try {
							startLatch.await(5, TimeUnit.SECONDS);
							Thread.sleep(50);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}

						sink.next(ToolResult.finalChunk("done"));
						sink.complete();
						concurrentCount.decrementAndGet();
					});
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(slowTool))
				.parallelToolExecution(true)
				.maxParallelTools(2) // Limit to 2 concurrent
				.build();

			// Create 4 tool calls
			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "slowTool", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "slowTool", "{}"),
					new AssistantMessage.ToolCall("call_3", "function", "slowTool", "{}"),
					new AssistantMessage.ToolCall("call_4", "function", "slowTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			startLatch.countDown();
			flux.collectList().block(Duration.ofSeconds(15));

			// Then - max concurrent should not exceed limit
			assertTrue(maxConcurrent.get() <= 2, "Max concurrent should not exceed limit: " + maxConcurrent.get());
		}

		@Test
		@DisplayName("should execute sequentially when parallel disabled")
		void shouldExecuteSequentially_whenParallelDisabled() throws InterruptedException {
			// Given
			List<String> executionOrder = new ArrayList<>();

			ToolCallback tool1 = createSimpleTool("tool1", args -> {
				synchronized (executionOrder) {
					executionOrder.add("tool1");
				}
				return "result1";
			});

			ToolCallback tool2 = createSimpleTool("tool2", args -> {
				synchronized (executionOrder) {
					executionOrder.add("tool2");
				}
				return "result2";
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2))
				.parallelToolExecution(false)
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "tool1", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "tool2", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			flux.collectList().block(Duration.ofSeconds(10));

			// Then - tools should execute in order
			assertEquals(List.of("tool1", "tool2"), executionOrder);
		}

	}

	@Nested
	@DisplayName("executeToolCallsStreaming Output Verification Tests")
	class ExecuteToolCallsStreamingOutputTests {

		@Test
		@DisplayName("should include tool identification in output")
		void shouldIncludeToolIdentification_inOutput() {
			// Given
			ToolCallback tool = createSimpleTool("identifiedTool", args -> "result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_xyz", "function", "identifiedTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the tool output
			ToolStreamingOutput<?> toolOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(toolOutput);
			assertEquals("call_xyz", toolOutput.getToolCallId());
			assertEquals("identifiedTool", toolOutput.getToolName());
		}

		@Test
		@DisplayName("should include ToolResult in final done map")
		void shouldIncludeToolResult_inFinalDoneMap() {
			// Given
			ToolCallback tool = createSimpleTool("resultTool", args -> "expected-result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "resultTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(results);
			GraphResponse<?> doneResponse = (GraphResponse<?>) results.get(results.size() - 1);
			assertTrue(doneResponse.isDone());
			// The done map should contain tool response info
			assertNotNull(doneResponse.resultValue());
		}

	}

	// Helper methods

	private StreamingToolCallback createStreamingTool(String name, int chunkCount) {
		return new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Streaming tool " + name)
					.inputSchema("{}")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				return Flux.range(1, chunkCount)
					.map(i -> i == chunkCount ? ToolResult.finalChunk("chunk" + i) : ToolResult.chunk("chunk" + i));
			}

			@Override
			public String call(String toolInput) {
				return callStream(toolInput, new ToolContext(Map.of())).reduce(ToolResult::merge)
					.map(ToolResult::toStringResult)
					.block();
			}
		};
	}

	private AsyncToolCallback createAsyncTool(String name, String result) {
		return new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Async tool " + name)
					.inputSchema("{}")
					.build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				return CompletableFuture.completedFuture(result);
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};
	}

	private ToolCallback createSimpleTool(String name, Function<String, String> logic) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Tool " + name)
					.inputSchema("{}")
					.build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				return logic.apply(toolInput);
			}

			@Override
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};
	}

	private OverAllState createStateWithMessages(Message... messages) {
		Map<String, Object> stateData = new HashMap<>();
		stateData.put("messages", new ArrayList<>(List.of(messages)));
		return new OverAllState(stateData);
	}

	@Nested
	@DisplayName("Config Metadata Merge Tests")
	class ConfigMetadataMergeTests {

		@Test
		@DisplayName("should pass config metadata to tool context in streaming mode")
		void shouldPassConfigMetadata_toToolContext_inStreamingMode() {
			// Given
			AtomicReference<Map<String, Object>> capturedContext = new AtomicReference<>();

			ToolCallback contextCapturingTool = new ToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("contextTool")
						.description("Tool that captures context")
						.inputSchema("{}")
						.build();
				}

				@Override
				public String call(String toolInput, ToolContext toolContext) {
					capturedContext.set(new HashMap<>(toolContext.getContext()));
					return "done";
				}

				@Override
				public String call(String toolInput) {
					return call(toolInput, new ToolContext(Map.of()));
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(contextCapturingTool)).build();

			// Config with metadata
			RunnableConfig config = RunnableConfig.builder()
				.addMetadata("custom_key", "custom_value")
				.addMetadata("tenant_id", "tenant123")
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "contextTool", "{}"));

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(capturedContext.get());
			assertEquals("custom_value", capturedContext.get().get("custom_key"));
			assertEquals("tenant123", capturedContext.get().get("tenant_id"));
		}

	}

	@Nested
	@DisplayName("CancellableAsyncToolCallback Streaming Tests")
	class CancellableAsyncToolCallbackStreamingTests {

		@Test
		@DisplayName("should pass cancellation token to CancellableAsyncToolCallback in streaming mode")
		void shouldPassCancellationToken_toCancellableAsyncToolCallback_inStreamingMode() {
			// Given
			AtomicReference<CancellationToken> capturedToken = new AtomicReference<>();

			CancellableAsyncToolCallback cancellableTool = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("cancellableTool")
						.description("Cancellable async tool")
						.inputSchema("{}")
						.build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					capturedToken.set(cancellationToken);
					return CompletableFuture.completedFuture("done");
				}

				@Override
				public String call(String toolInput) {
					return "done";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(cancellableTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "cancellableTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(capturedToken.get());
			assertFalse(capturedToken.get().isCancelled());
		}

		@Test
		@DisplayName("should cancel token on timeout for CancellableAsyncToolCallback in streaming mode")
		void shouldCancelToken_onTimeout_forCancellableAsyncToolCallback_inStreamingMode() throws Exception {
			// Given
			AtomicReference<CancellationToken> capturedToken = new AtomicReference<>();
			CountDownLatch tokenCancelledLatch = new CountDownLatch(1);

			CancellableAsyncToolCallback slowCancellableTool = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("slowCancellableTool")
						.description("Slow cancellable tool")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(200); // Short timeout
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					capturedToken.set(cancellationToken);
					cancellationToken.onCancel(() -> tokenCancelledLatch.countDown());

					// This will not complete before timeout
					return new CompletableFuture<>(); // Never completes
				}

				@Override
				public String call(String toolInput) {
					return "done";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(slowCancellableTool))
				.toolExecutionTimeout(Duration.ofMillis(200))
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "slowCancellableTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			flux.collectList().block(Duration.ofSeconds(10));

			// Then - token should have been cancelled
			assertNotNull(capturedToken.get());
			assertTrue(capturedToken.get().isCancelled());
			// Verify callback was invoked
			assertTrue(tokenCancelledLatch.await(2, TimeUnit.SECONDS));
		}

	}

	@Nested
	@DisplayName("Integration Tests for Bug Fixes")
	class BugFixIntegrationTests {

		@Test
		@DisplayName("should handle all fix scenarios in single streaming execution")
		void shouldHandle_allFixScenarios_inSingleStreamingExecution() {
			// This test verifies:
			// 1. Config metadata is passed correctly
			// 2. Tools can access metadata
			// 3. Streaming completes properly

			AtomicReference<String> capturedMetadata = new AtomicReference<>();

			ToolCallback metadataCapturingTool = new ToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("metadataTool")
						.description("Tool that uses metadata")
						.inputSchema("{}")
						.build();
				}

				@Override
				public String call(String toolInput, ToolContext toolContext) {
					Object value = toolContext.getContext().get("trace_id");
					capturedMetadata.set(value != null ? value.toString() : null);
					return "processed";
				}

				@Override
				public String call(String toolInput) {
					return call(toolInput, new ToolContext(Map.of()));
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(metadataCapturingTool)).build();

			RunnableConfig config = RunnableConfig.builder()
				.addMetadata("trace_id", "trace-12345")
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "metadataTool", "{}"));

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then
			assertNotNull(results);
			assertEquals("trace-12345", capturedMetadata.get());

			// Should end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
			assertTrue(((GraphResponse<?>) lastItem).isDone());
		}

		@Test
		@DisplayName("streaming execution should handle Reactor timeout gracefully")
		void streamingExecution_shouldHandle_reactorTimeoutGracefully() {
			// Given - tool that will timeout via Reactor .timeout()
			StreamingToolCallback slowStreamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("slowStream")
						.description("Slow streaming tool")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(200);
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					// Never emits - will timeout
					return Flux.never();
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(slowStreamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "slowStream", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should complete with error output, not crash
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find error output
			ToolStreamingOutput<?> errorOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(errorOutput);
			if (errorOutput.getChunkData() instanceof ToolResult result) {
				assertTrue(result.getTextContent().contains("timed out")
						|| result.getTextContent().contains("Error:"));
			}

			// Should still end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

	}

}
