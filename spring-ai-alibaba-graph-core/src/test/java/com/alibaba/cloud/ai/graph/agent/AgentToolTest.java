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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class AgentToolTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testAgentTool() throws Exception {
		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("可以写文章。")
			.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("可以对文章进行评论和修改。")
			.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
			.build();

		ReactAgent blogAgent = ReactAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.instruction("首先，根据用户给定的主题写一篇文章，然后将文章交给评论员进行审核，必要时做出修改。")
			.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent),
					AgentTool.getFunctionToolCallback(reviewerAgent)))
			.build();

		try {
			Optional<OverAllState> result = blogAgent
				.invoke(Map.of("messages", List.of(new UserMessage("帮我写一个100字左右的散文"))));

			// 验证结果不为空
			assertTrue(result.isPresent(), "Result should be present");

			OverAllState state = result.get();

			// 验证消息不为空
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			// 验证输出内容不为空
			Object messages = state.value("messages").get();
			assertNotNull(messages, "Messages should not be null");

			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("Agent tool execution failed: " + e.getMessage());
		}

		// Verify all hooks were executed
	}

}
