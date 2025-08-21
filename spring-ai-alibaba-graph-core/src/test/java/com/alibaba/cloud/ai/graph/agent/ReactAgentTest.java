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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.fastjson.JSON;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

import org.springframework.ai.chat.messages.Message;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

class ReactAgentTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private ChatClient.CallResponseSpec responseSpec;

	@Mock
	private ChatResponse chatResponse;

	@Mock
	private ToolCallbackResolver toolCallbackResolver;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		// 创建真实的WeatherTool工具
		ToolCallback[] toolCallbacks = ToolCallbacks.from(new WeatherTool());
		ToolCallback weatherTool = toolCallbacks[0]; // 获取第一个工具

		// Configure mock ChatClient with complete call chain
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.messages(anyList())).thenReturn(requestSpec);
		when(requestSpec.advisors(anyList())).thenReturn(requestSpec);
		when(requestSpec.toolCallbacks(anyList())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(responseSpec);

		// 配置真实的工具回调解析器
		when(toolCallbackResolver.resolve("weather_tool")).thenReturn(weatherTool);

		// 第一次调用：有工具调用
		List<ToolCall> currentToolCalls = List.of(new ToolCall("call_1", "function", "weather_tool",
				"{\"city\": \"Beijing\",\"currentTimestamp\": \"1000000\"}"));
		AssistantMessage assistantMessage = new AssistantMessage("北京温度20度", Map.of(), currentToolCalls,
				Collections.emptyList());
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder().finishReason("stop").build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponse responseWithToolCalls = ChatResponse.builder().generations(List.of(generation)).build();

		// 第二次调用：没有工具调用
		AssistantMessage assistantMessageNoTools = new AssistantMessage("北京温度20度", Map.of(), Collections.emptyList(),
				Collections.emptyList());
		Generation generationNoTools = new Generation(assistantMessageNoTools, generationMetadata);
		ChatResponse responseNoToolCalls = ChatResponse.builder().generations(List.of(generationNoTools)).build();

		// 使用链式调用来控制返回值
		when(responseSpec.chatResponse()).thenReturn(responseWithToolCalls) // 第一次调用返回有工具调用的响应
			.thenReturn(responseNoToolCalls); // 第二次调用返回没有工具调用的响应

	}

	@Test
	public void testReactAgentWithPreLlmHook() throws Exception {
		// 创建工具回调解析器，使用真实的WeatherTool
		ToolCallbackResolver dataToolResolver = toolName -> {
			if ("weather_tool".equals(toolName)) {
				return ToolCallbacks.from(new WeatherTool())[0]; // 获取第一个工具
			}
			return ToolCallbacks.from(new WeatherTool())[0]; // 默认返回第一个工具
		};
		AtomicBoolean truncated = new AtomicBoolean(false);
		ReactAgent agent = ReactAgent.builder()
			.name("dataAgent")
			.chatClient(chatClient)
			.resolver(dataToolResolver)
			.llmInputMessagesKey("llm_input_messages")
			.preLlmHook(state -> {
				// 消息裁剪功能
				if (!state.value("messages").isPresent()) {
					return Map.of();
				}

				List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
				List<Message> truncatedMessages = new ArrayList<>();
				List<Message> userMessages = messages.stream().filter(msg -> msg instanceof UserMessage).toList();
				truncatedMessages.addAll(userMessages);
				List<Message> nonUserMessages = messages.stream().filter(msg -> !(msg instanceof UserMessage)).toList();
				if (nonUserMessages.size() > 1) {
					truncatedMessages
						.addAll(nonUserMessages.subList(nonUserMessages.size() - 1, nonUserMessages.size()));
					truncated.set(true);
				}
				else {
					truncatedMessages.addAll(nonUserMessages);
				}

				// 更新 messages 状态
				state.updateState(Map.of("llm_input_messages", truncatedMessages));
				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();

		// 创建包含时间查询的提示词
		List<Message> messages = List.of(new UserMessage("请打印当前时间的天气"));
		Optional<OverAllState> result = graph.invoke(Map.of("messages", messages));
		Assert.assertEquals(truncated.get(), "消息裁剪成功");
	}

	@Test
	public void testReactAgentWithDPostLlmHook() throws Exception {
		// 创建工具回调解析器，使用真实的WeatherTool
		ToolCallbackResolver dataToolResolver = toolName -> {
			if ("weather_tool".equals(toolName)) {
				return ToolCallbacks.from(new WeatherTool())[0]; // 获取第一个工具
			}
			return ToolCallbacks.from(new WeatherTool())[0]; // 默认返回第一个工具
		};

		// 使用AtomicBoolean来跟踪调用状态
		AtomicBoolean isSecondCall = new AtomicBoolean(false);

		ReactAgent agent = ReactAgent.builder()
			.name("dataAgent")
			.chatClient(chatClient)
			.resolver(dataToolResolver)
			.postLlmHook(state -> {

				// 写一个判断是否工具避免被调用两次的逻辑
				List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
				if (!messages.isEmpty()) {
					Message lastMessage = messages.get(messages.size() - 1);
					if (lastMessage instanceof AssistantMessage) {
						AssistantMessage assistantMessage = (AssistantMessage) lastMessage;

						// 使用AtomicBoolean来判断是否是第二次调用
						if (isSecondCall.get()) {
							// 第二次调用：清掉toolCall
							System.out.println("postLlmHook: 第二次调用，清掉toolCall");

							// 创建没有toolCall的AssistantMessage
							AssistantMessage updatedAssistantMessage = new AssistantMessage(assistantMessage.getText(),
									assistantMessage.getMetadata(), Collections.emptyList(), // 清掉toolCall
									Collections.emptyList());

							// 更新消息列表
							List<Message> updatedMessages = new ArrayList<>(messages);
							updatedMessages.set(updatedMessages.size() - 1, updatedAssistantMessage);
							state.updateState(Map.of("messages", updatedMessages));
						}
						else {
							System.out.println("postLlmHook: 第一次调用，保留toolCall");
						}
					}
				}

				return Map.of();
			})
			.postToolHook(state -> {
				isSecondCall.set(true);
				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();

		// 创建包含时间查询的提示词
		List<Message> messages = List.of(new UserMessage("请打印当前时间的天气"));
		Optional<OverAllState> result = graph.invoke(Map.of("messages", messages));
		System.out.println(result);

	}

	@Test
	public void testReactAgentWithPreToolHook() throws Exception {
		// 创建工具回调解析器，使用真实的WeatherTool
		ToolCallbackResolver dataToolResolver = toolName -> {
			if ("weather_tool".equals(toolName)) {
				return ToolCallbacks.from(new WeatherTool())[0]; // 获取第一个工具
			}
			return ToolCallbacks.from(new WeatherTool())[0]; // 默认返回第一个工具
		};

		ReactAgent agent = ReactAgent.builder()
			.name("dataAgent")
			.chatClient(chatClient)
			.resolver(dataToolResolver)
			.preToolHook(state -> {
				// 在preToolHook中获取最新的时间戳 传给toolCall保证时效性
				long currentTimestamp = System.currentTimeMillis();
				String formattedTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(new java.util.Date(currentTimestamp));

				List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
				if (!messages.isEmpty()) {
					Message lastMessage = messages.get(messages.size() - 1);
					if (lastMessage instanceof AssistantMessage) {
						AssistantMessage assistantMessage = (AssistantMessage) lastMessage;
						if (assistantMessage.hasToolCalls()) {
							// 修改每个ToolCall的参数
							for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
								if ("weather_tool".equals(toolCall.name())) {
									// 替换原始参数中的时间戳
									String originalArgs = toolCall.arguments();
									Map<String, Object> updatedArgs = JSON.parseObject(originalArgs);
									updatedArgs.put("currentTimestamp", formattedTime);
									// 创建新的ToolCall，替换参数
									AssistantMessage.ToolCall updatedToolCall = new AssistantMessage.ToolCall(
											toolCall.id(), toolCall.type(), toolCall.name(),
											JSON.toJSONString(updatedArgs));

									// 更新消息中的ToolCall
									List<AssistantMessage.ToolCall> updatedToolCalls = new ArrayList<>();
									for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
										if (tc.id().equals(toolCall.id())) {
											updatedToolCalls.add(updatedToolCall);
										}
										else {
											updatedToolCalls.add(tc);
										}
									}

									// 创建新的AssistantMessage
									AssistantMessage updatedAssistantMessage = new AssistantMessage(
											assistantMessage.getText(), assistantMessage.getMetadata(),
											updatedToolCalls, Collections.emptyList());

									// 更新消息列表
									List<Message> updatedMessages = new ArrayList<>(messages);
									updatedMessages.set(updatedMessages.size() - 1, updatedAssistantMessage);
									state.updateState(Map.of("messages", updatedMessages));

									break;
								}
							}
						}
					}
				}

				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();

		// 创建包含时间查询的提示词
		List<Message> messages = List.of(new UserMessage("请打印当前时间的天气"));
		Optional<OverAllState> result = graph.invoke(Map.of("messages", messages));
		System.out.println(result);

	}

	/**
	 * 测试ReactAgent中KeyStrategyFactory的逻辑 验证当llmInputMessagesKey不为null时，是否正确添加到策略映射中
	 */
	@Test
	public void testReactAgentKeyStrategyFactory() throws Exception {
		// 创建工具回调解析器
		ToolCallbackResolver dataToolResolver = toolName -> {
			if ("weather_tool".equals(toolName)) {
				return ToolCallbacks.from(new WeatherTool())[0];
			}
			return ToolCallbacks.from(new WeatherTool())[0];
		};

		// 测试场景1：llmInputMessagesKey不为null
		ReactAgent agent1 = ReactAgent.builder()
			.name("testAgent1")
			.chatClient(chatClient)
			.resolver(dataToolResolver)
			.llmInputMessagesKey("llm_input_messages") // 设置自定义的llmInputMessagesKey
			.build();

		KeyStrategyFactory keyStrategyFactory = agent1.getKeyStrategyFactory();
		Map<String, KeyStrategy> keyStrategyMap = keyStrategyFactory.apply();
		assertTrue(keyStrategyMap.containsKey("llm_input_messages"));
		assertTrue(keyStrategyMap.containsKey("messages"));

		ReactAgent agent2 = ReactAgent.builder()
			.name("testAgent2")
			.chatClient(chatClient)
			.resolver(dataToolResolver)
			.llmInputMessagesKey("llm_input_messages")
			.state(() -> {
				Map<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("llm_input_messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.build();

		KeyStrategyFactory keyStrategyFactory2 = agent2.getKeyStrategyFactory();
		Map<String, KeyStrategy> keyStrategyMap2 = keyStrategyFactory2.apply();
		assertTrue(keyStrategyMap2.containsKey("llm_input_messages"));
		assertTrue(keyStrategyMap2.containsKey("messages"));
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

