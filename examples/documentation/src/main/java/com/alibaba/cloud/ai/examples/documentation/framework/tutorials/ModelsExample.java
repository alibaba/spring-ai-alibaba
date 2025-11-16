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
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * Models Tutorial - 完整代码示例
 * 展示如何使用Chat Model API与各种AI模型交互
 *
 * 来源：models.md
 */
public class ModelsExample {

	// ==================== DashScopeChatModel ====================

	/**
	 * 示例1：创建 ChatModel
	 */
	public static void createChatModel() {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();
	}

	/**
	 * 示例2：简单调用
	 */
	public static void simpleCall() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 使用字符串直接调用
		String response = chatModel.call("介绍一下Spring框架");
		System.out.println(response);
	}

	/**
	 * 示例3：使用 Prompt
	 */
	public static void usePrompt() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建 Prompt
		Prompt prompt = new Prompt(new UserMessage("解释什么是微服务架构"));

		// 调用并获取响应
		ChatResponse response = chatModel.call(prompt);
		String answer = response.getResult().getOutput().getText();
		System.out.println(answer);
	}

	// ==================== 配置选项 ====================

	/**
	 * 示例4：使用 ChatOptions
	 */
	public static void useChatOptions() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		DashScopeChatOptions options = DashScopeChatOptions.builder()
				.withModel("qwen-plus")           // 模型名称
				.withTemperature(0.7)              // 温度参数
				.withMaxToken(2000)               // 最大令牌数
				.withTopP(0.9)                     // Top-P 采样
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(options)
				.build();
	}

	/**
	 * 示例5：运行时覆盖选项
	 */
	public static void runtimeOptionsOverride() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建带有特定选项的 Prompt
		DashScopeChatOptions runtimeOptions = DashScopeChatOptions.builder()
				.withTemperature(0.3)  // 更低的温度，更确定的输出
				.withMaxToken(500)
				.build();

		Prompt prompt = new Prompt(
				new UserMessage("用一句话总结Java的特点"),
				runtimeOptions
		);

		ChatResponse response = chatModel.call(prompt);
	}

	// ==================== 流式响应 ====================

	/**
	 * 示例6：流式响应
	 */
	public static void streamingResponse() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 使用流式 API
		Flux<ChatResponse> responseStream = chatModel.stream(
				new Prompt("详细解释Spring Boot的自动配置原理")
		);

		// 订阅并处理流式响应
		responseStream.subscribe(
				chatResponse -> {
					String content = chatResponse.getResult()
							.getOutput()
							.getText();
					System.out.print(content);
				},
				error -> System.err.println("错误: " + error.getMessage()),
				() -> System.out.println("\n流式响应完成")
		);
	}

	// ==================== 多轮对话 ====================

	/**
	 * 示例7：多轮对话
	 */
	public static void multiTurnConversation() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建对话历史
		List<Message> messages = List.of(
				new SystemMessage("你是一个Java专家"),
				new UserMessage("什么是Spring Boot?"),
				new AssistantMessage("Spring Boot是..."),
				new UserMessage("它有什么优势?")
		);

		Prompt prompt = new Prompt(messages);
		ChatResponse response = chatModel.call(prompt);
	}

	// ==================== 函数调用 ====================

	/**
	 * 示例8：函数调用
	 */
	public static void functionCalling() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 定义函数
		ToolCallback weatherFunction = FunctionToolCallback.builder("getWeather", (city) -> {
					// 实际的天气查询逻辑
					return "晴朗，25°C";
				})
				.description("获取指定城市的天气")
				.inputType(String.class)
				.build();

		// 使用函数
		DashScopeChatOptions options = DashScopeChatOptions.builder()
				.withToolCallbacks(List.of(weatherFunction))
				.build();

		Prompt prompt = new Prompt("北京的天气怎么样?", options);
		ChatResponse response = chatModel.call(prompt);
	}

	// ==================== 与 ReactAgent 集成 ====================

	/**
	 * 示例9：与 ReactAgent 集成
	 */
	public static void integrationWithReactAgent() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.systemPrompt("你是一个有帮助的AI助手")
				.build();

		// 调用 Agent
		AssistantMessage response = agent.call("帮我分析这个问题");
	}

	// ==================== 高级配置示例 ====================

	/**
	 * 示例10：完整的配置示例
	 */
	public static void comprehensiveConfiguration() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 配置各种选项
		DashScopeChatOptions options = DashScopeChatOptions.builder()
				.withModel("qwen-max")              // 使用旗舰版模型
				.withTemperature(0.7)               // 控制随机性
				.withMaxToken(4000)                // 最大输出长度
				.withTopP(0.9)                      // 核采样
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(options)
				.build();

		// 创建复杂的对话
		List<Message> messages = List.of(
				new SystemMessage("你是一位资深的软件架构师，精通微服务和云原生技术。"),
				new UserMessage("如何设计一个高可用的微服务系统？")
		);

		Prompt prompt = new Prompt(messages);
		ChatResponse response = chatModel.call(prompt);

		System.out.println("Response: " + response.getResult().getOutput().getText());
	}

	/**
	 * 示例11：不同模型的使用
	 */
	public static void differentModelsUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// qwen-turbo: 通义千问超大规模语言模型
		ChatModel turboModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withModel("qwen-turbo")
						.build())
				.build();

		// qwen-plus: 通义千问增强版
		ChatModel plusModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withModel("qwen-plus")
						.build())
				.build();

		// qwen-max: 通义千问旗舰版
		ChatModel maxModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withModel("qwen-max")
						.build())
				.build();

		// 使用不同的模型
		String question = "什么是人工智能？";
		String turboResponse = turboModel.call(question);
		String plusResponse = plusModel.call(question);
		String maxResponse = maxModel.call(question);
	}

	/**
	 * 示例12：错误处理
	 */
	public static void errorHandling() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		try {
			ChatResponse response = chatModel.call(new Prompt("你好"));
			System.out.println("Response: " + response.getResult().getOutput().getText());
		}
		catch (Exception e) {
			System.err.println("Error calling model: " + e.getMessage());
			// 处理错误，例如重试、降级等
		}
	}

	/**
	 * 示例13：温度参数的影响
	 */
	public static void temperatureEffect() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		String question = "给我讲一个有趣的故事";

		// 低温度 - 更确定、更保守的输出
		ChatModel conservativeModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withTemperature(0.1)
						.build())
				.build();

		// 中温度 - 平衡的输出
		ChatModel balancedModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withTemperature(0.7)
						.build())
				.build();

		// 高温度 - 更有创意、更随机的输出
		ChatModel creativeModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withTemperature(1.5)
						.build())
				.build();

		String conservativeResponse = conservativeModel.call(question);
		String balancedResponse = balancedModel.call(question);
		String creativeResponse = creativeModel.call(question);

		System.out.println("Conservative (temp=0.1): " + conservativeResponse);
		System.out.println("Balanced (temp=0.7): " + balancedResponse);
		System.out.println("Creative (temp=1.5): " + creativeResponse);
	}

	// ==================== Main 方法 ====================

	public static void main(String[] args) {
		System.out.println("=== Models Tutorial Examples ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			System.out.println("\n--- 示例1：创建 ChatModel ---");
			createChatModel();

			System.out.println("\n--- 示例2：简单调用 ---");
			simpleCall();

			System.out.println("\n--- 示例3：使用 Prompt ---");
			usePrompt();

			System.out.println("\n--- 示例4：使用 ChatOptions ---");
			useChatOptions();

			System.out.println("\n--- 示例5：运行时选项覆盖 ---");
			runtimeOptionsOverride();

			System.out.println("\n--- 示例6：流式响应 ---");
			streamingResponse();

			System.out.println("\n--- 示例7：多轮对话 ---");
			multiTurnConversation();

			System.out.println("\n--- 示例8：函数调用 ---");
			functionCalling();

			System.out.println("\n--- 示例9：与 ReactAgent 集成 ---");
			integrationWithReactAgent();

			System.out.println("\n--- 示例10：综合配置 ---");
			comprehensiveConfiguration();

			System.out.println("\n--- 示例11：不同模型使用 ---");
			differentModelsUsage();

			System.out.println("\n--- 示例12：错误处理 ---");
			errorHandling();

			System.out.println("\n--- 示例13：温度效果 ---");
			temperatureEffect();

			System.out.println("\n=== 所有示例执行完成 ===");
		}
		catch (Exception e) {
			System.err.println("执行示例时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
