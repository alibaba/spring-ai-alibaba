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
package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategyRegistry;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.SequentialGraphBuildingStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the refactored FlowAgent architecture demonstrating the improved design
 * patterns. These tests verify that the new architecture provides extensibility,
 * consistency, and type safety.
 */
class FlowAgentArchitectureTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatModel chatModel;

	@Mock
	private ToolCallbackResolver resolver;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testSequentialAgentBuilderPattern() throws Exception {
		// Create a sub-agent
		ReactAgent subAgent = createMockReactAgent("dataProcessor", "processed_data");

		// Test that SequentialAgent uses the unified builder pattern
		SequentialAgent agent = SequentialAgent.builder()
			.name("sequentialWorkflow")
			.description("A sequential workflow")
			.outputKey("final_result")
			.inputKey("initial_data")
			.subAgents(List.of(subAgent))
			.state(() -> createDefaultStrategies())
			.build();

		// Verify agent properties
		assertNotNull(agent);
		assertEquals("sequentialWorkflow", agent.name());
		assertEquals("A sequential workflow", agent.description());
		assertEquals("final_result", agent.outputKey());
		assertEquals("initial_data", agent.inputKey());
		assertEquals(1, agent.subAgents().size());
	}

	@Test
	void testLlmRoutingAgentBuilderPattern() throws Exception {
		// Create sub-agents
		ReactAgent agent1 = createMockReactAgent("analysisAgent", "analysis_result");
		ReactAgent agent2 = createMockReactAgent("reportAgent", "report_result");

		// Test that LlmRoutingAgent uses the unified builder pattern with LLM-specific
		// features
		LlmRoutingAgent agent = LlmRoutingAgent.builder()
			.name("intelligentRouter")
			.description("Routes tasks intelligently")
			.outputKey("routed_result")
			.inputKey("task_description")
			.subAgents(List.of(agent1, agent2))
			.model(chatModel) // LLM-specific configuration
			.state(() -> createDefaultStrategies())
			.build();

		// Verify agent properties
		assertNotNull(agent);
		assertEquals("intelligentRouter", agent.name());
		assertEquals("Routes tasks intelligently", agent.description());
		assertEquals("routed_result", agent.outputKey());
		assertEquals("task_description", agent.inputKey());
		assertEquals(2, agent.subAgents().size());
	}

	@Test
	void testParallelAgentBuilderPattern() throws Exception {
		// Create sub-agents
		ReactAgent agent1 = createMockReactAgent("dataAnalyzer", "analysis_result");
		ReactAgent agent2 = createMockReactAgent("dataValidator", "validation_result");
		ReactAgent agent3 = createMockReactAgent("dataCleaner", "cleaning_result");

		// Test that ParallelAgent uses the unified builder pattern with parallel-specific
		// features
		ParallelAgent agent = ParallelAgent.builder()
			.name("dataProcessingPipeline")
			.description("Processes data in parallel")
			.outputKey("processing_result")
			.inputKey("raw_data")
			.subAgents(List.of(agent1, agent2, agent3))
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.maxConcurrency(3)
			.state(() -> createDefaultStrategies())
			.build();

		// Verify agent properties
		assertNotNull(agent);
		assertEquals("dataProcessingPipeline", agent.name());
		assertEquals("Processes data in parallel", agent.description());
		assertEquals("processing_result", agent.outputKey());
		assertEquals("raw_data", agent.inputKey());
		assertEquals(3, agent.subAgents().size());
		assertEquals(3, agent.maxConcurrency());
		assertTrue(agent.mergeStrategy() instanceof ParallelAgent.DefaultMergeStrategy);
	}

	@Test
	void testBuilderValidation() {
		// Test that builders properly validate required fields
		assertThrows(IllegalArgumentException.class, () -> {
			SequentialAgent.builder().description("Missing name").outputKey("output").build();
		});

		assertThrows(IllegalArgumentException.class, () -> {
			LlmRoutingAgent.builder()
				.name("router")
				.outputKey("output")
				.subAgents(List.of(createMockReactAgent("agent", "output")))
				.build(); // Missing ChatModel
		});

		assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder()
				.name("parallel")
				.outputKey("output")
				.subAgents(List.of(createMockReactAgent("agent", "output"))) // Need at
				// least 2
				// agents
				.build();
		});
	}

	@Test
	void testStrategyRegistryExtensibility() {
		// Test that the strategy registry allows for runtime extension
		FlowGraphBuildingStrategyRegistry registry = FlowGraphBuildingStrategyRegistry.getInstance();

		// Verify default strategies are registered
		assertTrue(registry.hasStrategy(FlowAgentEnum.SEQUENTIAL.getType()));
		assertTrue(registry.hasStrategy(FlowAgentEnum.ROUTING.getType()));
		assertTrue(registry.hasStrategy(FlowAgentEnum.PARALLEL.getType()));
		assertTrue(registry.hasStrategy(FlowAgentEnum.CONDITIONAL.getType()));

		// Test getting strategies
		FlowGraphBuildingStrategy sequentialStrategy = registry.getStrategy(FlowAgentEnum.SEQUENTIAL.getType());
		assertNotNull(sequentialStrategy);
		assertTrue(sequentialStrategy instanceof SequentialGraphBuildingStrategy);

		// Test that non-existent strategies throw exceptions
		assertThrows(IllegalArgumentException.class, () -> {
			registry.getStrategy("NON_EXISTENT_STRATEGY");
		});
	}

	@Test
	void testFlowGraphConfigCustomProperties() {
		// Test that FlowGraphConfig supports custom properties for extensibility
		FlowGraphBuilder.FlowGraphConfig config = FlowGraphBuilder.FlowGraphConfig.builder()
			.name("testConfig")
			.customProperty("maxRetries", 3)
			.customProperty("timeout", 5000L)
			.customProperty("enableDebug", true);

		assertEquals(3, config.getCustomProperty("maxRetries"));
		assertEquals(5000L, config.getCustomProperty("timeout"));
		assertEquals(true, config.getCustomProperty("enableDebug"));
		assertNull(config.getCustomProperty("nonExistentProperty"));
	}

	@Test
	void testMergeStrategies() {
		// Test different merge strategies for ParallelAgent
		ParallelAgent.DefaultMergeStrategy defaultStrategy = new ParallelAgent.DefaultMergeStrategy();
		ParallelAgent.ListMergeStrategy listStrategy = new ParallelAgent.ListMergeStrategy();
		ParallelAgent.ConcatenationMergeStrategy concatStrategy = new ParallelAgent.ConcatenationMergeStrategy(" | ");

		assertNotNull(defaultStrategy);
		assertNotNull(listStrategy);
		assertNotNull(concatStrategy);

		// Test strategy types
		assertTrue(defaultStrategy instanceof ParallelAgent.MergeStrategy);
		assertTrue(listStrategy instanceof ParallelAgent.MergeStrategy);
		assertTrue(concatStrategy instanceof ParallelAgent.MergeStrategy);
	}

	@Test
	void testFluentInterface() throws Exception {
		// Test that all builders support fluent interface
		ParallelAgent.ParallelAgentBuilder builder = ParallelAgent.builder();

		// All methods should return the same builder instance for method chaining
		assertSame(builder, builder.name("test"));
		assertSame(builder, builder.description("test description"));
		assertSame(builder, builder.outputKey("output"));
		assertSame(builder, builder.inputKey("input"));
		assertSame(builder, builder.maxConcurrency(5));
		assertSame(builder, builder.mergeStrategy(new ParallelAgent.DefaultMergeStrategy()));
	}

	@Test
	void testBuilderInheritance() {
		// Test that concrete builders properly inherit from FlowAgentBuilder
		assertTrue(SequentialAgent.builder() instanceof FlowAgentBuilder);
		assertTrue(LlmRoutingAgent.builder() instanceof FlowAgentBuilder);
		assertTrue(ParallelAgent.builder() instanceof FlowAgentBuilder);
	}

	private ReactAgent createMockReactAgent(String name, String outputKey) throws Exception {
		return ReactAgent.builder()
			.name(name)
			.description("Mock agent: " + name)
			.outputKey(outputKey)
			.chatClient(chatClient)
			.state(() -> createDefaultStrategies())
			.resolver(resolver)
			.build();
	}

	private HashMap<String, KeyStrategy> createDefaultStrategies() {
		HashMap<String, KeyStrategy> strategies = new HashMap<>();
		strategies.put("messages", new AppendStrategy());
		return strategies;
	}

}
