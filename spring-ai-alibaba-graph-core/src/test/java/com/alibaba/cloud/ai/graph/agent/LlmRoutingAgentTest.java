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
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

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
class LlmRoutingAgentTest {

	private ChatModel chatModel;

	public static ToolCallback createToolCallback() {
		return FunctionToolCallback.builder("poem", new PoemTool())
			.description("用来写诗的工具")
			.inputType(String.class)
			.build();
	}

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
			.instruction("你是一个���名的作家，擅长写散文。请根据用户的提问进行回答。")
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("可以写现代诗。")
			.instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问，调用工具进行回���。")
			.outputKey("poem_article")
			.tools(List.of(createToolCallback()))
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
			Optional<OverAllState> result = blogAgent.invoke(Map.of("input", "帮我写一个100字左右的现代诗"));
			blogAgent.invoke(Map.of("input", "帮我写一个100字左右的现代诗"));
			Optional<OverAllState> result3 = blogAgent.invoke(Map.of("input", "帮我写一个100字左右的现代诗"));

			// 验证结果不为空
			assertTrue(result.isPresent(), "Result should be present");
			assertTrue(result3.isPresent(), "Third result should be present");

			OverAllState state = result.get();
			OverAllState state3 = result3.get();

			// 验证输入被正确设置
			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一个100字左右的现代诗", state.value("input").get(), "Input should match the request");

			// 验证主题被设置
			assertTrue(state.value("topic").isPresent(), "Topic should be present in state");

			// 验证有诗歌输出
			assertTrue(state.value("poem_article").isPresent(), "Poem article should be present");
			String poemContent = (String) state.value("poem_article").get();
			assertNotNull(poemContent, "Poem content should not be null");
			assertFalse(poemContent.trim().isEmpty(), "Poem content should not be empty");

			System.out.println(result.get());
			System.out.println("------------------");
			System.out.println(result3.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("LlmRoutingAgent execution failed: " + e.getMessage());
		}

		// Verify all hooks were executed
	}

}
