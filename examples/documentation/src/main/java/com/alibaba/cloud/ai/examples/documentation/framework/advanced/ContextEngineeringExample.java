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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 上下文工程（Context Engineering）示例
 *
 * 演示如何通过上下文工程提高Agent的可靠性，包括：
 * 1. 模型上下文：系统提示、消息历史、工具、模型选择、响应格式
 * 2. 工具上下文：工具访问和修改状态
 * 3. 生命周期上下文：Hook机制
 *
 * 参考文档: advanced_doc/context-engineering.md
 */
public class ContextEngineeringExample {

	private final ChatModel chatModel;

	public ContextEngineeringExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示例
	 *
	 * 注意：需要配置ChatModel实例才能运行
	 */
	public static void main(String[] args) {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		if (chatModel == null) {
			System.err.println("错误：请先配置ChatModel实例");
			System.err.println("请设置 AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		ContextEngineeringExample example = new ContextEngineeringExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	/**
	 * 示例1：基于状态的动态提示
	 *
	 * 根据对话长度调整系统提示
	 */
	public void example1_stateAwarePrompt() throws GraphRunnerException {
		// 创建一个模型拦截器，根据对话长度调整系统提示
		class StateAwarePromptInterceptor extends ModelInterceptor {
			@Override
			public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
				List<Message> messages = request.getMessages();
				int messageCount = messages.size();

				// 基础提示
				String basePrompt = "你是一个有用的助手。";

				// 根据消息数量调整提示
				if (messageCount > 10) {
					basePrompt += "\n这是一个长对话 - 请尽量保持精准简捷。";
				}

				// 更新系统消息（参考 TodoListInterceptor 的实现方式）
				SystemMessage enhancedSystemMessage;
				if (request.getSystemMessage() == null) {
					enhancedSystemMessage = new SystemMessage(basePrompt);
				}
				else {
					enhancedSystemMessage = new SystemMessage(
							request.getSystemMessage().getText() + "\n\n" + basePrompt
					);
				}

				// 创建新的请求并继续
				ModelRequest updatedRequest = ModelRequest.builder(request)
						.systemMessage(enhancedSystemMessage)
						.build();

				return next.call(updatedRequest);
			}

			@Override
			public String getName() {
				return "";
			}
		}

		// 使用拦截器创建Agent
		ReactAgent agent = ReactAgent.builder()
				.name("context_aware_agent")
				.model(chatModel)
				.interceptors(new StateAwarePromptInterceptor())
				.build();

		// 测试
		agent.invoke("你好");
		System.out.println("基于状态的动态提示示例执行完成");
	}

	/**
	 * 示例2：基于存储的个性化提示
	 *
	 * 从长期记忆加载用户偏好并生成个性化提示
	 */
	public void example2_personalizedPrompt() throws GraphRunnerException {
		// 用户偏好类
		class UserPreferences {
			private String communicationStyle;
			private String language;
			private List<String> interests;

			public UserPreferences(String style, String lang, List<String> interests) {
				this.communicationStyle = style;
				this.language = lang;
				this.interests = interests;
			}

			public String getCommunicationStyle() {
				return communicationStyle;
			}

			public String getLanguage() {
				return language;
			}

			public List<String> getInterests() {
				return interests;
			}
		}

		// 简单的用户偏好存储
		class UserPreferenceStore {
			private Map<String, UserPreferences> store = new HashMap<>();

			public UserPreferences getPreferences(String userId) {
				return store.getOrDefault(userId,
						new UserPreferences("专业", "中文", List.of()));
			}

			public void savePreferences(String userId, UserPreferences prefs) {
				store.put(userId, prefs);
			}
		}

		UserPreferenceStore store = new UserPreferenceStore();
		store.savePreferences("user_001",
				new UserPreferences("友好轻松", "中文", List.of("技术", "阅读")));

		// 从长期记忆加载用户偏好
		class PersonalizedPromptInterceptor extends ModelInterceptor {
			private final UserPreferenceStore store;

			public PersonalizedPromptInterceptor(UserPreferenceStore store) {
				this.store = store;
			}

			@Override
			public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
				// 从运行时上下文获取用户ID
				String userId = getUserIdFromContext(request);

				// 从存储加载用户偏好
				UserPreferences prefs = store.getPreferences(userId);

				// 构建个性化提示
				String personalizedPrompt = buildPersonalizedPrompt(prefs);

				// 更新系统消息（参考 TodoListInterceptor 的实现方式）
				SystemMessage enhancedSystemMessage;
				if (request.getSystemMessage() == null) {
					enhancedSystemMessage = new SystemMessage(personalizedPrompt);
				}
				else {
					enhancedSystemMessage = new SystemMessage(
							request.getSystemMessage().getText() + "\n\n" + personalizedPrompt
					);
				}

				ModelRequest updatedRequest = ModelRequest.builder(request)
						.systemMessage(enhancedSystemMessage)
						.build();

				return next.call(updatedRequest);
			}

			private String getUserIdFromContext(ModelRequest request) {
				// 从请求上下文提取用户ID
				return "user_001"; // 简化示例
			}

			private String buildPersonalizedPrompt(UserPreferences prefs) {
				StringBuilder prompt = new StringBuilder("你是一个有用的助手。");

				if (prefs.getCommunicationStyle() != null) {
					prompt.append("\n沟通风格：").append(prefs.getCommunicationStyle());
				}

				if (prefs.getLanguage() != null) {
					prompt.append("\n使用语言：").append(prefs.getLanguage());
				}

				if (!prefs.getInterests().isEmpty()) {
					prompt.append("\n用户兴趣：").append(String.join(", ", prefs.getInterests()));
				}

				return prompt.toString();
			}

			@Override
			public String getName() {
				return "PersonalizedPromptInterceptor";
			}
		}

		ReactAgent agent = ReactAgent.builder()
				.name("personalized_agent")
				.model(chatModel)
				.interceptors(new PersonalizedPromptInterceptor(store))
				.build();

		agent.invoke("介绍一下最新的AI技术");
		System.out.println("个性化提示示例执行完成");
	}

	/**
	 * 示例3：消息过滤
	 *
	 * 只保留最近的N条消息，避免上下文过长
	 */
	public void example3_messageFilter() {
		class MessageFilterInterceptor extends ModelInterceptor {
			private final int maxMessages;

			public MessageFilterInterceptor(int maxMessages) {
				this.maxMessages = maxMessages;
			}

			@Override
			public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
				List<Message> messages = request.getMessages();

				// 只保留最近的N条消息
				if (messages.size() > maxMessages) {
					List<Message> filtered = new ArrayList<>();

					// 添加系统消息
					messages.stream()
							.filter(m -> m instanceof SystemMessage)
							.findFirst()
							.ifPresent(filtered::add);

					// 添加最近的消息
					int startIndex = Math.max(0, messages.size() - maxMessages + 1);
					filtered.addAll(messages.subList(startIndex, messages.size()));

					messages = filtered;
				}

				ModelRequest updatedRequest = ModelRequest.builder(request)
						.messages(messages)
						.build();

				return next.call(updatedRequest);
			}

			@Override
			public String getName() {
				return "MessageFilterInterceptor";
			}
		}

		ReactAgent agent = ReactAgent.builder()
				.name("message_filter_agent")
				.model(chatModel)
				.interceptors(new MessageFilterInterceptor(10))
				.build();

		System.out.println("消息过滤示例执行完成");
	}

	/**
	 * 示例4：基于上下文的工具选择
	 *
	 * 根据用户角色动态选择可用工具
	 */
	public void example4_contextualToolSelection() {
		class ContextualToolInterceptor extends ModelInterceptor {
			private final Map<String, List<ToolCallback>> roleBasedTools;

			public ContextualToolInterceptor(Map<String, List<ToolCallback>> roleBasedTools) {
				this.roleBasedTools = roleBasedTools;
			}

			@Override
			public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
				// 从上下文获取用户角色
				String userRole = getUserRole(request);

				// 根据角色选择工具
				List<ToolCallback> allowedTools = roleBasedTools.getOrDefault(
						userRole,
						Collections.emptyList()
				);

				// 更新工具选项（注：实际实现需要根据框架API调整）
				// 这里展示概念性代码
				System.out.println("为角色 " + userRole + " 选择了 " + allowedTools.size() + " 个工具");

				return next.call(request);
			}

			private String getUserRole(ModelRequest request) {
				// 从请求上下文提取用户角色
				return "user"; // 简化示例
			}

			@Override
			public String getName() {
				return "ContextualToolInterceptor";
			}
		}

		// 配置基于角色的工具（示例）
		Map<String, List<ToolCallback>> roleTools = Map.of(
				"admin", List.of(/* readTool, writeTool, deleteTool */),
				"user", List.of(/* readTool */),
				"guest", List.of()
		);

		ReactAgent agent = ReactAgent.builder()
				.name("role_based_agent")
				.model(chatModel)
				.interceptors(new ContextualToolInterceptor(roleTools))
				.build();

		System.out.println("基于上下文的工具选择示例执行完成");
	}

	/**
	 * 示例5：日志记录 Hook
	 *
	 * 使用Hook在模型调用前后记录日志
	 */
	public void example5_loggingHook() throws GraphRunnerException {
		class LoggingHook extends ModelHook {
			@Override
			public String getName() {
				return "logging_hook";
			}

			@Override
			public HookPosition[] getHookPositions() {
				return new HookPosition[] {
						HookPosition.BEFORE_MODEL,
						HookPosition.AFTER_MODEL
				};
			}

			@Override
			public List<JumpTo> canJumpTo() {
				return List.of();
			}

			@Override
			public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
				// 在模型调用前记录
				List<?> messages = (List<?>) state.value("messages").orElse(List.of());
				System.out.println("模型调用前 - 消息数: " + messages.size());
				return CompletableFuture.completedFuture(Map.of());
			}

			@Override
			public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
				// 在模型调用后记录
				System.out.println("模型调用后 - 响应已生成");
				return CompletableFuture.completedFuture(Map.of());
			}
		}

		// 使用Hook
		ReactAgent agent = ReactAgent.builder()
				.name("logged_agent")
				.model(chatModel)
				.hooks(new LoggingHook())
				.build();

		agent.invoke("测试日志记录");
		System.out.println("日志记录Hook示例执行完成");
	}

	/**
	 * 示例6：消息摘要 Hook
	 *
	 * 当对话过长时自动生成摘要
	 */
	public void example6_summarizationHook() {
		class SummarizationHook extends ModelHook {
			private final ChatModel summarizationModel;
			private final int triggerLength;

			public SummarizationHook(ChatModel model, int triggerLength) {
				this.summarizationModel = model;
				this.triggerLength = triggerLength;
			}

			@Override
			public String getName() {
				return "summarization_hook";
			}

			@Override
			public HookPosition[] getHookPositions() {
				return new HookPosition[] {HookPosition.BEFORE_MODEL};
			}

			@Override
			public List<JumpTo> canJumpTo() {
				return List.of();
			}

			@Override
			public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
				List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

				if (messages.size() > triggerLength) {
					// 生成对话摘要
					String summary = generateSummary(messages);

					// 查找是否已存在 SystemMessage
					SystemMessage existingSystemMessage = null;
					int systemMessageIndex = -1;
					for (int i = 0; i < messages.size(); i++) {
						Message msg = messages.get(i);
						if (msg instanceof SystemMessage) {
							existingSystemMessage = (SystemMessage) msg;
							systemMessageIndex = i;
							break;
						}
					}

					// 创建摘要 SystemMessage
					String summaryText = "之前对话摘要：" + summary;
					SystemMessage summarySystemMessage;
					if (existingSystemMessage != null) {
						// 如果存在 SystemMessage，追加摘要信息
						summarySystemMessage = new SystemMessage(
								existingSystemMessage.getText() + "\n\n" + summaryText
						);
					}
					else {
						// 如果不存在，创建新的
						summarySystemMessage = new SystemMessage(summaryText);
					}

					// 保留最近的几条消息
					int recentCount = Math.min(5, messages.size());
					List<Message> recentMessages = messages.subList(
							messages.size() - recentCount,
							messages.size()
					);

					// 构建新的消息列表
					List<Message> newMessages = new ArrayList<>();
					newMessages.add(summarySystemMessage);
					// 添加最近的消息，排除旧的 SystemMessage（如果存在）
					for (Message msg : recentMessages) {
						if (msg != existingSystemMessage) {
							newMessages.add(msg);
						}
					}

					return CompletableFuture.completedFuture(Map.of("messages", newMessages));
				}

				return CompletableFuture.completedFuture(Map.of());
			}

			private String generateSummary(List<Message> messages) {
				// 使用另一个模型生成摘要
				String conversation = messages.stream()
						.map(Message::getText)
						.collect(Collectors.joining("\n"));

				// 简化示例：返回固定摘要
				return "之前讨论了多个主题...";
			}
		}

		ReactAgent agent = ReactAgent.builder()
				.name("summarizing_agent")
				.model(chatModel)
				.hooks(new SummarizationHook(chatModel, 20))
				.build();

		System.out.println("消息摘要Hook示例执行完成");
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 上下文工程（Context Engineering）示例 ===\n");

		try {
			System.out.println("示例1: 基于状态的动态提示");
			example1_stateAwarePrompt();
			System.out.println();

			System.out.println("示例2: 基于存储的个性化提示");
			example2_personalizedPrompt();
			System.out.println();

			System.out.println("示例3: 消息过滤");
			example3_messageFilter();
			System.out.println();

			System.out.println("示例4: 基于上下文的工具选择");
			example4_contextualToolSelection();
			System.out.println();

			System.out.println("示例5: 日志记录Hook");
			example5_loggingHook();
			System.out.println();

			System.out.println("示例6: 消息摘要Hook");
			example6_summarizationHook();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

