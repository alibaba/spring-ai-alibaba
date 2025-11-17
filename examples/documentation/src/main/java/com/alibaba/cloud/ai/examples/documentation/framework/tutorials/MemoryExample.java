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
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.redisson.api.RedissonClient;

/**
 * Memory Tutorial - 完整代码示例
 * 展示如何使用短期记忆让Agent记住先前交互
 *
 * 来源：memory.md
 */
public class MemoryExample {

	// ==================== 基础使用 ====================

	/**
	 * 示例1：基础记忆配置
	 */
	public static void basicMemoryConfiguration() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建示例工具
		ToolCallback getUserInfoTool = createGetUserInfoTool();

		// 配置 checkpointer
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(getUserInfoTool)
				.saver(new MemorySaver())
				.build();

		// 使用 thread_id 维护对话上下文
		RunnableConfig config = RunnableConfig.builder()
				.threadId("1") // threadId 指定会话 ID
				.build();

		agent.call("你好！我叫 Bob。", config);
	}

	/**
	 * 示例2：生产环境使用 Redis Checkpointer
	 */
	public static void productionMemoryConfiguration(RedissonClient redissonClient) {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback getUserInfoTool = createGetUserInfoTool();

		// 配置 Redis checkpointer
		RedisSaver redisSaver = new RedisSaver(redissonClient);

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(getUserInfoTool)
				.saver(redisSaver)
				.build();
	}

	// ==================== 自定义 Agent 记忆 ====================

	/**
	 * 示例5：使用消息修剪
	 */
	public static void useMessageTrimming() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ToolCallback[] tools = new ToolCallback[0];

		// 使用
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(tools)
				.hooks(new MessageTrimmingHook())
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		agent.call("你好，我叫 bob", config);
		agent.call("写一首关于猫的短诗", config);
		agent.call("现在对狗做同样的事情", config);
		AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

		System.out.println(finalResponse.getText());
		// 输出：你的名字是 Bob。你之前告诉我的。
	}

	// ==================== 修剪消息 ====================

	/**
	 * 示例8：使用消息删除
	 */
	public static void useMessageDeletion() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.systemPrompt("请简洁明了。")
				.hooks(new MessageDeletionHook())
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		// 第一次调用
		agent.call("你好！我是 bob", config);
		// 输出：[('human', "你好！我是 bob"), ('assistant', '你好 Bob！很高兴见到你...')]

		// 第二次调用
		agent.call("我叫什么名字？", config);
		// 输出：[('human', "我叫什么名字？"), ('assistant', '你的名字是 Bob...')]
	}

	/**
	 * 示例10：使用消息总结
	 */
	public static void useMessageSummarization() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 用于总结的模型（可以是更便宜的模型）
		ChatModel summaryModel = chatModel;

		MessageSummarizationHook summarizationHook = new MessageSummarizationHook(
				summaryModel,
				4000,  // 在 4000 tokens 时触发总结
				20     // 总结后保留最后 20 条消息
		);

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.hooks(summarizationHook)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		agent.call("你好，我叫 bob", config);
		agent.call("写一首关于猫的短诗", config);
		agent.call("现在对狗做同样的事情", config);
		AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

		System.out.println(finalResponse.getText());
		// 输出：你的名字是 Bob！
	}

	// ==================== 删除消息 ====================

	/**
	 * 示例12：使用工具访问记忆
	 */
	public static void accessMemoryInTool() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 创建工具
		ToolCallback getUserInfoTool = FunctionToolCallback
				.builder("get_user_info", new UserInfoTool())
				.description("查找用户信息")
				.inputType(String.class)
				.build();

		// 使用
		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(getUserInfoTool)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.addMetadata("user_id", "user_123")
				.build();

		AssistantMessage response = agent.call("获取用户信息", config);
		System.out.println(response.getText());
	}

	/**
	 * 创建示例工具
	 */
	private static ToolCallback createGetUserInfoTool() {
		return FunctionToolCallback.builder("get_user_info", (String query) -> {
					return "User info: " + query;
				})
				.description("Get user information")
				.inputType(String.class)
				.build();
	}

	public static void main(String[] args) {
		System.out.println("=== Memory Tutorial Examples ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			// 示例1：基础记忆配置
			System.out.println("\n--- 示例1：基础记忆配置 ---");
			basicMemoryConfiguration();

			// 示例2：生产环境使用 Redis Checkpointer (需要 RedissonClient 实例，此处跳过)
			System.out.println("\n--- 示例2：生产环境使用 Redis Checkpointer (跳过，需要 RedissonClient) ---");
			// productionMemoryConfiguration(redissonClient);

			// 示例5：使用消息修剪
			System.out.println("\n--- 示例5：使用消息修剪 ---");
			useMessageTrimming();

			// 示例8：使用消息删除
			System.out.println("\n--- 示例8：使用消息删除 ---");
			useMessageDeletion();

			// 示例10：使用消息总结
			System.out.println("\n--- 示例10：使用消息总结 ---");
			useMessageSummarization();

			// 示例12：使用工具访问记忆
			System.out.println("\n--- 示例12：使用工具访问记忆 ---");
			accessMemoryInTool();

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

	// ==================== 总结消息 ====================

	/**
	 * 示例3：在 Hook 中访问和修改状态
	 */
	public static class CustomMemoryHook extends ModelHook {

		@Override
		public String getName() {
			return "custom_memory";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {HookPosition.BEFORE_MODEL};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			// 访问消息历史
			Optional<Object> messagesOpt = state.value("messages");
			if (messagesOpt.isPresent()) {
				List<Message> messages = (List<Message>) messagesOpt.get();
				// 处理消息...
			}

			// 添加自定义状态
			return CompletableFuture.completedFuture(Map.of(
					"user_id", "user_123",
					"preferences", Map.of("theme", "dark")
			));
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	/**
	 * 示例4：消息修剪 Hook
	 */
	public static class MessageTrimmingHook extends ModelHook {

		private static final int MAX_MESSAGES = 3;

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
			if (!messagesOpt.isPresent()) {
				return CompletableFuture.completedFuture(Map.of());
			}

			List<Message> messages = (List<Message>) messagesOpt.get();

			if (messages.size() <= MAX_MESSAGES) {
				return CompletableFuture.completedFuture(Map.of()); // 无需更改
			}

			int keepCount = messages.size() % 2 == 0 ? 3 : 4;

			List<Object> newMessages = new ArrayList<>();
			// 标记中间消息为删除（使用 RemoveByHash），其余消息会自动保留
			if (messages.size() - keepCount > 1) {
				for (Message msg : messages.subList(1, messages.size() - keepCount)) {
					newMessages.add(RemoveByHash.of(msg));
				}
			}

			return CompletableFuture.completedFuture(Map.of("messages", newMessages));
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	// ==================== 访问记忆 ====================

	/**
	 * 示例6：消息删除 Hook
	 */
	public static class MessageDeletionHook extends ModelHook {

		@Override
		public String getName() {
			return "message_deletion";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {HookPosition.AFTER_MODEL};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			Optional<Object> messagesOpt = state.value("messages");
			if (!messagesOpt.isPresent()) {
				return CompletableFuture.completedFuture(Map.of());
			}

			List<Message> messages = (List<Message>) messagesOpt.get();

			if (messages.size() > 2) {
				// 将最早的两条消息转为 RemoveByHash 对象以便从状态中删除
				List<Object> removeOldMessages = new ArrayList<>();
				removeOldMessages.add(RemoveByHash.of(messages.get(0)));
				removeOldMessages.add(RemoveByHash.of(messages.get(1)));
				return CompletableFuture.completedFuture(Map.of("messages", removeOldMessages));
			}

			return CompletableFuture.completedFuture(Map.of());
		}
	}

	/**
	 * 示例7：删除所有消息
	 */
	public static class ClearAllMessagesHook extends ModelHook {

		@Override
		public String getName() {
			return "clear_all_messages";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {HookPosition.AFTER_MODEL};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			Optional<Object> messagesOpt = state.value("messages");
			if (!messagesOpt.isPresent()) {
				return CompletableFuture.completedFuture(Map.of());
			}
			List<Message> messages = (List<Message>) messagesOpt.get();
			List<Object> removeAllMessages = new ArrayList<>();
			for (Message msg : messages) {
				removeAllMessages.add(RemoveByHash.of(msg));
			}
			return CompletableFuture.completedFuture(Map.of("messages", removeAllMessages));
		}
	}

	// ==================== 辅助方法 ====================

	/**
	 * 示例9：消息总结 Hook
	 */
	public static class MessageSummarizationHook extends ModelHook {

		private final ChatModel summaryModel;
		private final int maxTokensBeforeSummary;
		private final int messagesToKeep;

		public MessageSummarizationHook(
				ChatModel summaryModel,
				int maxTokensBeforeSummary,
				int messagesToKeep
		) {
			this.summaryModel = summaryModel;
			this.maxTokensBeforeSummary = maxTokensBeforeSummary;
			this.messagesToKeep = messagesToKeep;
		}

		@Override
		public String getName() {
			return "message_summarization";
		}

		@Override
		public HookPosition[] getHookPositions() {
			return new HookPosition[] {HookPosition.BEFORE_MODEL};
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			Optional<Object> messagesOpt = state.value("messages");
			if (!messagesOpt.isPresent()) {
				return CompletableFuture.completedFuture(Map.of());
			}

			List<Message> messages = (List<Message>) messagesOpt.get();

			// 估算 token 数量（简化版）
			int estimatedTokens = messages.stream()
					.mapToInt(m -> m.getText().length() / 4)
					.sum();

			if (estimatedTokens < maxTokensBeforeSummary) {
				return CompletableFuture.completedFuture(Map.of());
			}

			// 需要总结
			int messagesToSummarize = messages.size() - messagesToKeep;
			if (messagesToSummarize <= 0) {
				return CompletableFuture.completedFuture(Map.of());
			}

			List<Message> oldMessages = messages.subList(0, messagesToSummarize);
			List<Message> recentMessages = messages.subList(
					messagesToSummarize,
					messages.size()
			);

			// 生成摘要
			String summary = generateSummary(oldMessages);

			// 创建摘要消息
			SystemMessage summaryMessage = new SystemMessage(
					"## 之前对话摘要:\n" + summary
			);

			// 只需要把摘要消息和需要删除的消息保留在状态中，其余未包含的消息将会自动保留
			List<Object> newMessages = new ArrayList<>();
			newMessages.add(summaryMessage);
			// IMPORTANT! Convert summarized messages to RemoveByHash objects so we can remove them from state
			for (Message msg : oldMessages) {
				newMessages.add(RemoveByHash.of(msg));
			}

			return CompletableFuture.completedFuture(Map.of("messages", newMessages));
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of());
		}

		private String generateSummary(List<Message> messages) {
			StringBuilder conversation = new StringBuilder();
			for (Message msg : messages) {
				conversation.append(msg.getMessageType())
						.append(": ")
						.append(msg.getText())
						.append("\n");
			}

			String summaryPrompt = "请简要总结以下对话:\n\n" + conversation;

			ChatResponse response = summaryModel.call(
					new Prompt(new UserMessage(summaryPrompt))
			);

			return response.getResult().getOutput().getText();
		}
	}

	// ==================== Main 方法 ====================

	/**
	 * 示例11：在工具中读取短期记忆
	 */
	public static class UserInfoTool implements BiFunction<String, ToolContext, String> {

		@Override
		public String apply(String query, ToolContext toolContext) {
			// 从上下文中获取用户信息
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
			String userId = (String) config.metadata("user_id").orElse("");

			if ("user_123".equals(userId)) {
				return "用户是 John Smith";
			}
			else {
				return "未知用户";
			}
		}
	}
}

