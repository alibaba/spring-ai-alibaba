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
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Hooks & Interceptors Tutorial - hooks.md
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
				.interceptors(guardrailInterceptor)
				.interceptors(retryInterceptor)
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
	public static void humanInTheLoop() {
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
				.approvalOn("deleteDataTool", ToolConfig.builder()
						.description("Please confirm deleting the data.")
						.build())
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("supervised_agent")
				.model(chatModel)
				.tools(sendEmailTool, deleteDataTool)
				.hooks(humanReviewHook)
				.saver(new MemorySaver())
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
				.hooks(ModelCallLimitHook.builder().runLimit(5).build())  // 限制模型调用次数为5次
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

		PIIDetectionHook pii = PIIDetectionHook.builder()
				.piiType(PIIType.EMAIL)
				.strategy(RedactionStrategy.REDACT)
				.applyToInput(true)
				.build();

		// 使用
		ReactAgent agent = ReactAgent.builder()
				.name("secure_agent")
				.model(chatModel)
				.hooks(pii)
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
				.interceptors(ToolRetryInterceptor.builder().maxRetries(2)
						.onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE).build())
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
				.interceptors(TodoListInterceptor.builder().build())
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
				.interceptors(ToolSelectionInterceptor.builder().build())
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
				.interceptors(ToolEmulatorInterceptor.builder().model(chatModel).build())
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
				.interceptors(ContextEditingInterceptor.builder().trigger(120000).clearAtLeast(60000).build())
				.build();
	}

	// ==================== 自定义 Hooks ====================

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

	// ==================== 自定义 Interceptors ====================

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

	// ==================== 辅助类和方法 ====================

	private static ToolCallback createSampleTool() {
		return FunctionToolCallback.builder("sampleTool", (String input) -> "Sample result")
				.description("A sample tool")
				.inputType(String.class)
				.build();
	}

	public static void main(String[] args) {
		System.out.println("=== Hooks and Interceptors Tutorial Examples ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			System.out.println("\n--- 示例1：基础 Hooks 和 Interceptors ---");
			basicHooksAndInterceptors();

			System.out.println("\n--- 示例2：消息压缩 Hook ---");
			messageSummarization();

			System.out.println("\n--- 示例3：人工介入循环 ---");
			humanInTheLoop();

			System.out.println("\n--- 示例4：模型调用限制 ---");
			modelCallLimit();

			System.out.println("\n--- 示例5：PII 检测 ---");
			piiDetection();

			System.out.println("\n--- 示例6：工具重试 ---");
			toolRetry();

			System.out.println("\n--- 示例7：规划（Planning） ---");
			planning();

			System.out.println("\n--- 示例8：LLM 工具选择器 ---");
			llmToolSelector();

			System.out.println("\n--- 示例9：LLM 工具模拟器 ---");
			llmToolEmulator();

			System.out.println("\n--- 示例10：上下文编辑 ---");
			contextEditing();

			System.out.println("\n=== 所有示例执行完成 ===");
		}
		catch (Exception e) {
			System.err.println("执行示例时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 示例12：自定义 ModelHook
	 */
	@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
	public static class CustomModelHook extends ModelHook {

		@Override
		public String getName() {
			return "custom_model_hook";
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			// 在模型调用前执行
			System.out.println("准备调用模型...");

			// 可以修改状态
			// 例如：添加额外的上下文
			return CompletableFuture.completedFuture(Map.of("extra_context", "某些额外信息"));
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			// 在模型调用后执行
			System.out.println("模型调用完成");

			// 可以记录响应信息
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	/**
	 * 示例13：自定义 AgentHook
	 */
	@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
	public static class CustomAgentHook extends AgentHook {

		@Override
		public String getName() {
			return "custom_agent_hook";
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 开始执行");
			// 可以初始化资源、记录开始时间等
			return CompletableFuture.completedFuture(Map.of("start_time", System.currentTimeMillis()));
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
			System.out.println("Agent 执行完成");
			// 可以清理资源、计算执行时间等
			Optional<Object> startTime = state.value("start_time");
			if (startTime.isPresent()) {
				long duration = System.currentTimeMillis() - (Long) startTime.get();
				System.out.println("执行耗时: " + duration + "ms");
			}
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	/**
	 * 示例14：自定义 ModelInterceptor
	 */
	public static class LoggingInterceptor extends ModelInterceptor {

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

		@Override
		public String getName() {
			return "LoggingInterceptor";
		}
	}

	/**
	 * 示例15：自定义 ToolInterceptor
	 */
	public static class ToolMonitoringInterceptor extends ToolInterceptor {

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
			}
			catch (Exception e) {
				long duration = System.currentTimeMillis() - startTime;
				System.err.println("工具 " + toolName + " 执行失败 (耗时: " + duration + "ms): " + e.getMessage());

				return ToolCallResponse.of(
						request.getToolCallId(),
						request.getToolName(),
						"工具执行失败: " + e.getMessage()
				);
			}
		}

		@Override
		public String getName() {
			return "ToolMonitoringInterceptor";
		}
	}

	/**
	 * 日志记录 ModelHook
	 */
	@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
	private static class LoggingModelHook extends ModelHook {
		@Override
		public String getName() {
			return "logging_model_hook";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			System.out.println("Before model call");
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			System.out.println("After model call");
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	/**
	 * 消息修剪 Hook
	 */
	private static class MessageTrimmingHook extends ModelHook {
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
				if (messages.size() > 10) {
					return CompletableFuture.completedFuture(Map.of("messages", messages.subList(messages.size() - 10, messages.size())));
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
	 * 护栏拦截器
	 */
	private static class GuardrailInterceptor extends ModelInterceptor {
		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			// 简化的实现
			return handler.call(request);
		}

		@Override
		public String getName() {
			return "GuardrailInterceptor";
		}
	}

	// ==================== Main 方法 ====================

	/**
	 * 重试工具拦截器
	 */
	private static class RetryToolInterceptor extends ToolInterceptor {
		@Override
		public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
			// 简化的实现
			return handler.call(request);
		}

		@Override
		public String getName() {
			return "RetryToolInterceptor";
		}
	}
}

