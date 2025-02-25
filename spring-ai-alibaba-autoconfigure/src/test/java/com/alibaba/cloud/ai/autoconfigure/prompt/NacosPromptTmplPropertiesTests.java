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
package com.alibaba.cloud.ai.autoconfigure.prompt;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class NacosPromptTmplPropertiesTests {

	@Autowired
	private NacosPromptTmplProperties properties;

	@Test
	void testDefaultEnabled() {
		assertFalse(properties.isEnabled(), "Default enabled should be false");
	}

	@Test
	void testSetEnabled() {
		properties.setEnabled(true);
		assertTrue(properties.isEnabled(), "Enabled should be true after setting to true");
	}

	@Configuration
	@EnableConfigurationProperties(NacosPromptTmplProperties.class)
	static class TestConfig {

	}

}
