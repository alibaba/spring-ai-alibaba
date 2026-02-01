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
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.StateAwareToolCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for state isolation in sequential tool execution.
 *
 * <p>
 * This test validates the fix for the bug where a subsequent tool's timeout would clear
 * state updates from previously successful tools in sequential execution mode.
 * </p>
 *
 * <p>
 * Bug scenario before fix:
 * <ol>
 * <li>Tool1 executes successfully, writes {key1: value1} to shared map</li>
 * <li>Tool2 (async) times out</li>
 * <li>Timeout handler calls extraStateFromToolCall.clear() - cleared Tool1's data too</li>
 * <li>Final state merge lost Tool1's key1 update</li>
 * </ol>
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Sequential State Isolation Tests")
class AgentToolNodeSequentialStateIsolationTest {

	private static final String TOOL1_NAME = "successfulTool";

	private static final String TOOL2_NAME = "timeoutTool";

	private static final String TOOL1_STATE_KEY = "tool1_state_key";

	private static final String TOOL1_STATE_VALUE = "tool1_state_value";

	private AgentToolNode.Builder baseBuilder;

	@BeforeEach
	void setUp() {
		baseBuilder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofMillis(500)) // Short timeout for tests
			.toolExecutionExceptionProcessor(
					DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build());
	}

	@Test
	@Timeout(value = 15, unit = TimeUnit.SECONDS)
	@DisplayName("Sequential execution: Tool1's state should be preserved when Tool2 times out")
	void testSequentialStateIsolationOnTimeout() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(4);

		try {
			// Create tools where Tool1 succeeds and writes state, Tool2 times out
			ToolCallback tool1 = createStateWritingTool(TOOL1_NAME, TOOL1_STATE_KEY, TOOL1_STATE_VALUE);
			AsyncToolCallback tool2 = createTimeoutAsyncTool(TOOL2_NAME, Duration.ofSeconds(10)); // Will timeout

			AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2))
				.parallelToolExecution(false) // Sequential mode
				.wrapSyncToolsAsAsync(true) // Enable async wrapping to trigger timeout handling
				.toolExecutionTimeout(Duration.ofMillis(200)) // Short timeout
				.build();

			// Create tool calls: Tool1 first, then Tool2
			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call_1", TOOL1_NAME, "{}"), createToolCall("call_2", TOOL2_NAME, "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);

			RunnableConfig config = RunnableConfig.builder()
				.threadId("test-thread")
				.addParallelNodeExecutor("_AGENT_TOOL_", executor)
				.build();

			// Execute the tools
			Map<String, Object> result = toolNode.apply(state, config);

			// Tool1's state update should be preserved even though Tool2 timed out
			assertThat(result).containsKey(TOOL1_STATE_KEY);
			assertThat(result.get(TOOL1_STATE_KEY)).isEqualTo(TOOL1_STATE_VALUE);

			// Verify messages were returned
			assertThat(result).containsKey("messages");
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertThat(responseMessage.getResponses()).hasSize(2);

			// Tool1 should succeed, Tool2 should have error
			assertThat(responseMessage.getResponses().get(0).responseData()).contains("Success");
			assertThat(responseMessage.getResponses().get(1).responseData()).contains("timed out");
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	@Timeout(value = 15, unit = TimeUnit.SECONDS)
	@DisplayName("Sequential execution: Multiple successful tools should all have their state preserved")
	void testSequentialMultipleSuccessfulToolsStatePreserved() throws Exception {
		// Create multiple tools that all succeed and write state
		String tool2StateKey = "tool2_state_key";
		String tool2StateValue = "tool2_state_value";

		ToolCallback tool1 = createStateWritingTool(TOOL1_NAME, TOOL1_STATE_KEY, TOOL1_STATE_VALUE);
		ToolCallback tool2 = createStateWritingTool("tool2", tool2StateKey, tool2StateValue);

		AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2))
			.parallelToolExecution(false) // Sequential mode
			.toolExecutionTimeout(Duration.ofSeconds(30))
			.build();

		// Create tool calls
		AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
				createToolCall("call_1", TOOL1_NAME, "{}"), createToolCall("call_2", "tool2", "{}"));

		OverAllState state = createStateWithMessages(assistantMessage);

		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();

		// Execute
		Map<String, Object> result = toolNode.apply(state, config);

		// Both tools' state updates should be preserved
		assertThat(result).containsKey(TOOL1_STATE_KEY);
		assertThat(result.get(TOOL1_STATE_KEY)).isEqualTo(TOOL1_STATE_VALUE);
		assertThat(result).containsKey(tool2StateKey);
		assertThat(result.get(tool2StateKey)).isEqualTo(tool2StateValue);
	}

	@Test
	@Timeout(value = 15, unit = TimeUnit.SECONDS)
	@DisplayName("Sequential execution: State isolation should work with mixed sync/async tools")
	void testSequentialStateIsolationMixedTools() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(4);

		try {
			// Tool1: sync tool that succeeds and writes state
			// Tool2: async tool that succeeds and writes state
			// Tool3: async tool that times out
			String tool2StateKey = "tool2_state_key";
			String tool2StateValue = "tool2_state_value";

			ToolCallback tool1 = createStateWritingTool(TOOL1_NAME, TOOL1_STATE_KEY, TOOL1_STATE_VALUE);
			AsyncToolCallback tool2 = createStateWritingAsyncTool("asyncSuccessTool", tool2StateKey, tool2StateValue);
			AsyncToolCallback tool3 = createTimeoutAsyncTool(TOOL2_NAME, Duration.ofSeconds(10));

			AgentToolNode toolNode = baseBuilder.toolCallbacks(List.of(tool1, tool2, tool3))
				.parallelToolExecution(false) // Sequential mode
				.wrapSyncToolsAsAsync(true)
				.toolExecutionTimeout(Duration.ofMillis(300))
				.build();

			AssistantMessage assistantMessage = createAssistantMessageWithToolCalls(
					createToolCall("call_1", TOOL1_NAME, "{}"), createToolCall("call_2", "asyncSuccessTool", "{}"),
					createToolCall("call_3", TOOL2_NAME, "{}"));

			OverAllState state = createStateWithMessages(assistantMessage);

			RunnableConfig config = RunnableConfig.builder()
				.threadId("test-thread")
				.addParallelNodeExecutor("_AGENT_TOOL_", executor)
				.build();

			Map<String, Object> result = toolNode.apply(state, config);

			// Both successful tools' state should be preserved
			assertThat(result).containsKey(TOOL1_STATE_KEY);
			assertThat(result.get(TOOL1_STATE_KEY)).isEqualTo(TOOL1_STATE_VALUE);
			assertThat(result).containsKey(tool2StateKey);
			assertThat(result.get(tool2StateKey)).isEqualTo(tool2StateValue);

			// Verify all responses are present
			ToolResponseMessage responseMessage = (ToolResponseMessage) result.get("messages");
			assertThat(responseMessage.getResponses()).hasSize(3);
		}
		finally {
			executor.shutdownNow();
		}
	}

	// Helper methods

	private ToolCallback createStateWritingTool(String name, String stateKey, String stateValue) {
		return new StateWritingSyncTool(name, stateKey, stateValue);
	}

	private AsyncToolCallback createTimeoutAsyncTool(String name, Duration executionTime) {
		return new TimeoutAsyncTool(name, executionTime);
	}

	private AsyncToolCallback createStateWritingAsyncTool(String name, String stateKey, String stateValue) {
		return new StateWritingAsyncTool(name, stateKey, stateValue);
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

	/**
	 * A sync tool that writes state to the update map.
	 */
	private static class StateWritingSyncTool implements ToolCallback, StateAwareToolCallback {

		private final String name;

		private final String stateKey;

		private final String stateValue;

		StateWritingSyncTool(String name, String stateKey, String stateValue) {
			this.name = name;
			this.stateKey = stateKey;
			this.stateValue = stateValue;
		}

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder()
				.name(name)
				.description("Test tool")
				.inputSchema("{\"type\":\"object\",\"properties\":{}}")
				.build();
		}

		@Override
		@SuppressWarnings("unchecked")
		public String call(String toolInput, ToolContext toolContext) {
			// Write state to the update map
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
				.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put(stateKey, stateValue);
			}
			return "Success: " + name;
		}

		@Override
		public String call(String toolInput) {
			return call(toolInput, new ToolContext(Map.of()));
		}

	}

	/**
	 * An async tool that deliberately takes longer than the timeout.
	 */
	private static class TimeoutAsyncTool implements AsyncToolCallback, StateAwareToolCallback {

		private final String name;

		private final Duration executionTime;

		TimeoutAsyncTool(String name, Duration executionTime) {
			this.name = name;
			this.executionTime = executionTime;
		}

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder()
				.name(name)
				.description("Timeout test tool")
				.inputSchema("{\"type\":\"object\",\"properties\":{}}")
				.build();
		}

		@Override
		public String call(String toolInput, ToolContext toolContext) {
			throw new UnsupportedOperationException("Use callAsync instead");
		}

		@Override
		public String call(String toolInput) {
			throw new UnsupportedOperationException("Use callAsync instead");
		}

		@Override
		public CompletableFuture<String> callAsync(String toolInput, ToolContext toolContext) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					Thread.sleep(executionTime.toMillis());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return "Completed: " + name;
			});
		}

		@Override
		public Duration getTimeout() {
			// Return a short timeout so the tool will timeout quickly
			// (This timeout is used by executeAsyncTool)
			return Duration.ofMillis(100);
		}

	}

	/**
	 * An async tool that writes state and completes quickly.
	 */
	private static class StateWritingAsyncTool implements AsyncToolCallback, StateAwareToolCallback {

		private final String name;

		private final String stateKey;

		private final String stateValue;

		StateWritingAsyncTool(String name, String stateKey, String stateValue) {
			this.name = name;
			this.stateKey = stateKey;
			this.stateValue = stateValue;
		}

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder()
				.name(name)
				.description("State writing async tool")
				.inputSchema("{\"type\":\"object\",\"properties\":{}}")
				.build();
		}

		@Override
		public String call(String toolInput, ToolContext toolContext) {
			throw new UnsupportedOperationException("Use callAsync instead");
		}

		@Override
		public String call(String toolInput) {
			throw new UnsupportedOperationException("Use callAsync instead");
		}

		@Override
		@SuppressWarnings("unchecked")
		public CompletableFuture<String> callAsync(String toolInput, ToolContext toolContext) {
			// Write state to the update map
			Map<String, Object> updateMap = (Map<String, Object>) toolContext.getContext()
				.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
			if (updateMap != null) {
				updateMap.put(stateKey, stateValue);
			}
			return CompletableFuture.completedFuture("Success: " + name);
		}

		@Override
		public Duration getTimeout() {
			return Duration.ofSeconds(30);
		}

	}

}
