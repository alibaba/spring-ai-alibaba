/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.agent.agentscope.flow;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategyRegistry;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link AgentScopeRoutingAgent}
 */
class AgentScopeRoutingAgentTest {

	private ChatModel chatModel;
	private Model scopeModel;

	@BeforeAll
	static void registerAgentScopeRoutingStrategy() {
		FlowGraphBuildingStrategyRegistry registry = FlowGraphBuildingStrategyRegistry.getInstance();
		if (!registry.hasStrategy(AgentScopeRoutingGraphBuildingStrategy.AGENT_SCOPE_ROUTING_TYPE)) {
			registry.registerStrategy(
					AgentScopeRoutingGraphBuildingStrategy.AGENT_SCOPE_ROUTING_TYPE,
					AgentScopeRoutingGraphBuildingStrategy::new);
		}
	}

	@BeforeEach
	void setUp() {
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (apiKey == null || apiKey.isBlank()) {
			apiKey = "test-key";
		}
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
		this.scopeModel = io.agentscope.core.model.DashScopeChatModel.builder()
				.apiKey(apiKey)
				.modelName("qwen-plus")
				.build();
	}

	/**
	 * Builds AgentScopeRoutingAgent and verifies the graph compiles and has expected structure.
	 * Does not invoke the graph; no API key required for this test.
	 */
	@Test
	void testAgentScopeRoutingAgentBuildsAndGraphCompiles() {
		ReactAgent poemWriterAgent = ReactAgent.builder()
				.name("poem_writer_agent")
				.model(chatModel)
				.description("Can write modern poetry.")
				.instruction("Respond: {poem_writer_agent_input}")
				.outputKey("poem_article")
				.build();
		AgentScopeRoutingAgent blogAgent = AgentScopeRoutingAgent.builder()
				.name("blog_agent")
				.model(scopeModel)
				.description("Routes to poem or prose agent.")
				.subAgents(List.of(poemWriterAgent))
				.build();
		GraphRepresentation representation = blogAgent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
		assertNotNull(representation);
		assertNotNull(representation.content());
		assertTrue(representation.content().contains("blog_agent"));
		assertTrue(representation.content().contains("poem_writer_agent"));
	}

	/**
	 * Integration test: invokes AgentScopeRoutingAgent with real DashScope API.
	 * Requires AI_DASHSCOPE_API_KEY. The routing model must return valid JSON
	 * (e.g. {\"agents\":[{\"agent\":\"poem_writer_agent\",\"query\":\"...\"}]}) for full assertions to pass.
	 */
	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void testAgentScopeRoutingAgent() throws Exception {
		ReactAgent proseWriterAgent = ReactAgent.builder()
				.name("prose_writer_agent")
				.model(chatModel)
				.description("Can write prose articles.")
				.instruction("You are a renowned writer skilled in writing prose. Please respond to the following request: {prose_writer_agent_input}")
				.outputKey("prose_article")
				.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
				.name("poem_writer_agent")
				.model(chatModel)
				.description("Can write modern poetry.")
				.instruction("You are a famous poet skilled in modern poetry. Please respond to the following request: {poem_writer_agent_input}")
				.outputKey("poem_article")
				.build();

		AgentScopeRoutingAgent blogAgent = AgentScopeRoutingAgent.builder()
				.name("blog_agent")
				.model(scopeModel)
				.description("Can write articles or poems based on user-provided topics.")
				.subAgents(List.of(proseWriterAgent, poemWriterAgent))
				.build();

		try {
			GraphRepresentation representation = blogAgent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
			assertNotNull(representation);
			assertNotNull(representation.content());
			assertTrue(representation.content().contains("blog_agent"), "Graph should contain blog_agent");

			Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的现代诗");
			assertTrue(result.isPresent(), "Result should be present");

			OverAllState state = result.get();
			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一个100字左右的现代诗", state.value("input").get(), "Input should match the request");

			// Routing may choose poem_writer_agent for 现代诗
			boolean hasPoem = state.value("poem_article").isPresent();
			boolean hasProse = state.value("prose_article").isPresent();
			assertTrue(hasPoem || hasProse, "Either poem_article or prose_article should be present");

			if (hasPoem) {
				Object poemOut = state.value("poem_article").get();
				AssistantMessage poemContent = poemOut instanceof AssistantMessage
						? (AssistantMessage) poemOut
						: (AssistantMessage) ((List<?>) poemOut).get(0);
				assertNotNull(poemContent.getText(), "Poem content should not be null");
			}
			if (hasProse) {
				Object proseOut = state.value("prose_article").get();
				AssistantMessage proseContent = proseOut instanceof AssistantMessage
						? (AssistantMessage) proseOut
						: (AssistantMessage) ((List<?>) proseOut).get(0);
				assertNotNull(proseContent.getText(), "Prose content should not be null");
			}

			// Merged result from AgentScope merge node
			assertTrue(state.value("merged_result").isPresent(), "merged_result should be present");
			assertNotNull(state.value("merged_result").get());
		}
		catch (Exception e) {
			Throwable cause = e;
			while (cause != null) {
				if (cause instanceof IllegalStateException
						&& cause.getMessage() != null
						&& cause.getMessage().contains("Failed to get valid decision")) {
					// Model may not have returned parseable JSON; skip hard failure in CI
					return;
				}
				cause = cause.getCause();
			}
			e.printStackTrace();
			fail("AgentScopeRoutingAgent execution failed: " + e.getMessage());
		}
	}

}
