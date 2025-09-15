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
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.model.ChatModel;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class LlmRoutingAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testLlmRoutingAgent() throws Exception {
		KeyStrategyFactory stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("topic", new ReplaceStrategy());
			keyStrategyHashMap.put("article", new ReplaceStrategy());
			keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("可以写散文文章。")
			.instruction("你是一个知名的作家，擅长写散文。请根据用户的提问进行回答。")
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("可以写现代诗。")
			.instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问进行回答。")
			.outputKey("poem_article")
			.build();

		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.state(stateFactory)
			.description("可以根据用户给定的主题写文章或作诗。")
			.inputKey("input")
			.outputKey("topic")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.build();

		try {
			Optional<OverAllState> result = blogAgent.invoke(Map.of("input", "帮我写一个100字左右的散文"));
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
		}

		// Verify all hooks were executed
	}

}
