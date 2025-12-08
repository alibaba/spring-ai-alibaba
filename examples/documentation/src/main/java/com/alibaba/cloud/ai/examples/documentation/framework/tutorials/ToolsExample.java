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
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Tools Tutorial - 完整代码示例
 * 展示如何创建和使用Tools让Agent与外部系统交互
 *
 * 来源：tools.md
 */
public class ToolsExample {

	// ==================== 基础工具定义 ====================

	/**
	 * 示例1：编程方式规范 - FunctionToolCallback
	 */
	public static void programmaticToolSpecification() {
		ToolCallback toolCallback = FunctionToolCallback
				.builder("currentWeather", new WeatherService())
				.description("Get the weather in location")
				.inputType(WeatherRequest.class)
				.build();
	}

	/**
	 * 示例2：添加工具到 ChatClient（使用编程规范）
	 */
	public static void addToolToChatClient() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback toolCallback = FunctionToolCallback
				.builder("currentWeather", new WeatherService())
				.description("Get the weather in location")
				.inputType(WeatherRequest.class)
				.build();

		// Note: ChatClient usage would be shown here in actual implementation
		// This is a simplified example
	}

	/**
	 * 示例3：自定义工具名称
	 */
	public static void customToolName() {
		ToolCallback searchTool = FunctionToolCallback
				.builder("web_search", new SearchFunction())  // 自定义名称
				.description("Search the web for information")
				.inputType(String.class)
				.build();

		System.out.println(searchTool.getToolDefinition().name());  // web_search
	}

	/**
	 * 示例4：自定义工具描述
	 */
	public static void customToolDescription() {
		ToolCallback calculatorTool = FunctionToolCallback
				.builder("calculator", new CalculatorFunction())
				.description("Performs arithmetic calculations. Use this for any math problems.")
				.inputType(String.class)
				.build();
	}

	/**
	 * 示例5：高级模式定义
	 */
	public static void advancedSchemaDefinition() {
		ToolCallback weatherTool = FunctionToolCallback
				.builder("get_weather", new WeatherFunction())
				.description("Get current weather and optional forecast")
				.inputType(WeatherInput.class)
				.build();
	}

	/**
	 * 示例6：访问状态
	 */
	public static void accessingState() {
		// 创建工具
		ToolCallback summaryTool = FunctionToolCallback
				.builder("summarize_conversation", new ConversationSummaryTool())
				.description("Summarize the conversation so far")
				.inputType(String.class)
				.build();
	}

	// ==================== 自定义工具属性 ====================

	/**
	 * 示例7：访问上下文
	 */
	public static void accessingContext() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback accountTool = FunctionToolCallback
				.builder("get_account_info", new AccountInfoTool())
				.description("Get the current user's account information")
				.inputType(String.class)
				.build();

		// 在 ReactAgent 中使用
		ReactAgent agent = ReactAgent.builder()
				.name("financial_assistant")
				.model(chatModel)
				.tools(accountTool)
				.systemPrompt("You are a financial assistant.")
				.build();

		// 调用时传递上下文
		RunnableConfig config = RunnableConfig.builder()
				.addMetadata("user_id", "user123")
				.build();

		agent.call("question", config);
	}

	/**
	 * 示例8：使用存储访问跨对话的持久数据
	 */
	public static void accessingMemoryStore() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 配置持久化存储
		MemorySaver memorySaver = new MemorySaver();

		// 创建工具
		ToolCallback saveUserInfoTool = createSaveUserInfoTool();
		ToolCallback getUserInfoTool = createGetUserInfoTool();

		// 创建带有持久化记忆的 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(saveUserInfoTool, getUserInfoTool)
				.saver(memorySaver)
				.build();

		// 第一个会话：保存用户信息
		RunnableConfig config1 = RunnableConfig.builder()
				.threadId("session_1")
				.build();

		agent.call("Save user: userid: abc123, name: Foo, age: 25, email: foo@example.com", config1);

		// 第二个会话：获取用户信息，注意这里用的是不同的 threadId
		RunnableConfig config2 = RunnableConfig.builder()
				.threadId("session_2")
				.build();

		agent.call("Get user info for user with id 'abc123'", config2);
	}

	/**
	 * 示例9：在 ReactAgent 中使用工具
	 */
	public static void toolsInReactAgent() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具
		ToolCallback weatherTool = FunctionToolCallback
				.builder("get_weather", new WeatherFunction())
				.description("Get weather for a given city")
				.inputType(WeatherInput.class)
				.build();

		ToolCallback searchTool = FunctionToolCallback
				.builder("search", new SearchFunction())
				.description("Search for information")
				.inputType(String.class)
				.build();

		// 创建带有工具的 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(weatherTool, searchTool)
				.systemPrompt("You are a helpful assistant with access to weather and search tools.")
				.saver(new MemorySaver())
				.build();

		// 使用 Agent
		AssistantMessage response = agent.call("What's the weather like in San Francisco?");
		System.out.println(response.getText());
	}

	/**
	 * 示例10：完整的工具使用示例（使用 tools 方法）
	 */
	public static void comprehensiveToolExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 定义多个工具
		ToolCallback weatherTool = FunctionToolCallback
				.builder("get_weather", new WeatherFunction())
				.description("Get current weather and optional forecast for a city")
				.inputType(WeatherInput.class)
				.build();

		ToolCallback calculatorTool = FunctionToolCallback
				.builder("calculator", new CalculatorFunction())
				.description("Perform arithmetic calculations")
				.inputType(String.class)
				.build();

		ToolCallback searchTool = FunctionToolCallback
				.builder("web_search", new SearchFunction())
				.description("Search the web for information")
				.inputType(String.class)
				.build();

		// 创建 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.tools(weatherTool, calculatorTool, searchTool)
				.systemPrompt("""
						You are a helpful AI assistant with access to multiple tools:
						- Weather information
						- Calculator for math operations
						- Web search for general information
						
						Use the appropriate tool based on the user's question.
						""")
				.saver(new MemorySaver())
				.build();

		// 使用不同的工具
		RunnableConfig config = RunnableConfig.builder()
				.threadId("session_1")
				.build();

		agent.call("What's the weather in New York?", config);
		agent.call("Calculate 25 * 4 + 10", config);
		agent.call("Search for latest AI news", config);
	}

	/**
	 * 示例11：使用 methodTools - 基于 @Tool 注解的方法工具
	 */
	public static void methodToolsExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建带有 @Tool 注解方法的工具对象
		CalculatorTools calculatorTools = new CalculatorTools();

		// 使用 methodTools 方法，传入带有 @Tool 注解方法的对象
		ReactAgent agent = ReactAgent.builder()
				.name("calculator_agent")
				.model(chatModel)
				.description("An agent that can perform calculations")
				.instruction("You are a helpful calculator assistant. Use the available tools to perform calculations.")
				.methodTools(calculatorTools)  // 传入带有 @Tool 注解方法的对象
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("method_tools_session")
				.build();

		agent.call("What is 15 + 27?", config);
		agent.call("What is 8 * 9?", config);
	}

	/**
	 * 示例12：使用多个 methodTools 对象
	 */
	public static void multipleMethodToolsExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建多个工具对象
		CalculatorTools calculatorTools = new CalculatorTools();
		WeatherTools weatherTools = new WeatherTools();

		// 可以传入多个 methodTools 对象
		ReactAgent agent = ReactAgent.builder()
				.name("multi_method_tool_agent")
				.model(chatModel)
				.description("An agent with multiple method-based tools")
				.instruction("You are a helpful assistant with calculator and weather tools.")
				.methodTools(calculatorTools, weatherTools)  // 传入多个工具对象
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("multi_method_tools_session")
				.build();

		agent.call("What is 10 * 8 and what's the weather in Beijing?", config);
	}

	/**
	 * 示例13：使用 ToolCallbackProvider
	 */
	public static void toolCallbackProviderExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具
		ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchToolWithContext())
				.description("Search for information")
				.inputType(String.class)
				.build();

		// 创建 ToolCallbackProvider
		ToolCallbackProvider toolProvider = new CustomToolCallbackProvider(List.of(searchTool));

		// 使用 toolCallbackProviders 方法
		ReactAgent agent = ReactAgent.builder()
				.name("search_agent")
				.model(chatModel)
				.description("An agent that can search for information")
				.instruction("You are a helpful assistant with search capabilities.")
				.toolCallbackProviders(toolProvider)  // 使用 ToolCallbackProvider
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("tool_provider_session")
				.build();

		agent.call("Search for information about Spring AI", config);
	}

	/**
	 * 示例14：使用 toolNames 和 resolver（必须配合使用）
	 */
	public static void toolNamesWithResolverExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具（使用复合类型）
		ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchFunctionWithRequest())
				.description("Search for information")
				.inputType(SearchRequest.class)
				.build();

		ToolCallback calculatorTool = FunctionToolCallback.builder("calculator", new CalculatorFunctionWithRequest())
				.description("Perform arithmetic calculations")
				.inputType(CalculatorRequest.class)
				.build();

		// 创建 StaticToolCallbackResolver，包含所有工具
		StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(
				List.of(calculatorTool, searchTool));

		// 使用 toolNames 指定要使用的工具名称，必须配合 resolver 使用
		ReactAgent agent = ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.description("An agent with multiple tools")
				.instruction("You are a helpful assistant with access to calculator and search tools.")
				.toolNames("calculator", "search")  // 使用工具名称而不是 ToolCallback 实例
				.resolver(resolver)  // 必须提供 resolver 来解析工具名称
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("tool_names_session")
				.build();

		agent.call("Calculate 25 + 4 and then search for information about the result", config);
	}

	/**
	 * 示例15：使用 resolver 直接解析工具
	 */
	public static void resolverExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具
		ToolCallback calculatorTool = FunctionToolCallback.builder("calculator", new CalculatorFunctionWithContext())
				.description("Perform arithmetic calculations")
				.inputType(String.class)
				.build();

		// 创建 resolver
		StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(
				List.of(calculatorTool));

		// 使用 resolver，可以直接在 tools 中使用，也可以仅通过 resolver 提供
		ReactAgent agent = ReactAgent.builder()
				.name("resolver_agent")
				.model(chatModel)
				.description("An agent using ToolCallbackResolver")
				.instruction("You are a helpful calculator assistant.")
				.tools(calculatorTool)  // 直接指定工具
				.resolver(resolver)  // 同时设置 resolver 供工具节点使用
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("resolver_session")
				.build();

		agent.call("What is 100 divided by 4?", config);
	}

	/**
	 * 示例16：组合使用多种工具提供方式
	 */
	public static void combinedToolProvisionExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Method tools
		CalculatorTools calculatorTools = new CalculatorTools();

		// Direct tool
		ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchToolWithContext())
				.description("Search for information")
				.inputType(String.class)
				.build();

		// ToolCallbackProvider
		ToolCallbackProvider toolProvider = new CustomToolCallbackProvider(List.of(searchTool));

		// 组合使用多种方式
		ReactAgent agent = ReactAgent.builder()
				.name("combined_tool_agent")
				.model(chatModel)
				.description("An agent with multiple tool provision methods")
				.instruction("You are a helpful assistant with calculator and search capabilities.")
				.methodTools(calculatorTools)  // Method-based tools
				.toolCallbackProviders(toolProvider)  // Provider-based tools
				.tools(searchTool)  // Direct tools
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("combined_session")
				.build();

		agent.call("Calculate 50 + 75 and search for information about mathematics", config);
	}

	// ==================== 高级模式定义 ====================

	/**
	 * 创建保存用户信息工具
	 */
	private static ToolCallback createSaveUserInfoTool() {
		return FunctionToolCallback.builder("save_user_info", (String input) -> {
					// 简化的实现
					return "User info saved: " + input;
				})
				.description("Save user information")
				.inputType(String.class)
				.build();
	}

	/**
	 * 创建获取用户信息工具
	 */
	private static ToolCallback createGetUserInfoTool() {
		return FunctionToolCallback.builder("get_user_info", (String userId) -> {
					// 简化的实现
					return "User info for: " + userId;
				})
				.description("Get user information by ID")
				.inputType(String.class)
				.build();
	}

	public static void main(String[] args) {
		System.out.println("=== Tools Tutorial Examples ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			System.out.println("\n--- 示例1：编程式工具规范 ---");
			programmaticToolSpecification();

			System.out.println("\n--- 示例2：添加工具到 ChatClient ---");
			addToolToChatClient();

			System.out.println("\n--- 示例3：自定义工具名称 ---");
			customToolName();

			System.out.println("\n--- 示例4：自定义工具描述 ---");
			customToolDescription();

			System.out.println("\n--- 示例5：高级 Schema 定义 ---");
			advancedSchemaDefinition();

			System.out.println("\n--- 示例6：访问状态 ---");
			accessingState();

			System.out.println("\n--- 示例7：访问上下文 ---");
			accessingContext();

			System.out.println("\n--- 示例8：访问内存存储 ---");
			accessingMemoryStore();

			System.out.println("\n--- 示例9：ReactAgent 中的工具 ---");
			toolsInReactAgent();

			System.out.println("\n--- 示例10：综合工具示例（tools 方法） ---");
			comprehensiveToolExample();

			System.out.println("\n--- 示例11：使用 methodTools（@Tool 注解） ---");
			methodToolsExample();

			System.out.println("\n--- 示例12：多个 methodTools 对象 ---");
			multipleMethodToolsExample();

			System.out.println("\n--- 示例13：使用 ToolCallbackProvider ---");
			toolCallbackProviderExample();

			System.out.println("\n--- 示例14：使用 toolNames 和 resolver ---");
			toolNamesWithResolverExample();

			System.out.println("\n--- 示例15：使用 resolver ---");
			resolverExample();

			System.out.println("\n--- 示例16：组合使用多种工具提供方式 ---");
			combinedToolProvisionExample();

			System.out.println("\n=== 所有示例执行完成 ===");
		}
		catch (Exception e) {
			System.err.println("执行示例时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public enum Unit {C, F}

	// ==================== 访问上下文 ====================

	public enum UnitType {CELSIUS, FAHRENHEIT}

	/**
	 * 天气服务
	 */
	public static class WeatherService implements Function<WeatherRequest, WeatherResponse> {
		@Override
		public WeatherResponse apply(WeatherRequest request) {
			return new WeatherResponse(30.0, Unit.C);
		}
	}

	// ==================== Context（上下文） ====================

	public record WeatherRequest(
			@ToolParam(description = "城市或坐标") String location,
			Unit unit
	) { }

	public record WeatherResponse(double temp, Unit unit) { }

	// ==================== Memory（存储） ====================

	/**
	 * 搜索函数
	 */
	public static class SearchFunction implements Function<String, String> {
		@Override
		public String apply(String query) {
			return "Search results for: " + query;
		}
	}

	// ==================== 在 ReactAgent 中使用工具 ====================

	/**
	 * 计算器函数
	 */
	public static class CalculatorFunction implements Function<String, String> {
		@Override
		public String apply(String expression) {
			// 简化的计算逻辑
			return "Result: " + expression;
		}
	}

	// ==================== 完整示例 ====================

	/**
	 * 天气输入（使用记录类）
	 */
	public record WeatherInput(
			@ToolParam(description = "City name or coordinates") String location,
			@ToolParam(description = "Temperature unit preference") Unit units,
			@ToolParam(description = "Include 5-day forecast") boolean includeForecast
	) { }

	// ==================== 辅助方法 ====================

	/**
	 * 天气函数（高级版）
	 */
	public static class WeatherFunction implements Function<WeatherInput, String> {
		@Override
		public String apply(WeatherInput input) {
			double temp = input.units() == Unit.F ? 22 : 72;
			String result = String.format(
					"Current weather in %s: %.0f degrees %s",
					input.location(),
					temp,
					input.units().toString().substring(0, 1).toUpperCase()
			);

			if (input.includeForecast()) {
				result += "\nNext 5 days: Sunny";
			}

			return result;
		}
	}

	/**
	 * 对话摘要工具
	 */
	public static class ConversationSummaryTool implements BiFunction<String, ToolContext, String> {

		@Override
		public String apply(String input, ToolContext toolContext) {
			OverAllState state = (OverAllState) toolContext.getContext().get("state");
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");

			// 从state中获取消息
			Optional<Object> messagesOpt = state.value("messages");
			List<Message> messages = messagesOpt.isPresent()
					? (List<Message>) messagesOpt.get()
					: new ArrayList<>();

			if (messages.isEmpty()) {
				return "No conversation history available";
			}

			long userMsgs = messages.stream()
					.filter(m -> m.getMessageType().getValue().equals("user"))
					.count();
			long aiMsgs = messages.stream()
					.filter(m -> m.getMessageType().getValue().equals("assistant"))
					.count();
			long toolMsgs = messages.stream()
					.filter(m -> m.getMessageType().getValue().equals("tool"))
					.count();

			return String.format(
					"Conversation has %d user messages, %d AI responses, and %d tool results",
					userMsgs, aiMsgs, toolMsgs
			);
		}
	}

	// ==================== Main 方法 ====================

	/**
	 * 账户信息工具
	 */
	public static class AccountInfoTool implements BiFunction<String, ToolContext, String> {

		private static final Map<String, Map<String, Object>> USER_DATABASE = Map.of(
				"user123", Map.of(
						"name", "Alice Johnson",
						"account_type", "Premium",
						"balance", 5000,
						"email", "alice@example.com"
				),
				"user456", Map.of(
						"name", "Bob Smith",
						"account_type", "Standard",
						"balance", 1200,
						"email", "bob@example.com"
				)
		);

		@Override
		public String apply(String query, ToolContext toolContext) {
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
			String userId = (String) config.metadata("user_id").orElse(null);

			if (userId == null) {
				return "User ID not provided";
			}

			Map<String, Object> user = USER_DATABASE.get(userId);
			if (user != null) {
				return String.format(
						"Account holder: %s\nType: %s\nBalance: $%d",
						user.get("name"),
						user.get("account_type"),
						user.get("balance")
				);
			}

			return "User not found";
		}
	}

	// ==================== MethodTools 相关类 ====================

	/**
	 * 计算器工具类 - 使用 @Tool 注解
	 */
	public static class CalculatorTools {
		public static int callCount = 0;

		@Tool(description = "Add two numbers together")
		public String add(
				@ToolParam(description = "First number") int a,
				@ToolParam(description = "Second number") int b) {
			callCount++;
			return String.valueOf(a + b);
		}

		@Tool(description = "Multiply two numbers together")
		public String multiply(
				@ToolParam(description = "First number") int a,
				@ToolParam(description = "Second number") int b) {
			callCount++;
			return String.valueOf(a * b);
		}

		@Tool(description = "Subtract second number from first number")
		public String subtract(
				@ToolParam(description = "First number") int a,
				@ToolParam(description = "Second number") int b) {
			callCount++;
			return String.valueOf(a - b);
		}
	}

	/**
	 * 天气工具类 - 使用 @Tool 注解
	 */
	public static class WeatherTools {
		@Tool(description = "Get current weather for a location")
		public String getWeather(@ToolParam(description = "City name") String city) {
			return "Sunny, 25°C in " + city;
		}

		@Tool(description = "Get weather forecast for a location")
		public String getForecast(
				@ToolParam(description = "City name") String city,
				@ToolParam(description = "Number of days") int days) {
			return String.format("Weather forecast for %s for next %d days: Mostly sunny", city, days);
		}
	}

	// ==================== ToolCallbackProvider 相关类 ====================

	/**
	 * 自定义 ToolCallbackProvider 实现
	 */
	public static class CustomToolCallbackProvider implements ToolCallbackProvider {
		private final List<ToolCallback> toolCallbacks;

		public CustomToolCallbackProvider(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
		}

		@Override
		public ToolCallback[] getToolCallbacks() {
			return toolCallbacks.toArray(new ToolCallback[0]);
		}
	}

	/**
	 * 带上下文的搜索工具
	 */
	public static class SearchToolWithContext implements BiFunction<String, ToolContext, String> {
		@Override
		public String apply(String query, ToolContext toolContext) {
			return "Search results for: " + query;
		}
	}

	// ==================== Resolver 相关类 ====================

	/**
	 * 搜索请求类（用于复合类型）
	 */
	public static class SearchRequest {
		@JsonProperty(required = true)
		@JsonPropertyDescription("The search query string")
		public String query;

		public SearchRequest() {
		}

		public SearchRequest(String query) {
			this.query = query;
		}
	}

	/**
	 * 使用复合类型的搜索函数
	 */
	public static class SearchFunctionWithRequest implements BiFunction<SearchRequest, ToolContext, String> {
		@Override
		public String apply(SearchRequest request, ToolContext toolContext) {
			return "Search results for: " + request.query;
		}
	}

	/**
	 * 计算器请求类（用于复合类型）
	 */
	public static class CalculatorRequest {
		@JsonProperty(required = true)
		@JsonPropertyDescription("First number for the calculation")
		public int a;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Second number for the calculation")
		public int b;

		public CalculatorRequest() {
		}

		public CalculatorRequest(int a, int b) {
			this.a = a;
			this.b = b;
		}
	}

	/**
	 * 使用复合类型的计算器函数
	 */
	public static class CalculatorFunctionWithRequest implements BiFunction<CalculatorRequest, ToolContext, String> {
		@Override
		public String apply(CalculatorRequest request, ToolContext toolContext) {
			return String.valueOf(request.a + request.b);
		}
	}

	/**
	 * 带上下文的计算器函数
	 */
	public static class CalculatorFunctionWithContext implements BiFunction<String, ToolContext, String> {
		@Override
		public String apply(String expression, ToolContext toolContext) {
			// 简单的计算解析（用于演示）
			if (expression.contains("/")) {
				String[] parts = expression.split("/");
				double result = Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
				return String.valueOf(result);
			}
			if (expression.contains("*")) {
				String[] parts = expression.split("\\*");
				double result = Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
				return String.valueOf(result);
			}
			return "Calculation result for: " + expression;
		}
	}
}

