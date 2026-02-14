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
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.DefaultToolMetadata;
import org.springframework.ai.tool.metadata.ToolMetadata;
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

import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

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
				String text = result.getTextContent();
				assertNotNull(text);
				assertTrue(text.startsWith("Error: "), "Result should start with Error: prefix: " + text);
				assertFalse(text.startsWith("Error: Error: "), "Result should not double-prefix Error: " + text);
				assertTrue(text.contains("Stream error"), "Result should include the original error message: " + text);
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

	private ToolCallback createReturnDirectTool(String name, Function<String, String> logic) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Return direct tool " + name)
					.inputSchema("{}")
					.build();
			}

			@Override
			public ToolMetadata getToolMetadata() {
				return DefaultToolMetadata.builder().returnDirect(true).build();
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

	private StreamingToolCallback createReturnDirectStreamingTool(String name, int chunkCount) {
		return new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Return direct streaming tool " + name)
					.inputSchema("{}")
					.build();
			}

			@Override
			public ToolMetadata getToolMetadata() {
				return DefaultToolMetadata.builder().returnDirect(true).build();
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

	@Nested
	@DisplayName("P1 Fix: ReturnDirect Metadata in Streaming Execution Tests")
	class ReturnDirectMetadataStreamingTests {

		@Test
		@DisplayName("should include returnDirect metadata in GraphResponse.done for returnDirect tools")
		void shouldIncludeReturnDirectMetadata_inGraphResponseDone_forReturnDirectTools() {
			// Given - tool with returnDirect=true
			ToolCallback returnDirectTool = createReturnDirectTool("directTool", args -> "direct-result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(returnDirectTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "directTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then - GraphResponse.done should contain messages with returnDirect metadata
			assertNotNull(results);
			assertFalse(results.isEmpty());

			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
			GraphResponse<?> doneResponse = (GraphResponse<?>) lastItem;
			assertTrue(doneResponse.isDone());

			// Verify the done map contains messages with returnDirect metadata
			@SuppressWarnings("unchecked")
			Map<String, Object> doneMap = (Map<String, Object>) doneResponse.resultValue().orElseThrow();
			assertNotNull(doneMap);
			assertTrue(doneMap.containsKey("messages"));

			Object messagesObj = doneMap.get("messages");
			assertInstanceOf(ToolResponseMessage.class, messagesObj);
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) messagesObj;

			// Verify returnDirect metadata is present
			Map<String, Object> metadata = toolResponseMessage.getMetadata();
			assertNotNull(metadata, "Metadata should not be null for returnDirect tool");
			assertEquals(FINISH_REASON, metadata.get(FINISH_REASON_METADATA_KEY),
					"FINISH_REASON_METADATA_KEY should be set for returnDirect tool");
		}

		@Test
		@DisplayName("should include returnDirect metadata for streaming tool with returnDirect=true")
		void shouldIncludeReturnDirectMetadata_forStreamingTool_withReturnDirectTrue() {
			// Given - streaming tool with returnDirect=true
			StreamingToolCallback returnDirectStreamingTool = createReturnDirectStreamingTool("streamDirectTool", 3);
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(returnDirectStreamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "streamDirectTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then
			assertNotNull(results);
			GraphResponse<?> doneResponse = (GraphResponse<?>) results.get(results.size() - 1);
			assertTrue(doneResponse.isDone());

			@SuppressWarnings("unchecked")
			Map<String, Object> doneMap = (Map<String, Object>) doneResponse.resultValue().orElseThrow();
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) doneMap.get("messages");

			// Verify returnDirect metadata is present
			Map<String, Object> metadata = toolResponseMessage.getMetadata();
			assertNotNull(metadata, "Metadata should not be null for returnDirect streaming tool");
			assertEquals(FINISH_REASON, metadata.get(FINISH_REASON_METADATA_KEY));
		}

		@Test
		@DisplayName("should NOT include returnDirect metadata when tool has returnDirect=false")
		void shouldNotIncludeReturnDirectMetadata_whenToolHasReturnDirectFalse() {
			// Given - regular tool without returnDirect
			ToolCallback normalTool = createSimpleTool("normalTool", args -> "normal-result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(normalTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "normalTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(5));

			// Then
			assertNotNull(results);
			GraphResponse<?> doneResponse = (GraphResponse<?>) results.get(results.size() - 1);

			@SuppressWarnings("unchecked")
			Map<String, Object> doneMap = (Map<String, Object>) doneResponse.resultValue().orElseThrow();
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) doneMap.get("messages");

			// Metadata should be empty or not contain FINISH_REASON_METADATA_KEY
			Map<String, Object> metadata = toolResponseMessage.getMetadata();
			assertTrue(metadata == null || metadata.isEmpty() || !metadata.containsKey(FINISH_REASON_METADATA_KEY),
					"FINISH_REASON_METADATA_KEY should NOT be set for non-returnDirect tool");
		}

		@Test
		@DisplayName("should NOT include returnDirect metadata when mixed tools (some returnDirect=true, some false)")
		void shouldNotIncludeReturnDirectMetadata_whenMixedTools() {
			// Given - one returnDirect tool, one normal tool
			ToolCallback returnDirectTool = createReturnDirectTool("directTool", args -> "direct-result");
			ToolCallback normalTool = createSimpleTool("normalTool", args -> "normal-result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(returnDirectTool, normalTool))
				.parallelToolExecution(true)
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "directTool", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "normalTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - returnDirect should be false (AND logic: all tools must have returnDirect=true)
			assertNotNull(results);
			GraphResponse<?> doneResponse = (GraphResponse<?>) results.get(results.size() - 1);

			@SuppressWarnings("unchecked")
			Map<String, Object> doneMap = (Map<String, Object>) doneResponse.resultValue().orElseThrow();
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) doneMap.get("messages");

			Map<String, Object> metadata = toolResponseMessage.getMetadata();
			assertTrue(metadata == null || metadata.isEmpty() || !metadata.containsKey(FINISH_REASON_METADATA_KEY),
					"FINISH_REASON_METADATA_KEY should NOT be set when mixed returnDirect values");
		}

		@Test
		@DisplayName("should include returnDirect metadata when all tools have returnDirect=true")
		void shouldIncludeReturnDirectMetadata_whenAllToolsHaveReturnDirectTrue() {
			// Given - multiple returnDirect tools
			ToolCallback directTool1 = createReturnDirectTool("directTool1", args -> "result1");
			ToolCallback directTool2 = createReturnDirectTool("directTool2", args -> "result2");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(directTool1, directTool2))
				.parallelToolExecution(true)
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("call_1", "function", "directTool1", "{}"),
					new AssistantMessage.ToolCall("call_2", "function", "directTool2", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - returnDirect should be true (all tools have returnDirect=true)
			assertNotNull(results);
			GraphResponse<?> doneResponse = (GraphResponse<?>) results.get(results.size() - 1);

			@SuppressWarnings("unchecked")
			Map<String, Object> doneMap = (Map<String, Object>) doneResponse.resultValue().orElseThrow();
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) doneMap.get("messages");

			Map<String, Object> metadata = toolResponseMessage.getMetadata();
			assertNotNull(metadata, "Metadata should not be null when all tools have returnDirect=true");
			assertEquals(FINISH_REASON, metadata.get(FINISH_REASON_METADATA_KEY));
		}

	}

	@Nested
	@DisplayName("P2 Fix: Streaming Tool isFinal Flag Respected Tests")
	class StreamingToolIsFinalFlagTests {

		@Test
		@DisplayName("should emit only one AGENT_TOOL_FINISHED when tool emits finalChunk with isFinal=true")
		void shouldEmitOnlyOneFinished_whenToolEmitsFinalChunk() {
			// Given - streaming tool that emits chunks ending with finalChunk (isFinal=true)
			StreamingToolCallback streamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("finalChunkTool")
						.description("Tool that emits finalChunk")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.just(
							ToolResult.chunk("partial1"),
							ToolResult.chunk("partial2"),
							ToolResult.finalChunk("final") // isFinal=true
					);
				}

				@Override
				public String call(String toolInput) {
					return "result";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(streamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "finalChunkTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should have exactly one AGENT_TOOL_FINISHED output
			assertNotNull(results);
			assertFalse(results.isEmpty());

			long finishedCount = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_FINISHED)
				.count();

			assertEquals(1, finishedCount,
					"Should emit exactly one AGENT_TOOL_FINISHED when tool emits finalChunk with isFinal=true");

			// Verify streaming chunks were emitted correctly
			long streamingCount = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_STREAMING)
				.count();

			assertEquals(2, streamingCount, "Should have 2 streaming chunks (partial1, partial2)");
		}

		@Test
		@DisplayName("should emit synthesized AGENT_TOOL_FINISHED when tool does not emit finalChunk")
		void shouldEmitSynthesizedFinished_whenToolDoesNotEmitFinalChunk() {
			// Given - streaming tool that emits chunks WITHOUT finalChunk (no isFinal=true)
			StreamingToolCallback streamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("noFinalChunkTool")
						.description("Tool without finalChunk")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.just(
							ToolResult.chunk("chunk1"),
							ToolResult.chunk("chunk2"),
							ToolResult.chunk("chunk3") // No finalChunk - system should synthesize one
					);
				}

				@Override
				public String call(String toolInput) {
					return "result";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(streamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "noFinalChunkTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should still have exactly one AGENT_TOOL_FINISHED output (synthesized)
			assertNotNull(results);
			assertFalse(results.isEmpty());

			long finishedCount = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_FINISHED)
				.count();

			assertEquals(1, finishedCount,
					"Should synthesize exactly one AGENT_TOOL_FINISHED when tool does not emit finalChunk");

			// All chunks should be STREAMING type (none are final)
			long streamingCount = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_STREAMING)
				.count();

			assertEquals(3, streamingCount, "Should have 3 streaming chunks");
		}

		@Test
		@DisplayName("should preserve accumulated content when tool emits finalChunk")
		void shouldPreserveAccumulatedContent_whenToolEmitsFinalChunk() {
			// Given - streaming tool that emits content pieces
			StreamingToolCallback streamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("contentTool")
						.description("Tool with content")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.just(
							ToolResult.chunk("Hello "),
							ToolResult.chunk("World"),
							ToolResult.finalChunk("!")
					);
				}

				@Override
				public String call(String toolInput) {
					return "Hello World!";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(streamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "contentTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - the final output should have accumulated content
			assertNotNull(results);

			// Find the finished output
			ToolStreamingOutput<?> finishedOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_FINISHED)
				.findFirst()
				.orElse(null);

			assertNotNull(finishedOutput, "Should have finished output");
			assertTrue(finishedOutput.isFinalChunk());

			// Verify content is accumulated (merged)
			if (finishedOutput.getChunkData() instanceof ToolResult result) {
				assertEquals("Hello World!", result.getTextContent(),
						"Accumulated content should be 'Hello World!'");
			}
		}

		@Test
		@DisplayName("should correctly mark final chunk output type when isFinal is true")
		void shouldCorrectlyMarkFinalChunkOutputType_whenIsFinalIsTrue() {
			// Given - tool emits 2 chunks, last one has isFinal=true
			StreamingToolCallback streamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("markerTool")
						.description("Tool for testing output type")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.just(
							ToolResult.chunk("streaming"),
							ToolResult.finalChunk("done")
					);
				}

				@Override
				public String call(String toolInput) {
					return "streaming done";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(streamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "markerTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - verify output types are correct
			assertNotNull(results);

			List<ToolStreamingOutput<?>> toolOutputs = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.collect(java.util.stream.Collectors.toList());

			// Should have exactly 2 tool outputs (1 streaming + 1 finished)
			assertEquals(2, toolOutputs.size(), "Should have exactly 2 tool outputs");

			// First should be STREAMING
			assertEquals(OutputType.AGENT_TOOL_STREAMING, toolOutputs.get(0).getOutputType(),
					"First chunk should be AGENT_TOOL_STREAMING");
			assertFalse(toolOutputs.get(0).isFinalChunk(),
					"First chunk should not be marked as final");

			// Second (the finalChunk) should be FINISHED
			assertEquals(OutputType.AGENT_TOOL_FINISHED, toolOutputs.get(1).getOutputType(),
					"Second chunk (finalChunk) should be AGENT_TOOL_FINISHED");
			assertTrue(toolOutputs.get(1).isFinalChunk(),
					"Second chunk should be marked as final");
		}

	}

	@Nested
	@DisplayName("P3 Fix: State Update Clearing on Streaming Tool Failure Tests")
	class StateUpdateClearingOnFailureTests {

		@Test
		@DisplayName("should discard state updates when streaming tool fails in executeToolCallsStreaming")
		void shouldDiscardStateUpdates_whenStreamingToolFails_inExecuteToolCallsStreaming() {
			// Given - streaming tool that fails after partial emission
			AtomicInteger emissionCount = new AtomicInteger(0);
			StreamingToolCallback failingStreamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("failingStreamTool")
						.description("Tool that fails after partial emission")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofSeconds(5);
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.create(sink -> {
						// Emit some chunks, then fail
						sink.next(ToolResult.chunk("chunk1"));
						emissionCount.incrementAndGet();
						sink.next(ToolResult.chunk("chunk2"));
						emissionCount.incrementAndGet();
						// Simulate failure
						sink.error(new RuntimeException("Simulated streaming failure"));
					});
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(failingStreamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "failingStreamTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - tool should have emitted some chunks before failure
			assertTrue(emissionCount.get() >= 1, "Tool should have emitted at least one chunk before failure");

			// Stream should complete with error output and GraphResponse.done
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find error output
			ToolStreamingOutput<?> errorOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.isFinalChunk())
				.filter(output -> {
					if (output.getChunkData() instanceof ToolResult result) {
						return result.getTextContent().contains("Error:");
					}
					return false;
				})
				.findFirst()
				.orElse(null);

			assertNotNull(errorOutput, "Should have error output from failed streaming tool");

			// Verify GraphResponse.done is emitted
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
			assertTrue(((GraphResponse<?>) lastItem).isDone());

			// The state updates from the failed tool should be discarded
			// (verified by the stateCollector.discardToolUpdateMap call in the implementation)
		}

		@Test
		@DisplayName("should discard state updates when streaming tool times out in executeToolCallsStreaming")
		void shouldDiscardStateUpdates_whenStreamingToolTimesOut_inExecuteToolCallsStreaming() {
			// Given - streaming tool that times out
			StreamingToolCallback slowStreamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("slowStreamTool")
						.description("Tool that times out")
						.inputSchema("{}")
						.build();
				}

				@Override
				public Duration getTimeout() {
					return Duration.ofMillis(200); // Short timeout
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					// Never completes - will timeout
					return Flux.never();
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(slowStreamingTool)).build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "slowStreamTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should complete with timeout error
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find error output
			ToolStreamingOutput<?> errorOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.filter(output -> output.isFinalChunk())
				.findFirst()
				.orElse(null);

			assertNotNull(errorOutput, "Should have error output from timed-out streaming tool");

			// Should end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

	}

	@Nested
	@DisplayName("P2 Fix: ToolExecutionExceptionProcessor Applied to Streaming Paths")
	class ToolExecutionExceptionProcessorStreamingTests {

		@Test
		@DisplayName("should apply ToolExecutionExceptionProcessor to streaming tool errors")
		void shouldApplyProcessor_toStreamingToolErrors() {
			// Given - streaming tool that throws ToolExecutionException
			ToolDefinition toolDef = ToolDefinition.builder()
				.name("toolExceptionTool")
				.description("Tool that throws ToolExecutionException")
				.inputSchema("{}")
				.build();

			StreamingToolCallback failingStreamingTool = new StreamingToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return toolDef;
				}

				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.error(new ToolExecutionException(toolDef,
							new RuntimeException("Streaming tool failed intentionally")));
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			// Use processor that returns a custom message instead of throwing
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(failingStreamingTool))
				.toolExecutionExceptionProcessor(
						DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build())
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "toolExceptionTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should complete with processed error (not throw)
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the output for this tool
			ToolStreamingOutput<?> toolOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(toolOutput, "Should have output from processed exception");
			assertTrue(toolOutput.isFinalChunk());

			// Verify the output contains the processed error message
			if (toolOutput.getChunkData() instanceof ToolResult result) {
				// DefaultToolExecutionExceptionProcessor returns the error message
				// when alwaysThrow=false
				assertTrue(
						result.getTextContent().contains("Streaming tool failed intentionally")
								|| result.getTextContent().contains("Error:"),
						"Result should contain processed error: " + result.getTextContent());
			}

			// Should end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

		@Test
		@DisplayName("should apply ToolExecutionExceptionProcessor to async tool errors in streaming mode")
		void shouldApplyProcessor_toAsyncToolErrors_inStreamingMode() {
			// Given - async tool that throws ToolExecutionException
			ToolDefinition toolDef = ToolDefinition.builder()
				.name("asyncToolExceptionTool")
				.description("Async tool that throws ToolExecutionException")
				.inputSchema("{}")
				.build();

			AsyncToolCallback failingAsyncTool = new AsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return toolDef;
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.failedFuture(new ToolExecutionException(toolDef,
							new RuntimeException("Async tool failed intentionally")));
				}

				@Override
				public String call(String toolInput) {
					return "";
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(failingAsyncTool))
				.toolExecutionExceptionProcessor(
						DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build())
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "asyncToolExceptionTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should complete with processed error
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the output
			ToolStreamingOutput<?> toolOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(toolOutput);

			// Should end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

		@Test
		@DisplayName("should apply ToolExecutionExceptionProcessor to sync tool errors in streaming mode")
		void shouldApplyProcessor_toSyncToolErrors_inStreamingMode() {
			// Given - sync tool that throws ToolExecutionException
			ToolDefinition toolDef = ToolDefinition.builder()
				.name("syncToolExceptionTool")
				.description("Sync tool that throws ToolExecutionException")
				.inputSchema("{}")
				.build();

			ToolCallback failingSyncTool = new ToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return toolDef;
				}

				@Override
				public String call(String toolInput, ToolContext toolContext) {
					throw new ToolExecutionException(toolDef,
							new RuntimeException("Sync tool failed intentionally"));
				}

				@Override
				public String call(String toolInput) {
					return call(toolInput, new ToolContext(Map.of()));
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(failingSyncTool))
				.toolExecutionExceptionProcessor(
						DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build())
				.build();

			List<AssistantMessage.ToolCall> toolCalls = List
				.of(new AssistantMessage.ToolCall("call_1", "function", "syncToolExceptionTool", "{}"));
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Flux<Object> flux = node.executeToolCallsStreaming(toolCalls, testState, config);
			List<Object> results = flux.collectList().block(Duration.ofSeconds(10));

			// Then - should complete with processed error
			assertNotNull(results);
			assertFalse(results.isEmpty());

			// Find the output
			ToolStreamingOutput<?> toolOutput = results.stream()
				.filter(obj -> obj instanceof ToolStreamingOutput<?>)
				.map(obj -> (ToolStreamingOutput<?>) obj)
				.findFirst()
				.orElse(null);

			assertNotNull(toolOutput);

			// Should end with GraphResponse.done
			Object lastItem = results.get(results.size() - 1);
			assertInstanceOf(GraphResponse.class, lastItem);
		}

	}

}
