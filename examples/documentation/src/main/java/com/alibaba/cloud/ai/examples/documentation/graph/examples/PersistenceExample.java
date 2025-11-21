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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 为图添加持久化（记忆）示例
 * 演示如何使用 Checkpointer 为 StateGraph 提供持久化记忆
 */
public class PersistenceExample {

	/**
	 * 不使用 Checkpointer 的示例
	 */
	public static CompiledGraph createGraphWithoutCheckpointer(ChatClient.Builder chatClientBuilder) throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			strategies.put("user_name", new ReplaceStrategy());
			strategies.put("context", new ReplaceStrategy());
			return strategies;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("agent", node_async(state -> {
					List<String> messages = (List<String>) state.value("messages").orElse(List.of());
					String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1);
					return Map.of("messages", "Response to: " + lastMessage);
				}))
				.addEdge(START, "agent")
				.addEdge("agent", END);

		return workflow.compile();
	}

	/**
	 * 添加持久化（记忆）
	 */
	public static CompiledGraph createGraphWithCheckpointer(ChatClient.Builder chatClientBuilder) throws GraphStateException {
		// 创建 Checkpointer
		var checkpointer = new MemorySaver();

		// 配置持久化
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(checkpointer)
						.build())
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("agent", node_async(state -> {
					List<String> messages = (List<String>) state.value("messages").orElse(List.of());
					String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1);
					return Map.of("messages", "Response to: " + lastMessage);
				}))
				.addEdge(START, "agent")
				.addEdge("agent", END);

		// 编译带持久化的 Graph
		return workflow.compile(compileConfig);
	}

	/**
	 * 测试带持久化的 Graph
	 */
	public static void testGraphWithPersistence(CompiledGraph persistentGraph) {
		// 创建运行配置（使用 threadId 标识会话）
		var config = RunnableConfig.builder()
				.threadId("user-alice-session")
				.build();

		// 第一次调用 - 介绍自己
		System.out.println("=== First call with persistence - Introduction ===");
		var result1 = persistentGraph.invoke(
				Map.of("messages", List.of("Hi, I'm Alice, nice to meet you")),
				config
		);

		List<String> messages1 = (List<String>) result1.get().data().get("messages");
		System.out.println("Response: " + messages1.get(messages1.size() - 1));

		// 第二次调用 - 询问名字（有持久化，可以记住）
		System.out.println("=== Second call with persistence - Ask name ===");
		var result2 = persistentGraph.invoke(
				Map.of("messages", List.of("What's my name?")),
				config
		);

		List<String> messages2 = (List<String>) result2.get().data().get("messages");
		System.out.println("Response: " + messages2.get(messages2.size() - 1));
	}

	/**
	 * 多会话隔离
	 */
	public static void multiSessionIsolation(CompiledGraph persistentGraph) {
		// Alice 的会话
		var aliceConfig = RunnableConfig.builder()
				.threadId("user-alice")
				.build();

		persistentGraph.invoke(Map.of("messages", List.of("Hi, I'm Alice")), aliceConfig);

		// Bob 的会话
		var bobConfig = RunnableConfig.builder()
				.threadId("user-bob")
				.build();

		persistentGraph.invoke(Map.of("messages", List.of("Hi, I'm Bob")), bobConfig);

		// Alice 询问名字 - 能记住
		var aliceResult = persistentGraph.invoke(
				Map.of("messages", List.of("What's my name?")),
				aliceConfig
		);
		System.out.println("Alice: " + aliceResult.get().data().get("messages"));

		// Bob 询问名字 - 也能记住
		var bobResult = persistentGraph.invoke(
				Map.of("messages", List.of("What's my name?")),
				bobConfig
		);
		System.out.println("Bob: " + bobResult.get().data().get("messages"));
	}

	/**
	 * 获取当前状态
	 */
	public static void getCurrentState(CompiledGraph graph) {
		RunnableConfig config = RunnableConfig.builder()
				.threadId("user-alice")
				.build();

		StateSnapshot snapshot = graph.getState(config);

		System.out.println("Current node: " + snapshot.node());
		System.out.println("Current state: " + snapshot.state());
		System.out.println("Next node: " + snapshot.next());
		System.out.println("Checkpoint ID: " + snapshot.config().checkPointId().orElse("N/A"));
	}

	/**
	 * 获取状态历史
	 */
	public static void getStateHistory(CompiledGraph graph) {
		RunnableConfig config = RunnableConfig.builder()
				.threadId("user-alice")
				.build();

		// 获取所有历史状态
		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		System.out.println("=== State History ===");
		for (int i = 0; i < history.size(); i++) {
			StateSnapshot h = history.get(i);
			System.out.println("Step " + i + ": node=" + h.node() +
					", messages count=" + ((List<?>) h.state().data().get("messages")).size());
		}
	}

	public static void main(String[] args) {
		System.out.println("=== 持久化示例 ===\n");

		try {
			// 示例 1: 不使用 Checkpointer 的示例（需要 ChatClient）
			System.out.println("示例 1: 不使用 Checkpointer 的示例");
			System.out.println("注意: 此示例需要 ChatClient，跳过执行");
			// CompiledGraph graphWithoutCheckpointer = createGraphWithoutCheckpointer(ChatClient.builder(...));
			System.out.println();

			// 示例 2: 添加持久化（需要 ChatClient）
			System.out.println("示例 2: 添加持久化");
			System.out.println("注意: 此示例需要 ChatClient，跳过执行");
			// CompiledGraph persistentGraph = createGraphWithCheckpointer(ChatClient.builder(...));
			System.out.println();

			// 示例 3: 测试带持久化的 Graph（需要 CompiledGraph）
			System.out.println("示例 3: 测试带持久化的 Graph");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// testGraphWithPersistence(persistentGraph);
			System.out.println();

			// 示例 4: 多会话隔离（需要 CompiledGraph）
			System.out.println("示例 4: 多会话隔离");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// multiSessionIsolation(persistentGraph);
			System.out.println();

			// 示例 5: 获取当前状态（需要 CompiledGraph）
			System.out.println("示例 5: 获取当前状态");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// getCurrentState(persistentGraph);
			System.out.println();

			// 示例 6: 获取状态历史（需要 CompiledGraph）
			System.out.println("示例 6: 获取状态历史");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// getStateHistory(persistentGraph);
			System.out.println();

			System.out.println("所有示例执行完成");
			System.out.println("提示: 请配置 ChatClient 后运行完整示例");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

