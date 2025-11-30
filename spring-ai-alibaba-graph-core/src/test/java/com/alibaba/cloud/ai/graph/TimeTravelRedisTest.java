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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
class TimeTravelRedisTest {

	private static boolean isCI() {
		return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
	}

	@Container
	private static final GenericContainer<?> redisContainer = new GenericContainer<>(
			DockerImageName.parse("valkey/valkey:8.1.2"))
			.withExposedPorts(6379);

	static RedissonClient redisson;
	static RedisSaver redisSaver;
	static CompiledGraph graph;

	@BeforeAll
	static void setup() throws GraphStateException {
		redisContainer.start();
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379));
		redisson = Redisson.create(config);
		redisSaver = new RedisSaver(redisson);

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			strategies.put("step", new ReplaceStrategy());
			return strategies;
		};

		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("step1", node_async(state -> Map.of("messages", "Step 1", "step", 1)))
				.addNode("step2", node_async(state -> Map.of("messages", "Step 2", "step", 2)))
				.addNode("step3", node_async(state -> Map.of("messages", "Step 3", "step", 3)))
				.addEdge(START, "step1")
				.addEdge("step1", "step2")
				.addEdge("step2", "step3")
				.addEdge("step3", END);

		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(redisSaver)
						.build())
				.build();

		graph = stateGraph.compile(compileConfig);
	}

	@AfterAll
	static void tearDown() {
		if (redisson != null) {
			redisson.shutdown();
		}
	}

	@BeforeEach
	void cleanupRedis() {
		var config1 = RunnableConfig.builder().threadId("test-time-travel").build();
		redisSaver.clear(config1);
		var config2 = RunnableConfig.builder().threadId("test-branch").build();
		redisSaver.clear(config2);
	}

	@Test
	void testTimeTravelBasic() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		graph.invoke(Map.of("query", "First execution"), config);

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
		assertNotNull(history);
		assertFalse(history.isEmpty());

		assertTrue(history.size() >= 4, "Should have at least 4 states, actual: " + history.size());

		boolean hasStep1 = history.stream().anyMatch(s -> "step1".equals(s.node()));
		boolean hasStep2 = history.stream().anyMatch(s -> "step2".equals(s.node()));
		boolean hasStep3 = history.stream().anyMatch(s -> "step3".equals(s.node()));

		assertTrue(hasStep1);
		assertTrue(hasStep2);
		assertTrue(hasStep3);
	}

	@Test
	void testTimeTravelAndResume() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		graph.invoke(Map.of("query", "First execution"), config);

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
		assertTrue(history.size() >= 2);

		StateSnapshot step1Snapshot = history.stream()
				.filter(s -> "step1".equals(s.node()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Cannot find step1 snapshot"));

		assertNotNull(step1Snapshot);
		assertTrue(step1Snapshot.config().checkPointId().isPresent());

		var resumeConfig = RunnableConfig.builder()
				.threadId("test-time-travel")
				.checkPointId(step1Snapshot.config().checkPointId().get())
				.build();

		assertDoesNotThrow(() -> graph.invoke(Map.of("query", "Resume from step1"), resumeConfig));

		List<StateSnapshot> newHistory = (List<StateSnapshot>) graph.getStateHistory(config);
		assertTrue(newHistory.size() > history.size());
	}

	@Test
	void testBranchCreation() {
		var mainConfig = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		graph.invoke(Map.of("query", "Main execution"), mainConfig);

		List<StateSnapshot> mainHistory = (List<StateSnapshot>) graph.getStateHistory(mainConfig);
		assertTrue(mainHistory.size() >= 2);

		StateSnapshot step2Snapshot = mainHistory.stream()
				.filter(s -> "step2".equals(s.node()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Cannot find step2 snapshot"));

		var branchConfig = RunnableConfig.builder()
				.threadId("test-branch")
				.checkPointId(step2Snapshot.config().checkPointId().get())
				.build();

		assertDoesNotThrow(() -> graph.invoke(Map.of("query", "Branch execution"), branchConfig));

		List<StateSnapshot> branchHistory = (List<StateSnapshot>) graph.getStateHistory(
				RunnableConfig.builder().threadId("test-branch").build());
		assertNotNull(branchHistory);
		assertFalse(branchHistory.isEmpty());

		assertNotEquals(mainHistory.size(), branchHistory.size());
	}

	@Test
	void testMultipleTimeTravels() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		graph.invoke(Map.of("query", "First"), config);
		graph.invoke(Map.of("query", "Second"), config);

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
		int initialSize = history.size();
		assertTrue(initialSize >= 8, "Should have at least 8 states (2 executions x 4 steps)");

		List<StateSnapshot> step1Snapshots = history.stream()
				.filter(s -> "step1".equals(s.node()))
				.toList();
		assertTrue(step1Snapshots.size() >= 2);

		StateSnapshot firstStep1 = step1Snapshots.get(step1Snapshots.size() - 1);

		var resumeConfig = RunnableConfig.builder()
				.threadId("test-time-travel")
				.checkPointId(firstStep1.config().checkPointId().get())
				.build();

		assertDoesNotThrow(() -> graph.invoke(Map.of("query", "Third from first step1"), resumeConfig));

		List<StateSnapshot> finalHistory = (List<StateSnapshot>) graph.getStateHistory(config);
		assertTrue(finalHistory.size() > initialSize);
	}

	@Test
	void testTimeTravelFromDifferentNodes() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		graph.invoke(Map.of("query", "Test"), config);

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		StateSnapshot step1 = history.stream()
				.filter(s -> "step1".equals(s.node()))
				.findFirst()
				.orElseThrow();

		var step1Config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.checkPointId(step1.config().checkPointId().get())
				.build();

		assertDoesNotThrow(() -> graph.invoke(Map.of("query", "From step1"), step1Config));

		StateSnapshot step2 = history.stream()
				.filter(s -> "step2".equals(s.node()))
				.findFirst()
				.orElseThrow();

		var step2Config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.checkPointId(step2.config().checkPointId().get())
				.build();

		assertDoesNotThrow(() -> graph.invoke(Map.of("query", "From step2"), step2Config));

		StateSnapshot step3 = history.stream()
				.filter(s -> "step3".equals(s.node()))
				.findFirst()
				.orElseThrow();

		var step3Config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.checkPointId(step3.config().checkPointId().get())
				.build();

		assertDoesNotThrow(() -> graph.invoke(Map.of("query", "From step3"), step3Config));
	}

	@Test
	void testInvalidCheckpointId() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.checkPointId("non-existent-checkpoint-id")
				.build();

		assertThrows(IllegalStateException.class, () -> graph.invoke(Map.of("query", "Test"), config));
	}

	@Test
	void testTimeTravelWithoutHistory() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		assertTrue(history.isEmpty());
	}

	@Test
	void testStateCorrectness() {
		var config = RunnableConfig.builder()
				.threadId("test-time-travel")
				.build();

		graph.invoke(Map.of("query", "Test"), config);

		List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

		StateSnapshot step1 = history.stream()
				.filter(s -> "step1".equals(s.node()))
				.findFirst()
				.orElseThrow();

		Map<String, Object> step1State = step1.state().data();
		assertNotNull(step1State);

		assertTrue(step1State.containsKey("step"));
		assertEquals(1, step1State.get("step"));

		StateSnapshot step3 = history.stream()
				.filter(s -> "step3".equals(s.node()))
				.findFirst()
				.orElseThrow();

		Map<String, Object> step3State = step3.state().data();
		assertNotNull(step3State);
		assertTrue(step3State.containsKey("step"));
		assertEquals(3, step3State.get("step"));

		assertTrue(step3State.containsKey("messages"));
		Object messagesObj = step3State.get("messages");
		assertTrue(messagesObj instanceof List);

		@SuppressWarnings("unchecked")
		List<String> messages = (List<String>) messagesObj;
		assertEquals(3, messages.size());
		assertTrue(messages.contains("Step 1"));
		assertTrue(messages.contains("Step 2"));
		assertTrue(messages.contains("Step 3"));
	}
}
