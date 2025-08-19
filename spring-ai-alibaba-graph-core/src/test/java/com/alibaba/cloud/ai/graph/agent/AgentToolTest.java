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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class AgentToolTest {

	private ChatClient writerChatClient;
	private ChatClient reviewerChatClient;

	@BeforeEach
	void setUp() {
		// 先创建 DashScopeApi 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		// 创建 DashScope ChatModel 实例
		DashScopeChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建 ChatClient 实例
		this.writerChatClient = ChatClient.builder(chatModel)
			.defaultSystem("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
			.build();

		// 创建 ChatClient 实例
		this.reviewerChatClient = ChatClient.builder(chatModel)
				.defaultSystem("你是一个知名的评论家，擅长对文章进行评论和修改。请根据用户的提问进行回答。")
				.build();
	}

	@Test
	public void testAgentTool() throws Exception {
		ReactAgent agent1 = ReactAgent.builder().name("writer_agent").chatClient(writerChatClient).description("").build();

		ReactAgent agent2 = ReactAgent.builder().name("review_agent").chatClient(reviewerChatClient).subAgents(List.of(agent1)).tools(List.of(AgentTool.getFunctionToolCallback(agent1))).build();

		try {
			Optional<OverAllState> result = agent2.invoke(Map.of("messages", List.of(new UserMessage("test"))));

		}
		catch (java.util.concurrent.CompletionException e) {
			// Ignore max iterations exception
		}

		// Verify all hooks were executed

	}

	@Test
	public void testSubAgents() throws Exception {
		ReactAgent agent = ReactAgent.builder().name("testAgent").chatClient(writerChatClient).state(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			keyStrategyHashMap.put("toolOutput", new ReplaceStrategy());
			keyStrategyHashMap.put("toolParams", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).build();

		CompiledGraph graph = agent.getAndCompileGraph();
		try {
			Optional<OverAllState> invoke = graph.invoke(Map.of("messages", List.of(new UserMessage("test"))));
		}
		catch (java.util.concurrent.CompletionException e) {
			// Ignore max iterations exception
		}

	}

}
