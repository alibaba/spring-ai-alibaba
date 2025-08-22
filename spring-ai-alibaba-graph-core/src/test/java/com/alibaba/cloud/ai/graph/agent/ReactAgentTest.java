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

import java.util.*;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;


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
			Optional<OverAllState> result = agent.invoke(Map.of("messages", List.of(new UserMessage("帮我写一首现代诗歌。"))));
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testReactAgentWithPreLlmHook() throws Exception {
		// 创建工具回调解析器，使用真实的WeatherTool
		ToolCallback toolCallback = ToolCallbacks.from(new WeatherTool())[0];
		ReactAgent agent = ReactAgent.builder()
				.name("weather_agent")
				.model(chatModel)
				.tools(List.of(toolCallback))
				.llmInputMessagesKey("llm_input_messages")
				.preLlmHook(state -> {
					// 消息裁剪功能
					if (!state.value("messages").isPresent()) {
						return Map.of();
					}
					List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
					//如果message超过20条
					if (messages.size() > 20) {
						messages = messages.subList(messages.size() - 20, messages.size());
					}
					state.updateState(Map.of("llm_input_messages", messages));
					return Map.of();
				})
				.build();

		CompiledGraph graph = agent.getAndCompileGraph();

		// 创建包含时间查询的提示词
		List<Message> messages = List.of(new UserMessage("查询北京天气"));
		Optional<OverAllState> result = graph.invoke(Map.of("messages", messages));
		System.out.println(result.get());
	}

	/**
	 * 真实的天气工具类，用于演示工具的实际调用
	 */
	public static class WeatherTool {

		@Tool(name = "weather_tool", description = "获取指定城市的天气信息")
		public String getWeather(@ToolParam(description = "城市名称") String city,
								 @ToolParam(description = "当前时间戳") String currentTimestamp) {

			return String.format("城市：%s，温度：20度，时间：%s", city, currentTimestamp);
		}

	}

}
