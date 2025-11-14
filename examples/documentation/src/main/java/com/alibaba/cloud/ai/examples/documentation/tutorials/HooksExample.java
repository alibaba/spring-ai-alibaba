package com.alibaba.cloud.ai.graph.agent.documentation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.*;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.*;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Hooks 和 Interceptors Tutorial - 完整代码示例
 * 展示如何使用Hooks和Interceptors精细控制Agent执行
 *
 * 来源：hooks.md
 */
public class HooksExample {

	// ==================== 基础 Hook 和 Interceptor 配置 ====================

	/**
	 * 示例1：添加 Hooks 和 Interceptors
	 */
	public static void basicHooksAndInterceptors() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建工具（示例）
		ToolCallback[] tools = new ToolCallback[0];

		// 创建 Hooks 和 Interceptors
		ModelHook loggingHook = new LoggingModelHook();
		ModelHook messageTrimmingHook = new MessageTrimmingHook();
		ModelInterceptor guardrailInterceptor = new GuardrailInterceptor();
		ToolInterceptor retryInterceptor = new RetryToolInterceptor();

		ReactAgent agent = ReactAgent.builder()
			.name("my_agent")
			.model(chatModel)
			.tools(tools)
			.hooks(loggingHook, messageTrimmingHook)
			.modelInterceptors(guardrailInterceptor)
			.toolInterceptors(retryInterceptor)
			.build();
	}

	// ==================== 消息压缩（Summarization） ====================

	/**
	 * 示例2：消息压缩 Hook
	 */
	public static void messageSummarization() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建消息压缩 Hook
		SummarizationHook summarizationHook = SummarizationHook.builder()
			.model(chatModel)
			.maxTokensBeforeSummary(4000)
			.messagesToKeep(20)
			.build();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("my_agent")
			.model(chatModel)
			.hooks(summarizationHook)
			.build();
	}

	// ==================== Human-in-the-Loop ====================

	/**
	 * 示例3：Human-in-the-Loop Hook
	 */
	public static void humanInTheLoop(RedisConnectionFactory redisConnectionFactory) {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建工具（示例）
		ToolCallback sendEmailTool = createSendEmailTool();
		ToolCallback deleteDataTool = createDeleteDataTool();

		// 创建 Human-in-the-Loop Hook
		HumanInTheLoopHook humanReviewHook = HumanInTheLoopHook.builder()
			.approvalOn("sendEmailTool", ToolConfig.builder()
				.description("Please confirm sending the email.")
				.build())
			.approvalOn("deleteDataTool")
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("supervised_agent")
			.model(chatModel)
			.tools(sendEmailTool, deleteDataTool)
			.hooks(humanReviewHook)
			.saver(new RedisSaver(redisConnectionFactory))
			.build();
	}

	// ==================== 模型调用限制 ====================

	/**
	 * 示例4：模型调用限制
	 */
	public static void modelCallLimit() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("my_agent")
			.model(chatModel)
			.maxIterations(10)  // 最多 10 次迭代（默认为 10）
			.saver(new MemorySaver())
			.build();
	}

	/**
	 * 示例5：自定义停止条件
	 */
	public static void customStopCondition() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		Function<OverAllState, Boolean> customStopCondition = state -> {
			// 如果找到答案或错误过多则停止
			Optional<Object> foundAnswer = state.value("answer_found");
			if (foundAnswer.isPresent() && (Boolean) foundAnswer.get()) {
				return false;  // 停止执行
			}

			Optional<Object> errorCount = state.value("error_count");
			if (errorCount.isPresent() && (Integer) errorCount.get() > 3) {
				return false;  // 停止执行
			}

			return true;  // 继续执行
		};

		ReactAgent agent = ReactAgent.builder()
			.name("controlled_agent")
			.model(chatModel)
			.shouldContinueFunction(customStopCondition)
			.saver(new MemorySaver())
			.build();
	}

	// ==================== PII 检测 ====================

	/**
	 * 示例6：PII 检测
	 */
	public static void piiDetection() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("secure_agent")
			.model(chatModel)
			.modelInterceptors(new PIIDetectionInterceptor())
			.build();
	}

	// ==================== 工具重试 ====================

	/**
	 * 示例7：工具重试
	 */
	public static void toolRetry() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建工具（示例）
		ToolCallback searchTool = createSearchTool();
		ToolCallback databaseTool = createDatabaseTool();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("resilient_agent")
			.model(chatModel)
			.tools(searchTool, databaseTool)
			.toolInterceptors(new ToolRetryInterceptor(3, 1000, 2.0))
			.build();
	}

	// ==================== Planning ====================

	/**
	 * 示例8：Planning Hook
	 */
	public static void planning() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ToolCallback myTool = createSampleTool();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("planning_agent")
			.model(chatModel)
			.tools(myTool)
			.hooks(new PlanningHook())
			.build();
	}

	// ==================== LLM Tool Selector ====================

	/**
	 * 示例9：LLM 工具选择器
	 */
	public static void llmToolSelector() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ChatModel selectorModel = chatModel; // 用于选择的另一个ChatModel

		ToolCallback tool1 = createSampleTool();
		ToolCallback tool2 = createSampleTool();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("smart_selector_agent")
			.model(chatModel)
			.tools(tool1, tool2)
			.toolInterceptors(new LLMToolSelectorInterceptor(selectorModel))
			.build();
	}

	// ==================== LLM Tool Emulator ====================

	/**
	 * 示例10：LLM 工具模拟器
	 */
	public static void llmToolEmulator() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ToolCallback simulatedTool = createSampleTool();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("emulator_agent")
			.model(chatModel)
			.tools(simulatedTool)
			.toolInterceptors(new ToolEmulatorInterceptor(chatModel))
			.build();
	}

	// ==================== Context Editing ====================

	/**
	 * 示例11：上下文编辑
	 */
	public static void contextEditing() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 使用
		ReactAgent agent = ReactAgent.builder()
			.name("context_aware_agent")
			.model(chatModel)
			.hooks(new ContextEditingHook("Remember to be polite and helpful."))
			.build();
	}

	// ==================== 自定义 Hooks ====================

	/**
	 * 示例12：自定义 ModelHook
	 */
	public static class CustomModelHook implements ModelHook {

		@Override
		public String getName() {
			return "custom_model_hook";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[]{
				HookPosition.BEFORE_MODEL,
				HookPosition.AFTER_MODEL
			};
		}

		@Override
		public Map<String, Object> beforeModel(OverAllState state, RunnableConfig config) {
			// 在模型调用前执行
			System.out.println("准备调用模型...");

			// 可以修改状态
			// 例如：添加额外的上下文
			return Map.of("extra_context", "某些额外信息");
		}

		@Override
		public Map<String, Object> afterModel(OverAllState state, RunnableConfig config) {
			// 在模型调用后执行
			System.out.println("模型调用完成");

			// 可以记录响应信息
			return Map.of();
		}
	}

	/**
	 * 示例13：自定义 AgentHook
	 */
	public static class CustomAgentHook implements AgentHook {

		@Override
		public String getName() {
			return "custom_agent_hook";
		}

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
			// 可以初始化资源、记录开始时间等
			return Map.of("start_time", System.currentTimeMillis());
		}

		@Override
		public Map<String, Object> afterAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 执行完成");
			// 可以清理资源、计算执行时间等
			Optional<Object> startTime = state.value("start_time");
			if (startTime.isPresent()) {
				long duration = System.currentTimeMillis() - (Long) startTime.get();
				System.out.println("执行耗时: " + duration + "ms");
			}
			return Map.of();
		}
	}

	// ==================== 自定义 Interceptors ====================

	/**
	 * 示例14：自定义 ModelInterceptor
	 */
	public static class LoggingInterceptor implements ModelInterceptor {

		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 请求前记录
			System.out.println("发送请求到模型: " + request.getMessages().size() + " 条消息");

			long startTime = System.currentTimeMillis();

			// 执行实际调用
			ModelResponse response = handler.call(request);

			// 响应后记录
			long duration = System.currentTimeMillis() - startTime;
			System.out.println("模型响应耗时: " + duration + "ms");

			return response;
		}
	}

	/**
	 * 示例15：自定义 ToolInterceptor
	 */
	public static class ToolMonitoringInterceptor implements ToolInterceptor {

		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			String toolName = request.getToolName();
			long startTime = System.currentTimeMillis();

			System.out.println("执行工具: " + toolName);

			try {
				ToolCallResponse response = handler.call(request);

				long duration = System.currentTimeMillis() - startTime;
				System.out.println("工具 " + toolName + " 执行成功 (耗时: " + duration + "ms)");

				return response;
			} catch (Exception e) {
				long duration = System.currentTimeMillis() - startTime;
				System.err.println("工具 " + toolName + " 执行失败 (耗时: " + duration + "ms): " + e.getMessage());

				return ToolCallResponse.error(
					request.getToolCall(),
					"工具执行失败: " + e.getMessage()
				);
			}
		}
	}

	// ==================== 辅助类和方法 ====================

	/**
	 * 日志记录 ModelHook
	 */
	private static class LoggingModelHook implements ModelHook {
		@Override
		public String getName() {
			return "logging_model_hook";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
		}

		@Override
		public Map<String, Object> beforeModel(OverAllState state, RunnableConfig config) {
			System.out.println("Before model call");
			return Map.of();
		}

		@Override
		public Map<String, Object> afterModel(OverAllState state, RunnableConfig config) {
			System.out.println("After model call");
			return Map.of();
		}
	}

	/**
	 * 消息修剪 Hook
	 */
	private static class MessageTrimmingHook implements ModelHook {
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
				if (messages.size() > 10) {
					return Map.of("messages", messages.subList(messages.size() - 10, messages.size()));
				}
			}
			return Map.of();
		}

		@Override
		public Map<String, Object> afterModel(OverAllState state, RunnableConfig config) {
			return Map.of();
		}
	}

	/**
	 * 护栏拦截器
	 */
	private static class GuardrailInterceptor implements ModelInterceptor {
		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 简化的实现
			return handler.call(request);
		}
	}

	/**
	 * 重试工具拦截器
	 */
	private static class RetryToolInterceptor implements ToolInterceptor {
		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			// 简化的实现
			return handler.call(request);
		}
	}

	// 创建示例工具的辅助方法
	private static ToolCallback createSendEmailTool() {
		return FunctionToolCallback.builder("sendEmailTool", (String input) -> "Email sent")
			.description("Send an email")
			.inputType(String.class)
			.build();
	}

	private static ToolCallback createDeleteDataTool() {
		return FunctionToolCallback.builder("deleteDataTool", (String input) -> "Data deleted")
			.description("Delete data")
			.inputType(String.class)
			.build();
	}

	private static ToolCallback createSearchTool() {
		return FunctionToolCallback.builder("searchTool", (String input) -> "Search results")
			.description("Search the web")
			.inputType(String.class)
			.build();
	}

	private static ToolCallback createDatabaseTool() {
		return FunctionToolCallback.builder("databaseTool", (String input) -> "Database query results")
			.description("Query database")
			.inputType(String.class)
			.build();
	}

	private static ToolCallback createSampleTool() {
		return FunctionToolCallback.builder("sampleTool", (String input) -> "Sample result")
			.description("A sample tool")
			.inputType(String.class)
			.build();
	}

	// ==================== Main 方法 ====================

	public static void main(String[] args) {
		System.out.println("=== Hooks and Interceptors Tutorial Examples ===");

		// 运行示例（需要设置 AI_DASHSCOPE_API_KEY 环境变量）
		// basicHooksAndInterceptors();
		// messageSummarization();
		// modelCallLimit();
		// customStopCondition();
		// piiDetection();
		// toolRetry();
	}
}

