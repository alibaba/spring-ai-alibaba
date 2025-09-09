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
package com.alibaba.cloud.ai.memory.mem0.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Mem0ChatMemoryProperties
 *
 * @author Morain Miao
 * @since 1.0.0
 */
class Mem0ChatMemoryPropertiesTest {

	@Test
	void testDefaultConstructor() {
		// When
		Mem0ChatMemoryProperties properties = new Mem0ChatMemoryProperties();

		// Then
		assertThat(properties).isNotNull();
		// Note: The client and server fields are null in the default constructor and need
		// to be manually initialized.
		assertThat(properties.getClient()).isNull();
		assertThat(properties.getServer()).isNull();
	}

	@Test
	void testClientProperties() {
		// Given
		Mem0ChatMemoryProperties properties = new Mem0ChatMemoryProperties();
		Mem0ChatMemoryProperties.Client client = new Mem0ChatMemoryProperties.Client();

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
		Mem0ChatMemoryProperties properties = new Mem0ChatMemoryProperties();
		Mem0ChatMemoryProperties.Server server = new Mem0ChatMemoryProperties.Server();

		// When
		server.setVersion("v1.1");
		properties.setServer(server);

		// Then
		assertThat(properties.getServer().getVersion()).isEqualTo("v1.1");
	}

	@Test
	void testServerCustomPrompts() {
		// Given
		Mem0ChatMemoryProperties.Server server = new Mem0ChatMemoryProperties.Server();

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
		Mem0ChatMemoryProperties.Client client = new Mem0ChatMemoryProperties.Client();

		// Then - Verify default values
		assertThat(client.getBaseUrl()).isEqualTo("http://localhost:8888");
		assertThat(client.getTimeoutSeconds()).isEqualTo(30);
	}

	@Test
	void testServerDefaultValues() {
		// Given
		Mem0ChatMemoryProperties.Server server = new Mem0ChatMemoryProperties.Server();

		// Then - Verify default values
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
		Mem0ChatMemoryProperties properties1 = new Mem0ChatMemoryProperties();
		Mem0ChatMemoryProperties properties2 = new Mem0ChatMemoryProperties();

		// When - The client object needs to be initialized first
		Mem0ChatMemoryProperties.Client client1 = new Mem0ChatMemoryProperties.Client();
		Mem0ChatMemoryProperties.Client client2 = new Mem0ChatMemoryProperties.Client();
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
		Mem0ChatMemoryProperties properties = new Mem0ChatMemoryProperties();

		// When
		String toString = properties.toString();

		// Then
		assertThat(toString).isNotNull();
		assertThat(toString).contains("Mem0ChatMemoryProperties");
	}

}
