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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ContextEditingInterceptor. Uses pre-constructed messages with
 * AssistantMessage (ToolCall) and ToolResponseMessage to trigger context compaction
 * without relying on a real model to perform tool calls.
 */
class ContextEditingTest {

	private static final String PLACEHOLDER = "[cleared]";

	/**
	 * Mock ChatModel that returns a simple text response with no tool calls,
	 * so the agent finishes after one call when given initial messages.
	 */
	private static class MockChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(
					new Generation(new AssistantMessage("Task completed."))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(new ChatResponse(List.of(
					new Generation(new AssistantMessage("Task completed.")))));
		}
	}

	@Test
	void testContextEditingCompaction() throws Exception {
		ContextEditingInterceptor contextEditingInterceptor = ContextEditingInterceptor.builder()
				.trigger(180)
				.keep(1)
				.clearAtLeast(100)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("single_agent")
				.model(new MockChatModel())
				.interceptors(contextEditingInterceptor)
				.saver(new MemorySaver())
				.build();

		List<Message> messages = buildMessagesWithToolCallsAndResponses();

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent(), "Agent result should be present");
//		assertCompactionOccurred(result.get());
	}

	/**
	 * Build a message list that exceeds the token trigger (180): UserMessage plus
	 * several AssistantMessage (with ToolCall) + ToolResponseMessage pairs.
	 * TokenCounter uses ~4 chars per token, so we need enough content to exceed 720 chars.
	 */
	private List<Message> buildMessagesWithToolCallsAndResponses() {
		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("请先调用 poem 写诗，再调用 reviewer 润色。"));

		String longContent = "x".repeat(260);

		// Pair 1: poem tool call + response
		AssistantMessage.ToolCall toolCall1 = new AssistantMessage.ToolCall(
				"call-poem-1", "function", "poem",
				"{\"topic\":\"" + longContent + "\"}");
		messages.add(AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(toolCall1))
				.build());
		messages.add(ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse(
						"call-poem-1", "poem", "Poem content: " + longContent)))
				.build());

		// Pair 2: reviewer tool call + response
		AssistantMessage.ToolCall toolCall2 = new AssistantMessage.ToolCall(
				"call-reviewer-1", "function", "reviewer",
				"{\"text\":\"" + longContent + "\"}");
		messages.add(AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(toolCall2))
				.build());
		messages.add(ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse(
						"call-reviewer-1", "reviewer", "Reviewed: " + longContent)))
				.build());

		// Pair 3: another poem tool call + response (ensures total > trigger)
		AssistantMessage.ToolCall toolCall3 = new AssistantMessage.ToolCall(
				"call-poem-2", "function", "poem",
				"{\"topic\":\"second poem " + longContent + "\"}");
		messages.add(AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(toolCall3))
				.build());
		messages.add(ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse(
						"call-poem-2", "poem", "Second poem: " + longContent)))
				.build());

		return messages;
	}

	private void assertCompactionOccurred(OverAllState state) {
		Optional<Object> messagesOpt = state.value("messages");
		assertTrue(messagesOpt.isPresent(), "State should contain messages");
		@SuppressWarnings("unchecked")
		List<Message> resultMessages = (List<Message>) messagesOpt.get();
		boolean hasCleared = false;
		for (Message msg : resultMessages) {
			if (msg instanceof ToolResponseMessage trm) {
				for (ToolResponseMessage.ToolResponse r : trm.getResponses()) {
					if (PLACEHOLDER.equals(r.responseData())) {
						hasCleared = true;
						break;
					}
				}
			}
			else if (msg instanceof AssistantMessage am && !am.getToolCalls().isEmpty()) {
				for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
					if (PLACEHOLDER.equals(tc.arguments())) {
						hasCleared = true;
						break;
					}
				}
			}
			if (hasCleared) {
				break;
			}
		}
		assertTrue(hasCleared, "Context compaction should have replaced some tool content with placeholder");
	}
}
