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
package com.alibaba.cloud.ai.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Libres-coder
 */
@EnabledIfEnvironmentVariable(named = "REDIS_ENABLED", matches = "true")
class RedisChatMemoryRepositoryTests {

	private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");

	private static final int REDIS_PORT = Integer
		.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

	private static final int REDIS_DATABASE = Integer
		.parseInt(System.getenv().getOrDefault("REDIS_DATABASE", "0"));

	@Test
	void testAddAndGetMessages() {
		// Create repository with specific database
		RedisChatMemoryRepository repository = new RedisChatMemoryRepository.Builder().host(REDIS_HOST)
			.port(REDIS_PORT)
			.database(REDIS_DATABASE)
			.build();

		String conversationId = "test-conversation-1";

		// Clear any existing data
		repository.clear(conversationId);

		// Add messages
		List<Message> messages = List.of(new UserMessage("Hello"), new AssistantMessage("Hi there!"));
		repository.add(conversationId, messages);

		// Retrieve messages
		List<Message> retrieved = repository.get(conversationId);

		// Verify
		assertThat(retrieved).hasSize(2);
		assertThat(retrieved.get(0).getText()).isEqualTo("Hello");
		assertThat(retrieved.get(1).getText()).isEqualTo("Hi there!");

		// Clean up
		repository.clear(conversationId);
	}

	@Test
	void testClearMessages() {
		RedisChatMemoryRepository repository = new RedisChatMemoryRepository.Builder().host(REDIS_HOST)
			.port(REDIS_PORT)
			.database(REDIS_DATABASE)
			.build();

		String conversationId = "test-conversation-2";

		// Add messages
		repository.add(conversationId, List.of(new UserMessage("Test message")));

		// Verify messages exist
		assertThat(repository.get(conversationId)).isNotEmpty();

		// Clear messages
		repository.clear(conversationId);

		// Verify messages are cleared
		assertThat(repository.get(conversationId)).isEmpty();
	}

	@Test
	void testMultipleConversations() {
		RedisChatMemoryRepository repository = new RedisChatMemoryRepository.Builder().host(REDIS_HOST)
			.port(REDIS_PORT)
			.database(REDIS_DATABASE)
			.build();

		String conversation1 = "test-conversation-3";
		String conversation2 = "test-conversation-4";

		// Clear any existing data
		repository.clear(conversation1);
		repository.clear(conversation2);

		// Add messages to different conversations
		repository.add(conversation1, List.of(new UserMessage("Message 1")));
		repository.add(conversation2, List.of(new UserMessage("Message 2")));

		// Verify each conversation has its own messages
		List<Message> messages1 = repository.get(conversation1);
		List<Message> messages2 = repository.get(conversation2);

		assertThat(messages1).hasSize(1);
		assertThat(messages1.get(0).getText()).isEqualTo("Message 1");

		assertThat(messages2).hasSize(1);
		assertThat(messages2.get(0).getText()).isEqualTo("Message 2");

		// Clean up
		repository.clear(conversation1);
		repository.clear(conversation2);
	}

	@Test
	void testDatabaseIndexIsolation() {
		// Create repositories with different database indexes
		RedisChatMemoryRepository repositoryDb0 = new RedisChatMemoryRepository.Builder().host(REDIS_HOST)
			.port(REDIS_PORT)
			.database(0)
			.build();

		RedisChatMemoryRepository repositoryDb1 = new RedisChatMemoryRepository.Builder().host(REDIS_HOST)
			.port(REDIS_PORT)
			.database(1)
			.build();

		String conversationId = "test-database-isolation";

		// Clear data in both databases
		repositoryDb0.clear(conversationId);
		repositoryDb1.clear(conversationId);

		// Add different messages to the same conversation ID in different databases
		repositoryDb0.add(conversationId, List.of(new UserMessage("Message in DB0")));
		repositoryDb1.add(conversationId, List.of(new UserMessage("Message in DB1")));

		// Verify isolation - each database should have its own data
		List<Message> messagesDb0 = repositoryDb0.get(conversationId);
		List<Message> messagesDb1 = repositoryDb1.get(conversationId);

		assertThat(messagesDb0).hasSize(1);
		assertThat(messagesDb0.get(0).getText()).isEqualTo("Message in DB0");

		assertThat(messagesDb1).hasSize(1);
		assertThat(messagesDb1.get(0).getText()).isEqualTo("Message in DB1");

		// Clean up
		repositoryDb0.clear(conversationId);
		repositoryDb1.clear(conversationId);
	}

}

