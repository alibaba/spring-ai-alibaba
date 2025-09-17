/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.llm;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class DynamicHeaderPreservationTest {

	@Mock
	private ManusProperties manusProperties;

	private LlmService llmService;

	@BeforeEach
	void setUp() {
		llmService = new LlmService();
	}

	@Test
	void testDynamicChatClientCreationWithHeaders() {
		DynamicModelEntity model = new DynamicModelEntity();
		model.setId(1L);
		model.setBaseUrl("https://test.example.com");
		model.setApiKey("test-api-key");
		model.setModelName("test-model");

		Map<String, String> headers = new HashMap<>();
		headers.put("Custom-Header", "test-value");
		headers.put("Authorization", "Bearer test-token");
		model.setHeaders(headers);

		ChatClient chatClient = llmService.buildOrUpdateDynamicChatClient(model);

		assertNotNull(chatClient, "ChatClient should be created successfully");

	}

	@Test
	void testDynamicChatClientCreationWithoutHeaders() {
		DynamicModelEntity model = new DynamicModelEntity();
		model.setId(2L);
		model.setBaseUrl("https://test.example.com");
		model.setApiKey("test-api-key");
		model.setModelName("test-model");

		ChatClient chatClient = llmService.buildOrUpdateDynamicChatClient(model);
		assertNotNull(chatClient, "ChatClient should be created successfully even without headers");
	}

}
