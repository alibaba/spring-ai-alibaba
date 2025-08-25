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
package com.alibaba.example.chatmemory.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试 for MemZeroChatMemoryProperties
 *
 * @author Morain Miao
 * @since 1.0.0
 */
class MemZeroChatMemoryPropertiesTest {

	@Test
	void testDefaultConstructor() {
		// When
		MemZeroChatMemoryProperties properties = new MemZeroChatMemoryProperties();

		// Then
		assertThat(properties).isNotNull();
		// 注意：client和server字段在默认构造函数中为null，需要手动初始化
		assertThat(properties.getClient()).isNull();
		assertThat(properties.getServer()).isNull();
	}

	@Test
	void testClientProperties() {
		// Given
		MemZeroChatMemoryProperties properties = new MemZeroChatMemoryProperties();
		MemZeroChatMemoryProperties.Client client = new MemZeroChatMemoryProperties.Client();

		// When
		client.setBaseUrl("http://localhost:8888");
		client.setTimeoutSeconds(30);
		properties.setClient(client);

		// Then
		assertThat(properties.getClient().getBaseUrl()).isEqualTo("http://localhost:8888");
		assertThat(properties.getClient().getTimeoutSeconds()).isEqualTo(30);
	}

	@Test
	void testServerProperties() {
		// Given
		MemZeroChatMemoryProperties properties = new MemZeroChatMemoryProperties();
		MemZeroChatMemoryProperties.Server server = new MemZeroChatMemoryProperties.Server();

		// When
		server.setVersion("v1.1");
		properties.setServer(server);

		// Then
		assertThat(properties.getServer().getVersion()).isEqualTo("v1.1");
	}

	@Test
	void testServerCustomPrompts() {
		// Given
		MemZeroChatMemoryProperties.Server server = new MemZeroChatMemoryProperties.Server();

		// When
		server.setCustomFactExtractionPrompt("fact extraction prompt");
		server.setCustomUpdateMemoryPrompt("update memory prompt");

		// Then
		assertThat(server.getCustomFactExtractionPrompt()).isEqualTo("fact extraction prompt");
		assertThat(server.getCustomUpdateMemoryPrompt()).isEqualTo("update memory prompt");
	}

	@Test
	void testClientDefaultValues() {
		// Given
		MemZeroChatMemoryProperties.Client client = new MemZeroChatMemoryProperties.Client();

		// Then - 验证默认值
		assertThat(client.getBaseUrl()).isEqualTo("http://localhost:8888");
		assertThat(client.getTimeoutSeconds()).isEqualTo(30);
	}

	@Test
	void testServerDefaultValues() {
		// Given
		MemZeroChatMemoryProperties.Server server = new MemZeroChatMemoryProperties.Server();

		// Then - 验证默认值
		assertThat(server.getVersion()).isNull();
		assertThat(server.getProject()).isNull();
		assertThat(server.getVectorStore()).isNull();
		assertThat(server.getGraphStore()).isNull();
		assertThat(server.getCustomFactExtractionPrompt()).isNull();
		assertThat(server.getCustomUpdateMemoryPrompt()).isNull();
	}

	@Test
	void testPropertiesEquality() {
		// Given
		MemZeroChatMemoryProperties properties1 = new MemZeroChatMemoryProperties();
		MemZeroChatMemoryProperties properties2 = new MemZeroChatMemoryProperties();

		// When - 需要先初始化client对象
		MemZeroChatMemoryProperties.Client client1 = new MemZeroChatMemoryProperties.Client();
		MemZeroChatMemoryProperties.Client client2 = new MemZeroChatMemoryProperties.Client();
		client1.setBaseUrl("http://localhost:8888");
		client2.setBaseUrl("http://localhost:8888");
		properties1.setClient(client1);
		properties2.setClient(client2);

		// Then
		assertThat(properties1.getClient().getBaseUrl()).isEqualTo(properties2.getClient().getBaseUrl());
	}

	@Test
	void testPropertiesToString() {
		// Given
		MemZeroChatMemoryProperties properties = new MemZeroChatMemoryProperties();

		// When
		String toString = properties.toString();

		// Then
		assertThat(toString).isNotNull();
		assertThat(toString).contains("MemZeroChatMemoryProperties");
	}

}
