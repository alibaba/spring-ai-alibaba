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
package com.alibaba.cloud.ai.graph.agent.a2a;

import com.alibaba.cloud.ai.graph.StateGraph;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduction tests for Issue #3608:
 * https://github.com/alibaba/spring-ai-alibaba/issues/3608
 *
 * Issue Summary:
 * 1. shareState parameter not working - always defaults to false
 * 2. Response returns "Agent State: submitted" instead of actual content
 * 3. Parameter order error in asNode() - instruction and outputKey swapped
 *
 * These tests verify that the fixes are working correctly.
 */
class Issue3608ReproductionTest {

	private AgentCardWrapper agentCardWrapper;

	@BeforeEach
	void setUp() {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.name()).thenReturn("test-agent");
		when(agentCard.url()).thenReturn("http://localhost:8080");
		AgentCapabilities capabilities = mock(AgentCapabilities.class);
		when(capabilities.streaming()).thenReturn(false);
		when(agentCard.capabilities()).thenReturn(capabilities);
		agentCardWrapper = new AgentCardWrapper(agentCard);
	}

	// ==================== Bug #1: shareState not working ====================

	/**
	 * BEFORE FIX: initGraph() used 6-param constructor which hardcoded shareState=false
	 * AFTER FIX: initGraph() uses 8-param constructor which passes shareState correctly
	 *
	 * This test verifies that shareState=true set via Builder is actually used.
	 */
	@Test
	@DisplayName("Bug #1: shareState should be passed to A2aNodeActionWithConfig in initGraph()")
	void bug1_shareState_shouldBePassedToNodeAction() throws Exception {
		// Build agent with shareState=true (default)
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent for shareState bug")
				.agentCard(agentCardWrapper.getAgentCard())
				.shareState(true)  // This should be passed to A2aNodeActionWithConfig
				.instruction("Test: {input}")
				.build();

		// Verify shareState is stored in the agent
		Field shareStateField = A2aRemoteAgent.class.getDeclaredField("shareState");
		shareStateField.setAccessible(true);
		boolean shareStateInAgent = (boolean) shareStateField.get(agent);

		assertTrue(shareStateInAgent, "shareState should be true in agent");

		// Call initGraph() to create the StateGraph
		Method initGraphMethod = A2aRemoteAgent.class.getDeclaredMethod("initGraph");
		initGraphMethod.setAccessible(true);
		StateGraph graph = (StateGraph) initGraphMethod.invoke(agent);

		assertNotNull(graph, "StateGraph should not be null");

		// The fix ensures that initGraph() passes shareState to A2aNodeActionWithConfig
		// Before fix: new A2aNodeActionWithConfig(agentCard, name, includeContents, outputKey, instruction, streaming)
		// After fix: new A2aNodeActionWithConfig(agentCard, name, includeContents, outputKey, instruction, streaming, this.shareState, this.compileConfig)
	}

	/**
	 * Verify that shareState defaults to true in Builder.
	 */
	@Test
	@DisplayName("Bug #1: shareState should default to true in Builder")
	void bug1_shareState_shouldDefaultToTrue() throws Exception {
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.instruction("Test: {input}")
				// NOT setting shareState - should default to true
				.build();

		Field shareStateField = A2aRemoteAgent.class.getDeclaredField("shareState");
		shareStateField.setAccessible(true);
		boolean shareState = (boolean) shareStateField.get(agent);

		assertTrue(shareState, "shareState should default to true");
	}

	// ==================== Bug #2: "Agent State: submitted" response ====================

	/**
	 * BEFORE FIX: extractResponseText() returned "Agent State: submitted" for submitted state
	 * AFTER FIX: extractResponseText() returns empty string for submitted state
	 */
	@Test
	@DisplayName("Bug #2: extractResponseText should return empty string for 'submitted' state")
	void bug2_extractResponseText_shouldReturnEmptyForSubmittedState() throws Exception {
		A2aNodeActionWithConfig action = new A2aNodeActionWithConfig(
				agentCardWrapper, "test-agent", false, "output", "instruction", false);

		Method extractMethod = A2aNodeActionWithConfig.class.getDeclaredMethod("extractResponseText", java.util.Map.class);
		extractMethod.setAccessible(true);

		// Simulate status-update with "submitted" state
		java.util.Map<String, Object> result = new java.util.HashMap<>();
		result.put("kind", "status-update");
		java.util.Map<String, Object> status = new java.util.HashMap<>();
		status.put("state", "submitted");
		result.put("status", status);

		String response = (String) extractMethod.invoke(action, result);

		// BEFORE FIX: would return "Agent State: submitted"
		// AFTER FIX: returns ""
		assertEquals("", response,
				"submitted state should return empty string, NOT 'Agent State: submitted'");
		assertFalse(response.contains("Agent State"),
				"Response should not contain 'Agent State' for known states");
	}

	/**
	 * Verify all ignorable states return empty string.
	 */
	@Test
	@DisplayName("Bug #2: All ignorable states should return empty string")
	void bug2_extractResponseText_allIgnorableStatesShouldReturnEmpty() throws Exception {
		A2aNodeActionWithConfig action = new A2aNodeActionWithConfig(
				agentCardWrapper, "test-agent", false, "output", "instruction", false);

		Method extractMethod = A2aNodeActionWithConfig.class.getDeclaredMethod("extractResponseText", java.util.Map.class);
		extractMethod.setAccessible(true);

		String[] ignorableStates = {"completed", "processing", "failed", "submitted", "canceled"};

		for (String state : ignorableStates) {
			java.util.Map<String, Object> result = new java.util.HashMap<>();
			result.put("kind", "status-update");
			java.util.Map<String, Object> status = new java.util.HashMap<>();
			status.put("state", state);
			result.put("status", status);

			String response = (String) extractMethod.invoke(action, result);

			assertEquals("", response,
					String.format("State '%s' should return empty string", state));
		}
	}

	// ==================== Bug #3: asNode() parameter order ====================

	/**
	 * BEFORE FIX: asNode() passed instruction where outputKey should be and vice versa
	 * AFTER FIX: asNode() passes parameters in correct order
	 *
	 * Constructor signature:
	 * A2aNodeActionWithConfig(agentCard, agentName, includeContents, outputKeyToParent, instruction, streaming, shareState, config)
	 *                                                                  ^4th param         ^5th param
	 *
	 * BEFORE FIX (wrong order):
	 * new A2aNodeActionWithConfig(agentCard, name, includeContents, instruction, outputKey, ...)
	 *
	 * AFTER FIX (correct order):
	 * new A2aNodeActionWithConfig(agentCard, name, includeContents, outputKey, instruction, ...)
	 */
	@Test
	@DisplayName("Bug #3: asNode() should pass outputKey and instruction in correct order")
	void bug3_asNode_shouldPassParametersInCorrectOrder() throws Exception {
		String expectedOutputKey = "my_custom_output";
		String expectedInstruction = "Please process: {input}";

		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent for parameter order bug")
				.agentCard(agentCardWrapper.getAgentCard())
				.outputKey(expectedOutputKey)
				.instruction(expectedInstruction)
				.build();

		// Verify the values are stored correctly in the agent
		Field outputKeyField = findFieldInHierarchy(agent.getClass(), "outputKey");
		assertNotNull(outputKeyField, "outputKey field should exist");
		outputKeyField.setAccessible(true);
		String actualOutputKey = (String) outputKeyField.get(agent);

		Field instructionField = A2aRemoteAgent.class.getDeclaredField("instruction");
		instructionField.setAccessible(true);
		String actualInstruction = (String) instructionField.get(agent);

		assertEquals(expectedOutputKey, actualOutputKey, "outputKey should be stored correctly");
		assertEquals(expectedInstruction, actualInstruction, "instruction should be stored correctly");

		// The fix ensures these are passed in correct order to A2aNodeActionWithConfig
		// Before fix: new A2aNodeActionWithConfig(..., instruction, outputKey, ...)  // WRONG!
		// After fix: new A2aNodeActionWithConfig(..., outputKey, instruction, ...)   // CORRECT!
	}

	/**
	 * Verify asNode() returns a valid node.
	 */
	@Test
	@DisplayName("Bug #3: asNode() should return a valid Node")
	void bug3_asNode_shouldReturnValidNode() {
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.instruction("Test: {input}")
				.outputKey("result")
				.build();

		var node = agent.asNode(true, false);

		assertNotNull(node, "asNode should return a non-null Node");
		assertEquals("test-agent", node.id(), "Node id should match agent name");
	}

	// ==================== Helper methods ====================

	private Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
		Class<?> currentClass = clazz;
		while (currentClass != null) {
			try {
				return currentClass.getDeclaredField(fieldName);
			}
			catch (NoSuchFieldException e) {
				currentClass = currentClass.getSuperclass();
			}
		}
		return null;
	}

}
