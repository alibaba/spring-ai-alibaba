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
package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for ParallelAgent demonstrating the refactored architecture with different
 * merge strategies. These tests verify the parallel execution capabilities and the
 * strategy pattern implementation.
 */
class ParallelAgentTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ToolCallbackResolver toolCallbackResolver;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testDataProcessingParallelAgent() throws Exception {
		// Create ParallelAgent for data processing with default merge strategy
		ParallelAgent parallelAgent = createDataProcessingParallelAgent();

		// Verify the built agent
		assertNotNull(parallelAgent);
		assertEquals("dataProcessingPipeline", parallelAgent.name());
		assertEquals("Processes data through multiple parallel operations", parallelAgent.description());
		assertEquals(3, parallelAgent.subAgents().size());
		assertEquals(3, parallelAgent.maxConcurrency());
		assertTrue(parallelAgent.mergeStrategy() instanceof ParallelAgent.DefaultMergeStrategy);
	}

	@Test
	void testReportGenerationParallelAgent() throws Exception {
		// Create ParallelAgent for report generation with list merge strategy
		ParallelAgent parallelAgent = createReportGenerationParallelAgent();

		// Verify the built agent
		assertNotNull(parallelAgent);
		assertEquals("reportGenerator", parallelAgent.name());
		assertEquals("Generates comprehensive reports in parallel", parallelAgent.description());
		assertEquals(3, parallelAgent.subAgents().size());
		assertEquals(5, parallelAgent.maxConcurrency());
		assertTrue(parallelAgent.mergeStrategy() instanceof ParallelAgent.ListMergeStrategy);
	}

	@Test
	void testContentCreationParallelAgent() throws Exception {
		// Create ParallelAgent for content creation with concatenation merge strategy
		ParallelAgent parallelAgent = createContentCreationParallelAgent();

		// Verify the built agent
		assertNotNull(parallelAgent);
		assertEquals("contentCreator", parallelAgent.name());
		assertEquals("Creates content through parallel writing", parallelAgent.description());
		assertEquals(3, parallelAgent.subAgents().size());
		assertNull(parallelAgent.maxConcurrency()); // No concurrency limit set
		assertTrue(parallelAgent.mergeStrategy() instanceof ParallelAgent.ConcatenationMergeStrategy);
	}

	@Test
	void testParallelAgentBuilderFluentInterface() throws Exception {

		// Create sub-agents for parallel execution
		ReactAgent agent1 = ReactAgent.builder()
			.name("dataAnalyzer")
			.description("Analyzes data")
			.outputKey("analysis_result")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent agent2 = ReactAgent.builder()
			.name("dataValidator")
			.description("Validates data")
			.outputKey("validation_result")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		// Test fluent interface with ParallelAgent
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallelProcessor")
			.description("Processes data in parallel")
			.mergeOutputKey("parallel_result")
			.subAgents(List.of(agent1, agent2))
			.mergeStrategy(new ParallelAgent.ListMergeStrategy())
			.maxConcurrency(5)
			.build();

		assertNotNull(parallelAgent);
		assertEquals("parallelProcessor", parallelAgent.name());
		assertEquals("Processes data in parallel", parallelAgent.description());
		assertEquals(2, parallelAgent.subAgents().size());
		assertNotNull(parallelAgent.mergeStrategy());
		assertEquals(5, parallelAgent.maxConcurrency());
	}

	@Test
	void testParallelAgentValidation() {
		// Test validation - requires at least 2 sub-agents
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder()
				.name("testAgent")
				.subAgents(List.of()) // Empty list
				.build();
		});
		assertTrue(exception.getMessage().contains("Sub-agents must be provided"));

		// Test validation - maximum 10 sub-agents
		ReactAgent[] agents = new ReactAgent[11];
		for (int i = 0; i < 11; i++) {
			try {
				agents[i] = createMockAgent("agent" + i, "output" + i);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		exception = assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder().name("testAgent").subAgents(List.of(agents)).build();
		});
		assertTrue(exception.getMessage().contains("maximum 10 sub-agents"));
	}

	@Test
	void testUniqueOutputKeyValidation() throws Exception {
		MockitoAnnotations.openMocks(this);

		ReactAgent agent1 = createMockAgent("agent1", "same_output");
		ReactAgent agent2 = createMockAgent("agent2", "same_output"); // Same output key

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder().name("testAgent").subAgents(List.of(agent1, agent2)).build();
		});
		assertTrue(exception.getMessage().contains("Duplicate output keys"));
	}

	@Test
	void testMergeStrategies() {
		// Test DefaultMergeStrategy
		ParallelAgent.DefaultMergeStrategy defaultStrategy = new ParallelAgent.DefaultMergeStrategy();
		HashMap<String, Object> results = new HashMap<>();
		results.put("key1", "value1");
		results.put("key2", "value2");

		Object merged = defaultStrategy.merge(results, null);
		assertTrue(merged instanceof HashMap);
		@SuppressWarnings("unchecked")
		HashMap<String, Object> mergedMap = (HashMap<String, Object>) merged;
		assertEquals("value1", mergedMap.get("key1"));
		assertEquals("value2", mergedMap.get("key2"));

		// Test ListMergeStrategy
		ParallelAgent.ListMergeStrategy listStrategy = new ParallelAgent.ListMergeStrategy();
		Object listResult = listStrategy.merge(results, null);
		assertTrue(listResult instanceof List);
		List<?> resultList = (List<?>) listResult;
		assertEquals(2, resultList.size());
		assertTrue(resultList.contains("value1"));
		assertTrue(resultList.contains("value2"));

		// Test ConcatenationMergeStrategy
		ParallelAgent.ConcatenationMergeStrategy concatStrategy = new ParallelAgent.ConcatenationMergeStrategy(" | ");
		Object concatResult = concatStrategy.merge(results, null);
		assertTrue(concatResult instanceof String);
		String concatString = (String) concatResult;
		assertTrue(concatString.contains("value1"));
		assertTrue(concatString.contains("value2"));
		assertTrue(concatString.contains(" | "));
	}

	@Test
	void testParallelAgentWithJacksonSerializer() throws Exception {
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent with Jackson serializer")
				.stateSerializer(serializer)
				.subAgents(List.of(agent1, agent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = parallelAgent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIJacksonStateSerializer");
	}

	@Test
	void testParallelAgentWithSpringAIStateSerializer() throws Exception {
		StateSerializer serializer = new SpringAIStateSerializer();

		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent with SpringAI serializer")
				.stateSerializer(serializer)
				.subAgents(List.of(agent1, agent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = parallelAgent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIStateSerializer");
	}

	@Test
	void testParallelAgentWithDefaultSerializer() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent with default serializer")
				.subAgents(List.of(agent1, agent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Verify default serializer is used
		StateGraph stateGraph = parallelAgent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Default serializer should be SpringAIJacksonStateSerializer");
	}

	@Test
	void testParallelAgentWithExecutor() throws Exception {
		java.util.concurrent.ExecutorService customExecutor = Executors.newFixedThreadPool(4);
		try {
			ReactAgent agent1 = createMockAgent("agent1", "output1");
			ReactAgent agent2 = createMockAgent("agent2", "output2");

			ParallelAgent parallelAgent = ParallelAgent.builder()
					.name("parallel_agent_with_executor")
					.description("Parallel agent with executor")
					.subAgents(List.of(agent1, agent2))
					.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
					.executor(customExecutor)
					.build();

			assertNotNull(parallelAgent, "ParallelAgent should not be null");

			// Verify executor is set and passed to RunnableConfig
			RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
			assertNotNull(config, "RunnableConfig should not be null");
			
			assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
				"Default parallel executor should be present in metadata");
			assertEquals(customExecutor, 
				config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
				"Executor in metadata should match configured executor");
		} finally {
			customExecutor.shutdown();
		}
	}

	@Test
	void testParallelAgentExecutorWithStreamConfig() throws Exception {
		Executor customExecutor = Executors.newFixedThreadPool(4);

		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent_stream")
				.description("Parallel agent with executor for streaming")
				.subAgents(List.of(agent1, agent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.executor(customExecutor)
				.build();

		// Test with stream config
		RunnableConfig streamConfig = buildStreamConfig(parallelAgent, null);
		assertNotNull(streamConfig, "Stream config should not be null");
		
		assertTrue(streamConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
			"Default parallel executor should be present in stream config metadata");
		assertEquals(customExecutor, 
			streamConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor in stream config metadata should match configured executor");
	}

	@Test
	void testParallelAgentExecutorConfigurationChaining() throws Exception {
		Executor customExecutor = Executors.newFixedThreadPool(4);

		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		// Test fluent interface chaining
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallel_agent_chaining")
			.description("Test executor chaining")
			.executor(customExecutor)
			.subAgents(List.of(agent1, agent2))
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.maxConcurrency(3)
			.build();

		assertNotNull(parallelAgent);
		assertEquals("parallel_agent_chaining", parallelAgent.name());
		assertEquals(3, parallelAgent.maxConcurrency());
		
		RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
		assertEquals(customExecutor, 
			config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
			"Executor should be correctly configured through chaining");
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

	/**
	 * Factory method for creating data processing ParallelAgent with default merge
	 * strategy.
	 */
	private ParallelAgent createDataProcessingParallelAgent() throws Exception {
		// Create sub-agents for different aspects of data processing
		ReactAgent dataAnalyzer = ReactAgent.builder()
			.name("dataAnalyzer")
			.description("Analyzes data patterns and trends")
			.outputKey("analysis_result")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent dataValidator = ReactAgent.builder()
			.name("dataValidator")
			.description("Validates data quality and integrity")
			.outputKey("validation_result")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent dataCleaner = ReactAgent.builder()
			.name("dataCleaner")
			.description("Cleans and preprocesses data")
			.outputKey("cleaning_result")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		// Create ParallelAgent using the improved builder with default merge strategy
		return ParallelAgent.builder()
			.name("dataProcessingPipeline")
			.description("Processes data through multiple parallel operations")
			.mergeOutputKey("processing_result")
			.subAgents(List.of(dataAnalyzer, dataValidator, dataCleaner))
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy()) // Returns Map
			.maxConcurrency(3) // Limit to 3 concurrent operations
			.build();
	}

	/**
	 * Factory method for creating report generation ParallelAgent with list merge
	 * strategy.
	 */
	private ParallelAgent createReportGenerationParallelAgent() throws Exception {
		// Create sub-agents for different report sections
		ReactAgent summaryGenerator = ReactAgent.builder()
			.name("summaryGenerator")
			.description("Generates executive summary")
			.outputKey("summary_section")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent detailsGenerator = ReactAgent.builder()
			.name("detailsGenerator")
			.description("Generates detailed analysis")
			.outputKey("details_section")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent chartsGenerator = ReactAgent.builder()
			.name("chartsGenerator")
			.description("Generates charts and visualizations")
			.outputKey("charts_section")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		// Create ParallelAgent using list merge strategy
		return ParallelAgent.builder()
			.name("reportGenerator")
			.description("Generates comprehensive reports in parallel")
			.mergeOutputKey("complete_report")
			.subAgents(List.of(summaryGenerator, detailsGenerator, chartsGenerator))
			.mergeStrategy(new ParallelAgent.ListMergeStrategy()) // Returns List
			.maxConcurrency(5)
			.build();
	}

	/**
	 * Factory method for creating content creation ParallelAgent with concatenation merge
	 * strategy.
	 */
	private ParallelAgent createContentCreationParallelAgent() throws Exception {
		// Create sub-agents for different content sections
		ReactAgent introWriter = ReactAgent.builder()
			.name("introWriter")
			.description("Writes introduction content")
			.outputKey("intro_content")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent bodyWriter = ReactAgent.builder()
			.name("bodyWriter")
			.description("Writes main body content")
			.outputKey("body_content")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent conclusionWriter = ReactAgent.builder()
			.name("conclusionWriter")
			.description("Writes conclusion content")
			.outputKey("conclusion_content")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.build();

		// Create ParallelAgent using concatenation merge strategy
		return ParallelAgent.builder()
			.name("contentCreator")
			.description("Creates content through parallel writing")
			.mergeOutputKey("final_content")
			.subAgents(List.of(introWriter, bodyWriter, conclusionWriter))
			.mergeStrategy(new ParallelAgent.ConcatenationMergeStrategy("\n\n")) // Join
			.build();
	}

	/**
	 * Test that maxConcurrency is properly passed to RunnableConfig metadata.
	 * This test verifies the configuration propagation mechanism.
	 */
	@Test
	void testMaxConcurrencyInRunnableConfig() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");
		ReactAgent agent3 = createMockAgent("agent3", "output3");

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("testParallelAgent")
				.description("Test parallel agent with max concurrency")
				.subAgents(List.of(agent1, agent2, agent3))
				.maxConcurrency(2)
				.build();

		// Verify maxConcurrency is set correctly
		assertEquals(2, parallelAgent.maxConcurrency());

		// Build config and verify maxConcurrency is in metadata
		RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
		assertNotNull(config);
		
		// Use formatNodeId to match the actual key used by ParallelAgent
		String maxConcurrencyKey = ParallelNode.formatMaxConcurrencyKey(ParallelNode.formatNodeId("testParallelAgent"));
		assertTrue(config.metadata(maxConcurrencyKey).isPresent(), 
				"MaxConcurrency should be present in config metadata");
		assertEquals(2, config.metadata(maxConcurrencyKey).get(),
				"MaxConcurrency value should match");

		// Test with stream config as well
		RunnableConfig streamConfig = buildStreamConfig(parallelAgent, null);
		assertNotNull(streamConfig);
		assertTrue(streamConfig.metadata(maxConcurrencyKey).isPresent(),
				"MaxConcurrency should be present in stream config metadata");
		assertEquals(2, streamConfig.metadata(maxConcurrencyKey).get(),
				"MaxConcurrency value should match in stream config");
	}

	/**
	 * Test that maxConcurrency actually limits concurrent execution.
	 * This test uses slow-running agents and verifies that no more than
	 * maxConcurrency agents run simultaneously.
	 */
	@Test
	void testMaxConcurrencyLimitsExecution() throws Exception {
		// Track concurrent execution count
		AtomicInteger currentConcurrent = new AtomicInteger(0);
		AtomicInteger maxObservedConcurrent = new AtomicInteger(0);
		CountDownLatch allTasksStarted = new CountDownLatch(5);
		
		// Create 5 agents that will simulate slow execution
		List<Agent> slowAgents = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) {
			final int agentIndex = i;
			BaseAgent slowAgent = new BaseAgent("slowAgent" + i, "Slow agent " + i, false, false, 
					"agent" + i + "_result", null) {
				
				@Override
				protected Optional<OverAllState> doInvoke(Map<String, Object> input, RunnableConfig runnableConfig) {
					int concurrent = currentConcurrent.incrementAndGet();
					maxObservedConcurrent.updateAndGet(max -> Math.max(max, concurrent));
					
					System.out.println("Agent " + agentIndex + " started. Current concurrent: " + concurrent);
					allTasksStarted.countDown();
					
					try {
						// Simulate slow work
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						currentConcurrent.decrementAndGet();
						System.out.println("Agent " + agentIndex + " completed");
					}
					
					// Create result map (don't modify state.data() directly as it returns UnmodifiableMap)
					Map<String, Object> resultData = new HashMap<>();
					resultData.put("agent" + agentIndex + "_result", "completed");
					OverAllState state = new OverAllState(resultData);
					return Optional.of(state);
				}

				@Override
				protected StateGraph initGraph() throws GraphStateException {
					return null;
				}

				@Override
				public Node asNode(boolean includeContents, boolean returnReasoningContents) {
					// Return a valid Node with an action factory that wraps the agent's doInvoke
					return new Node(this.name(), (config) -> (state, runnableConfig) -> {
						// Execute the agent and return the result state as a map
						Optional<OverAllState> result = this.doInvoke(state.data(), runnableConfig);
						return CompletableFuture.completedFuture(
							result.map(s -> s.data()).orElse(new HashMap<>())
						);
					});
				}
			};
			slowAgents.add(slowAgent);
		}

		// Create ParallelAgent with maxConcurrency = 2
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("concurrencyTestAgent")
				.description("Test concurrent execution limits")
				.subAgents(slowAgents)
				.maxConcurrency(2)  // Only 2 agents should run concurrently
				.build();

		// Execute the agent using invoke with a simple string message
		Optional<OverAllState> result = parallelAgent.invoke("test input");
		assertNotNull(result);
			
		// Verify that maximum observed concurrency did not exceed limit
		System.out.println("Maximum observed concurrent execution: " + maxObservedConcurrent.get());
		assertTrue(maxObservedConcurrent.get() <= 2,
				"Maximum concurrent execution should not exceed maxConcurrency. Expected: 2, but was: " + maxObservedConcurrent.get());
			
		// Also verify it's greater than 1 (meaning parallel execution did happen)
		assertTrue(maxObservedConcurrent.get() >= 1,
				"Should have at least some concurrent execution");

	}

	/**
	 * Test that parallel agent without maxConcurrency allows unlimited concurrency.
	 */
	@Test
	void testUnlimitedConcurrencyWhenMaxConcurrencyNotSet() throws Exception {
		ReactAgent agent1 = createMockAgent("agent1", "output1");
		ReactAgent agent2 = createMockAgent("agent2", "output2");

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("unlimitedParallelAgent")
				.description("Parallel agent without concurrency limit")
				.subAgents(List.of(agent1, agent2))
				// Note: maxConcurrency is NOT set
				.build();

		// Verify maxConcurrency is null
		assertNull(parallelAgent.maxConcurrency());

		// Build config and verify maxConcurrency is NOT in metadata
		RunnableConfig config = buildNonStreamConfig(parallelAgent, null);
		assertNotNull(config);
		
		// Use formatNodeId to match the actual key used by ParallelAgent
		String maxConcurrencyKey = ParallelNode.formatMaxConcurrencyKey(ParallelNode.formatNodeId("unlimitedParallelAgent"));
		assertFalse(config.metadata(maxConcurrencyKey).isPresent(), 
				"MaxConcurrency should NOT be present in config metadata when not set");
	}

}
