/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hooks.summarization;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SummarizationHookTests {

	private ChatModel model;

	@BeforeEach
	void setUp() {
		this.model = mock(ChatModel.class);
		when(this.model.call(any(Prompt.class)))
			.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Summary")))));
	}

	@Test
	void includesTextMessagesWithTheirRoles() {
		String input = summarize(List.of(new SystemMessage("System context"), new UserMessage("Earlier question"),
				new AssistantMessage("Earlier answer")));

		assertThat(input).contains("System: System context", "Human: Earlier question", "Assistant: Earlier answer");
	}

	@ParameterizedTest
	@NullAndEmptySource
	void includesToolCallsWithoutAssistantText(String text) {
		AssistantMessage call = toolCall(text, "call-1", "search", "{\"query\":\"release notes\"}");
		ToolResponseMessage response = toolResponse("call-1", "search", "Retrieved release notes");

		String input = summarize(List.of(call, response));

		assertThat(input).contains("call-1", "search", "{\"query\":\"release notes\"}");
	}

	@Test
	void includesAssistantTextAlongsideToolCalls() {
		AssistantMessage call = toolCall("Checking the source", "call-2", "lookup", "{\"id\":42}");
		ToolResponseMessage response = toolResponse("call-2", "lookup", "Record 42");

		String input = summarize(List.of(call, response));

		assertThat(input).contains("Assistant: Checking the source", "call-2", "lookup", "{\"id\":42}");
	}

	@Test
	void includesToolResponseDataAndSourceUrls() {
		String data = "{\"fact\":\"SENTINEL_EVIDENCE\",\"url\":\"https://example.org/source\"}";
		AssistantMessage call = toolCall("", "call-3", "search", "{\"query\":\"source\"}");
		ToolResponseMessage response = toolResponse("call-3", "search", data);

		String input = summarize(List.of(call, response));

		assertThat(input).contains(data);
		assertThat(response.getResponses().get(0).responseData()).isEqualTo(data);
	}

	@Test
	void includesAllToolCallsAndResponsesWithTheirIdentifiers() {
		AssistantMessage calls = AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(new AssistantMessage.ToolCall("first", "function", "search", "{\"query\":\"alpha\"}"),
					new AssistantMessage.ToolCall("second", "function", "search", "{\"query\":\"beta\"}")))
			.build();
		ToolResponseMessage responses = ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("second", "search", "Beta evidence"),
					new ToolResponseMessage.ToolResponse("first", "search", "Alpha evidence")))
			.build();

		String input = summarize(List.of(calls, responses));

		assertThat(input).contains("Tool call [id=first, name=search]: {\"query\":\"alpha\"}",
				"Tool call [id=second, name=search]: {\"query\":\"beta\"}",
				"Tool result [id=second, name=search]: Beta evidence",
				"Tool result [id=first, name=search]: Alpha evidence");
	}

	@Test
	void excludesPreservedMessagesFromSummaryInput() {
		String input = summarize(List.of(new UserMessage("Older question"), new AssistantMessage("Older answer")));

		assertThat(input).contains("Older question", "Older answer")
			.doesNotContain("Original user request", "Recent question", "Recent answer");
	}

	@Test
	void doesNotCallSummaryModelBelowThreshold() {
		SummarizationHook hook = SummarizationHook.builder()
			.model(this.model)
			.tokenCounter(messages -> 0)
			.maxTokensBeforeSummary(1)
			.messagesToKeep(2)
			.build();

		hook.beforeModel(List.of(new UserMessage("Question"), new AssistantMessage("Answer"),
				new UserMessage("Follow-up")), RunnableConfig.builder().build());

		verifyNoInteractions(this.model);
	}

	private String summarize(List<Message> olderMessages) {
		List<Message> history = new ArrayList<>();
		history.add(new UserMessage("Original user request"));
		history.addAll(olderMessages);
		history.add(new UserMessage("Recent question"));
		history.add(new AssistantMessage("Recent answer"));
		List<Message> originalHistory = List.copyOf(history);
		SummarizationHook hook = SummarizationHook.builder()
			.model(this.model)
			.tokenCounter(messages -> 100)
			.maxTokensBeforeSummary(1)
			.messagesToKeep(2)
			.build();

		hook.beforeModel(history, RunnableConfig.builder().build());

		assertThat(history).containsExactlyElementsOf(originalHistory);
		ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
		verify(this.model).call(prompt.capture());
		return prompt.getValue().getContents();
	}

	private AssistantMessage toolCall(String text, String id, String name, String arguments) {
		return AssistantMessage.builder()
			.content(text)
			.toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
			.build();
	}

	private ToolResponseMessage toolResponse(String id, String name, String data) {
		return ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse(id, name, data)))
			.build();
	}

}
