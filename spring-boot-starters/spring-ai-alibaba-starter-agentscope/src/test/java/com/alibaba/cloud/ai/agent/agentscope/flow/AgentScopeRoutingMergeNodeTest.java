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

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentScopeRoutingMergeNodeTest {

	@Test
	void returnsSingleRoutedResultWithoutResynthesis() throws Exception {
		Model model = mock(Model.class);
		BaseAgent subAgent = mock(BaseAgent.class);
		when(subAgent.getOutputKey()).thenReturn("writer_output");
		when(subAgent.name()).thenReturn("writer_agent");

		OverAllState state = new OverAllState();
		state.updateState(Map.of(
				"messages", List.of(new AssistantMessage("original question")),
				"writer_output", new AssistantMessage("final single answer")
		));

		AgentScopeRoutingMergeNode node = new AgentScopeRoutingMergeNode(model, List.of(subAgent));

		Map<String, Object> result = node.apply(state);

		assertEquals("final single answer", result.get("merged_result"));
	}
}
