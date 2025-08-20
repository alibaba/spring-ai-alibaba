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
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.flow.SequentialAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.model.ChatModel;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class RemoteAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// 先创建 DashScopeApi 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 DashScope ChatModel 实例
		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();
	}

	@Test
	public void testSequentialAgent() throws Exception {
		KeyStrategyFactory stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("topic", new ReplaceStrategy());
			keyStrategyHashMap.put("article", new ReplaceStrategy());
			keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		A2aRemoteAgent remoteWriterAgent = A2aRemoteAgent.builder()
				.name("writer_agent")
				.agentCard(new AgentCard.Builder().url("https://writer.agent.com").build())
				.description("可以写文章。")
				.outputKey("article")
				.build();

		A2aRemoteAgent remoteReviewerAgent = A2aRemoteAgent.builder()
				.name("reviewer_agent")
				.agentCard(new AgentCard.Builder().url("https://reviewer.agent.com").build())
				.description("可以对文章进行评论和修改。")
				.outputKey("reviewed_article")
				.build();

		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.model(chatModel)
				.state(stateFactory)
				.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论，必要时做出修改。")
				.inputKey("input")
				.outputKey("topic")
				.subAgents(List.of(remoteWriterAgent, remoteReviewerAgent))
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