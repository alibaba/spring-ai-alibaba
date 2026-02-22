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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainAgentNodeActionTest {

	@Test
	void applyShouldAppendInstructionMessageWhenMessagesExist() throws Exception {
		ReactAgent mainAgent = mock(ReactAgent.class);
		CompiledGraph graph = mock(CompiledGraph.class);
		when(mainAgent.name()).thenReturn("main-agent");
		when(mainAgent.instruction()).thenReturn("route by {input}");
		when(mainAgent.getAndCompileGraph()).thenReturn(graph);
		when(graph.graphResponseStream(org.mockito.ArgumentMatchers.<Map<String, Object>>any(), any(RunnableConfig.class)))
				.thenReturn(Flux.empty());

		MainAgentNodeAction action = new MainAgentNodeAction(mainAgent, List.of());
		OverAllState state = new OverAllState(Map.of("messages", List.of(new UserMessage("original")), "input", "hello"));
		action.apply(state, RunnableConfig.builder().build());

		ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(graph).graphResponseStream(stateCaptor.capture(), any(RunnableConfig.class));
		List<?> messages = assertInstanceOf(List.class, stateCaptor.getValue().get("messages"));
		assertEquals(2, messages.size());
		assertInstanceOf(UserMessage.class, messages.get(0));
		AgentInstructionMessage instructionMessage = assertInstanceOf(AgentInstructionMessage.class, messages.get(1));
		assertEquals("route by {input}", instructionMessage.getText());
	}

	@Test
	void applyShouldCreateMessagesListWhenMissing() throws Exception {
		ReactAgent mainAgent = mock(ReactAgent.class);
		CompiledGraph graph = mock(CompiledGraph.class);
		when(mainAgent.name()).thenReturn("main-agent");
		when(mainAgent.instruction()).thenReturn("route by {input}");
		when(mainAgent.getAndCompileGraph()).thenReturn(graph);
		when(graph.graphResponseStream(org.mockito.ArgumentMatchers.<Map<String, Object>>any(), any(RunnableConfig.class)))
				.thenReturn(Flux.empty());

		MainAgentNodeAction action = new MainAgentNodeAction(mainAgent, List.of());
		OverAllState state = new OverAllState(Map.of("input", "hello"));
		action.apply(state, RunnableConfig.builder().build());

		ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(graph).graphResponseStream(stateCaptor.capture(), any(RunnableConfig.class));
		List<?> messages = assertInstanceOf(List.class, stateCaptor.getValue().get("messages"));
		assertEquals(1, messages.size());
		assertInstanceOf(AgentInstructionMessage.class, messages.get(0));
	}

	@Test
	void applyShouldKeepOriginalMessagesWhenInstructionIsEmpty() throws Exception {
		ReactAgent mainAgent = mock(ReactAgent.class);
		CompiledGraph graph = mock(CompiledGraph.class);
		when(mainAgent.name()).thenReturn("main-agent");
		when(mainAgent.instruction()).thenReturn("");
		when(mainAgent.getAndCompileGraph()).thenReturn(graph);
		when(graph.graphResponseStream(org.mockito.ArgumentMatchers.<Map<String, Object>>any(), any(RunnableConfig.class)))
				.thenReturn(Flux.empty());

		MainAgentNodeAction action = new MainAgentNodeAction(mainAgent, List.of());
		OverAllState state = new OverAllState(Map.of("messages", List.of(new UserMessage("original"))));
		action.apply(state, RunnableConfig.builder().build());

		ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(graph).graphResponseStream(stateCaptor.capture(), any(RunnableConfig.class));
		List<?> messages = assertInstanceOf(List.class, stateCaptor.getValue().get("messages"));
		assertEquals(1, messages.size());
		UserMessage original = assertInstanceOf(UserMessage.class, messages.get(0));
		assertEquals("original", original.getText());
	}

}
