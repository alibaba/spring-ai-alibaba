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
package com.alibaba.cloud.ai.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test cases for ConfigurablePromptTemplate. Tests template creation, rendering, and
 * message generation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class ConfigurablePromptTemplateTests {

	private static final String TEST_NAME = "test-template";

	private static final String TEST_TEMPLATE = "Hello, {name}!";

	private static final String TEST_RESOURCE_CONTENT = "Resource content: {value}";

	private Map<String, Object> model;

	private Resource resource;

	@BeforeEach
	void setUp() {
		// Initialize test data
		model = new HashMap<>();
		model.put("name", "John");
		model.put("value", "test");

		resource = new ByteArrayResource(TEST_RESOURCE_CONTENT.getBytes());
	}

	@Test
	void testConstructorWithNameAndTemplate() {
		// Test constructor with name and template string
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, TEST_TEMPLATE);

		// Verify template renders correctly with model
		assertThat(template.render(model)).isEqualTo("Hello, John!");
	}

	@Test
	void testConstructorWithNameAndResource() {
		// Test constructor with name and resource
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, resource);

		// Verify template renders correctly with model
		assertThat(template.render(model)).isEqualTo("Resource content: test");
	}

	@Test
	void testConstructorWithNameTemplateAndModel() {
		// Test constructor with name, template and model
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, TEST_TEMPLATE, model);

		// Verify template renders correctly
		assertThat(template.render()).isEqualTo("Hello, John!");
	}

	@Test
	void testConstructorWithNameResourceAndModel() {
		// Test constructor with name, resource and model
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, resource, model);

		// Verify template renders correctly
		assertThat(template.render()).isEqualTo("Resource content: test");
	}

	@Test
	void testCreatePrompt() {
		// Test create() method
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, TEST_TEMPLATE);

		// Create prompt with model
		Prompt prompt = template.create(model);

		// Verify prompt is created correctly
		assertThat(prompt).isNotNull();
		List<Message> messages = prompt.getInstructions();
		assertThat(messages).hasSize(1);
		assertThat(((UserMessage) messages.get(0)).getText()).isEqualTo("Hello, John!");
	}

	@Test
	void testCreatePromptWithOptions() {
		// Test create() method with options
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, TEST_TEMPLATE);
		ChatOptions options = mock(ChatOptions.class);

		// Create prompt with model and options
		Prompt prompt = template.create(model, options);

		// Verify prompt is created correctly
		assertThat(prompt).isNotNull();
		List<Message> messages = prompt.getInstructions();
		assertThat(messages).hasSize(1);
		assertThat(((UserMessage) messages.get(0)).getText()).isEqualTo("Hello, John!");
		assertThat(prompt.getOptions()).isEqualTo(options);
	}

	@Test
	void testCreateMessage() {
		// Test createMessage() method
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, TEST_TEMPLATE);

		// Create message with model
		Message message = template.createMessage(model);

		// Verify message is created correctly
		assertThat(message).isNotNull();
		assertThat(message).isInstanceOf(UserMessage.class);
		assertThat(((UserMessage) message).getText()).isEqualTo("Hello, John!");
	}

	@Test
	void testCreateMessageWithMedia() {
		// Test createMessage() method with media
		Map<String, Object> model = new HashMap<>();
		model.put("name", "John");
		ConfigurablePromptTemplate template = new ConfigurablePromptTemplate(TEST_NAME, TEST_TEMPLATE, model);

		// Create media list
		List<Media> mediaList = new ArrayList<>();
		Media media = mock(Media.class);
		mediaList.add(media);

		// Create message with media
		Message message = template.createMessage(mediaList);

		// Verify message is created correctly
		assertThat(message).isNotNull();
		assertThat(message).isInstanceOf(UserMessage.class);
		assertThat(((UserMessage) message).getMedia()).containsExactly(media);
		assertThat(((UserMessage) message).getText()).isEqualTo("Hello, John!");
	}

}
