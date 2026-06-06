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
package com.alibaba.cloud.ai.studio.core.agent.memory;

import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationChatMemoryTest {

	@Test
	void shouldEvictOldMessagesBasedOnStoredHistorySize() {
		RedisManager redisManager = mock(RedisManager.class);
		CommonConfig commonConfig = new CommonConfig();
		commonConfig.setMaxConversationRoundInCache(3);
		ConversationChatMemory chatMemory = new ConversationChatMemory(redisManager, commonConfig);
		String conversationId = "conversation-id";
		String key = String.format(ConversationChatMemory.CONVERSATION_CHAT_MEMORY_PREFIX, conversationId);
		Deque<Message> historyMessages = new ArrayDeque<>(List.of(new UserMessage("message-1"),
				new UserMessage("message-2"), new UserMessage("message-3"), new UserMessage("message-4")));

		when(redisManager.get(key)).thenReturn(historyMessages);

		chatMemory.add(conversationId, List.of(new UserMessage("message-5")));

		ArgumentCaptor<Deque<Message>> savedMessages = messageDequeCaptor();
		verify(redisManager).put(eq(key), savedMessages.capture());
		assertThat(savedMessages.getValue()).extracting(Message::getText)
			.containsExactly("message-3", "message-4", "message-5");
	}

	@Test
	void shouldKeepLatestMessagesWhenIncomingBatchExceedsLimit() {
		RedisManager redisManager = mock(RedisManager.class);
		CommonConfig commonConfig = new CommonConfig();
		commonConfig.setMaxConversationRoundInCache(3);
		ConversationChatMemory chatMemory = new ConversationChatMemory(redisManager, commonConfig);
		String conversationId = "conversation-id";
		String key = String.format(ConversationChatMemory.CONVERSATION_CHAT_MEMORY_PREFIX, conversationId);

		when(redisManager.get(key)).thenReturn(null);

		chatMemory.add(conversationId, List.of(new UserMessage("message-1"), new UserMessage("message-2"),
				new UserMessage("message-3"), new UserMessage("message-4"), new UserMessage("message-5")));

		ArgumentCaptor<Deque<Message>> savedMessages = messageDequeCaptor();
		verify(redisManager).put(eq(key), savedMessages.capture());
		assertThat(savedMessages.getValue()).extracting(Message::getText)
			.containsExactly("message-3", "message-4", "message-5");
	}

	@Test
	void shouldTreatNegativeMessageLimitAsZero() {
		RedisManager redisManager = mock(RedisManager.class);
		CommonConfig commonConfig = new CommonConfig();
		commonConfig.setMaxConversationRoundInCache(-1);
		ConversationChatMemory chatMemory = new ConversationChatMemory(redisManager, commonConfig);
		String conversationId = "conversation-id";
		String key = String.format(ConversationChatMemory.CONVERSATION_CHAT_MEMORY_PREFIX, conversationId);
		Deque<Message> historyMessages = new ArrayDeque<>(
				List.of(new UserMessage("message-1"), new UserMessage("message-2")));

		when(redisManager.get(key)).thenReturn(historyMessages);

		chatMemory.add(conversationId, List.of(new UserMessage("message-3")));

		ArgumentCaptor<Deque<Message>> savedMessages = messageDequeCaptor();
		verify(redisManager).put(eq(key), savedMessages.capture());
		assertThat(savedMessages.getValue()).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<Deque<Message>> messageDequeCaptor() {
		return ArgumentCaptor.forClass(Deque.class);
	}

}
