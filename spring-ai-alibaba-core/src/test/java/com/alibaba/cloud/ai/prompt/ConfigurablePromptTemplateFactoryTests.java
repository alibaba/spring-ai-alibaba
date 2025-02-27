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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for ConfigurablePromptTemplateFactory. Tests template creation, retrieval,
 * and configuration change handling.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class ConfigurablePromptTemplateFactoryTests {

	private ConfigurablePromptTemplateFactory factory;

	private static final String TEST_TEMPLATE_NAME = "test-template";

	private static final String TEST_TEMPLATE_CONTENT = "Hello, {name}!";

	private static final String TEST_RESOURCE_CONTENT = "Resource content: {value}";

	@BeforeEach
	void setUp() {
		// Initialize the factory before each test
		factory = new ConfigurablePromptTemplateFactory();
	}

	@Test
	void testCreateWithNameAndTemplate() {
		// Test creating template with name and template string
		ConfigurablePromptTemplate template = factory.create(TEST_TEMPLATE_NAME, TEST_TEMPLATE_CONTENT);

		// Verify template is created correctly
		assertThat(template).isNotNull();

		// Create a model with required variable
		Map<String, Object> model = new HashMap<>();
		model.put("name", "John");

		// Verify template renders correctly with model
		assertThat(template.render(model)).isEqualTo("Hello, John!");
		assertThat(factory.getTemplate(TEST_TEMPLATE_NAME)).isEqualTo(template);
	}

	@Test
	void testCreateWithNameAndResource() {
		// Test creating template with name and resource
		Resource resource = new ByteArrayResource(TEST_RESOURCE_CONTENT.getBytes());
		ConfigurablePromptTemplate template = factory.create(TEST_TEMPLATE_NAME, resource);

		// Verify template is created correctly
		assertThat(template).isNotNull();

		// Create a model with required variable
		Map<String, Object> model = new HashMap<>();
		model.put("value", "test");

		// Verify template renders correctly with model
		assertThat(template.render(model)).isEqualTo("Resource content: test");
		assertThat(factory.getTemplate(TEST_TEMPLATE_NAME)).isEqualTo(template);
	}

	@Test
	void testCreateWithNameTemplateAndModel() {
		// Test creating template with name, template string and model
		Map<String, Object> model = new HashMap<>();
		model.put("name", "John");

		ConfigurablePromptTemplate template = factory.create(TEST_TEMPLATE_NAME, TEST_TEMPLATE_CONTENT, model);

		// Verify template is created correctly
		assertThat(template).isNotNull();
		assertThat(template.render()).isEqualTo("Hello, John!");
		assertThat(factory.getTemplate(TEST_TEMPLATE_NAME)).isEqualTo(template);
	}

	@Test
	void testCreateWithNameResourceAndModel() {
		// Test creating template with name, resource and model
		Resource resource = new ByteArrayResource(TEST_RESOURCE_CONTENT.getBytes());
		Map<String, Object> model = new HashMap<>();
		model.put("value", "test");

		ConfigurablePromptTemplate template = factory.create(TEST_TEMPLATE_NAME, resource, model);

		// Verify template is created correctly
		assertThat(template).isNotNull();
		assertThat(template.render()).isEqualTo("Resource content: test");
		assertThat(factory.getTemplate(TEST_TEMPLATE_NAME)).isEqualTo(template);
	}

	@Test
	void testGetNonExistentTemplate() {
		// Test getting a template that doesn't exist
		ConfigurablePromptTemplate template = factory.getTemplate("non-existent");

		// Verify null is returned
		assertThat(template).isNull();
	}

	@Test
	void testOnConfigChangeWithValidConfigs() {
		// Test configuration change with valid configurations
		List<ConfigurablePromptTemplateFactory.ConfigurablePromptTemplateModel> configList = new ArrayList<>();
		Map<String, Object> model = new HashMap<>();
		model.put("name", "John");

		configList.add(new ConfigurablePromptTemplateFactory.ConfigurablePromptTemplateModel(TEST_TEMPLATE_NAME,
				TEST_TEMPLATE_CONTENT, model));

		// Trigger configuration change
		factory.onConfigChange(configList);

		// Verify template is updated
		ConfigurablePromptTemplate template = factory.getTemplate(TEST_TEMPLATE_NAME);
		assertThat(template).isNotNull();
		assertThat(template.render()).isEqualTo("Hello, John!");
	}

	@Test
	void testOnConfigChangeWithEmptyConfigs() {
		// Test configuration change with empty config list
		factory.onConfigChange(new ArrayList<>());

		// Verify no changes are made
		assertThat(factory.getTemplate(TEST_TEMPLATE_NAME)).isNull();
	}

	@Test
	void testOnConfigChangeWithInvalidConfigs() {
		// Test configuration change with invalid configurations
		List<ConfigurablePromptTemplateFactory.ConfigurablePromptTemplateModel> configList = new ArrayList<>();
		configList.add(new ConfigurablePromptTemplateFactory.ConfigurablePromptTemplateModel("", "", null));

		// Trigger configuration change
		factory.onConfigChange(configList);

		// Verify no changes are made for invalid config
		assertThat(factory.getTemplate("")).isNull();
	}

	@Test
	void testTemplateReuse() {
		// Test that same name returns same template instance
		ConfigurablePromptTemplate template1 = factory.create(TEST_TEMPLATE_NAME, TEST_TEMPLATE_CONTENT);
		ConfigurablePromptTemplate template2 = factory.create(TEST_TEMPLATE_NAME, TEST_TEMPLATE_CONTENT);

		// Verify both references point to same instance
		assertThat(template1).isSameAs(template2);
	}

}
