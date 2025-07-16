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
package com.alibaba.cloud.ai.memory.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using Testcontainers to automatically manage Redis test environment
 */
@SpringBootTest(classes = RedisChatMemoryRepositoryIT.TestConfiguration.class)
@Testcontainers
class RedisChatMemoryRepositoryIT {

	private static final int REDIS_PORT = 6379;

	// Define and start Redis container
	@Container
	private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
		.withExposedPorts(REDIS_PORT);

	/**
	 * Dynamically configure Redis properties
	 */
	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.redis.host", redisContainer::getHost);
		registry.add("spring.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
	}

	@Autowired
	private ChatMemoryRepository chatMemoryRepository;

	@Test
	void correctChatMemoryRepositoryInstance() {
		assertThat(chatMemoryRepository).isInstanceOf(RedisChatMemoryRepository.class);
	}

	@ParameterizedTest
	@CsvSource({ "Message from assistant,ASSISTANT", "Message from user,USER", "Message from system,SYSTEM" })
	void saveMessagesSingleMessage(String content, MessageType messageType) {
		var conversationId = UUID.randomUUID().toString();
		var message = switch (messageType) {
			case ASSISTANT -> new AssistantMessage(content + " - " + conversationId);
			case USER -> new UserMessage(content + " - " + conversationId);
			case SYSTEM -> new SystemMessage(content + " - " + conversationId);
			default -> throw new IllegalArgumentException("Type not supported: " + messageType);
		};

		chatMemoryRepository.saveAll(conversationId, List.of(message));

		var messages = chatMemoryRepository.findByConversationId(conversationId);
		assertThat(messages).hasSize(1);

		var savedMessage = messages.get(0);
		assertThat(savedMessage.getText()).isEqualTo(message.getText());
		assertThat(savedMessage.getMessageType()).isEqualTo(messageType);
	}

	@Test
	void saveMessagesMultipleMessages() {
		var conversationId = UUID.randomUUID().toString();
		var messages = List.<Message>of(new AssistantMessage("Message from assistant - " + conversationId),
				new UserMessage("Message from user - " + conversationId),
				new SystemMessage("Message from system - " + conversationId));

		chatMemoryRepository.saveAll(conversationId, messages);

		var savedMessages = chatMemoryRepository.findByConversationId(conversationId);
		assertThat(savedMessages.size()).isEqualTo(messages.size());

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
	void findMessagesByConversationId() {
		var conversationId = UUID.randomUUID().toString();
		var messages = List.<Message>of(new AssistantMessage("Message from assistant 1 - " + conversationId),
				new AssistantMessage("Message from assistant 2 - " + conversationId),
				new UserMessage("Message from user - " + conversationId),
				new SystemMessage("Message from system - " + conversationId));

		chatMemoryRepository.saveAll(conversationId, messages);

		var results = chatMemoryRepository.findByConversationId(conversationId);

		assertThat(results.size()).isEqualTo(messages.size());
		assertThat(results).isEqualTo(messages);
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
	void clearOverLimit() {
		var conversationId = UUID.randomUUID().toString();
		var messages = List.<Message>of(new UserMessage("Message 1 from user - " + conversationId),
				new AssistantMessage("Message 1 from assistant - " + conversationId),
				new UserMessage("Message 2 from user - " + conversationId),
				new AssistantMessage("Message 2 from assistant - " + conversationId),
				new UserMessage("Message 3 from user - " + conversationId));

		chatMemoryRepository.saveAll(conversationId, messages);

		// 验证所有消息都已保存
		var savedMessages = chatMemoryRepository.findByConversationId(conversationId);
		assertThat(savedMessages.size()).isEqualTo(messages.size());

		// 执行清理操作，设置最大限制为3，删除数量为2
		RedisChatMemoryRepository redisRepository = (RedisChatMemoryRepository) chatMemoryRepository;
		redisRepository.clearOverLimit(conversationId, 3, 2);

		// 验证只保留了后3个消息
		savedMessages = chatMemoryRepository.findByConversationId(conversationId);
		assertThat(savedMessages.size()).isEqualTo(3);
		assertThat(savedMessages.get(0).getText()).isEqualTo(messages.get(2).getText());
		assertThat(savedMessages.get(1).getText()).isEqualTo(messages.get(3).getText());
		assertThat(savedMessages.get(2).getText()).isEqualTo(messages.get(4).getText());
	}

	@SpringBootConfiguration
	static class TestConfiguration {

		@Bean
		ChatMemoryRepository chatMemoryRepository() {
			// 使用容器中的Redis连接信息创建Redis存储库
			return RedisChatMemoryRepository.builder()
				.host(redisContainer.getHost())
				.port(redisContainer.getMappedPort(REDIS_PORT))
				.build();
		}

	}

}
