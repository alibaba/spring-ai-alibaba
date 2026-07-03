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

package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for routing to nested flow agents.
 */
class LlmRoutingFlowAgentIntegrationTest {

	@Test
	void llmRoutingAgentCanInvokeSequentialFlowAgentSubAgent() throws Exception {
		// The scripted responses drive the full graph without a remote model:
		// route to the workflow, generate a draft, then return the reviewed final answer.
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write and review article\"}]}",
				"Draft article",
				"Reviewed article"));

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("Writes draft articles")
			.instruction("Write a draft for: {writing_workflow_input}")
			.outputKey("draft_article")
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("Reviews draft articles")
			.instruction("Review this draft: {draft_article}")
			.outputKey("reviewed_article")
			.build();

		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes and reviews an article")
			.subAgents(List.of(writerAgent, reviewerAgent))
			.build();

		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes writing tasks")
			.subAgents(List.of(writingWorkflow))
			.build();

		Optional<OverAllState> result = routingAgent.invoke("Please write an article");

		assertTrue(result.isPresent());
		// These assertions prove the routed SequentialAgent actually ran both nested agents.
		assertMessageText(result.get().value("draft_article"), "Draft article");
		assertMessageText(result.get().value("reviewed_article"), "Reviewed article");
		// The routing merge should expose the workflow's final nested output as the merged result.
		assertEquals("Reviewed article", result.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		assertEquals(3, chatModel.callCount());
	}

	private static void assertMessageText(Optional<Object> value, String expectedText) {
		assertTrue(value.isPresent());
		AssistantMessage message = assertInstanceOf(AssistantMessage.class, value.get());
		assertEquals(expectedText, message.getText());
	}

	private static final class ScriptedChatModel implements ChatModel {

		private final List<String> responses;

		private final AtomicInteger calls = new AtomicInteger();

		private ScriptedChatModel(List<String> responses) {
			this.responses = responses;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			int index = calls.getAndIncrement();
			if (index >= responses.size()) {
				throw new IllegalStateException("No scripted response for call " + index);
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.get(index)))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private int callCount() {
			return calls.get();
		}

	}

}
