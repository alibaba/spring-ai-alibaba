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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ReactAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testReactAgent() throws Exception {
		ReactAgent agent = ReactAgent.builder().name("single_agent").model(chatModel).build();
		try {
			Optional<OverAllState> result = agent
				.invoke(Map.of("messages", List.of(new UserMessage("帮我写一篇100字左右散文。"))));
			Optional<OverAllState> result2 = agent.invoke(Map.of("messages", List.of(new UserMessage("帮我写一首现代诗歌。"))));

			// 验证第一个结果不为空
			assertTrue(result.isPresent(), "First result should be present");
			OverAllState state1 = result.get();

			// 验证消息存在
			assertTrue(state1.value("messages").isPresent(), "Messages should be present in first result");
			Object messages1 = state1.value("messages").get();
			assertNotNull(messages1, "Messages should not be null in first result");

			// 验证第二个结果不为空
			assertTrue(result2.isPresent(), "Second result should be present");
			OverAllState state2 = result2.get();

			// 验证消息存在
			assertTrue(state2.value("messages").isPresent(), "Messages should be present in second result");
			Object messages2 = state2.value("messages").get();
			assertNotNull(messages2, "Messages should not be null in second result");

			// 验证两个结果不同（因为输入不同）
			assertNotEquals(messages1, messages2, "Results should be different for different inputs");

			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

}
