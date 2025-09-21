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
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class SequentialAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testSequentialAgent() throws Exception {
		DashScopeChatOptions.builder().withHttpHeaders(new HashMap<>()).build();
		KeyStrategyFactory stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("article", new ReplaceStrategy());
			keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("可以写文章。")
			.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
			.outputKey("article")
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("可以对文章进行评论和修改。")
			.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
			.outputKey("reviewed_article")
			.build();

		SequentialAgent blogAgent = SequentialAgent.builder()
			.name("blog_agent")
			.state(stateFactory)
			.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论，必要时做出修改。")
			.inputKey("input")
			.outputKey("reviewed_article")
			.subAgents(List.of(writerAgent, reviewerAgent))
			.build();

		try {
			Optional<OverAllState> result = blogAgent.invoke(Map.of("input", "帮我写一个100字左右的散文"));

			// 验证结果不为空
			assertTrue(result.isPresent(), "Result should be present");

			OverAllState state = result.get();

			// 验证输入被正确设置
			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一个100字左右的散文", state.value("input").get(), "Input should match the request");

			// 验证文章被创建
			assertTrue(state.value("article").isPresent(), "Article should be present after writer agent");
			String article = (String) state.value("article").get();
			assertNotNull(article, "Article content should not be null");
			assertFalse(article.trim().isEmpty(), "Article content should not be empty");

			// 验证评审后的文章存在
			assertTrue(state.value("reviewed_article").isPresent(),
					"Reviewed article should be present after reviewer agent");
			String reviewedArticle = (String) state.value("reviewed_article").get();
			assertNotNull(reviewedArticle, "Reviewed article content should not be null");
			assertFalse(reviewedArticle.trim().isEmpty(), "Reviewed article content should not be empty");

			// 验证评审后的文章应该包含西湖相关内容（根据评审员的指令）
			assertTrue(reviewedArticle.contains("西湖") || reviewedArticle.toLowerCase().contains("west lake"),
					"Reviewed article should contain West Lake description as per reviewer instructions");

			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("SequentialAgent execution failed: " + e.getMessage());
		}

		// Verify all hooks were executed
	}

}
