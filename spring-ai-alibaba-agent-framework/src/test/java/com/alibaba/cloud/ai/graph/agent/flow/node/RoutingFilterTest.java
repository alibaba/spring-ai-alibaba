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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ContentFilteredException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutingFilterTest {

	@Test
	void singleAgentRouterRejectsFilteredJsonWithoutRetrying() {
		var calls = new AtomicInteger();
		var child = mock(Agent.class);
		when(child.name()).thenReturn("child");
		var router = new RoutingEdgeAction(filteredModel("{\"agent\":\"child\"}", calls), mock(Agent.class), List.of(child));
		var error = assertThrows(CompletionException.class, () -> router.apply(input()).join());
		assertInstanceOf(ContentFilteredException.class, error.getCause());
		assertEquals(1, calls.get());
	}

	@Test
	void parallelRouterPreservesContentFilterFailureWithoutRetrying() {
		var calls = new AtomicInteger();
		var child = mock(Agent.class);
		when(child.name()).thenReturn("child");
		var router = new RoutingNode(filteredModel("I cannot help with that", calls), mock(Agent.class), List.of(child));
		assertThrows(ContentFilteredException.class, () -> router.apply(input(), RunnableConfig.builder().build()));
		assertEquals(1, calls.get());
	}

	private static OverAllState input() {
		return new OverAllState(Map.of("messages", List.of(new UserMessage("select an agent"))));
	}

	private static ChatModel filteredModel(String response, AtomicInteger calls) {
		return prompt -> {
			calls.incrementAndGet();
			var metadata = ChatGenerationMetadata.builder().finishReason("provider_guardrail")
					.finishReasonCategory(FinishReasonCategory.FILTERED).build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage(response), metadata)));
		};
	}

}
