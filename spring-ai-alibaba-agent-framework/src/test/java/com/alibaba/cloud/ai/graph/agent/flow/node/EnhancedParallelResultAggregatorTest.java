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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EnhancedParallelResultAggregator} covering:
 * <ul>
 *   <li>Extra state key collision with default REPLACE strategy</li>
 *   <li>Extra state key collision with custom APPEND strategy from hooks</li>
 *   <li>System key prefix filtering (keys starting with "__")</li>
 *   <li>Backward compatibility with the legacy 4-arg constructor</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class EnhancedParallelResultAggregatorTest {

	/**
	 * When two sub-agents write the same extra key and no KeyStrategy is configured,
	 * the default REPLACE strategy should keep the last value (last-write-wins).
	 */
	@Test
	void extraStateKeyCollisionUsesDefaultReplaceStrategy() throws Exception {
		BaseAgent agent1 = mockAgent("agent1", "output1");
		BaseAgent agent2 = mockAgent("agent2", "output2");

		// Both sub-graphs write the same extra key "shared_data" with different values
		Map<String, Object> subGraph1State = new HashMap<>();
		subGraph1State.put("output1", "result1");
		subGraph1State.put("shared_data", "value_from_agent1");

		Map<String, Object> subGraph2State = new HashMap<>();
		subGraph2State.put("output2", "result2");
		subGraph2State.put("shared_data", "value_from_agent2");

		OverAllState state = new OverAllState(Map.of(
				"output1", GraphResponse.done(subGraph1State),
				"output2", GraphResponse.done(subGraph2State)
		));

		// No keyStrategies configured — uses legacy constructor
		EnhancedParallelResultAggregator aggregator = new EnhancedParallelResultAggregator(
				null, List.of(agent1, agent2), null, null);

		Map<String, Object> result = aggregator.apply(state);

		// Default REPLACE: last writer wins (agent2 processed after agent1)
		assertEquals("value_from_agent2", result.get("shared_data"));
	}

	/**
	 * When a KeyStrategy (MERGE) is configured for a specific key via hooks,
	 * colliding Map values should be deep-merged.
	 */
	@Test
	@SuppressWarnings("unchecked")
	void extraStateKeyCollisionUsesConfiguredMergeStrategy() throws Exception {
		BaseAgent agent1 = mockAgent("agent1", "output1");
		BaseAgent agent2 = mockAgent("agent2", "output2");

		// Both sub-graphs write the same extra key "shared_data" with Map values
		Map<String, Object> subGraph1State = new HashMap<>();
		subGraph1State.put("output1", "result1");
		subGraph1State.put("shared_data", new HashMap<>(Map.of("key1", "val1")));

		Map<String, Object> subGraph2State = new HashMap<>();
		subGraph2State.put("output2", "result2");
		subGraph2State.put("shared_data", new HashMap<>(Map.of("key2", "val2")));

		OverAllState state = new OverAllState(Map.of(
				"output1", GraphResponse.done(subGraph1State),
				"output2", GraphResponse.done(subGraph2State)
		));

		// Configure MERGE strategy for "shared_data" key (as if from Hook.getKeyStrategys())
		Map<String, KeyStrategy> hookStrategies = Map.of("shared_data", KeyStrategy.MERGE);

		EnhancedParallelResultAggregator aggregator = new EnhancedParallelResultAggregator(
				null, List.of(agent1, agent2), null, null, hookStrategies);

		Map<String, Object> result = aggregator.apply(state);

		// MERGE strategy: Map values should be deep-merged
		Object sharedData = result.get("shared_data");
		assertInstanceOf(Map.class, sharedData, "MERGE strategy should produce a merged Map");
		Map<String, Object> mergedMap = (Map<String, Object>) sharedData;
		assertEquals("val1", mergedMap.get("key1"));
		assertEquals("val2", mergedMap.get("key2"));
	}

	/**
	 * Keys starting with the system prefix "__" should be filtered out and not
	 * propagated to the parent state, even if they are not in the hardcoded set.
	 */
	@Test
	void systemKeyPrefixFiltersPrefixedKeys() throws Exception {
		BaseAgent agent1 = mockAgent("agent1", "output1");

		Map<String, Object> subGraphState = new HashMap<>();
		subGraphState.put("output1", "result1");
		subGraphState.put("__internal_tracking", "should_be_filtered");
		subGraphState.put("__execution_metadata", "should_be_filtered_too");
		subGraphState.put("user_data", "should_be_propagated");

		OverAllState state = new OverAllState(Map.of(
				"output1", GraphResponse.done(subGraphState)
		));

		EnhancedParallelResultAggregator aggregator = new EnhancedParallelResultAggregator(
				null, List.of(agent1), null, null);

		Map<String, Object> result = aggregator.apply(state);

		// Prefixed keys should be filtered
		assertFalse(result.containsKey("__internal_tracking"),
				"Keys with __ prefix should be filtered out");
		assertFalse(result.containsKey("__execution_metadata"),
				"Keys with __ prefix should be filtered out");

		// User data should be propagated
		assertEquals("should_be_propagated", result.get("user_data"));
	}

	/**
	 * Legacy system keys (input, messages, __execution_id) from the hardcoded set
	 * should still be filtered even without the prefix convention.
	 */
	@Test
	void legacySystemKeysAreStillFiltered() throws Exception {
		BaseAgent agent1 = mockAgent("agent1", "output1");

		Map<String, Object> subGraphState = new HashMap<>();
		subGraphState.put("output1", "result1");
		subGraphState.put("input", "should_be_filtered");
		subGraphState.put("messages", "should_be_filtered");
		subGraphState.put("custom_data", "should_be_propagated");

		OverAllState state = new OverAllState(Map.of(
				"output1", GraphResponse.done(subGraphState)
		));

		EnhancedParallelResultAggregator aggregator = new EnhancedParallelResultAggregator(
				null, List.of(agent1), null, null);

		Map<String, Object> result = aggregator.apply(state);

		assertFalse(result.containsKey("input"), "Legacy system key 'input' should be filtered");
		assertFalse(result.containsKey("messages"), "Legacy system key 'messages' should be filtered");
		assertEquals("should_be_propagated", result.get("custom_data"));
	}

	/**
	 * The legacy 4-arg constructor should work identically to the 5-arg constructor
	 * with an empty keyStrategies map — no collision handling, just last-write-wins.
	 */
	@Test
	void backwardCompatibilityWithLegacyConstructor() throws Exception {
		BaseAgent agent1 = mockAgent("agent1", "output1");

		Map<String, Object> subGraphState = new HashMap<>();
		subGraphState.put("output1", "result1");
		subGraphState.put("extra_data", "extra_value");

		OverAllState state = new OverAllState(Map.of(
				"output1", GraphResponse.done(subGraphState)
		));

		// Legacy 4-arg constructor
		EnhancedParallelResultAggregator aggregator = new EnhancedParallelResultAggregator(
				"merged_output", List.of(agent1), null, null);

		Map<String, Object> result = aggregator.apply(state);

		// Extra data should still be propagated
		assertEquals("extra_value", result.get("extra_data"));
		// Sub-agent result should be present
		assertEquals("result1", result.get("output1"));
		// Merged output should contain sub-agent results
		assertTrue(result.containsKey("merged_output"));
	}

	private static BaseAgent mockAgent(String name, String outputKey) {
		BaseAgent agent = mock(BaseAgent.class);
		when(agent.name()).thenReturn(name);
		when(agent.getOutputKey()).thenReturn(outputKey);
		return agent;
	}

}
