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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Redis 检查点持久化示�?
 * 演示如何使用 Redis 数据库持久化工作流状�?
 */
public class CheckpointRedisExample {

	/**
	 * 初始�?RedisSaver
	 */
	public static RedisSaver createRedisSaver() {
		// 配置 Redisson 客户�?
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://localhost:6379");  // Redis 地址

		RedissonClient redisson = Redisson.create(config);
		return RedisSaver.builder().redisson(redisson).build();
	}

	/**
	 * 使用自定�?Redis 地址创建 RedisSaver
	 */
	public static RedisSaver createRedisSaver(String host, int port) {
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://" + host + ":" + port);

		RedissonClient redisson = Redisson.create(config);
		return RedisSaver.builder().redisson(redisson).build();
	}

	/**
	 * 完整示例: 使用 Redis 检查点持久�?
	 *
	 * @return
	 */
	public static void testCheckpointWithRedis(StateGraph stateGraph) throws Exception {
		// 初始�?Redis Saver
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://localhost:6379");

		RedissonClient redisson = Redisson.create(config);
		try {
			RedisSaver saver = RedisSaver.builder().redisson(redisson).build();

			SaverConfig saverConfig = SaverConfig.builder()
					.register(saver)
					.build();

			// 使用检查点编译�?
			CompiledGraph workflow = stateGraph.compile(
					CompileConfig.builder()
							.saverConfig(saverConfig)
							.build()
			);

			// 执行工作�?
			RunnableConfig runnableConfig = RunnableConfig.builder()
					.threadId("test-thread-1")
					.build();

			Map<String, Object> inputs = Map.of("input", "test1");
			OverAllState result = workflow.invoke(inputs, runnableConfig).orElseThrow();

			// 获取检查点历史
			List<StateSnapshot> history = (List<StateSnapshot>) workflow.getStateHistory(runnableConfig);

			System.out.println("检查点历史数量: " + history.size());

			// 获取最后保存的检查点
			StateSnapshot lastSnapshot = workflow.getState(runnableConfig);

			System.out.println("最后检查点节点: " + lastSnapshot.node());
			
		} finally {
			redisson.shutdown();
		}
	}

	/**
	 * �?Redis 重新加载检查点
	 *
	 * @return
	 */
	public static void reloadCheckpointFromRedis(StateGraph stateGraph) throws GraphStateException {
		// 创建新的 saver（重置缓存）
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://localhost:6379");

		RedissonClient redisson = Redisson.create(config);
		try {
			RedisSaver newSaver = RedisSaver.builder().redisson(redisson).build();
			
			SaverConfig newSaverConfig = SaverConfig.builder()
					.register(newSaver)
					.build();
			
			// 重新编译�?
			CompiledGraph reloadedWorkflow = stateGraph.compile(
					CompileConfig.builder()
							.saverConfig(newSaverConfig)
							.build()
			);
			
			// 使用相同�?threadId 获取历史
			RunnableConfig reloadConfig = RunnableConfig.builder()
					.threadId("test-thread-1")
					.build();
			
			Collection<StateSnapshot> reloadedHistory = reloadedWorkflow.getStateHistory(reloadConfig);
			
			System.out.println("重新加载的检查点历史数量: " + reloadedHistory.size());
		} finally {
			redisson.shutdown();
		}
		
	}

	/**
	 * 从特定检查点恢复
	 */
	public static void restoreFromCheckpoint(StateGraph stateGraph) throws GraphStateException{
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://localhost:6379");
		
		RedissonClient redisson = Redisson.create(config);
		try {
			RedisSaver newSaver = RedisSaver.builder().redisson(redisson).build();
			
			SaverConfig newSaverConfig = SaverConfig.builder()
					.register(newSaver)
					.build();
			
			// 重新编译�?
			CompiledGraph reloadedWorkflow = stateGraph.compile(
					CompileConfig.builder()
							.saverConfig(newSaverConfig)
							.build()
			);
			// 获取特定检查点
			RunnableConfig checkpointConfig = RunnableConfig.builder()
					.threadId("thread-id")
					.checkPointId("specific-checkpoint-id")
					.build();
			
			// 从该检查点继续
			reloadedWorkflow.invoke(Map.of(), checkpointConfig);
			System.out.println("从检查点恢复执行完成");
		}
		finally {
			redisson.shutdown();
		}
		
	}

	public static void main(String[] args) {
		System.out.println("=== Redis 检查点持久化示�?===\n");

		try {
			
			// 定义状态策�?
			KeyStrategyFactory keyStrategyFactory = () -> {
				Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
				keyStrategyMap.put("input", new ReplaceStrategy());
				keyStrategyMap.put("agent_1:prop1", new ReplaceStrategy());
				return keyStrategyMap;
			};
			
			// 定义节点
			var agent1 = node_async(state -> {
				System.out.println("agent_1 执行�?);
				return Map.of("agent_1:prop1", "agent_1:test");
			});
			
			// 构建�?
			StateGraph stateGraph = new StateGraph(keyStrategyFactory)
					.addNode("agent_1", agent1)
					.addEdge(START, "agent_1")
					.addEdge("agent_1", END);
			
			// 示例 1: 完整示例 - 使用 Redis 检查点持久�?
			System.out.println("示例 1: 使用 Redis 检查点持久�?);
			System.out.println("注意: 此示例需�?Redis 连接");
			testCheckpointWithRedis(stateGraph);
			System.out.println();

			// 示例 2: �?Redis 重新加载检查点
			System.out.println("示例 2: �?Redis 重新加载检查点");
			System.out.println("注意: 此示例需�?Redis 连接");
			reloadCheckpointFromRedis(stateGraph);
			System.out.println();

			// 示例 3: 从特定检查点恢复
			System.out.println("示例 3: 从特定检查点恢复");
			System.out.println("注意: 此示例需要有效的 CompiledGraph �?checkpointId");
			restoreFromCheckpoint(stateGraph);
			System.out.println();

			System.out.println("所有示例执行完�?);
			System.out.println("提示: 请配�?Redis 连接后运行完整示�?);
			System.out.println("提示: 需要添�?Redisson 依赖: org.redisson:redisson");
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

