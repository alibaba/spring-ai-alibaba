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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 内存管理示例
 * 演示短期和长期内存管�?
 */
public class MemoryExample {

	/**
	 * 示例 1: 添加短期内存
	 */
	public static void addShortTermMemory(ChatClient.Builder chatClientBuilder) throws GraphStateException {
		// 创建内存检查点�?
		MemorySaver checkpointer = new MemorySaver();

		SaverConfig saverConfig = SaverConfig.builder()
				.register(checkpointer)
				.build();

		// 定义状态策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		};

		// 创建聊天节点
		var chatNode = node_async(state -> {
			List<Map<String, String>> messages =
					(List<Map<String, String>>) state.value("messages").orElse(List.of());

			// 使用 ChatClient 调用 AI 模型
			ChatClient chatClient = chatClientBuilder.build();
			String response = chatClient.prompt()
					.user(messages.get(messages.size() - 1).get("content"))
					.call()
					.content();

			return Map.of("messages", List.of(
					Map.of("role", "assistant", "content", response)
			));
		});

		// 构建�?
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("chat", chatNode)
				.addEdge(START, "chat")
				.addEdge("chat", END);

		// 编译�?
		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.saverConfig(saverConfig)
						.build()
		);

		// 第一轮对�?
		RunnableConfig config = RunnableConfig.builder()
				.threadId("conversation-1")
				.build();

		graph.invoke(Map.of("messages", List.of(
				Map.of("role", "user", "content", "你好！我�?Bob")
		)), config);

		// 第二轮对话（使用相同�?threadId�?
		graph.invoke(Map.of("messages", List.of(
				Map.of("role", "user", "content", "我的名字是什么？")
		)), config);
		// AI 将能够记住之前的对话，回�?"Bob"
		System.out.println("Short-term memory example executed");
	}

	/**
	 * 示例 2: 使用 Store 实现长期内存
	 */
	public static void longTermMemoryWithDatabase() throws GraphStateException {
		// 在节点中使用 Store 存储用户信息
		var userProfileNode = com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async((state, config) -> {
			String userId = (String) state.value("userId").orElse("");

			if (userId.isEmpty()) {
				return Map.of("userProfile", Map.of("name", "Unknown", "preferences", "default"));
			}

			// �?Store 获取用户配置
			Store store = config.store();
			if (store != null) {
				Optional<StoreItem> itemOpt = store.getItem(List.of("user_profiles"), userId);
				if (itemOpt.isPresent()) {
					Map<String, Object> userProfile = itemOpt.get().getValue();
					return Map.of("userProfile", userProfile);
				}
			}

			// 如果未找到，返回默认�?
			Map<String, Object> userProfile = Map.of("name", "User", "preferences", "default");
			return Map.of("userProfile", userProfile);
		});

		// 创建�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("userId", new ReplaceStrategy());
			keyStrategyMap.put("userProfile", new ReplaceStrategy());
			return keyStrategyMap;
		};

		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("load_profile", userProfileNode)
				.addEdge(START, "load_profile")
				.addEdge("load_profile", END);

		CompiledGraph graph = stateGraph.compile(CompileConfig.builder().build());

		// 创建长期记忆存储并预填充数据
		MemoryStore memoryStore = new MemoryStore();
		Map<String, Object> profileData = new HashMap<>();
		profileData.put("name", "张三");
		profileData.put("preferences", "喜欢编程");
		StoreItem profileItem = StoreItem.of(List.of("user_profiles"), "user_001", profileData);
		memoryStore.putItem(profileItem);

		// 运行�?
		RunnableConfig config = RunnableConfig.builder()
				.threadId("profile_thread")
				.store(memoryStore)
				.build();

		Optional<OverAllState> stateOptiona = graph.invoke(Map.of("userId", "user_001"), config);
		Map<String, Object> result = stateOptiona.get().data();
		System.out.println("加载的用户配�? " + result.get("userProfile"));

		System.out.println("Long-term memory with Store example executed");
	}

	/**
	 * 示例 3: 使用 Store 缓存实现长期内存
	 */
	public static void longTermMemoryWithRedis() throws GraphStateException {
		var cacheNode = com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async((state, config) -> {
			String key = (String) state.value("cacheKey").orElse("");

			if (key.isEmpty()) {
				return Map.of("result", "no_key");
			}

			// �?Store 获取缓存数据
			Store store = config.store();
			if (store != null) {
				Optional<StoreItem> itemOpt = store.getItem(List.of("cache"), key);
				if (itemOpt.isPresent()) {
					// 缓存命中
					Map<String, Object> cachedData = itemOpt.get().getValue();
					return Map.of("result", cachedData.get("value"));
				}
			}

			// 缓存未命中，执行计算或查�?
			Object computedData = performExpensiveOperation(key);

			// 存储�?Store
			if (store != null) {
				Map<String, Object> cacheValue = new HashMap<>();
				cacheValue.put("value", computedData);
				StoreItem cacheItem = StoreItem.of(List.of("cache"), key, cacheValue);
				store.putItem(cacheItem);
			}

			return Map.of("result", computedData);
		});

		// 创建�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("cacheKey", new ReplaceStrategy());
			keyStrategyMap.put("result", new ReplaceStrategy());
			return keyStrategyMap;
		};

		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("cache", cacheNode)
				.addEdge(START, "cache")
				.addEdge("cache", END);

		CompiledGraph graph = stateGraph.compile(CompileConfig.builder().build());

		// 创建长期记忆存储
		MemoryStore memoryStore = new MemoryStore();

		// 第一次调用（缓存未命中）
		RunnableConfig config = RunnableConfig.builder()
				.threadId("cache_thread")
				.store(memoryStore)
				.build();

		Optional<OverAllState> stateOptional = graph.invoke(Map.of("cacheKey", "expensive_key"), config);
		Map<String, Object> result1 = stateOptional.get().data();
		System.out.println("第一次调用结�? " + result1.get("result"));

		// 第二次调用（缓存命中�?
		Optional<OverAllState> stateOptiona = graph.invoke(Map.of("cacheKey", "expensive_key"), config);
		Map<String, Object> result2 = stateOptional.get().data();
		System.out.println("第二次调用结果（从缓存）: " + result2.get("result"));

		System.out.println("Long-term memory with Store cache example executed");
	}

	// 模拟耗时操作
	private static Object performExpensiveOperation(String key) {
		// 模拟耗时计算
		return "computed_result_for_" + key;
	}

	/**
	 * 示例 4: 结合短期和长期内�?
	 */
	public static void combinedMemoryExample(ChatClient.Builder chatClientBuilder) throws GraphStateException {
		// 定义状�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("userId", new ReplaceStrategy());
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("userPreferences", new ReplaceStrategy());
			return keyStrategyMap;
		};

		// 加载用户偏好（长期内存）
		var loadUserPreferences = com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async((state, config) -> {
			String userId = (String) state.value("userId").orElse("");

			if (userId.isEmpty()) {
				return Map.of("userPreferences", Map.of("theme", "default", "language", "zh"));
			}

			// �?Store 加载用户偏好
			Store store = config.store();
			if (store != null) {
				Optional<StoreItem> itemOpt = store.getItem(List.of("user_preferences"), userId);
				if (itemOpt.isPresent()) {
					Map<String, Object> preferences = itemOpt.get().getValue();
					return Map.of("userPreferences", preferences);
				}
			}

			// 如果未找到，返回默认偏好
			Map<String, Object> preferences = Map.of("theme", "dark", "language", "zh");
			return Map.of("userPreferences", preferences);
		});

		// 聊天节点（使用短期和长期内存�?
		var chatNode = node_async(state -> {
			List<Map<String, String>> messages =
					(List<Map<String, String>>) state.value("messages").orElse(List.of());
			Map<String, Object> preferences =
					(Map<String, Object>) state.value("userPreferences").orElse(Map.of());

			// 构建包含用户偏好的提�?
			String userPrompt = messages.get(messages.size() - 1).get("content");
			String enhancedPrompt = "用户偏好: " + preferences + "\n用户问题: " + userPrompt;

			// 调用 AI
			ChatClient chatClient = chatClientBuilder.build();
			String response = chatClient.prompt()
					.user(enhancedPrompt)
					.call()
					.content();

			return Map.of("messages", List.of(
					Map.of("role", "assistant", "content", response)
			));
		});

		// 构建�?
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("load_preferences", loadUserPreferences)
				.addNode("chat", chatNode)
				.addEdge(START, "load_preferences")
				.addEdge("load_preferences", "chat")
				.addEdge("chat", END);

		// 配置检查点（短期内存）
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();

		// 编译�?
		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.saverConfig(saverConfig)
						.build()
		);

		// 创建长期记忆存储并预填充用户偏好
		MemoryStore memoryStore = new MemoryStore();
		Map<String, Object> preferencesData = new HashMap<>();
		preferencesData.put("theme", "dark");
		preferencesData.put("language", "zh");
		preferencesData.put("timezone", "Asia/Shanghai");
		StoreItem preferencesItem = StoreItem.of(List.of("user_preferences"), "user_002", preferencesData);
		memoryStore.putItem(preferencesItem);

		// 运行�?
		RunnableConfig config = RunnableConfig.builder()
				.threadId("combined_thread")
				.store(memoryStore)
				.build();

		// 第一轮对话（加载偏好并开始对话）
		graph.invoke(Map.of(
				"userId", "user_002",
				"messages", List.of(Map.of("role", "user", "content", "你好"))
		), config);

		// 第二轮对话（使用短期和长期记忆）
		graph.invoke(Map.of(
				"userId", "user_002",
				"messages", List.of(Map.of("role", "user", "content", "根据我的偏好给我一些建�?))
		), config);

		System.out.println("Combined memory example created");
	}

	public static void main(String[] args) {
		System.out.println("=== 内存管理示例 ===\n");

		try {
			// 示例 1: 添加短期内存（需�?ChatClient�?
			System.out.println("示例 1: 添加短期内存");
			System.out.println("注意: 此示例需�?ChatClient，跳过执�?);
			// addShortTermMemory(ChatClient.builder(...));
			System.out.println();

			// 示例 2: 使用 Store 实现长期内存
			System.out.println("示例 2: 使用 Store 实现长期内存");
			longTermMemoryWithDatabase();
			System.out.println();

			// 示例 3: 使用 Store 缓存实现长期内存
			System.out.println("示例 3: 使用 Store 缓存实现长期内存");
			longTermMemoryWithRedis();
			System.out.println();

			// 示例 4: 结合短期和长期内存（需�?ChatClient�?
			System.out.println("示例 4: 结合短期和长期内�?);
			System.out.println("注意: 此示例需�?ChatClient，跳过执�?);
			// combinedMemoryExample(ChatClient.builder(...));
			System.out.println();

			System.out.println("所有示例执行完�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

