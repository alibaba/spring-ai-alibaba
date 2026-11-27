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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmRoutingAgentOrchestrationTest {

	@Test
	void combinesTheCurrentTurnParallelResultsAndSkipsStaleOutputOnTheNextTurn() throws Exception {
		var routes = new AtomicInteger();
		var merges = new AtomicInteger();
		var leftCalls = new AtomicInteger();
		var rightCalls = new AtomicInteger();
		ChatModel routerModel = prompt -> {
			String system = prompt.getSystemMessage().getText();
			if (system.startsWith("Synthesize these search results")) {
				merges.incrementAndGet();
				assertTrue(system.contains("original query"));
				String sources = prompt.getUserMessage().getText();
				assertTrue(sources.contains("left answer") && sources.contains("right answer 1"));
				return response("synthesized answer");
			}
			int turn = routes.incrementAndGet();
			return response(turn == 1 ? """
					{"agents":[{"agent":"left","query":"left-specific"},{"agent":"right","query":"right-specific"}]}
					""" : """
					{"agents":[{"agent":"right","query":"right-specific"}]}
					""");
		};
		var left = ReactAgent.builder().name("left").description("Left specialist").instruction("{left_input}")
				.outputKey("left_result").model(prompt -> {
					leftCalls.incrementAndGet();
					assertTrue(prompt.getInstructions().stream().map(Message::getText).anyMatch("left-specific"::equals));
					return response("left answer");
				}).build();
		var right = ReactAgent.builder().name("right").description("Right specialist").instruction("{right_input}")
				.outputKey("right_result").model(prompt -> {
					assertTrue(prompt.getInstructions().stream().map(Message::getText).anyMatch("right-specific"::equals));
					return response("right answer " + rightCalls.incrementAndGet());
				}).build();
		var router = LlmRoutingAgent.builder().name("router").model(routerModel).subAgents(List.of(left, right)).build();
		var run = RunnableConfig.builder().threadId("parallel-router").build();

		var first = router.invoke("original query", run).orElseThrow();
		assertEquals("synthesized answer", first.value("merged_result").orElseThrow());
		assertEquals(1, merges.get());
		var second = router.invoke("follow-up for right only", run).orElseThrow();
		assertEquals("right answer 2", second.value("merged_result").orElseThrow());
		assertEquals(1, leftCalls.get());
		assertEquals(2, rightCalls.get());
		assertEquals(2, routes.get());
		assertEquals(1, merges.get());
	}

	private static ChatResponse response(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

}
