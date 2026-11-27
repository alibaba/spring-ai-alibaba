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

package com.alibaba.cloud.ai.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmNodeSessionMemoryTest {

	@Test
	void restoresHistoryAcrossBlockingAndStreamingCallsAndIsolatesSessions() throws Exception {
		var prompts = new ArrayList<List<String>>();
		var model = new ChatModel() {

			@Override
			public ChatResponse call(Prompt prompt) {
				prompts.add(prompt.getInstructions().stream().map(Message::getText).toList());
				return new ChatResponse(List.of(new Generation(new AssistantMessage("reply"))));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.just(call(prompt));
			}

		};
		var service = DefaultSessionService.builder()
				.sessionRepository(InMemorySessionRepository.builder().build()).build();
		var node = LlmNode.builder().chatClient(ChatClient.builder(model).build())
				.messagesKey("turn").sessionMemory(service, "session").build();

		node.apply(state("first", "my first question"));
		node.stream(state("first", "my follow-up")).then().block();
		node.apply(state("second", "another user's question"));

		assertEquals(List.of(List.of("my first question"), List.of("my first question", "reply", "my follow-up"),
				List.of("another user's question")), prompts);
		assertEquals(List.of("my first question", "reply", "my follow-up", "reply"),
				service.getMessages("first").stream().map(Message::getText).toList());
		assertEquals(2, service.getMessages("second").size());
		assertThrows(IllegalArgumentException.class,
				() -> node.apply(new OverAllState(Map.of("turn", new UserMessage("missing session")))));
	}

	@Test
	void storesTheToolCallAndResultInTheSameSession() throws Exception {
		ChatModel model = new ChatModel() {

			@Override
			public ChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}

			@Override
			public ChatResponse call(Prompt prompt) {
				if (prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance)) {
					return new ChatResponse(List.of(new Generation(new AssistantMessage("tool finished"))));
				}
				var call = new AssistantMessage.ToolCall("call-1", "function", "lookup", "{}");
				return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(call)).build())));
			}

		};
		var service = DefaultSessionService.builder()
				.sessionRepository(InMemorySessionRepository.builder().build()).build();
		var node = LlmNode.builder().chatClient(ChatClient.builder(model).build()).messagesKey("turn")
				.sessionMemory(service, "session").toolCallbacks(List.of(ToolCallbacks.from(new TestTools()))).build();

		node.apply(state("tools", "look up data"));

		var messages = service.getMessages("tools");
		assertEquals(4, messages.size());
		assertEquals("call-1", ((AssistantMessage) messages.get(1)).getToolCalls().get(0).id());
		assertEquals("call-1", ((ToolResponseMessage) messages.get(2)).getResults().get(0).id());
		assertEquals("tool finished", messages.get(3).getText());
	}

	private static class TestTools {

		@Tool(name = "lookup", description = "Look up a fixture")
		String lookup() {
			return "fixture";
		}

	}

	private static OverAllState state(String session, String text) {
		return new OverAllState(Map.of("session", session, "turn", List.of(new UserMessage(text))));
	}

}
