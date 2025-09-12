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
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ReactAgentHookTest {

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

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {

		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

		MockitoAnnotations.openMocks(this);

		// Configure mock ChatClient with complete call chain
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.options(any())).thenReturn(requestSpec);
		when(requestSpec.messages(anyList())).thenReturn(requestSpec);
		when(requestSpec.advisors(anyList())).thenReturn(requestSpec);
		when(requestSpec.toolCallbacks(anyList())).thenReturn(requestSpec);
		when(requestSpec.system(anyString())).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(responseSpec);

		// 使用真实的 WeatherTool 创建 ToolCallback
		ToolCallback weatherToolCallback = ToolCallbacks.from(new WeatherTool())[0];
		when(toolCallbackResolver.resolve(anyString())).thenReturn(weatherToolCallback);

		// Configure mock ChatResponse with ToolCalls
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", "stop");
		List<ToolCall> toolCalls = List.of(new ToolCall("call_1", "function", "weather_tool",
				"{\"city\": \"北京\", \"currentTimestamp\": \"1693665600\"}"));
		AssistantMessage assistantMessage = new AssistantMessage("test response", metadata, toolCalls,
				Collections.emptyList());
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder().finishReason("stop").build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponseMetadata responseMetadata = ChatResponseMetadata.builder()
			.id("test-id")
			.usage(new DefaultUsage(10, 20, 30))
			.build();
		ChatResponse response = ChatResponse.builder()
			.generations(List.of(generation))
			.metadata(responseMetadata)
			.build();
		when(responseSpec.chatResponse()).thenReturn(response);
	}

	/**
	 * Tests ReactAgent with preLlmHook that modifies system prompt before LLM call.
	 */
	@Test
	public void testReactAgentWithPreLlmHook() throws Exception {
		ToolCallback toolCallback = ToolCallbacks.from(new WeatherTool())[0];
		ReactAgent agent = ReactAgent.builder()
			.name("weather_agent")
			.model(chatModel)
			.tools(List.of(toolCallback))
			.inputKey("llm_input_messages")
			.preLlmHook(state -> {
				if (!state.value("messages").isPresent()) {
					return Map.of();
				}
				List<Message> messages = (List<Message>) state.value("messages").orElseThrow();

				// 消息裁剪功能
				if (messages.size() > 20) {
					List<Message> userMessages = messages.stream().filter(msg -> msg instanceof UserMessage).toList();
					List<Message> last20Messages = messages.subList(messages.size() - 20, messages.size());
					List<Message> resultMessages = new ArrayList<>(last20Messages);
					for (Message userMsg : userMessages) {
						if (!resultMessages.contains(userMsg)) {
							resultMessages.add(userMsg);
						}
					}
					messages = resultMessages;
				}

				state.updateState(Map.of("llm_input_messages", messages));
				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();

		// 创建包含时间查询的提示词
		List<Message> messages = List.of(new UserMessage("查询北京天气"));
		Optional<OverAllState> result = graph.call(Map.of("llm_input_messages", messages));
		System.out.println(result.get());

		System.out.println("==testReactAgentWithPreLlmHook==");

	}

	/**
	 * Tests ReactAgent with postLlmHook that processes LLM response.
	 */
	@Test
	public void testReactAgentWithPostLlmHook() throws Exception {
		// 使用AtomicBoolean来跟踪调用状态
		ToolCallback toolCallback = ToolCallbacks.from(new WeatherTool())[0];
		AtomicBoolean isSecondCall = new AtomicBoolean(false);

		ReactAgent agent = ReactAgent.builder()
			.name("dataAgent")
			.model(chatModel)
			.inputKey("llm_input_messages")
			.tools(List.of(toolCallback))
			.postLlmHook(state -> {

				return Map.of();
			})
			.postToolHook(state -> {
				List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
				state.updateState(Map.of("llm_input_messages", messages));
				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();
		// 创建包含时间查询的提示词
		List<Message> messages = List.of(new UserMessage("查询北京天气"));
		Optional<OverAllState> result = graph.call(Map.of("messages", messages));
		System.out.println(result);

		System.out.println("==testReactAgentWithPostLlmHook==");
	}

	/**
	 * Tests ReactAgent with preToolHook that prepares tool parameters.
	 */
	@Test
	public void testReactAgentWithPreToolHook() throws Exception {
		// 使用AtomicBoolean来跟踪调用状态
		ToolCallback toolCallback = ToolCallbacks.from(new WeatherTool())[0];

		ReactAgent agent = ReactAgent.builder()
			.name("dataAgent")
			.model(chatModel)
			.tools(List.of(toolCallback))
			.inputKey("llm_input_messages")
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
							// 更新ToolCall参数，把时间换成当前系统最新时间
							for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
								if ("weather_tool".equals(toolCall.name())) {
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
									AssistantMessage updatedAssistantMessage = new AssistantMessage(
											assistantMessage.getText(), assistantMessage.getMetadata(),
											updatedToolCalls, Collections.emptyList());
									List<Message> updatedMessages = new ArrayList<>(messages);
									updatedMessages.set(updatedMessages.size() - 1, updatedAssistantMessage);
									state.updateState(Map.of("llm_input_messages", updatedMessages));
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
		List<Message> messages = List.of(new UserMessage("查询北京天气"));
		Optional<OverAllState> result = graph.call(Map.of("llm_input_messages", messages));
		System.out.println(result);

		System.out.println("==testReactAgentWithPreToolHook==");

	}

	/**
	 * 天气工具类，用于演示工具的实际调用
	 */
	public static class WeatherTool {

		@Tool(name = "weather_tool", description = "获取指定城市的天气信息")
		public String getWeather(@ToolParam(description = "城市名称") String city,
				@ToolParam(description = "当前时间戳") String currentTimestamp) {
			System.out.println("==TOOL被调用==");
			return String.format("{\"city\": \"%s\", \"temperature\": -50, \"time\": \"%s\"}", city, currentTimestamp);
		}

	}

}
