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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests recovery and loop-termination behavior for structured tool execution failures.
 */
class ReactAgentToolExecutionFailureRecoveryTest {

	@Test
	@DisplayName("should let model recover after a tool execution failure")
	void shouldLetModelRecoverAfterAToolExecutionFailure() throws Exception {
		AtomicInteger successfulToolInvocations = new AtomicInteger();
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "failing_tool"),
				assistantWithToolCall("call-2", "echo"),
				new AssistantMessage("final answer after fixing tool choice"));
		ToolCallback failingTool = createFailingTool("failing_tool", "transient failure");
		ToolCallback echoTool = createSimpleTool("echo", args -> {
			successfulToolInvocations.incrementAndGet();
			return "echo: " + args;
		});

		ReactAgent agent = ReactAgent.builder()
				.name("tool-execution-failure-recovery-agent")
				.model(model)
				.tools(failingTool, echoTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");
		assertNotNull(response.getText());
		assertEquals("final answer after fixing tool choice", response.getText());
		assertEquals(3, model.getCallCount());
		assertEquals(1, successfulToolInvocations.get());
	}

	@Test
	@DisplayName("should let model answer directly after consecutive tool execution failures")
	void shouldLetModelAnswerDirectlyAfterConsecutiveToolExecutionFailures() throws Exception {
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "failing_tool"),
				assistantWithToolCall("call-2", "failing_tool"),
				new AssistantMessage("final answer after tool execution failure guard"));
		ToolCallback failingTool = createFailingTool("failing_tool", "persistent failure");

		ReactAgent agent = ReactAgent.builder()
				.name("tool-execution-failure-guard-agent")
				.model(model)
				.tools(failingTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");
		assertNotNull(response.getText());
		assertEquals("final answer after tool execution failure guard", response.getText());
		assertEquals(3, model.getCallCount());
	}

	@Test
	@DisplayName("should terminate immediately when model still emits tool calls in execution-failure final-answer mode")
	void shouldTerminateImmediatelyWhenModelStillEmitsToolCallsInExecutionFailureFinalAnswerMode() throws Exception {
		AtomicInteger echoInvocations = new AtomicInteger();
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "failing_tool"),
				assistantWithToolCall("call-2", "failing_tool"),
				assistantWithToolCall("call-3", "echo"));
		ToolCallback failingTool = createFailingTool("failing_tool", "persistent failure");
		ToolCallback echoTool = createSimpleTool("echo", args -> {
			echoInvocations.incrementAndGet();
			return "echo: " + args;
		});

		ReactAgent agent = ReactAgent.builder()
				.name("tool-execution-failure-final-answer-retry-agent")
				.model(model)
				.tools(failingTool, echoTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");
		assertNotNull(response.getText());
		assertEquals(
				"I had to stop calling tools because tool execution kept failing repeatedly, " +
						"and I could not safely complete your request without them in this turn. " +
						"Would you like me to continue with a best-effort answer based on the current context, " +
						"or would you prefer to adjust the tool setup and try again?",
				response.getText());
		assertEquals(3, model.getCallCount());
		assertEquals(0, echoInvocations.get());
	}

	@Test
	@DisplayName("should not leak tool-execution-failure state across runs in the same thread")
	void shouldNotLeakToolExecutionFailureStateAcrossRunsInTheSameThread() throws Exception {
		AtomicInteger echoInvocations = new AtomicInteger();
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "failing_tool"),
				assistantWithToolCall("call-2", "failing_tool"),
				new AssistantMessage("first run final answer after tool execution failure guard"),
				assistantWithToolCall("call-4", "echo"),
				new AssistantMessage("second run final answer after normal tool call"));
		ToolCallback failingTool = createFailingTool("failing_tool", "persistent failure");
		ToolCallback echoTool = createSimpleTool("echo", args -> {
			echoInvocations.incrementAndGet();
			return "echo: " + args;
		});

		ReactAgent agent = ReactAgent.builder()
				.name("tool-execution-failure-thread-state-agent")
				.model(model)
				.tools(failingTool, echoTool)
				.saver(new MemorySaver())
				.build();

		RunnableConfig firstRunConfig = RunnableConfig.builder().threadId("tool-execution-failure-thread").build();
		AssistantMessage firstRunResponse = agent.call("hello", firstRunConfig);
		assertNotNull(firstRunResponse.getText());
		assertEquals("first run final answer after tool execution failure guard", firstRunResponse.getText());
		assertEquals(3, model.getCallCount());
		assertEquals(0, echoInvocations.get());

		RunnableConfig secondRunConfig = RunnableConfig.builder().threadId("tool-execution-failure-thread").build();
		AssistantMessage secondRunResponse = agent.call("hello again", secondRunConfig);
		assertNotNull(secondRunResponse.getText());
		assertEquals("second run final answer after normal tool call", secondRunResponse.getText());
		assertEquals(5, model.getCallCount());
		assertEquals(1, echoInvocations.get());
	}

	private static AssistantMessage assistantWithToolCall(String id, String toolName) {
		return AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", toolName, "{}")))
				.build();
	}

	private static ToolCallback createSimpleTool(String name, java.util.function.Function<String, String> logic) {
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

	private static ToolCallback createFailingTool(String name, String message) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name(name).description("Failing tool " + name).inputSchema("{}").build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				throw new RuntimeException(message);
			}

			@Override
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};
	}

	static final class SequenceChatModel implements ChatModel {

		private final List<AssistantMessage> responses;

		private final AtomicInteger callCount = new AtomicInteger();

		SequenceChatModel(AssistantMessage... responses) {
			this.responses = List.of(responses);
		}

		int getCallCount() {
			return callCount.get();
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			return createResponse();
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(createResponse());
		}

		private ChatResponse createResponse() {
			int index = callCount.getAndIncrement();
			AssistantMessage message = index < responses.size() ? responses.get(index) : responses.get(responses.size() - 1);
			return new ChatResponse(List.of(new Generation(message)));
		}

	}

}

