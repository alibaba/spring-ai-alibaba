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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlowAgent StateSerializer configuration.
 */
class FlowAgentSerializerTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ToolCallbackResolver toolCallbackResolver;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	/**
	 * Test that SequentialAgent can be configured with SpringAIJacksonStateSerializer.
	 */
	@Test
	void testSequentialAgentWithJacksonSerializer() throws Exception {
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		SequentialAgent agent = SequentialAgent.builder()
				.name("sequential_agent")
				.description("Sequential agent with Jackson serializer")
				.stateSerializer(serializer)
				.subAgents(List.of(subAgent1, subAgent2))
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIJacksonStateSerializer");
	}

	/**
	 * Test that SequentialAgent can be configured with SpringAIStateSerializer.
	 */
	@Test
	void testSequentialAgentWithSpringAIStateSerializer() throws Exception {
		StateSerializer serializer = new SpringAIStateSerializer();

		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		SequentialAgent agent = SequentialAgent.builder()
				.name("sequential_agent")
				.description("Sequential agent with SpringAI serializer")
				.stateSerializer(serializer)
				.subAgents(List.of(subAgent1, subAgent2))
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIStateSerializer");
	}

	/**
	 * Test that SequentialAgent uses default serializer when not specified.
	 */
	@Test
	void testSequentialAgentWithDefaultSerializer() throws Exception {
		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		SequentialAgent agent = SequentialAgent.builder()
				.name("sequential_agent")
				.description("Sequential agent with default serializer")
				.subAgents(List.of(subAgent1, subAgent2))
				.build();

		// Verify default serializer is used
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		// Default should be JacksonSerializer (extends SpringAIJacksonStateSerializer)
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Default serializer should be SpringAIJacksonStateSerializer");
	}

	/**
	 * Test that ParallelAgent can be configured with SpringAIJacksonStateSerializer.
	 */
	@Test
	void testParallelAgentWithJacksonSerializer() throws Exception {
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		ParallelAgent agent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent with Jackson serializer")
				.stateSerializer(serializer)
				.subAgents(List.of(subAgent1, subAgent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIJacksonStateSerializer");
	}

	/**
	 * Test that ParallelAgent can be configured with SpringAIStateSerializer.
	 */
	@Test
	void testParallelAgentWithSpringAIStateSerializer() throws Exception {
		StateSerializer serializer = new SpringAIStateSerializer();

		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		ParallelAgent agent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent with SpringAI serializer")
				.stateSerializer(serializer)
				.subAgents(List.of(subAgent1, subAgent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIStateSerializer");
	}

	/**
	 * Test that ParallelAgent uses default serializer when not specified.
	 */
	@Test
	void testParallelAgentWithDefaultSerializer() throws Exception {
		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		ParallelAgent agent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent with default serializer")
				.subAgents(List.of(subAgent1, subAgent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Verify default serializer is used
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Default serializer should be SpringAIJacksonStateSerializer");
	}

	/**
	 * Test that LoopAgent can be configured with SpringAIJacksonStateSerializer.
	 */
	@Test
	void testLoopAgentWithJacksonSerializer() throws Exception {
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

		ReactAgent subAgent = createMockAgent("sub_agent", "output");

		LoopAgent agent = LoopAgent.builder()
				.name("loop_agent")
				.description("Loop agent with Jackson serializer")
				.stateSerializer(serializer)
				.subAgent(subAgent)
				.loopStrategy(LoopMode.count(2))
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIJacksonStateSerializer");
	}

	/**
	 * Test that LoopAgent can be configured with SpringAIStateSerializer.
	 */
	@Test
	void testLoopAgentWithSpringAIStateSerializer() throws Exception {
		StateSerializer serializer = new SpringAIStateSerializer();

		ReactAgent subAgent = createMockAgent("sub_agent", "output");

		LoopAgent agent = LoopAgent.builder()
				.name("loop_agent")
				.description("Loop agent with SpringAI serializer")
				.stateSerializer(serializer)
				.subAgent(subAgent)
				.loopStrategy(LoopMode.count(2))
				.build();

		// Verify serializer is set correctly in StateGraph
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIStateSerializer.class, graphSerializer,
				"Serializer should be SpringAIStateSerializer");
	}

	/**
	 * Test that LoopAgent uses default serializer when not specified.
	 */
	@Test
	void testLoopAgentWithDefaultSerializer() throws Exception {
		ReactAgent subAgent = createMockAgent("sub_agent", "output");

		LoopAgent agent = LoopAgent.builder()
				.name("loop_agent")
				.description("Loop agent with default serializer")
				.subAgent(subAgent)
				.loopStrategy(LoopMode.count(2))
				.build();

		// Verify default serializer is used
		StateGraph stateGraph = agent.asStateGraph();
		assertNotNull(stateGraph, "StateGraph should not be null");
		StateSerializer graphSerializer = stateGraph.getStateSerializer();
		assertNotNull(graphSerializer, "Serializer should not be null");
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer,
				"Default serializer should be SpringAIJacksonStateSerializer");
	}

	/**
	 * Test serializer consistency across multiple FlowAgents.
	 */
	@Test
	void testFlowAgentSerializerConsistency() throws Exception {
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("sequential_agent")
				.description("Sequential agent")
				.stateSerializer(serializer)
				.subAgents(List.of(subAgent1, subAgent2))
				.build();

		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent")
				.description("Parallel agent")
				.stateSerializer(serializer)
				.subAgents(List.of(subAgent1, subAgent2))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// Both should use the same serializer type
		StateSerializer sequentialSerializer = sequentialAgent.asStateGraph().getStateSerializer();
		StateSerializer parallelSerializer = parallelAgent.asStateGraph().getStateSerializer();

		assertNotNull(sequentialSerializer);
		assertNotNull(parallelSerializer);
		assertEquals(sequentialSerializer.getClass(), parallelSerializer.getClass(),
				"Both agents should use the same serializer type");
	}

	/**
	 * Test that FlowAgent builder fluent interface works with stateSerializer.
	 */
	@Test
	void testFlowAgentBuilderFluentInterface() throws Exception {
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

		ReactAgent subAgent1 = createMockAgent("agent1", "output1");
		ReactAgent subAgent2 = createMockAgent("agent2", "output2");

		SequentialAgent agent = SequentialAgent.builder()
				.name("test_agent")
				.description("Test agent")
				.stateSerializer(serializer)  // Test fluent interface
				.subAgents(List.of(subAgent1, subAgent2))
				.build();

		assertNotNull(agent);
		assertEquals("test_agent", agent.name());
		assertEquals("Test agent", agent.description());

		// Verify serializer is set
		StateSerializer graphSerializer = agent.asStateGraph().getStateSerializer();
		assertNotNull(graphSerializer);
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer);
	}

	/**
	 * Helper method to create a mock ReactAgent.
	 */
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
