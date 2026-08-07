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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoutingMergeNode}.
 *
 * <p>Covers the gh-4616 fix: when {@code LlmRoutingAgent} hands off to a single sub-agent,
 * the merge node must return that agent's answer verbatim instead of running it through the
 * synthesis LLM again (which produced a redundant model call and a duplicated, rephrased
 * answer for the user). Genuine multi-agent results are still synthesized.</p>
 */
class RoutingMergeNodeTest {

	/**
	 * When the router delegated to a single sub-agent, the merge node should pass that
	 * agent's answer through unchanged and must NOT call the synthesis LLM.
	 */
	@Test
	void singleRoutedResultIsPassedThroughWithoutSynthesis() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

		BaseAgent poemAgent = mockAgent("poem_writer_agent", "poem_article");
		BaseAgent proseAgent = mockAgent("prose_writer_agent", "prose_article");

		// Router picked only the poem agent, so only its output key is present in state.
		OverAllState state = new OverAllState(Map.of(
				"poem_article", new AssistantMessage("A short modern poem about spring."),
				"messages", List.<Message>of(new UserMessage("Write a poem about spring"))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(poemAgent, proseAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals("A short modern poem about spring.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Single routed result must be returned verbatim, not re-synthesized");
		verify(chatModel, never()).call(any(Prompt.class));
	}

	/**
	 * When multiple sub-agents produced results, the merge node should synthesize them
	 * through the LLM and return the synthesized answer.
	 */
	@Test
	void multipleResultsAreSynthesizedViaLlm() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

		BaseAgent poemAgent = mockAgent("poem_writer_agent", "poem_article");
		BaseAgent proseAgent = mockAgent("prose_writer_agent", "prose_article");

		OverAllState state = new OverAllState(Map.of(
				"poem_article", new AssistantMessage("A poem."),
				"prose_article", new AssistantMessage("A prose piece."),
				"messages", List.<Message>of(new UserMessage("Write something about spring"))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(poemAgent, proseAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Multiple results must be synthesized via the LLM");
		verify(chatModel, times(1)).call(any(Prompt.class));
	}

	private static BaseAgent mockAgent(String name, String outputKey) {
		BaseAgent agent = mock(BaseAgent.class);
		when(agent.name()).thenReturn(name);
		when(agent.getOutputKey()).thenReturn(outputKey);
		return agent;
	}

}
