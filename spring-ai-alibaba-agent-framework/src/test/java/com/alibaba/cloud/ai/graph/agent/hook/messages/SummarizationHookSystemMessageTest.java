/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.hook.messages;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying that {@link SummarizationHook} preserves the original
 * {@link SystemMessage} when summarization is triggered, instead of summarizing
 * it away together with the conversation history.
 *
 * <p>This test lives in the {@code hook.messages} package to access the
 * package-private accessors of {@link AgentCommand}.</p>
 *
 * <p>Regression test for issue #4048.</p>
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4048">Issue #4048</a>
 */
public class SummarizationHookSystemMessageTest {

	private static final String SYSTEM_PROMPT = "You are a helpful assistant with very important custom instructions.";

	private static final String SUMMARY_TEXT = "MOCK_SUMMARY_CONTENT";

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		this.chatModel = mock(ChatModel.class);
		when(this.chatModel.call(any(Prompt.class)))
			.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(SUMMARY_TEXT)))));
	}

	private static List<Message> createConversation(boolean withSystemMessage, int rounds) {
		List<Message> messages = new ArrayList<>();
		if (withSystemMessage) {
			messages.add(new SystemMessage(SYSTEM_PROMPT));
		}
		messages.add(new UserMessage("first user question"));
		messages.add(new AssistantMessage("first assistant answer"));
		for (int i = 0; i < rounds; i++) {
			messages.add(new UserMessage("user message " + i + " with enough content to accumulate tokens"));
			messages.add(new AssistantMessage("assistant message " + i + " with enough content to accumulate tokens"));
		}
		return messages;
	}

	private SummarizationHook createHook(boolean keepFirstUserMessage) {
		return SummarizationHook.builder()
			.model(this.chatModel)
			.maxTokensBeforeSummary(1)
			.messagesToKeep(4)
			.keepFirstUserMessage(keepFirstUserMessage)
			.build();
	}

	@Test
	void systemMessagePreservedAtTopWhenSummarizationTriggered() {
		List<Message> conversation = createConversation(true, 20);
		Message originalSystemMessage = conversation.get(0);
		Message firstUserMessage = conversation.get(1);

		SummarizationHook hook = createHook(true);
		AgentCommand command = hook.beforeModel(conversation, RunnableConfig.builder().build());

		List<Message> result = command.getMessages();
		assertTrue(result.size() < conversation.size(), "summarization should shrink the message list");

		// The original SystemMessage must be preserved as the first message
		assertSame(originalSystemMessage, result.get(0), "original SystemMessage should be preserved at the top");
		// Followed by the first user message and the summary message
		assertSame(firstUserMessage, result.get(1), "first UserMessage should be preserved");
		assertInstanceOf(SystemMessage.class, result.get(2), "summary SystemMessage should follow");
		assertTrue(result.get(2).getText().contains(SUMMARY_TEXT), "summary message should contain the summary");

		// The recent messages are kept verbatim at the tail
		List<Message> recent = conversation.subList(conversation.size() - 4, conversation.size());
		assertEquals(recent, result.subList(result.size() - 4, result.size()),
				"recent messages should be kept verbatim");
	}

	@Test
	void systemMessageExcludedFromSummarizationInput() {
		List<Message> conversation = createConversation(true, 20);

		SummarizationHook hook = createHook(true);
		hook.beforeModel(conversation, RunnableConfig.builder().build());

		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(this.chatModel).call(promptCaptor.capture());

		String summarizationInput = promptCaptor.getValue().getContents();
		assertTrue(summarizationInput.contains("user message 0"),
				"conversation history should be sent for summarization");
		assertTrue(!summarizationInput.contains(SYSTEM_PROMPT),
				"the original SystemMessage must not be part of the summarization input");
	}

	@Test
	void systemMessagePreservedWhenKeepFirstUserMessageDisabled() {
		List<Message> conversation = createConversation(true, 20);
		Message originalSystemMessage = conversation.get(0);

		SummarizationHook hook = createHook(false);
		AgentCommand command = hook.beforeModel(conversation, RunnableConfig.builder().build());

		List<Message> result = command.getMessages();
		assertSame(originalSystemMessage, result.get(0), "original SystemMessage should be preserved at the top");
		assertInstanceOf(SystemMessage.class, result.get(1), "summary SystemMessage should follow");
		assertTrue(result.get(1).getText().contains(SUMMARY_TEXT), "summary message should contain the summary");
	}

	@Test
	void noChangeWhenBelowTokenThreshold() {
		List<Message> conversation = createConversation(true, 2);

		SummarizationHook hook = SummarizationHook.builder()
			.model(this.chatModel)
			.maxTokensBeforeSummary(Integer.MAX_VALUE)
			.messagesToKeep(4)
			.build();

		AgentCommand command = hook.beforeModel(conversation, RunnableConfig.builder().build());
		assertEquals(conversation, command.getMessages(), "messages should be untouched below the threshold");
	}

}
