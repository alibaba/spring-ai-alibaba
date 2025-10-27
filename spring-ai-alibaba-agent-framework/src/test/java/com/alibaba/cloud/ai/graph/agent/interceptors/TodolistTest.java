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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.HotelTool;
import com.alibaba.cloud.ai.graph.agent.tools.TicketTool;
import com.alibaba.cloud.ai.graph.agent.tools.WeatherTool;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class TodolistTest {

	private ChatModel chatModel;

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

	}

	@Test
	public void testReactAgent() throws Exception {
		
		TodoListInterceptor todoListInterceptor = TodoListInterceptor.builder().build();

		ToolCallback weatherTool = WeatherTool.createWeatherTool("weather_tool", new WeatherTool());
		ToolCallback ticketTool = TicketTool.createTicketToolCallback("ticket_booking_tool", new TicketTool());
		ToolCallback hotelTool = HotelTool.createHotelTool("hotel_booking_tool", new HotelTool());


		ReactAgent agent =
				ReactAgent.builder()
						.name("single_agent")
						.model(chatModel)
						.interceptors(todoListInterceptor)
						.tools(weatherTool, ticketTool, hotelTool)
						.saver(new MemorySaver())
						.build();

		try {
			Optional<OverAllState> result = agent.invoke("帮我制定一个12月30日开始去北京的3天旅游计划，并完成车票和酒店预订。这个任务比较复杂，你先分解成几个小任务，然后逐个完成每个小任务，最后汇总输出整个旅游计划。调用 `write_todos` 记录并跟踪任务执行过程。");

			assertTrue(result.isPresent(), "Agent result should be present");
			result.ifPresent(overAllState -> {
				List<Message> messages = overAllState.value("messages", new ArrayList<>());

				// Assert that messages contain at least one ToolResponseMessage with name "write_todos"
				boolean hasWriteTodosToolResponse = messages.stream()
						.filter(msg -> msg instanceof ToolResponseMessage)
						.map(msg -> (ToolResponseMessage) msg)
						.flatMap(toolMsg -> toolMsg.getResponses().stream())
						.anyMatch(response -> "write_todos".equals(response.name()));

				assertTrue(hasWriteTodosToolResponse,
						"Messages should contain at least one ToolResponseMessage with ToolResponse name 'write_todos'");
			});

		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

}
