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
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
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
		assertEquals("processing_result", parallelAgent.outputKey());
		assertEquals("raw_data", parallelAgent.inputKey());
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
		assertEquals("complete_report", parallelAgent.outputKey());
		assertEquals("report_data", parallelAgent.inputKey());
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
		assertEquals("final_content", parallelAgent.outputKey());
		assertEquals("content_requirements", parallelAgent.inputKey());
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
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent agent2 = ReactAgent.builder()
			.name("dataValidator")
			.description("Validates data")
			.outputKey("validation_result")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Test fluent interface with ParallelAgent
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallelProcessor")
			.description("Processes data in parallel")
			.outputKey("parallel_result")
			.inputKey("input_data")
			.subAgents(List.of(agent1, agent2))
			.mergeStrategy(new ParallelAgent.ListMergeStrategy())
			.maxConcurrency(5)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("parallel_state", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.build();

		assertNotNull(parallelAgent);
		assertEquals("parallelProcessor", parallelAgent.name());
		assertEquals("Processes data in parallel", parallelAgent.description());
		assertEquals("parallel_result", parallelAgent.outputKey());
		assertEquals("input_data", parallelAgent.inputKey());
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
		assertTrue(exception.getMessage().contains("at least 2 sub-agents"));

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
			ParallelAgent.builder().name("testAgent").subAgents(List.of(agents)).state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			}).build();
		});
		assertTrue(exception.getMessage().contains("maximum 10 sub-agents"));
	}

	@Test
	void testUniqueOutputKeyValidation() throws Exception {
		MockitoAnnotations.openMocks(this);

		ReactAgent agent1 = createMockAgent("agent1", "same_output");
		ReactAgent agent2 = createMockAgent("agent2", "same_output"); // Same output key

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder().name("testAgent").subAgents(List.of(agent1, agent2)).state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			}).build();
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

	private ReactAgent createMockAgent(String name, String outputKey) throws Exception {
		return ReactAgent.builder()
			.name(name)
			.description("Mock agent")
			.outputKey(outputKey)
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
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
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent dataValidator = ReactAgent.builder()
			.name("dataValidator")
			.description("Validates data quality and integrity")
			.outputKey("validation_result")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent dataCleaner = ReactAgent.builder()
			.name("dataCleaner")
			.description("Cleans and preprocesses data")
			.outputKey("cleaning_result")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Create ParallelAgent using the improved builder with default merge strategy
		return ParallelAgent.builder()
			.name("dataProcessingPipeline")
			.description("Processes data through multiple parallel operations")
			.outputKey("processing_result")
			.inputKey("raw_data")
			.subAgents(List.of(dataAnalyzer, dataValidator, dataCleaner))
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy()) // Returns Map
			.maxConcurrency(3) // Limit to 3 concurrent operations
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("processing_state", new AppendStrategy());
				return strategies;
			})
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
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent detailsGenerator = ReactAgent.builder()
			.name("detailsGenerator")
			.description("Generates detailed analysis")
			.outputKey("details_section")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent chartsGenerator = ReactAgent.builder()
			.name("chartsGenerator")
			.description("Generates charts and visualizations")
			.outputKey("charts_section")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Create ParallelAgent using list merge strategy
		return ParallelAgent.builder()
			.name("reportGenerator")
			.description("Generates comprehensive reports in parallel")
			.outputKey("complete_report")
			.inputKey("report_data")
			.subAgents(List.of(summaryGenerator, detailsGenerator, chartsGenerator))
			.mergeStrategy(new ParallelAgent.ListMergeStrategy()) // Returns List
			.maxConcurrency(5)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("report_state", new AppendStrategy());
				return strategies;
			})
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
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent bodyWriter = ReactAgent.builder()
			.name("bodyWriter")
			.description("Writes main body content")
			.outputKey("body_content")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		ReactAgent conclusionWriter = ReactAgent.builder()
			.name("conclusionWriter")
			.description("Writes conclusion content")
			.outputKey("conclusion_content")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Create ParallelAgent using concatenation merge strategy
		return ParallelAgent.builder()
			.name("contentCreator")
			.description("Creates content through parallel writing")
			.outputKey("final_content")
			.inputKey("content_requirements")
			.subAgents(List.of(introWriter, bodyWriter, conclusionWriter))
			.mergeStrategy(new ParallelAgent.ConcatenationMergeStrategy("\n\n")) // Join
			// with
			// double
			// newline
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("content_state", new AppendStrategy());
				return strategies;
			})
			.build();
	}

}
