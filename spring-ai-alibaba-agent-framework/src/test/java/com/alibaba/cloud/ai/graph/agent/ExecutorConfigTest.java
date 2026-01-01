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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for executor configuration in agents.
 * Verifies that executor can be configured through builders and is properly
 * passed to RunnableConfig.
 */
class ExecutorConfigTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ToolCallbackResolver toolCallbackResolver;

	@Mock
	private ChatModel chatModel;

	private Executor customExecutor;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		customExecutor = Executors.newFixedThreadPool(4);
	}
	
	@AfterEach
	void tearDown() {
		if (customExecutor instanceof ExecutorService) {
			((ExecutorService) customExecutor).shutdown();
		}
	}

	@Test
	void testReactAgentExecutorConfiguration() throws Exception {
		// Create ReactAgent with executor
		ReactAgent agent = ReactAgent.builder()
			.name("testAgent")
			.description("Test agent")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.executor(customExecutor)
			.build();

		assertNotNull(agent);
		
		// Verify executor is set in agent using reflection
		RunnableConfig config = buildNonStreamConfig(agent, null);
		assertNotNull(config);
		
		// Verify executor is in metadata
		assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in metadata");
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in metadata should match configured executor");
	}

	@Test
	void testReactAgentWithoutExecutor() throws Exception {
		// Create ReactAgent without executor
		ReactAgent agent = ReactAgent.builder()
			.name("testAgent")
			.description("Test agent")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		assertNotNull(agent);
		
		// Verify no executor in metadata when not configured
		RunnableConfig config = buildNonStreamConfig(agent, null);
		assertNotNull(config);
		
		// When executor is not set, metadata should not contain it
		assertFalse(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should not be present when not configured");
	}

	@Test
	void testParallelAgentExecutorConfiguration() throws Exception {
		// Create sub-agents
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		// Create ParallelAgent with executor
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallelAgent")
			.description("Parallel agent with executor")
			.subAgents(List.of(agent1, agent2))
			.executor(customExecutor)
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.build();

		assertNotNull(parallelAgent);
		
		// Verify executor is set and passed to RunnableConfig
		RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
		assertNotNull(config);
		
		assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in metadata");
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in metadata should match configured executor");
	}

	@Test
	void testParallelAgentExecutorWithStreamConfig() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallelAgent")
			.description("Parallel agent with executor")
			.subAgents(List.of(agent1, agent2))
			.executor(customExecutor)
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.build();

		// Test with stream config
		RunnableConfig streamConfig = buildStreamConfig(parallelAgent, null);
		assertNotNull(streamConfig);
		
		assertTrue(streamConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in stream config metadata");
		assertEquals(customExecutor, 
			streamConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in stream config metadata should match configured executor");
	}

	@Test
	void testSequentialAgentExecutorConfiguration() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		SequentialAgent sequentialAgent = SequentialAgent.builder()
			.name("sequentialAgent")
			.description("Sequential agent with executor")
			.subAgents(List.of(agent1, agent2))
			.executor(customExecutor)
			.build();

		assertNotNull(sequentialAgent);
		
		RunnableConfig config = buildNonStreamConfig(sequentialAgent, null);
		assertNotNull(config);
		
		assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in metadata");
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in metadata should match configured executor");
	}

	@Test
	void testLoopAgentExecutorConfiguration() throws Exception {
		ReactAgent subAgent = createMockAgent("subAgent", "output");

		LoopAgent loopAgent = LoopAgent.builder()
			.name("loopAgent")
			.description("Loop agent with executor")
			.subAgent(subAgent)
			.executor(customExecutor)
			.loopStrategy(LoopMode.count(3))
			.build();

		assertNotNull(loopAgent);
		
		RunnableConfig config = buildNonStreamConfig(loopAgent, null);
		assertNotNull(config);
		
		assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in metadata");
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in metadata should match configured executor");
	}

	@Test
	void testLlmRoutingAgentExecutorConfiguration() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routingAgent")
			.description("Routing agent with executor")
			.subAgents(List.of(agent1, agent2))
			.model(chatModel)
			.executor(customExecutor)
			.build();

		assertNotNull(routingAgent);
		
		RunnableConfig config = buildNonStreamConfig(routingAgent, null);
		assertNotNull(config);
		
		assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in metadata");
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in metadata should match configured executor");
	}

	@Test
	void testExecutorWithDifferentExecutorTypes() throws Exception {
		// Test with ForkJoinPool
		Executor forkJoinExecutor = ForkJoinPool.commonPool();
		
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallelAgent")
			.description("Parallel agent with ForkJoinPool")
			.subAgents(List.of(agent1, agent2))
			.executor(forkJoinExecutor)
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.build();

		RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
		assertEquals(forkJoinExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"ForkJoinPool executor should be correctly stored");

		// Test with ThreadPoolExecutor
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
		
		ParallelAgent parallelAgent2 = ParallelAgent.builder()
			.name("parallelAgent2")
			.description("Parallel agent with ThreadPoolExecutor")
			.subAgents(List.of(agent1, agent2))
			.executor(threadPoolExecutor)
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.build();

		RunnableConfig config2 = buildNonStreamConfig(parallelAgent2, null);
		assertEquals(threadPoolExecutor, 
			config2.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"ThreadPoolExecutor should be correctly stored");
		
		// Cleanup
		threadPoolExecutor.shutdown();
	}

	@Test
	void testExecutorConfigurationChaining() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		// Test fluent interface chaining
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallelAgent")
			.description("Test chaining")
			.executor(customExecutor)
			.subAgents(List.of(agent1, agent2))
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.maxConcurrency(3)
			.build();

		assertNotNull(parallelAgent);
		assertEquals("parallelAgent", parallelAgent.name());
		assertEquals(3, parallelAgent.maxConcurrency());
		
		RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor should be correctly configured through chaining");
	}

	@Test
	void testExecutorWithExistingRunnableConfig() throws Exception {
		ReactAgent agent = ReactAgent.builder()
			.name("testAgent")
			.description("Test agent")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.executor(customExecutor)
			.build();

		// Create an existing RunnableConfig
		RunnableConfig existingConfig = RunnableConfig.builder()
			.threadId("test-thread")
			.build();

		// Build config with existing config
		RunnableConfig newConfig = buildNonStreamConfig(agent, existingConfig);
		
		// Verify existing config properties are preserved
		assertTrue(newConfig.threadId().isPresent());
		assertEquals("test-thread", newConfig.threadId().get());
		
		// Verify executor is added
		assertTrue(newConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent());
		assertEquals(customExecutor, 
			newConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get());
	}

	@Test
	void testRunnableConfigDefaultParallelExecutorMethod() {
		// Test RunnableConfig.Builder.defaultParallelExecutor method directly
		RunnableConfig config = RunnableConfig.builder()
			.defaultParallelExecutor(customExecutor)
			.build();

		assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present");
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor should match");
	}

	@Test
	void testRunnableConfigDefaultParallelExecutorKeyConstant() {
		// Verify the constant is defined correctly
		assertNotNull(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY);
		assertEquals("_DEFAULT_PARALLEL_EXECUTOR_", RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY);
	}

	/**
	 * Helper method to call protected buildNonStreamConfig using reflection.
	 */
	private RunnableConfig buildNonStreamConfig(Agent agent, RunnableConfig config) throws Exception {
		Method method = Agent.class.getDeclaredMethod("buildNonStreamConfig", RunnableConfig.class);
		method.setAccessible(true);
		return (RunnableConfig) method.invoke(agent, config);
	}

	/**
	 * Helper method to call protected buildStreamConfig using reflection.
	 */
	private RunnableConfig buildStreamConfig(Agent agent, RunnableConfig config) throws Exception {
		Method method = Agent.class.getDeclaredMethod("buildStreamConfig", RunnableConfig.class);
		method.setAccessible(true);
		return (RunnableConfig) method.invoke(agent, config);
	}

	private ReactAgent createMockAgent(String name, String outputKey) throws Exception {
		return ReactAgent.builder()
			.name(name)
			.description("Mock agent")
			.outputKey(outputKey)
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();
	}

}

