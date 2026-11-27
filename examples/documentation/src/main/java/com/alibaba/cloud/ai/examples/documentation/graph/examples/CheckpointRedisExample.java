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
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class CheckpointRedisExample {

	public static CompiledGraph createGraph(RedissonClient client) throws GraphStateException {
		var saver = RedisSaver.builder().redisson(client).build();
		var compile = CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build())
				.interruptAfter("prepare").build();
		return new StateGraph(() -> Map.of("steps", new AppendStrategy()))
				.addNode("prepare", node_async(state -> Map.of("steps", List.of("prepared"))))
				.addNode("commit", node_async(state -> Map.of("steps", List.of("committed"))))
				.addEdge(START, "prepare").addEdge("prepare", "commit").addEdge("commit", END)
				.compile(compile);
	}

	public static void reconnectAndResume(String address) throws GraphStateException {
		var thread = RunnableConfig.builder().threadId("redis-example-" + UUID.randomUUID()).build();
		var config = new Config();
		config.useSingleServer().setAddress(address);

		var initialClient = Redisson.create(config);
		try {
			createGraph(initialClient).stream(Map.of(), thread).blockLast(Duration.ofSeconds(10));
		}
		finally {
			initialClient.shutdown();
		}

		var newClient = Redisson.create(config);
		try {
			var graph = createGraph(newClient);
			System.out.println("Saved node: " + graph.getState(thread).node());
			var result = graph.invoke(Map.of(), thread.withResume()).orElseThrow();
			System.out.println("Steps: " + result.value("steps").orElseThrow());
		}
		finally {
			newClient.shutdown();
		}
	}

	public static void main(String[] args) throws GraphStateException {
		reconnectAndResume(args.length == 0 ? "redis://localhost:6379" : args[0]);
	}

}

