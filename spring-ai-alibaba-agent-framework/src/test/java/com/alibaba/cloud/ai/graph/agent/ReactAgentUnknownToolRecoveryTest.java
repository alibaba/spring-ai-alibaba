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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	@DisplayName("should stop loop after two consecutive unknown tool rounds")
	void shouldStopLoopAfterTwoConsecutiveUnknownToolRounds() throws Exception {
		SequenceChatModel model = new SequenceChatModel(
				assistantWithToolCall("call-1", "missing_tool_1"),
				assistantWithToolCall("call-2", "missing_tool_2"),
				new AssistantMessage("should not be reached"));
		ToolCallback echoTool = createSimpleTool("echo", args -> "echo: " + args);

		ReactAgent agent = ReactAgent.builder()
				.name("unknown-tool-guard-agent")
				.model(model)
				.tools(echoTool)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("hello");

		assertNotNull(response.getText());
		assertTrue(response.getText().contains("tool calling has been stopped"));
		assertEquals(2, model.getCallCount());
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


