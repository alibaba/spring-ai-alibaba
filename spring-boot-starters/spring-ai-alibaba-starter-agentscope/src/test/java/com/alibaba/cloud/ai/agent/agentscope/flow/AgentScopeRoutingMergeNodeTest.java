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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentScopeRoutingMergeNodeTest {

	@Test
	void singleRoutedResultIsPassedThroughWithoutSynthesis() throws Exception {
		Model model = mock(Model.class);
		BaseAgent poemAgent = mockAgent("poem_writer_agent", "poem_article");
		BaseAgent proseAgent = mockAgent("prose_writer_agent", "prose_article");

		OverAllState state = new OverAllState(Map.of(
				"poem_article", new AssistantMessage("A short modern poem about spring."),
				"messages", List.<Message>of(new UserMessage("Write a poem about spring"))));

		AgentScopeRoutingMergeNode node = new AgentScopeRoutingMergeNode(model, List.of(poemAgent, proseAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals("A short modern poem about spring.", result.get(RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY),
				"Single routed result must be returned verbatim, not re-synthesized");
		verifyNoInteractions(model);
	}

	private static BaseAgent mockAgent(String name, String outputKey) {
		BaseAgent agent = mock(BaseAgent.class);
		when(agent.name()).thenReturn(name);
		when(agent.getOutputKey()).thenReturn(outputKey);
		return agent;
	}

}
