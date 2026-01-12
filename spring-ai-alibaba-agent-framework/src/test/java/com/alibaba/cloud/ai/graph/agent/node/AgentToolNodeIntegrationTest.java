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
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StateAwareToolCallback;

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
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for AgentToolNode covering apply(), sequential execution,
 * partial response handling, interceptor chain, and tool resolution.
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Integration Tests")
class AgentToolNodeIntegrationTest {

	private AgentToolNode.Builder baseBuilder;

	@BeforeEach
	void setUp() {
		baseBuilder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofSeconds(5))
			.toolExecutionExceptionProcessor(
					DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build());
	}

	@Nested
	@DisplayName("apply() Method Tests")
	class ApplyMethodTests {

		@Test
		@DisplayName("should execute single tool and return updated state")
		void apply_shouldExecuteSingleTool_andReturnUpdatedState() throws Exception {
			// Given
			ToolCallback echoTool = createSimpleTool("echo", args -> "Echo: " + args);
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(echoTool)).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "echo", "{\"message\":\"hello\"}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			assertNotNull(result);
			assertTrue(result.containsKey("messages"));
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertEquals(1, responseMessage.getResponses().size());
			assertEquals("Echo: {\"message\":\"hello\"}", responseMessage.getResponses().get(0).responseData());
		}

		@Test
		@DisplayName("should execute multiple tools sequentially when parallel disabled")
		void apply_shouldExecuteMultipleToolsSequentially_whenParallelDisabled() throws Exception {
			// Given
			AtomicInteger executionOrder = new AtomicInteger(0);
			List<Integer> orderRecord = new ArrayList<>();

			ToolCallback tool1 = createSimpleTool("tool1", args -> {
				orderRecord.add(executionOrder.incrementAndGet());
				return "result1";
			});
			ToolCallback tool2 = createSimpleTool("tool2", args -> {
				orderRecord.add(executionOrder.incrementAndGet());
				return "result2";
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2))
				.parallelToolExecution(false)
				.build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "tool1", "{}"), createToolCall("call-2", "tool2", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertEquals(2, responseMessage.getResponses().size());
			assertEquals(List.of(1, 2), orderRecord, "Tools should execute in order");
		}

		@Test
		@DisplayName("should execute tools in parallel when enabled")
		void apply_shouldExecuteToolsInParallel_whenEnabled() throws Exception {
			// Given
			CountDownLatch startLatch = new CountDownLatch(2);
			CountDownLatch continueLatch = new CountDownLatch(1);

			ToolCallback tool1 = createSimpleTool("tool1", args -> {
				startLatch.countDown();
				try {
					continueLatch.await(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return "result1";
			});
			ToolCallback tool2 = createSimpleTool("tool2", args -> {
				startLatch.countDown();
				try {
					continueLatch.await(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return "result2";
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2))
				.parallelToolExecution(true)
				.maxParallelTools(5)
				.build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "tool1", "{}"), createToolCall("call-2", "tool2", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			CompletableFuture<Map<String, Object>> futureResult = CompletableFuture.supplyAsync(() -> {
				try {
					return node.apply(state, config);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			// Wait for both tools to start (proves parallel execution)
			boolean bothStarted = startLatch.await(2, TimeUnit.SECONDS);
			assertTrue(bothStarted, "Both tools should start in parallel");

			// Allow tools to complete
			continueLatch.countDown();

			// Then
			Map<String, Object> result = futureResult.get(5, TimeUnit.SECONDS);
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertEquals(2, responseMessage.getResponses().size());
		}

		@Test
		@DisplayName("should inject state for StateAwareToolCallback")
		void apply_shouldInjectState_forStateAwareToolCallback() throws Exception {
			// Given
			AtomicReference<OverAllState> capturedState = new AtomicReference<>();
			AtomicReference<RunnableConfig> capturedConfig = new AtomicReference<>();
			AtomicReference<Map<String, Object>> capturedUpdateMap = new AtomicReference<>();

			StateAwareToolCallback stateAwareTool = new StateAwareToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("stateAware").description("State aware tool").inputSchema("{}").build();
				}

				@Override
				@SuppressWarnings("unchecked")
				public String call(String toolInput, ToolContext toolContext) {
					capturedState.set((OverAllState) toolContext.getContext().get(AGENT_STATE_CONTEXT_KEY));
					capturedConfig.set((RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY));
					capturedUpdateMap
						.set((Map<String, Object>) toolContext.getContext().get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY));
					return "state-aware-result";
				}

				@Override
				public String call(String toolInput) {
					return call(toolInput, new ToolContext(Map.of()));
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(stateAwareTool)).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "stateAware", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			node.apply(state, config);

			// Then
			assertNotNull(capturedState.get(), "State should be injected");
			assertNotNull(capturedConfig.get(), "Config should be injected");
			assertNotNull(capturedUpdateMap.get(), "Update map should be injected");
		}

		@Test
		@DisplayName("should throw exception for non-AssistantMessage and non-ToolResponseMessage")
		void apply_shouldThrowException_forInvalidMessageType() {
			// Given
			ToolCallback tool = createSimpleTool("tool", args -> "result");
			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool)).build();

			// Create state with a user message (invalid)
			Map<String, Object> stateData = new HashMap<>();
			stateData.put("messages", List.of(new org.springframework.ai.chat.messages.UserMessage("test")));
			OverAllState state = new OverAllState(stateData);

			RunnableConfig config = RunnableConfig.builder().build();

			// When/Then
			assertThrows(IllegalStateException.class, () -> node.apply(state, config));
		}

	}

	@Nested
	@DisplayName("Sequential Execution Tests")
	class SequentialExecutionTests {

		@Test
		@DisplayName("should maintain tool response order in sequential mode")
		void executeSequential_shouldMaintainOrder() throws Exception {
			// Given
			ToolCallback tool1 = createSimpleTool("tool1", args -> "result1");
			ToolCallback tool2 = createSimpleTool("tool2", args -> "result2");
			ToolCallback tool3 = createSimpleTool("tool3", args -> "result3");

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2, tool3))
				.parallelToolExecution(false)
				.build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "tool1", "{}"), createToolCall("call-2", "tool2", "{}"),
					createToolCall("call-3", "tool3", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			List<ToolResponseMessage.ToolResponse> responses = responseMessage.getResponses();
			assertEquals(3, responses.size());
			assertEquals("call-1", responses.get(0).id());
			assertEquals("call-2", responses.get(1).id());
			assertEquals("call-3", responses.get(2).id());
		}

		@Test
		@DisplayName("should accumulate state updates from all tools")
		void executeSequential_shouldAccumulateStateUpdates() throws Exception {
			// Given
			StateAwareToolCallback tool1 = createStateAwareToolWithUpdate("tool1", "key1", "value1");
			StateAwareToolCallback tool2 = createStateAwareToolWithUpdate("tool2", "key2", "value2");

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2))
				.parallelToolExecution(false)
				.build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "tool1", "{}"), createToolCall("call-2", "tool2", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			assertEquals("value1", result.get("key1"));
			assertEquals("value2", result.get("key2"));
		}

	}

	@Nested
	@DisplayName("Partial Response Handling Tests")
	class PartialResponseHandlingTests {

		@Test
		@DisplayName("should resume from partial tool responses")
		void handlePartialResponses_shouldResume() throws Exception {
			// Given
			ToolCallback tool1 = createSimpleTool("tool1", args -> "result1");
			ToolCallback tool2 = createSimpleTool("tool2", args -> "result2");
			ToolCallback tool3 = createSimpleTool("tool3", args -> "result3");

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1, tool2, tool3))
				.parallelToolExecution(false)
				.build();

			// Create assistant message with 3 tool calls
			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "tool1", "{}"), createToolCall("call-2", "tool2", "{}"),
					createToolCall("call-3", "tool3", "{}"));

			// Create partial response (only tool1 completed)
			ToolResponseMessage partialResponse = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "tool1", "result1")))
				.build();

			// State with both messages (last message is partial response)
			Map<String, Object> stateData = new HashMap<>();
			List<Message> messages = new ArrayList<>();
			messages.add(assistantMessage);
			messages.add(partialResponse);
			stateData.put("messages", messages);
			OverAllState state = new OverAllState(stateData);

			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			assertTrue(result.containsKey("messages"));
			// The result should contain merged responses for all tools
			Object messagesResult = result.get("messages");
			assertTrue(messagesResult instanceof List, "Messages should be a list with merged response and removal");
		}

		@Test
		@DisplayName("should return empty map when all tools already completed")
		void handlePartialResponses_shouldReturnEmpty_whenAllCompleted() throws Exception {
			// Given
			ToolCallback tool1 = createSimpleTool("tool1", args -> "result1");

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool1)).parallelToolExecution(false).build();

			// Create assistant message with 1 tool call
			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "tool1", "{}"));

			// Create complete response (all tools completed)
			ToolResponseMessage completeResponse = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "tool1", "result1")))
				.build();

			Map<String, Object> stateData = new HashMap<>();
			List<Message> messages = new ArrayList<>();
			messages.add(assistantMessage);
			messages.add(completeResponse);
			stateData.put("messages", messages);
			OverAllState state = new OverAllState(stateData);

			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			assertTrue(result.isEmpty(), "Should return empty map when all tools completed");
		}

	}

	@Nested
	@DisplayName("Interceptor Chain Integration Tests")
	class InterceptorChainTests {

		@Test
		@DisplayName("should execute interceptors before and after tool")
		void interceptorChain_shouldExecuteBeforeAndAfter() throws Exception {
			// Given
			List<String> executionOrder = new ArrayList<>();

			ToolInterceptor interceptor = new ToolInterceptor() {
				@Override
				public String getName() {
					return "testInterceptor";
				}

				@Override
				public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
					executionOrder.add("before-" + request.getToolName());
					ToolCallResponse response = handler.call(request);
					executionOrder.add("after-" + request.getToolName());
					return response;
				}
			};

			ToolCallback tool = createSimpleTool("testTool", args -> {
				executionOrder.add("execute-testTool");
				return "result";
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool)).build();
			node.setToolInterceptors(List.of(interceptor));

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "testTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			node.apply(state, config);

			// Then
			assertEquals(List.of("before-testTool", "execute-testTool", "after-testTool"), executionOrder);
		}

		@Test
		@DisplayName("should allow request modification in interceptor")
		void interceptorChain_shouldAllowRequestModification() throws Exception {
			// Given
			AtomicReference<String> capturedArgs = new AtomicReference<>();

			ToolInterceptor modifyingInterceptor = new ToolInterceptor() {
				@Override
				public String getName() {
					return "modifyingInterceptor";
				}

				@Override
				public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
					// Modify the request arguments
					ToolCallRequest modifiedRequest = ToolCallRequest.builder(request)
						.arguments("{\"modified\":true}")
						.build();
					return handler.call(modifiedRequest);
				}
			};

			ToolCallback tool = createSimpleTool("testTool", args -> {
				capturedArgs.set(args);
				return "result";
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool)).build();
			node.setToolInterceptors(List.of(modifyingInterceptor));

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "testTool", "{\"original\":true}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			node.apply(state, config);

			// Then
			assertEquals("{\"modified\":true}", capturedArgs.get());
		}

		@Test
		@DisplayName("should chain multiple interceptors in order")
		void interceptorChain_shouldChainMultipleInterceptors() throws Exception {
			// Given
			List<String> executionOrder = new ArrayList<>();

			ToolInterceptor interceptor1 = new ToolInterceptor() {
				@Override
				public String getName() {
					return "interceptor1";
				}

				@Override
				public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
					executionOrder.add("interceptor1-before");
					ToolCallResponse response = handler.call(request);
					executionOrder.add("interceptor1-after");
					return response;
				}
			};

			ToolInterceptor interceptor2 = new ToolInterceptor() {
				@Override
				public String getName() {
					return "interceptor2";
				}

				@Override
				public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
					executionOrder.add("interceptor2-before");
					ToolCallResponse response = handler.call(request);
					executionOrder.add("interceptor2-after");
					return response;
				}
			};

			ToolCallback tool = createSimpleTool("testTool", args -> {
				executionOrder.add("execute");
				return "result";
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(tool)).build();
			node.setToolInterceptors(List.of(interceptor1, interceptor2));

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "testTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			node.apply(state, config);

			// Then
			assertEquals(
					List.of("interceptor1-before", "interceptor2-before", "execute", "interceptor2-after",
							"interceptor1-after"),
					executionOrder);
		}

	}

	@Nested
	@DisplayName("Tool Resolution Tests")
	class ToolResolutionTests {

		@Test
		@DisplayName("should use callback resolver when direct lookup fails")
		void resolve_shouldUseCallbackResolver_whenDirectLookupFails() throws Exception {
			// Given
			AtomicBoolean resolverCalled = new AtomicBoolean(false);
			ToolCallback resolvedTool = createSimpleTool("dynamicTool", args -> "resolved-result");

			ToolCallbackResolver resolver = toolName -> {
				resolverCalled.set(true);
				if ("dynamicTool".equals(toolName)) {
					return resolvedTool;
				}
				return null;
			};

			// Empty tool callbacks list - will fall back to resolver
			AgentToolNode node = baseBuilder.toolCallbacks(List.of()).toolCallbackResolver(resolver).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "dynamicTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			assertTrue(resolverCalled.get(), "Resolver should be called");
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertEquals("resolved-result", responseMessage.getResponses().get(0).responseData());
		}

		@Test
		@DisplayName("should throw exception when tool not found anywhere")
		void resolve_shouldThrow_whenToolNotFound() {
			// Given
			AgentToolNode node = baseBuilder.toolCallbacks(List.of()).toolCallbackResolver(null).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "nonExistentTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When/Then
			assertThrows(IllegalStateException.class, () -> node.apply(state, config));
		}

	}

	@Nested
	@DisplayName("Sync Tool Execution Tests")
	class SyncToolExecutionTests {

		@Test
		@DisplayName("should handle generic exceptions")
		void executeSyncTool_shouldHandleGenericException() throws Exception {
			// Given
			ToolCallback failingTool = createSimpleTool("failingTool", args -> {
				throw new RuntimeException("Unexpected error");
			});

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(failingTool)).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "failingTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then - should return error response
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertNotNull(responseMessage);
			assertTrue(responseMessage.getResponses().get(0).responseData().contains("Error:"));
		}

	}

	@Nested
	@DisplayName("Async Tool Execution Tests")
	class AsyncToolExecutionIntegrationTests {

		@Test
		@DisplayName("should execute async tool successfully")
		void executeAsyncTool_shouldSucceed() throws Exception {
			// Given
			AsyncToolCallback asyncTool = new AsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("asyncTool").description("Async tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
					return CompletableFuture.completedFuture("async-result");
				}

				@Override
				public String call(String toolInput) {
					return callAsync(toolInput, new ToolContext(Map.of())).join();
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(asyncTool)).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "asyncTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			Map<String, Object> result = node.apply(state, config);

			// Then
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertEquals("async-result", responseMessage.getResponses().get(0).responseData());
		}

		@Test
		@DisplayName("should pass real cancellation token to CancellableAsyncToolCallback")
		void executeAsyncTool_shouldPassRealToken_toCancellableCallback() throws Exception {
			// Given
			AtomicReference<CancellationToken> capturedToken = new AtomicReference<>();

			CancellableAsyncToolCallback cancellableTool = new CancellableAsyncToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder().name("cancellableTool").description("Cancellable tool").inputSchema("{}").build();
				}

				@Override
				public CompletableFuture<String> callAsync(String arguments, ToolContext context,
						CancellationToken cancellationToken) {
					capturedToken.set(cancellationToken);
					return CompletableFuture.completedFuture("cancellable-result");
				}

				@Override
				public String call(String toolInput) {
					return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
				}
			};

			AgentToolNode node = baseBuilder.toolCallbacks(List.of(cancellableTool)).build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call-1", "cancellableTool", "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);
			RunnableConfig config = RunnableConfig.builder().build();

			// When
			node.apply(state, config);

			// Then
			assertNotNull(capturedToken.get(), "Token should be passed");
			// Should NOT be NONE token
			assertFalse(capturedToken.get() == CancellationToken.NONE, "Should receive real token, not NONE");
		}

	}

	// Helper methods

	private ToolCallback createSimpleTool(String name, Function<String, String> logic) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name(name).description("Test tool " + name).inputSchema("{}").build();
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

	@SuppressWarnings("unchecked")
	private StateAwareToolCallback createStateAwareToolWithUpdate(String name, String updateKey, String updateValue) {
		return new StateAwareToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name(name).description("State aware tool " + name).inputSchema("{}").build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
					.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
				if (updateMap != null) {
					updateMap.put(updateKey, updateValue);
				}
				return "result-" + name;
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
		return AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(toolCalls))
			.build();
	}

	private OverAllState createStateWithMessages(Message... messages) {
		Map<String, Object> stateData = new HashMap<>();
		stateData.put("messages", new ArrayList<>(List.of(messages)));
		return new OverAllState(stateData);
	}

}
