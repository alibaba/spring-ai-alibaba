/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.memory.tablestore;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using Testcontainers to automatically manage Redis test environment
 */
class TablestoreChatMemoryRepositoryTest {

	private static final Logger log = LoggerFactory.getLogger(TablestoreChatMemoryRepositoryTest.class);

	private static TablestoreChatMemoryRepository chatMemoryRepository;

	@BeforeAll
	static void beforeAllSetUp() {
		chatMemoryRepository = new TablestoreChatMemoryRepository(EnvUtil.getClient());
	}

	@BeforeEach
	void cleanDatabase() {
		chatMemoryRepository.getStore().deleteAllSessions();
		chatMemoryRepository.getStore().deleteAllMessages();
	}

	@Test
	void saveMessagesMultipleMessages() {
		var conversationId = UUID.randomUUID().toString();
		var messages = List.<Message>of(new AssistantMessage("idx:1 Message from assistant - " + conversationId),
				new UserMessage("idx:2 Message from user - " + conversationId),
				new SystemMessage("idx:3 Message from system - " + conversationId),
				new ToolResponseMessage(List.of(
						new ToolResponseMessage.ToolResponse("idx:4-1", "tool1",
								"Message from tool - " + conversationId),
						new ToolResponseMessage.ToolResponse("idx:4-2", "tool2",
								"Message from tool - " + conversationId),
						new ToolResponseMessage.ToolResponse("idx:4-3", "tool3",
								"Message from tool - " + conversationId))));

		chatMemoryRepository.saveAll(conversationId, messages);

		List<Message> savedMessages = chatMemoryRepository.findByConversationId(conversationId);
		log.info("savedMessages:{}", savedMessages);
		assertThat(savedMessages).hasSameSizeAs(messages);

		for (var i = 0; i < messages.size(); i++) {
			var message = messages.get(i);
			var savedMessage = savedMessages.get(i);

			assertThat(savedMessage.getText()).isEqualTo(message.getText());
			assertThat(savedMessage.getMessageType()).isEqualTo(message.getMessageType());
		}

		var count = chatMemoryRepository.findByConversationId(conversationId).size();
		assertThat(count).isEqualTo(messages.size());

		chatMemoryRepository.saveAll(conversationId, List.of(new UserMessage("Hello")));

		count = chatMemoryRepository.findByConversationId(conversationId).size();
		assertThat(count).isEqualTo(1);
	}

	@Test
	void deleteMessagesByConversationId() {
		var conversationId = UUID.randomUUID().toString();
		var messages = List.<Message>of(new AssistantMessage("Message from assistant - " + conversationId),
				new UserMessage("Message from user - " + conversationId),
				new SystemMessage("Message from system - " + conversationId));

		chatMemoryRepository.saveAll(conversationId, messages);

		chatMemoryRepository.deleteByConversationId(conversationId);

		var results = chatMemoryRepository.findByConversationId(conversationId);
		assertThat(results).isEmpty();
	}

	@Test
	void findConversationIds() {
		int count = ThreadLocalRandom.current().nextInt(1, 20);
		Set<String> savedConversationIds = new HashSet<>();
		for (int i = 0; i < count; i++) {
			var conversationId = UUID.randomUUID().toString();
			var messages = List.<Message>of(new UserMessage("Message from user - " + conversationId));
			chatMemoryRepository.saveAll(conversationId, messages);
			savedConversationIds.add(conversationId);

		}
		List<String> conversationIds = chatMemoryRepository.findConversationIds();
		assertThat(conversationIds.size()).isEqualTo(savedConversationIds.size());
		assertThat(conversationIds).containsAll(savedConversationIds);

		for (String conversationId : conversationIds) {
			chatMemoryRepository.deleteByConversationId(conversationId);
		}
		conversationIds = chatMemoryRepository.findConversationIds();
		assertThat(conversationIds).isEmpty();
	}

}
