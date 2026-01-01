/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

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

		// 使用 thread_id 维护对话上下�?
		RunnableConfig config = RunnableConfig.builder()
				.threadId("1") // threadId 指定会话 ID
				.build();

		agent.call("你好！我�?Bob�?, config);
	}

	/**
	 * 示例2：生产环境使�?Redis Checkpointer
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
		RedisSaver redisSaver = RedisSaver.builder().redisson(redissonClient).build();

		ReactAgent agent = ReactAgent.builder()
				.name("my_agent")
				.model(chatModel)
				.tools(getUserInfoTool)
				.saver(redisSaver)
				.build();
	}

	// ==================== 自定�?Agent 记忆 ====================

	/**
	 * 示例5：使用消息修�?
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

		agent.call("你好，我�?bob", config);
		agent.call("写一首关于猫的短�?, config);
		agent.call("现在对狗做同样的事情", config);
		AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

		System.out.println(finalResponse.getText());
		// 输出：你的名字是 Bob。你之前告诉我的�?
	}

	// ==================== 修剪消息 ====================

	/**
	 * 示例8：使用消息删�?
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
				.systemPrompt("请简洁明了�?)
				.hooks(new MessageDeletionHook())
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("1")
				.build();

		// 第一次调�?
		agent.call("你好！我�?bob", config);
		// 输出：[('human', "你好！我�?bob"), ('assistant', '你好 Bob！很高兴见到�?..')]

		// 第二次调�?
		agent.call("我叫什么名字？", config);
		// 输出：[('human', "我叫什么名字？"), ('assistant', '你的名字�?Bob...')]
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
				4000,  // �?4000 tokens 时触发总结
				20     // 总结后保留最�?20 条消�?
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

		agent.call("你好，我�?bob", config);
		agent.call("写一首关于猫的短�?, config);
		agent.call("现在对狗做同样的事情", config);
		AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

		System.out.println(finalResponse.getText());
		// 输出：你的名字是 Bob�?
	}

	// ==================== 删除消息 ====================

	/**
	 * 示例12：使用工具访问记�?
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
		System.out.println("注意：需要设�?AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			// 示例1：基础记忆配置
			System.out.println("\n--- 示例1：基础记忆配置 ---");
			basicMemoryConfiguration();

			// 示例2：生产环境使�?Redis Checkpointer (需�?RedissonClient 实例，此处跳�?
			System.out.println("\n--- 示例2：生产环境使�?Redis Checkpointer (跳过，需�?RedissonClient) ---");
			// productionMemoryConfiguration(redissonClient);

			// 示例5：使用消息修�?
			System.out.println("\n--- 示例5：使用消息修�?---");
			useMessageTrimming();

			// 示例8：使用消息删�?
			System.out.println("\n--- 示例8：使用消息删�?---");
			useMessageDeletion();

			// 示例10：使用消息总结
			System.out.println("\n--- 示例10：使用消息总结 ---");
			useMessageSummarization();

			// 示例12：使用工具访问记�?
			System.out.println("\n--- 示例12：使用工具访问记�?---");
			accessMemoryInTool();

			System.out.println("\n=== 所有示例执行完�?===");
		}
		catch (GraphRunnerException e) {
			System.err.println("执行示例时发生错�? " + e.getMessage());
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println("发生未预期的错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// ==================== 总结消息 ====================

	/**
	 * 示例3：在 Hook 中访问和修改状�?
	 * 注意：这�?Hook 主要用于访问消息历史，不修改消息，所以可以继续使�?ModelHook
	 * 但如果需要修改消息，应该使用 MessagesModelHook
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	public static class CustomMemoryHook extends MessagesModelHook {

		@Override
		public String getName() {
			return "custom_memory";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			// 访问消息历史（previousMessages 已经提供了消息列表）
			// 处理消息...
			// 如果需要修改消息，可以返回新的 AgentCommand
			// 这里只是访问，不修改消息，所以返回原始消�?
			return new AgentCommand(previousMessages);
		}
	}

	/**
	 * 示例4：消息修�?Hook
	 * 使用 MessagesModelHook 实现，在模型调用前修剪消息列�?
	 * 保留第一条消息和最�?keepCount 条消息，删除中间的消�?
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	public static class MessageTrimmingHook extends MessagesModelHook {

		private static final int MAX_MESSAGES = 3;

		@Override
		public String getName() {
			return "message_trimming";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			if (previousMessages.size() <= MAX_MESSAGES) {
				// 如果消息数量未超过限制，无需更改
				return new AgentCommand(previousMessages);
			}

			int keepCount = previousMessages.size() % 2 == 0 ? 3 : 4;

			// 构建要保留的消息列表：第一条消�?+ 最�?keepCount 条消�?
			List<Message> trimmedMessages = new ArrayList<>();
			// 保留第一条消�?
			if (!previousMessages.isEmpty()) {
				trimmedMessages.add(previousMessages.get(0));
			}
			// 保留最�?keepCount 条消�?
			if (previousMessages.size() - keepCount > 0) {
				trimmedMessages.addAll(previousMessages.subList(
						previousMessages.size() - keepCount,
						previousMessages.size()
				));
			}

			// 使用 REPLACE 策略替换所有消�?
			return new AgentCommand(trimmedMessages, UpdatePolicy.REPLACE);
		}
	}

	// ==================== 访问记忆 ====================

	/**
	 * 示例6：消息删�?Hook
	 * 使用 MessagesModelHook 实现，在模型调用后删除最早的两条消息
	 */
	@HookPositions({HookPosition.AFTER_MODEL})
	public static class MessageDeletionHook extends MessagesModelHook {

		@Override
		public String getName() {
			return "message_deletion";
		}

		@Override
		public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
			if (previousMessages.size() <= 2) {
				// 如果消息数量不超�?条，无需删除
				return new AgentCommand(previousMessages);
			}

			// 删除最早的两条消息，保留其余消�?
			List<Message> remainingMessages = previousMessages.subList(2, previousMessages.size());

			// 使用 REPLACE 策略替换所有消�?
			return new AgentCommand(remainingMessages, UpdatePolicy.REPLACE);
		}
	}

	/**
	 * 示例7：删除所有消�?
	 * 使用 MessagesModelHook 实现，在模型调用后删除所有消�?
	 */
	@HookPositions({HookPosition.AFTER_MODEL})
	public static class ClearAllMessagesHook extends MessagesModelHook {

		@Override
		public String getName() {
			return "clear_all_messages";
		}

		@Override
		public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
			// 删除所有消息，返回空列�?
			List<Message> emptyMessages = new ArrayList<>();
			// 使用 REPLACE 策略替换所有消息为空列�?
			return new AgentCommand(emptyMessages, UpdatePolicy.REPLACE);
		}
	}

	// ==================== 辅助方法 ====================

	/**
	 * 示例9：消息总结 Hook
	 * 使用 MessagesModelHook 实现，在模型调用前检查消息数量，如果超过阈值则生成摘要
	 * 删除旧消息，保留摘要消息和最近的消息
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	public static class MessageSummarizationHook extends MessagesModelHook {

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
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			// 估算 token 数量（简化版�?
			int estimatedTokens = previousMessages.stream()
					.mapToInt(m -> m.getText().length() / 4)
					.sum();

			if (estimatedTokens < maxTokensBeforeSummary) {
				// 如果 token 数量未超过阈值，无需总结
				return new AgentCommand(previousMessages);
			}

			// 需要总结
			int messagesToSummarize = previousMessages.size() - messagesToKeep;
			if (messagesToSummarize <= 0) {
				// 如果消息数量不足以总结，无需更改
				return new AgentCommand(previousMessages);
			}

			List<Message> oldMessages = previousMessages.subList(0, messagesToSummarize);
			List<Message> recentMessages = previousMessages.subList(
					messagesToSummarize,
					previousMessages.size()
			);

			// 生成摘要
			String summary = generateSummary(oldMessages);

			// 创建摘要消息
			SystemMessage summaryMessage = new SystemMessage(
					"## 之前对话摘要:\n" + summary
			);

			// 构建新的消息列表：摘要消�?+ 最近的消息
			List<Message> newMessages = new ArrayList<>();
			newMessages.add(summaryMessage);
			newMessages.addAll(recentMessages);

			// 使用 REPLACE 策略替换所有消�?
			return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
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
	 * 示例11：在工具中读取短期记�?
	 */
	public static class UserInfoTool implements BiFunction<String, ToolContext, String> {

		@Override
		public String apply(String query, ToolContext toolContext) {
			// 从上下文中获取用户信�?
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
			String userId = (String) config.metadata("user_id").orElse("");

			if ("user_123".equals(userId)) {
				return "用户�?John Smith";
			}
			else {
				return "未知用户";
			}
		}
	}
}

