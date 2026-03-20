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

public class ReactAgentUnknownToolRecoveryTest {

	@Test
	@DisplayName("should let model recover after selecting an unknown tool")
	void shouldLetModelRecoverAfterSelectingUnknownTool() throws Exception {
		AtomicInteger toolInvocations = new AtomicInteger();
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "missing_tool"),
				assistantWithToolCall("call-2", "echo"),
				new AssistantMessage("final answer after fixing tool choice"));
		ToolCallback echoTool = createSimpleTool("echo", args -> {
			toolInvocations.incrementAndGet();
			return "echo: " + args;
		});

		ReactAgent agent = ReactAgent.builder()
				.name("unknown-tool-recovery-agent")
				.model(model)
				.tools(echoTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");
		assertNotNull(response.getText());
		assertEquals("final answer after fixing tool choice", response.getText());
		assertEquals(3, model.getCallCount());
		assertEquals(1, toolInvocations.get());
	}

	@Test
	@DisplayName("should let model answer directly after two consecutive unknown tool rounds")
	void shouldLetModelAnswerDirectlyAfterTwoConsecutiveUnknownToolRounds() throws Exception {
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "missing_tool_1"),
				assistantWithToolCall("call-2", "missing_tool_2"),
				new AssistantMessage("final answer after unknown tool guard"));
		ToolCallback echoTool = createSimpleTool("echo", args -> "echo: " + args);

		ReactAgent agent = ReactAgent.builder()
				.name("unknown-tool-guard-agent")
				.model(model)
				.tools(echoTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");

		assertNotNull(response.getText());
		assertEquals("final answer after unknown tool guard", response.getText());
		assertEquals(3, model.getCallCount());
	}

	@Test
	@DisplayName("should terminate immediately without executing tools when model still emits tool calls in final-answer mode")
	void shouldTerminateImmediatelyWithoutExecutingToolsWhenModelStillCallsTools() throws Exception {
		AtomicInteger toolInvocations = new AtomicInteger();
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "missing_tool_1"),
				assistantWithToolCall("call-2", "missing_tool_2"),
				assistantWithToolCall("call-3", "echo"));
		ToolCallback echoTool = createSimpleTool("echo", args -> {
			toolInvocations.incrementAndGet();
			return "echo: " + args;
		});

		ReactAgent agent = ReactAgent.builder()
				.name("unknown-tool-final-answer-retry-agent")
				.model(model)
				.tools(echoTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");

		assertNotNull(response.getText());
		assertEquals(
				"I could not continue with tool calls because the requested tools were unavailable, and I was still unable to produce a direct answer without tools.",
				response.getText());
		assertEquals(3, model.getCallCount());
		assertEquals(0, toolInvocations.get());
	}

	@Test
	@DisplayName("should not leak unknown-tool final-answer state across runs in the same thread")
	void shouldNotLeakUnknownToolFinalAnswerStateAcrossRunsInTheSameThread() throws Exception {
		AtomicInteger toolInvocations = new AtomicInteger();
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "missing_tool_1"),
				assistantWithToolCall("call-2", "missing_tool_2"),
				new AssistantMessage("first run final answer after unknown tool guard"),
				assistantWithToolCall("call-4", "echo"),
				new AssistantMessage("second run final answer after normal tool call"));
		ToolCallback echoTool = createSimpleTool("echo", args -> {
			toolInvocations.incrementAndGet();
			return "echo: " + args;
		});

		ReactAgent agent = ReactAgent.builder()
				.name("unknown-tool-thread-state-agent")
				.model(model)
				.tools(echoTool)
				.saver(new MemorySaver())
				.build();

		RunnableConfig firstRunConfig = RunnableConfig.builder().threadId("unknown-tool-thread").build();
		AssistantMessage firstRunResponse = agent.call("hello", firstRunConfig);
		assertNotNull(firstRunResponse.getText());
		assertEquals("first run final answer after unknown tool guard", firstRunResponse.getText());
		assertEquals(3, model.getCallCount());
		assertEquals(0, toolInvocations.get());

		RunnableConfig secondRunConfig = RunnableConfig.builder().threadId("unknown-tool-thread").build();
		AssistantMessage secondRunResponse = agent.call("hello again", secondRunConfig);
		assertNotNull(secondRunResponse.getText());
		assertEquals("second run final answer after normal tool call", secondRunResponse.getText());
		assertEquals(5, model.getCallCount());
		assertEquals(1, toolInvocations.get());
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


