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
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import reactor.core.publisher.Flux;

/**
 * Agents Tutorial - agents.md
 */
public class AgentsExample {

	// ==================== 基础模型配置 ====================

	/**
	 * 示例1：基础模型配置
	 */
	public static void basicModelConfiguration() {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建 Agent
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.build();
	}

	/**
	 * 示例2：高级模型配置
	 */
	public static void advancedModelConfiguration() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder()
						.withTemperature(0.7)      // 控制随机性
						.withMaxToken(2000)       // 最大输出长度
						.withTopP(0.9)            // 核采样参数
						.build())
				.build();
	}

	// ==================== 工具定义 ====================

	public static void toolUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具回调
		ToolCallback searchTool = FunctionToolCallback
				.builder("search", new SearchTool())
				.description("搜索信息的工具")
				.inputType(String.class)
				.build();

		// 使用多个工具
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(searchTool)
				.build();
	}

	/**
	 * 示例5：基础 System Prompt
	 */
	public static void basicSystemPrompt() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.systemPrompt("你是一个专业的技术助手。请准确、简洁地回答问题。")
				.build();
	}

	/**
	 * 示例6：使用 instruction
	 */
	public static void instructionUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		String instruction = """
				你是一个经验丰富的软件架构师。
				
				在回答问题时，请：
				1. 首先理解用户的核心需求
				2. 分析可能的技术方案
				3. 提供清晰的建议和理由
				4. 如果需要更多信息，主动询问
				
				保持专业、友好的语气。
				""";

		ReactAgent agent = ReactAgent.builder()
				.name("architect_agent")
				.model(chatModel)
				.instruction(instruction)
				.build();
	}

	// ==================== System Prompt ====================

	public static void dynamicSystemPrompt() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("adaptive_agent")
				.model(chatModel)
				.interceptors(new DynamicPromptInterceptor())
				.build();
	}

	/**
	 * 示例8：基础调用
	 */
	public static void basicInvocation() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.build();

		// 字符串输入
		AssistantMessage response = agent.call("杭州的天气怎么样？");
		System.out.println(response.getText());

		// UserMessage 输入
		UserMessage userMessage = new UserMessage("帮我分析这个问题");
		AssistantMessage response2 = agent.call(userMessage);

		// 多个消息
		List<Message> messages = List.of(
				new UserMessage("我想了解 Java 多线程"),
				new UserMessage("特别是线程池的使用")
		);
		AssistantMessage response3 = agent.call(messages);
	}

	/**
	 * 示例9：获取完整状态
	 */
	public static void getFullState() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.build();

		Optional<OverAllState> result = agent.invoke("帮我写一首诗");

		if (result.isPresent()) {
			OverAllState state = result.get();

			// 访问消息历史
			Optional<Object> messages = state.value("messages");
			List<Message> messageList = (List<Message>) messages.get();

			// 访问自定义状态
			Optional<Object> customData = state.value("custom_key");

			System.out.println("完整状态：" + state);
		}
	}

	/**
	 * 示例10：使用配置
	 */
	public static void useConfiguration() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.build();

		String threadId = "thread_123";
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId(threadId)
				.addMetadata("key", "value")
				.build();

		AssistantMessage response = agent.call("你的问题", runnableConfig);
	}

	// ==================== 调用 Agent ====================

	/**
	 * 示例10.1：流式调用 - 基础用法
	 */
	public static void basicStreamInvocation() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("streaming_agent")
				.model(chatModel)
				.build();

		// 流式输出
		Flux<NodeOutput> stream = agent.stream("帮我写一首关于春天的诗");

		stream.subscribe(
				output -> {
					// 处理每个节点输出
					System.out.println("节点: " + output.node());
					System.out.println("Agent: " + output.agent());
					if (output.tokenUsage() != null) {
						System.out.println("Token使用: " + output.tokenUsage());
					}
				},
				error -> System.err.println("错误: " + error.getMessage()),
				() -> System.out.println("流式输出完成")
		);
	}

	/**
	 * 示例10.2：流式调用 - 高级用法
	 */
	public static void advancedStreamInvocation() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("streaming_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("stream_thread_1")
				.build();

		// 使用配置的流式调用
		Flux<NodeOutput> stream = agent.stream(new UserMessage("解释一下量子计算"), config);

		// 使用 doOnNext 处理中间输出
		stream.doOnNext(output -> {
					if (!output.isSTART() && !output.isEND()) {
						System.out.println("处理中...");
						System.out.println("当前节点: " + output.node());
					}
				})
				.doOnComplete(() -> System.out.println("所有节点处理完成"))
				.doOnError(e -> System.err.println("流处理错误: " + e.getMessage()))
				.blockLast(); // 阻塞等待完成
	}

	/**
	 * 示例10.3：流式调用 - 收集所有输出
	 */
	public static void collectStreamOutputs() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("streaming_agent")
				.model(chatModel)
				.build();

		Flux<NodeOutput> stream = agent.stream("分析机器学习的应用场景");

		// 收集所有输出
		List<NodeOutput> outputs = stream.collectList().block();

		if (outputs != null) {
			System.out.println("总共收到 " + outputs.size() + " 个节点输出");

			// 获取最终输出
			NodeOutput lastOutput = outputs.get(outputs.size() - 1);
			System.out.println("最终状态: " + lastOutput.state());

			// 获取消息
			Optional<Object> messages = lastOutput.state().value("messages");
			if (messages.isPresent()) {
				List<Message> messageList = (List<Message>) messages.get();
				Message lastMessage = messageList.get(messageList.size() - 1);
				if (lastMessage instanceof AssistantMessage assistantMsg) {
					System.out.println("最终回复: " + assistantMsg.getText());
				}
			}
		}
	}

	public static void structuredOutputWithType() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("poem_agent")
				.model(chatModel)
				.outputType(PoemOutput.class)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("写一首关于春天的诗");
		// 输出会遵循 PoemOutput 的结构
		System.out.println(response.getText());
	}

	/**
	 * 示例12：使用 outputSchema
	 */
	public static void structuredOutputWithSchema() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		String customSchema = """
				请严格按照以下JSON格式返回结果：
				{
					"summary": "内容摘要",
					"keywords": ["关键词1", "关键词2", "关键词3"],
					"sentiment": "情感倾向（正面/负面/中性）",
					"confidence": 0.95
				}
				""";

		ReactAgent agent = ReactAgent.builder()
				.name("analysis_agent")
				.model(chatModel)
				.outputSchema(customSchema)
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("分析这段文本：春天来了，万物复苏。");
	}

	/**
	 * 示例13：配置记忆
	 */
	public static void configureMemory() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 配置内存存储
		ReactAgent agent = ReactAgent.builder()
				.name("chat_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.build();

		// 使用 thread_id 维护对话上下文
		RunnableConfig config = RunnableConfig.builder()
				.threadId("user_123")
				.build();

		agent.call("我叫张三", config);
		agent.call("我叫什么名字？", config);  // 输出: "你叫张三"
	}

	// ==================== 结构化输出 ====================

	public static void main(String[] args) {
		System.out.println("=== Agents Tutorial Examples ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			System.out.println("\n--- 示例1：基础模型配置 ---");
			basicModelConfiguration();

			System.out.println("\n--- 示例2：高级模型配置 ---");
			advancedModelConfiguration();

			System.out.println("\n--- 示例3：工具使用 ---");
			toolUsage();

			System.out.println("\n--- 示例5：基础 System Prompt ---");
			basicSystemPrompt();

			System.out.println("\n--- 示例6：使用 instruction ---");
			instructionUsage();

			System.out.println("\n--- 示例7：动态 System Prompt ---");
			dynamicSystemPrompt();

			System.out.println("\n--- 示例8：基础调用 ---");
			basicInvocation();

			System.out.println("\n--- 示例9：获取完整状态 ---");
			getFullState();

			System.out.println("\n--- 示例10：使用配置 ---");
			useConfiguration();

			System.out.println("\n--- 示例10.1：流式调用 - 基础用法 ---");
			basicStreamInvocation();

			System.out.println("\n--- 示例10.2：流式调用 - 高级用法 ---");
			advancedStreamInvocation();

			System.out.println("\n--- 示例10.3：流式调用 - 收集所有输出 ---");
			collectStreamOutputs();

			System.out.println("\n--- 示例11：使用 outputType ---");
			structuredOutputWithType();

			System.out.println("\n--- 示例12：使用 outputSchema ---");
			structuredOutputWithSchema();

			System.out.println("\n--- 示例13：配置记忆 ---");
			configureMemory();

			System.out.println("\n=== 所有示例执行完成 ===");
		}
		catch (GraphRunnerException e) {
			System.err.println("执行示例时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println("发生未预期的错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 示例3：定义和使用工具
	 */
	public static class SearchTool implements BiFunction<String, ToolContext, String> {
		@Override
		public String apply(
				@ToolParam(description = "搜索关键词") String query,
				ToolContext toolContext) {
			return "搜索结果：" + query;
		}
	}

	/**
	 * 示例4：工具错误处理
	 */
	public static class ToolErrorInterceptor extends ToolInterceptor {
		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			try {
				return handler.call(request);
			}
			catch (Exception e) {
				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
						"Tool failed: " + e.getMessage());
			}
		}

		@Override
		public String getName() {
			return "ToolErrorInterceptor";
		}
	}

	// ==================== Memory ====================

	/**
	 * 示例7：动态 System Prompt
	 */
	public static class DynamicPromptInterceptor extends ModelInterceptor {
		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 基于上下文动态调整 system prompt
			Map<String, Object> context = request.getContext();

			// 根据上下文构建动态提示词
			String dynamicPrompt = buildDynamicPrompt(context);

			// 增强 system message
			SystemMessage enhancedSystemMessage;
			if (request.getSystemMessage() == null) {
				enhancedSystemMessage = new SystemMessage(dynamicPrompt);
			}
			else {
				enhancedSystemMessage = new SystemMessage(
						request.getSystemMessage().getText() + "\n\n" + dynamicPrompt
				);
			}

			// 创建增强的请求
			ModelRequest modifiedRequest = ModelRequest.builder(request)
					.systemMessage(enhancedSystemMessage)
					.build();

			return handler.call(modifiedRequest);
		}

		private String buildDynamicPrompt(Map<String, Object> context) {
			// 示例：根据用户角色动态生成提示词
			String userRole = (String) context.getOrDefault("user_role", "default");

			return switch (userRole) {
				case "expert" -> """
						你正在与技术专家对话。
						- 使用专业术语
						- 深入技术细节
						- 提供高级建议
						""";
				case "beginner" -> """
						你正在与初学者对话。
						- 使用简单易懂的语言
						- 详细解释概念
						- 提供入门级建议
						""";
				default -> """
						你是一个专业的助手。
						- 根据问题复杂度调整回答
						- 保持友好和专业
						""";
			};
		}

		@Override
		public String getName() {
			return "DynamicPromptInterceptor";
		}
	}

	// ==================== Hooks ====================

	/**
	 * 示例11：使用 outputType
	 */
	public static class PoemOutput {
		private String title;
		private String content;
		private String style;

		// Getters and Setters
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getStyle() {
			return style;
		}

		public void setStyle(String style) {
			this.style = style;
		}
	}

	/**
	 * 示例14：AgentHook - 在 Agent 开始/结束时执行
	 */
	public static class LoggingHook extends AgentHook {
		@Override
		public String getName() {
			return "logging";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {
					HookPosition.BEFORE_AGENT,
					HookPosition.AFTER_AGENT
			};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 开始执行");
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 执行完成");
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	// ==================== Interceptors ====================

	/**
	 * 示例15：ModelHook - 在模型调用前后执行
	 */
	public static class MessageTrimmingHook extends ModelHook {
		private static final int MAX_MESSAGES = 10;

		@Override
		public String getName() {
			return "message_trimming";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {HookPosition.BEFORE_MODEL};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			Optional<Object> messagesOpt = state.value("messages");
			if (messagesOpt.isPresent()) {
				List<Message> messages = (List<Message>) messagesOpt.get();
				if (messages.size() > MAX_MESSAGES) {
					return CompletableFuture.completedFuture(Map.of("messages",
							messages.subList(messages.size() - MAX_MESSAGES, messages.size())));
				}
			}
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	/**
	 * 示例16：ModelInterceptor - 内容安全检查
	 */
	public static class GuardrailInterceptor extends ModelInterceptor {
		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 前置：检查输入
			if (containsSensitiveContent(request.getMessages())) {
				return ModelResponse.of(new AssistantMessage("检测到不适当的内容"));
			}

			// 执行调用
			ModelResponse response = handler.call(request);

			// 后置：检查输出
			return sanitizeIfNeeded(response);
		}

		private boolean containsSensitiveContent(List<Message> messages) {
			// 实现敏感内容检测逻辑
			return false;
		}

		private ModelResponse sanitizeIfNeeded(ModelResponse response) {
			// 实现响应清理逻辑
			return response;
		}

		@Override
		public String getName() {
			return "GuardrailInterceptor";
		}
	}

	// ==================== Main 方法 ====================

	/**
	 * 示例17：ToolInterceptor - 监控和错误处理
	 */
	public static class ToolMonitoringInterceptor extends ToolInterceptor {
		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			long startTime = System.currentTimeMillis();
			try {
				ToolCallResponse response = handler.call(request);
				logSuccess(request, System.currentTimeMillis() - startTime);
				return response;
			}
			catch (Exception e) {
				logError(request, e, System.currentTimeMillis() - startTime);
				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
						"工具执行遇到问题，请稍后重试");
			}
		}

		private void logSuccess(ToolCallRequest request, long duration) {
			System.out.println("Tool " + request.getToolName() + " succeeded in " + duration + "ms");
		}

		private void logError(ToolCallRequest request, Exception e, long duration) {
			System.err.println("Tool " + request.getToolName() + " failed in " + duration + "ms: " + e.getMessage());
		}

		@Override
		public String getName() {
			return "ToolMonitoringInterceptor";
		}
	}
}

