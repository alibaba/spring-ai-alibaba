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

package com.alibaba.cloud.ai.graph.utils;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageSequenceValidatorTest {

	@Test
	void shouldAcceptHistoryClosedByFinalAssistantMessage() {
		assertTrue(MessageSequenceValidator.isCheckpointReadyForNewInput(state(List.of(
				new UserMessage("search"),
				assistantToolCall("call_1"),
				toolResponse("call_1"),
				new AssistantMessage("answer")))));
	}

	@Test
	void shouldRejectAssistantToolCallWithoutToolResponse() {
		assertFalse(MessageSequenceValidator.isCheckpointReadyForNewInput(state(List.of(
				new UserMessage("search"),
				assistantToolCall("call_1")))));
	}

	@Test
	void shouldRejectToolResponseWithoutFinalAssistantMessage() {
		assertFalse(MessageSequenceValidator.isCheckpointReadyForNewInput(state(List.of(
				new UserMessage("search"),
				assistantToolCall("call_1"),
				toolResponse("call_1")))));
	}

	@Test
	void shouldRejectPartialToolResponsesForMultipleToolCalls() {
		AssistantMessage.ToolCall first = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		AssistantMessage.ToolCall second = new AssistantMessage.ToolCall("call_2", "function", "lookup", "{}");

		assertFalse(MessageSequenceValidator.isCheckpointReadyForNewInput(state(List.of(
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(first, second)).build(),
				toolResponse("call_1")))));
	}

	private static Map<String, Object> state(List<Message> messages) {
		return Map.of("messages", messages);
	}

	private static AssistantMessage assistantToolCall(String id) {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(id, "function", "search", "{}");
		return AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build();
	}

	private static ToolResponseMessage toolResponse(String id) {
		return ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse(id, "search", "ok")))
			.build();
	}

}
