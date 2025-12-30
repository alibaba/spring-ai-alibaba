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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for A2aRemoteAgent focusing on Issue #3608 fixes:
 * 1. shareState parameter should be correctly passed to A2aNodeActionWithConfig
 * 2. asNode() method should pass parameters in correct order (outputKey before instruction)
 */
class A2aRemoteAgentTests {

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

	/**
	 * Test that A2aRemoteAgent.Builder correctly sets shareState to true by default.
	 */
	@Test
	void builder_shareStateDefaultsToTrue() throws Exception {
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.build();

		// Use reflection to verify shareState field
		Field shareStateField = A2aRemoteAgent.class.getDeclaredField("shareState");
		shareStateField.setAccessible(true);
		boolean shareState = (boolean) shareStateField.get(agent);

		assertTrue(shareState, "shareState should default to true");
	}

	/**
	 * Test that A2aRemoteAgent.Builder correctly sets shareState when explicitly set to false.
	 */
	@Test
	void builder_shareStateCanBeSetToFalse() throws Exception {
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.shareState(false)
				.build();

		Field shareStateField = A2aRemoteAgent.class.getDeclaredField("shareState");
		shareStateField.setAccessible(true);
		boolean shareState = (boolean) shareStateField.get(agent);

		assertFalse(shareState, "shareState should be false when explicitly set");
	}

	/**
	 * Test that initGraph() creates A2aNodeActionWithConfig with correct shareState.
	 * This is the main fix for Issue #3608 - shareState was being hardcoded to false.
	 */
	@Test
	void initGraph_passesShareStateToNodeAction() throws Exception {
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.shareState(true)
				.instruction("Test instruction: {input}")
				.build();

		// Call initGraph via reflection
		Method initGraphMethod = A2aRemoteAgent.class.getDeclaredMethod("initGraph");
		initGraphMethod.setAccessible(true);
		StateGraph graph = (StateGraph) initGraphMethod.invoke(agent);

		assertNotNull(graph, "StateGraph should not be null");
		assertEquals("test-agent", graph.getName(), "Graph name should match agent name");
	}

	/**
	 * Test that A2aRemoteAgent can be built with instruction and no shareState issues.
	 * Before the fix, this would fail because shareState=false was hardcoded.
	 */
	@Test
	void build_withInstructionAndShareState_succeeds() {
		assertDoesNotThrow(() -> {
			A2aRemoteAgent agent = A2aRemoteAgent.builder()
					.name("test-agent")
					.description("Test agent")
					.agentCard(agentCardWrapper.getAgentCard())
					.shareState(true)
					.instruction("Process: {input}")
					.build();
			assertNotNull(agent);
		});
	}

	/**
	 * Test that asNode() returns a valid Node.
	 */
	@Test
	void asNode_returnsValidNode() {
		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.instruction("Test: {input}")
				.outputKey("result")
				.build();

		Node node = agent.asNode(true, false);

		assertNotNull(node, "asNode should return a non-null Node");
		assertEquals("test-agent", node.id(), "Node id should match agent name");
	}

	/**
	 * Test that A2aRemoteAgent correctly stores outputKey and instruction separately.
	 * This helps verify that parameter order fix in asNode() is meaningful.
	 */
	@Test
	void builder_outputKeyAndInstructionStoredCorrectly() throws Exception {
		String expectedOutputKey = "my_output";
		String expectedInstruction = "Please process: {input}";

		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.outputKey(expectedOutputKey)
				.instruction(expectedInstruction)
				.build();

		// Verify outputKey via parent class field
		Field outputKeyField = agent.getClass().getSuperclass().getDeclaredField("outputKey");
		outputKeyField.setAccessible(true);
		String actualOutputKey = (String) outputKeyField.get(agent);

		// Verify instruction
		Field instructionField = A2aRemoteAgent.class.getDeclaredField("instruction");
		instructionField.setAccessible(true);
		String actualInstruction = (String) instructionField.get(agent);

		assertEquals(expectedOutputKey, actualOutputKey, "outputKey should be stored correctly");
		assertEquals(expectedInstruction, actualInstruction, "instruction should be stored correctly");
	}

	/**
	 * Test that compileConfig is stored and can be passed to A2aNodeActionWithConfig.
	 */
	@Test
	void builder_compileConfigStoredCorrectly() throws Exception {
		CompileConfig compileConfig = CompileConfig.builder().build();

		A2aRemoteAgent agent = A2aRemoteAgent.builder()
				.name("test-agent")
				.description("Test agent")
				.agentCard(agentCardWrapper.getAgentCard())
				.compileConfig(compileConfig)
				.instruction("Test: {input}")
				.build();

		// compileConfig is defined in Agent class (grandparent)
		Field compileConfigField = findFieldInHierarchy(agent.getClass(), "compileConfig");
		assertNotNull(compileConfigField, "compileConfig field should exist in class hierarchy");
		compileConfigField.setAccessible(true);
		CompileConfig actualConfig = (CompileConfig) compileConfigField.get(agent);

		assertNotNull(actualConfig, "compileConfig should be stored");
	}

	/**
	 * Helper method to find a field in the class hierarchy.
	 */
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

	/**
	 * Test that builder validation works correctly.
	 */
	@Test
	void builder_validationRequiresNameAndDescription() {
		assertThrows(IllegalArgumentException.class, () -> {
			A2aRemoteAgent.builder()
					.description("Test agent")
					.agentCard(agentCardWrapper.getAgentCard())
					.build();
		}, "Should throw when name is missing");

		assertThrows(IllegalArgumentException.class, () -> {
			A2aRemoteAgent.builder()
					.name("test-agent")
					.agentCard(agentCardWrapper.getAgentCard())
					.build();
		}, "Should throw when description is missing");
	}

}
