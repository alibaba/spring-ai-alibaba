package com.alibaba.cloud.ai.graph.agent.documentation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Agents Tutorial - 完整代码示例
 * 展示如何使用ReactAgent创建智能代理系统
 *
 * 来源：agents.md
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
				.temperature(0.7)      // 控制随机性
				.maxTokens(2000)       // 最大输出长度
				.topP(0.9)            // 核采样参数
				.build())
			.build();
	}

	// ==================== 工具定义 ====================

	/**
	 * 示例3：定义和使用工具
	 */
	public static class SearchTool implements BiFunction<String, Object, String> {
		@Override
		public String apply(
			@ToolParam(description = "搜索关键词") String query,
			Object toolContext) {
			return "搜索结果：" + query;
		}
	}

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
	 * 示例4：工具错误处理
	 */
	public static class ToolErrorInterceptor implements ToolInterceptor {
		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			try {
				return handler.call(request);
			} catch (Exception e) {
				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
						"Tool failed: " + e.getMessage());
			}
		}
	}

	// ==================== System Prompt ====================

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

	/**
	 * 示例7：动态 System Prompt
	 */
	public static class DynamicPromptInterceptor implements ModelInterceptor {
		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 基于用户角色动态调整提示
			List<Message> messages = request.getMessages();
			Map<String, Object> context = request.getContext();

			// do anything with messages to adjust prompt, history messages, user request dynamically

			// create modified request
			ModelRequest modifiedRequest = ModelRequest.builder()
					.messages(messages)
					.options(request.getOptions())
					.tools(request.getTools())
					.build();
			return handler.call(modifiedRequest);
		}
	}

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
			.modelInterceptors(new DynamicPromptInterceptor())
			.build();
	}

	// ==================== 调用 Agent ====================

	/**
	 * 示例8：基础调用
	 */
	public static void basicInvocation() {
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
	public static void getFullState() {
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
	public static void useConfiguration() {
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

	// ==================== 结构化输出 ====================

	/**
	 * 示例11：使用 outputType
	 */
	public static class PoemOutput {
		private String title;
		private String content;
		private String style;

		// Getters and Setters
		public String getTitle() { return title; }
		public void setTitle(String title) { this.title = title; }

		public String getContent() { return content; }
		public void setContent(String content) { this.content = content; }

		public String getStyle() { return style; }
		public void setStyle(String style) { this.style = style; }
	}

	public static void structuredOutputWithType() {
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
	public static void structuredOutputWithSchema() {
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

	// ==================== Memory ====================

	/**
	 * 示例13：配置记忆
	 */
	public static void configureMemory() {
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

	// ==================== Hooks ====================

	/**
	 * 示例14：AgentHook - 在 Agent 开始/结束时执行
	 */
	public static class LoggingHook implements com.alibaba.cloud.ai.graph.agent.hook.AgentHook {
		@Override
		public String getName() { return "logging"; }

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[]{
				HookPosition.BEFORE_AGENT,
				HookPosition.AFTER_AGENT
			};
		}

		@Override
		public Map<String, Object> beforeAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 开始执行");
			return Map.of();
		}

		@Override
		public Map<String, Object> afterAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 执行完成");
			return Map.of();
		}
	}

	/**
	 * 示例15：ModelHook - 在模型调用前后执行
	 */
	public static class MessageTrimmingHook implements ModelHook {
		private static final int MAX_MESSAGES = 10;

		@Override
		public String getName() {
			return "message_trimming";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[]{HookPosition.BEFORE_MODEL};
		}

		@Override
		public Map<String, Object> beforeModel(OverAllState state, RunnableConfig config) {
			Optional<Object> messagesOpt = state.value("messages");
			if (messagesOpt.isPresent()) {
				List<Message> messages = (List<Message>) messagesOpt.get();
				if (messages.size() > MAX_MESSAGES) {
					return Map.of("messages",
						messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
				}
			}
			return Map.of();
		}

		@Override
		public Map<String, Object> afterModel(OverAllState state, RunnableConfig config) {
			return Map.of();
		}
	}

	// ==================== Interceptors ====================

	/**
	 * 示例16：ModelInterceptor - 内容安全检查
	 */
	public static class GuardrailInterceptor implements ModelInterceptor {
		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 前置：检查输入
			if (containsSensitiveContent(request.getMessages())) {
				return ModelResponse.blocked("检测到不适当的内容");
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
	}

	/**
	 * 示例17：ToolInterceptor - 监控和错误处理
	 */
	public static class ToolMonitoringInterceptor implements ToolInterceptor {
		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			long startTime = System.currentTimeMillis();
			try {
				ToolCallResponse response = handler.call(request);
				logSuccess(request, System.currentTimeMillis() - startTime);
				return response;
			} catch (Exception e) {
				logError(request, e, System.currentTimeMillis() - startTime);
				return ToolCallResponse.error(request.getToolCall(),
					"工具执行遇到问题，请稍后重试");
			}
		}

		private void logSuccess(ToolCallRequest request, long duration) {
			System.out.println("Tool " + request.getToolName() + " succeeded in " + duration + "ms");
		}

		private void logError(ToolCallRequest request, Exception e, long duration) {
			System.err.println("Tool " + request.getToolName() + " failed in " + duration + "ms: " + e.getMessage());
		}
	}

	// ==================== Main 方法 ====================

	public static void main(String[] args) {
		System.out.println("=== Agents Tutorial Examples ===");

		// 运行示例（需要设置 AI_DASHSCOPE_API_KEY 环境变量）
		// basicModelConfiguration();
		// advancedModelConfiguration();
		// toolUsage();
		// basicSystemPrompt();
		// instructionUsage();
		// basicInvocation();
		// structuredOutputWithType();
		// configureMemory();
	}
}

