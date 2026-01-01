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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

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
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * 记忆管理（Memory）示�?
 *
 * 演示如何在Agent中使用记忆管理功能，包括�?
 * 1. 在工具中读取长期记忆
 * 2. 在工具中写入长期记忆
 * 3. 使用ModelHook管理长期记忆
 * 4. 结合短期和长期记�?
 * 5. 跨会话记�?
 * 6. 用户偏好学习
 *
 * 参考文�? advanced_doc/memory.md
 */
public class MemoryExample {

	private final ChatModel chatModel;

	public MemoryExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示�?
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
			System.err.println("请设�?AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		MemoryExample example = new MemoryExample(chatModel);

		// 运行所有示�?
		example.runAllExamples();
	}

	private static void mockInsertToStore(MemoryStore store) {
		// 向存储中写入示例数据
		Map<String, Object> userData = new HashMap<>();
		userData.put("name", "张三");
		userData.put("language", "中文");

		StoreItem userItem = StoreItem.of(List.of("users"), "user_123", userData);
		store.putItem(userItem);
	}

	/**
	 * 示例1：在工具中读取长期记�?
	 *
	 * 创建一个工具，让Agent能够查询用户信息
	 */
	public void example1_readMemoryInTool() throws GraphRunnerException {
		// 定义请求和响应记�?
		record GetMemoryRequest(List<String> namespace, String key) { }
		record MemoryResponse(String message, Map<String, Object> value) { }

		// 创建获取用户信息的工�?
		BiFunction<GetMemoryRequest, ToolContext, MemoryResponse> getUserInfoFunction =
				(request, context) -> {
					RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("config");
					Store store = runnableConfig.store();
					Optional<StoreItem> itemOpt = store.getItem(request.namespace(), request.key());
					if (itemOpt.isPresent()) {
						Map<String, Object> value = itemOpt.get().getValue();
						return new MemoryResponse("找到用户信息", value);
					}
					return new MemoryResponse("未找到用�?, Map.of());
				};

		ToolCallback getUserInfoTool = FunctionToolCallback.builder("getUserInfo", getUserInfoFunction)
				.description("查询用户信息")
				.inputType(GetMemoryRequest.class)
				.build();

		// 创建Agent
		ReactAgent agent = ReactAgent.builder()
				.name("memory_agent")
				.model(chatModel)
				.tools(getUserInfoTool)
				.saver(new MemorySaver())
				.build();


		// 创建内存存储
		MemoryStore store = new MemoryStore();
		// 在Store中放入模拟数据，实际应用中，存储可能是其他流程中生成
		mockInsertToStore(store);
		// 运行Agent
		RunnableConfig config = RunnableConfig.builder()
				.threadId("session_001")
				.addMetadata("user_id", "user_123")
				.store(store)
				.build();

		agent.invoke("查询用户信息，namespace=['users'], key='user_123'", config);

		System.out.println("工具读取长期记忆示例执行完成");
	}

	/**
	 * 示例2：在工具中写入长期记�?
	 *
	 * 创建一个更新用户信息的工具
	 */
	public void example2_writeMemoryInTool() throws GraphRunnerException {
		// 定义请求记录
		record SaveMemoryRequest(List<String> namespace, String key, Map<String, Object> value) { }
		record MemoryResponse(String message, Map<String, Object> value) { }

		// 创建保存用户信息的工�?
		BiFunction<SaveMemoryRequest, ToolContext, MemoryResponse> saveUserInfoFunction =
				(request, context) -> {
					RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("config");
					Store store = runnableConfig.store();
					StoreItem item = StoreItem.of(request.namespace(), request.key(), request.value());
					store.putItem(item);
					return new MemoryResponse("成功保存用户信息", request.value());
				};

		ToolCallback saveUserInfoTool = FunctionToolCallback.builder("saveUserInfo", saveUserInfoFunction)
				.description("保存用户信息")
				.inputType(SaveMemoryRequest.class)
				.build();

		// 创建Agent
		ReactAgent agent = ReactAgent.builder()
				.name("save_memory_agent")
				.model(chatModel)
				.tools(saveUserInfoTool)
				.saver(new MemorySaver())
				.build();

		// 创建内存存储
		MemoryStore store = new MemoryStore();
		RunnableConfig config = RunnableConfig.builder()
				.threadId("session_001")
				.addMetadata("user_id", "user_123")
				.store(store)
				.build();
		// 运行Agent
		agent.invoke(
				"我叫张三，请保存我的信息。使�?saveUserInfo 工具，namespace=['users'], key='user_123', value={'name': '张三'}",
				config
		);

		// 可以直接访问存储获取�?
		Optional<StoreItem> savedItem = store.getItem(List.of("users"), "user_123");
		if (savedItem.isPresent()) {
			Map<String, Object> savedValue = savedItem.get().getValue();
			System.out.println("保存的数�? " + savedValue);
		}

		System.out.println("工具写入长期记忆示例执行完成");
	}

	/**
	 * 示例3：使用MessagesModelHook管理长期记忆
	 *
	 * 在模型调用前后自动加载和保存长期记忆
	 */
	public void example3_memoryWithModelHook() throws GraphRunnerException {
		// 创建记忆拦截�?
		@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
		class MemoryInterceptor extends MessagesModelHook {
			@Override
			public String getName() {
				return "memory_interceptor";
			}

			@Override
			public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
				// 从配置中获取用户ID
				String userId = (String) config.metadata("user_id").orElse(null);
				if (userId == null) {
					return new AgentCommand(previousMessages);
				}

				Store store = config.store();
				// 从记忆存储中加载用户画像
				Optional<StoreItem> itemOpt = store.getItem(List.of("user_profiles"), userId);
				if (itemOpt.isPresent()) {
					Map<String, Object> profile = itemOpt.get().getValue();

					// 将用户上下文注入系统消息
					String userContext = String.format(
							"用户信息：姓�?%s, 年龄=%s, 邮箱=%s, 偏好=%s",
							profile.get("name"),
							profile.get("age"),
							profile.get("email"),
							profile.get("preferences")
					);

					// 查找是否已存�?SystemMessage
					SystemMessage existingSystemMessage = null;
					int systemMessageIndex = -1;
					for (int i = 0; i < previousMessages.size(); i++) {
						Message msg = previousMessages.get(i);
						if (msg instanceof SystemMessage) {
							existingSystemMessage = (SystemMessage) msg;
							systemMessageIndex = i;
							break;
						}
					}

					// 如果找到 SystemMessage，更新它；否则创建新�?
					SystemMessage enhancedSystemMessage;
					if (existingSystemMessage != null) {
						// 更新现有�?SystemMessage
						enhancedSystemMessage = new SystemMessage(
								existingSystemMessage.getText() + "\n\n" + userContext
						);
					}
					else {
						// 创建新的 SystemMessage
						enhancedSystemMessage = new SystemMessage(userContext);
					}

					// 构建新的消息列表
					List<Message> newMessages = new ArrayList<>();
					if (systemMessageIndex >= 0) {
						// 如果找到�?SystemMessage，替换它
						for (int i = 0; i < previousMessages.size(); i++) {
							if (i == systemMessageIndex) {
								newMessages.add(enhancedSystemMessage);
							}
							else {
								newMessages.add(previousMessages.get(i));
							}
						}
					}
					else {
						// 如果没有找到 SystemMessage，在开头添加新�?
						newMessages.add(enhancedSystemMessage);
						newMessages.addAll(previousMessages);
					}

					// 使用 REPLACE 策略替换所有消�?
					return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
				}

				return new AgentCommand(previousMessages);
			}

			@Override
			public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
				// 可以在这里实现对话后的记忆保存逻辑
				// 不修改消息，返回原始消息
				return new AgentCommand(previousMessages);
			}
		}

		MessagesModelHook memoryInterceptor = new MemoryInterceptor();

		// 创建带有记忆拦截器的Agent
		ReactAgent agent = ReactAgent.builder()
				.name("memory_agent")
				.model(chatModel)
				.hooks(memoryInterceptor)
				.saver(new MemorySaver())
				.build();


		// 创建内存存储
		MemoryStore memoryStore = new MemoryStore();

		// 模拟数据，预先填充用户画�?
		Map<String, Object> profileData = new HashMap<>();
		profileData.put("name", "王小�?);
		profileData.put("age", 28);
		profileData.put("email", "wang@example.com");
		profileData.put("preferences", List.of("喜欢咖啡", "喜欢阅读"));

		StoreItem profileItem = StoreItem.of(List.of("user_profiles"), "user_001", profileData);
		memoryStore.putItem(profileItem);
		RunnableConfig config = RunnableConfig.builder()
				.threadId("session_001")
				.addMetadata("user_id", "user_001")
				.store(memoryStore)
				.build();

		// Agent会自动加载用户画像信�?
		agent.invoke("请介绍一下我的信息�?, config);

		System.out.println("ModelHook管理长期记忆示例执行完成");
	}

	/**
	 * 示例4：结合短期和长期记忆
	 *
	 * 短期记忆用于存储对话上下文，长期记忆用于存储持久化数�?
	 * 使用MessagesModelHook实现
	 */
	public void example4_combinedMemory() throws GraphRunnerException {
		// 创建组合记忆Hook
		@HookPositions({HookPosition.BEFORE_MODEL})
		class CombinedMemoryHook extends MessagesModelHook {
			@Override
			public String getName() {
				return "combined_memory";
			}

			@Override
			public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
				Optional<Object> userIdOpt = config.metadata("user_id");
				if (userIdOpt.isEmpty()) {
					return new AgentCommand(previousMessages);
				}
				String userId = (String) userIdOpt.get();

				Store memoryStore = config.store();
				// 从长期记忆加�?
				Optional<StoreItem> profileOpt = memoryStore.getItem(List.of("profiles"), userId);
				if (profileOpt.isEmpty()) {
					return new AgentCommand(previousMessages);
				}

				Map<String, Object> profile = profileOpt.get().getValue();
				String contextInfo = String.format("长期记忆：用�?%s, 职业: %s",
						profile.get("name"), profile.get("occupation"));

				// 查找是否已存�?SystemMessage
				SystemMessage existingSystemMessage = null;
				int systemMessageIndex = -1;
				for (int i = 0; i < previousMessages.size(); i++) {
					Message msg = previousMessages.get(i);
					if (msg instanceof SystemMessage) {
						existingSystemMessage = (SystemMessage) msg;
						systemMessageIndex = i;
						break;
					}
				}

				// 如果找到 SystemMessage，更新它；否则创建新�?
				SystemMessage enhancedSystemMessage;
				if (existingSystemMessage != null) {
					// 更新现有�?SystemMessage
					enhancedSystemMessage = new SystemMessage(
							existingSystemMessage.getText() + "\n\n" + contextInfo
					);
				}
				else {
					// 创建新的 SystemMessage
					enhancedSystemMessage = new SystemMessage(contextInfo);
				}

				// 构建新的消息列表
				List<Message> newMessages = new ArrayList<>();
				if (systemMessageIndex >= 0) {
					// 如果找到�?SystemMessage，替换它
					for (int i = 0; i < previousMessages.size(); i++) {
						if (i == systemMessageIndex) {
							newMessages.add(enhancedSystemMessage);
						}
						else {
							newMessages.add(previousMessages.get(i));
						}
					}
				}
				else {
					// 如果没有找到 SystemMessage，在开头添加新�?
					newMessages.add(enhancedSystemMessage);
					newMessages.addAll(previousMessages);
				}

				// 使用 REPLACE 策略替换所有消�?
				return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
			}
		}

		MessagesModelHook combinedMemoryHook = new CombinedMemoryHook();

		// 创建Agent
		ReactAgent agent = ReactAgent.builder()
				.name("combined_memory_agent")
				.model(chatModel)
				.hooks(combinedMemoryHook)
				.saver(new MemorySaver()) // 短期记忆
				.build();

		// 创建记忆存储
		MemoryStore memoryStore = new MemoryStore();
		// 设置长期记忆
		Map<String, Object> userProfile = new HashMap<>();
		userProfile.put("name", "李工程师");
		userProfile.put("occupation", "软件工程�?);
		StoreItem profileItem = StoreItem.of(List.of("profiles"), "user_002", userProfile);
		memoryStore.putItem(profileItem);

		RunnableConfig config = RunnableConfig.builder()
				.threadId("combined_thread")
				.addMetadata("user_id", "user_002")
				.store(memoryStore)
				.build();

		// 短期记忆：在对话中记�?
		agent.invoke("我今天在做一�?Spring 项目�?, config);

		// 提出需要同时使用两种记忆的问题
		agent.invoke("根据我的职业和今天的工作，给我一些建议�?, config);
		// 响应会同时使用长期记忆（职业）和短期记忆（Spring项目�?

		System.out.println("结合短期和长期记忆示例执行完�?);
	}

	/**
	 * 示例5：跨会话记忆
	 *
	 * 同一用户在不同会话中应该能够访问相同的长期记�?
	 */
	public void example5_crossSessionMemory() throws GraphRunnerException {
		record SaveMemoryRequest(List<String> namespace, String key, Map<String, Object> value) { }
		record GetMemoryRequest(List<String> namespace, String key) { }
		record MemoryResponse(String message, Map<String, Object> value) { }


		ToolCallback saveMemoryTool = FunctionToolCallback.builder("saveMemory",
						(BiFunction<SaveMemoryRequest, ToolContext, MemoryResponse>) (request, context) -> {
							StoreItem item = StoreItem.of(request.namespace(), request.key(), request.value());
							RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("config");
							Store memoryStore = runnableConfig.store();
							memoryStore.putItem(item);
							return new MemoryResponse("已保�?, request.value());
						})
				.description("保存到长期记�?)
				.inputType(SaveMemoryRequest.class)
				.build();

		ToolCallback getMemoryTool = FunctionToolCallback.builder("getMemory",
						(BiFunction<GetMemoryRequest, ToolContext, MemoryResponse>) (request, context) -> {
							RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("config");
							Store memoryStore = runnableConfig.store();
							Optional<StoreItem> itemOpt = memoryStore.getItem(request.namespace(), request.key());
							return new MemoryResponse(
									itemOpt.isPresent() ? "找到" : "未找�?,
									itemOpt.map(StoreItem::getValue).orElse(Map.of())
							);
						})
				.description("从长期记忆获�?)
				.inputType(GetMemoryRequest.class)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("session_agent")
				.model(chatModel)
				.tools(saveMemoryTool, getMemoryTool)
				.saver(new MemorySaver())
				.build();

		// 创建记忆存储和工�?
		MemoryStore memoryStore = new MemoryStore();
		// 会话1：保存信�?
		RunnableConfig session1 = RunnableConfig.builder()
				.threadId("session_morning")
				.addMetadata("user_id", "user_003")
				.store(memoryStore)
				.build();

		agent.invoke(
				"记住我的密码�?secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}�?,
				session1
		);

		// 会话2：检索信息（不同的线程，同一用户�?
		RunnableConfig session2 = RunnableConfig.builder()
				.threadId("session_afternoon")
				.addMetadata("user_id", "user_003")
				.store(memoryStore)
				.build();

		agent.invoke(
				"我的密码是什么？�?getMemory 获取，namespace=['credentials'], key='user_003_password'�?,
				session2
		);
		// 长期记忆在不同会话间持久�?

		System.out.println("跨会话记忆示例执行完�?);
	}

	/**
	 * 示例6：用户偏好学�?
	 *
	 * Agent可以随着时间的推移学习并存储用户偏好
	 * 使用MessagesModelHook实现
	 */
	public void example6_preferLearning() throws GraphRunnerException {
		MemoryStore memoryStore = new MemoryStore();

		@HookPositions({HookPosition.AFTER_MODEL})
		class PreferenceLearningHook extends MessagesModelHook {
			private final MemoryStore store;

			public PreferenceLearningHook(MemoryStore store) {
				this.store = store;
			}

			@Override
			public String getName() {
				return "preference_learning";
			}

			@Override
			public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
				String userId = (String) config.metadata("user_id").orElse(null);
				if (userId == null) {
					return new AgentCommand(previousMessages);
				}

				// 提取用户输入
				if (previousMessages.isEmpty()) {
					return new AgentCommand(previousMessages);
				}

				// 加载现有偏好
				Optional<StoreItem> prefsOpt = store.getItem(List.of("user_data"), userId + "_preferences");
				List<String> prefs = new ArrayList<>();
				if (prefsOpt.isPresent()) {
					Map<String, Object> prefsData = prefsOpt.get().getValue();
					prefs = (List<String>) prefsData.getOrDefault("items", new ArrayList<>());
				}

				// 简单的偏好提取（实际应用中使用NLP�?
				for (Message msg : previousMessages) {
					String content = msg.getText().toLowerCase();
					if (content.contains("喜欢") || content.contains("偏好")) {
						prefs.add(msg.getText());

						Map<String, Object> prefsData = new HashMap<>();
						prefsData.put("items", prefs);
						StoreItem item = StoreItem.of(List.of("user_data"), userId + "_preferences", prefsData);
						store.putItem(item);

						System.out.println("学习到用户偏�?" + userId + ": " + msg.getText());
					}
				}

				// 不修改消息，返回原始消息
				return new AgentCommand(previousMessages);
			}
		}

		MessagesModelHook preferenceLearningHook = new PreferenceLearningHook(memoryStore);

		ReactAgent agent = ReactAgent.builder()
				.name("learning_agent")
				.model(chatModel)
				.hooks(preferenceLearningHook)
				.saver(new MemorySaver())
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.threadId("learning_thread")
				.addMetadata("user_id", "user_004")
				.build();

		// 用户表达偏好
		agent.invoke("我喜欢喝绿茶�?, config);
		agent.invoke("我偏好早上运动�?, config);

		// 验证偏好已被存储
		Optional<StoreItem> savedPrefs = memoryStore.getItem(List.of("user_data"), "user_004_preferences");
		if (savedPrefs.isPresent()) {
			System.out.println("已保存的偏好: " + savedPrefs.get().getValue());
		}

		System.out.println("用户偏好学习示例执行完成");
	}

	/**
	 * 运行所有示�?
	 */
	public void runAllExamples() {
		System.out.println("=== 记忆管理（Memory）示�?===\n");

		try {
			System.out.println("示例1: 在工具中读取长期记忆");
			example1_readMemoryInTool();
			System.out.println();

			System.out.println("示例2: 在工具中写入长期记忆");
			example2_writeMemoryInTool();
			System.out.println();

			System.out.println("示例3: 使用ModelHook管理长期记忆");
			example3_memoryWithModelHook();
			System.out.println();

			System.out.println("示例4: 结合短期和长期记�?);
			example4_combinedMemory();
			System.out.println();

			System.out.println("示例5: 跨会话记�?);
			example5_crossSessionMemory();
			System.out.println();

			System.out.println("示例6: 用户偏好学习");
			example6_preferLearning();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

