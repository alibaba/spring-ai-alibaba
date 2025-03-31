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
package com.alibaba.cloud.ai.memory.jdbc;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import static org.mockito.Mockito.mock;

/**
 * @author future0923
 *
 */
class PostgresChatMemoryTest {

	@Test
	public void test() {
		PostgresChatMemory mock = mock(PostgresChatMemory.class);
		Assertions.assertNotNull(mock);
	}

	// @Test
	public void postgresql() {
		ChatMemory chatMemory = new PostgresChatMemory("root", "123456",
				"jdbc:postgresql://127.0.0.1:5432/spring_ai_alibaba_chat_memory");
		String apiKey = System.getenv().getOrDefault("AI_DASHSCOPE_API_KEY", "test-api-key");
		ChatClient chatClient = ChatClient.create(new DashScopeChatModel(new DashScopeApi(apiKey)));
		String content1 = chatClient.prompt()
			.advisors(new MessageChatMemoryAdvisor(chatMemory))
			.user("我是张三😄")
			.call()
			.content();
		System.out.println(content1);
		String content2 = chatClient.prompt()
			.advisors(new MessageChatMemoryAdvisor(chatMemory))
			.user("我是谁")
			.call()
			.content();
		System.out.println(content2);
		Assertions.assertTrue(content2.contains("张三"));
	}

}
