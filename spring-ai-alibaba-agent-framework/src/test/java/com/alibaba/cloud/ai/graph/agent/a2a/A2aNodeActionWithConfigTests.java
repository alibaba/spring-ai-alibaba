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
package com.alibaba.cloud.ai.graph.agent.a2a;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;

import org.springframework.ai.chat.metadata.EmptyUsage;

import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A2aNodeActionWithConfigTests {

	private static final Method TO_FLUX = initToFluxMethod();

	private final A2aNodeActionWithConfig action = new A2aNodeActionWithConfig(createAgentCardWrapper(), "", false,
			"messages", "instruction", true);

	@Test
	void toFluxEmitsAsyncData() throws Exception {
		AsyncGenerator<NodeOutput> generator = new AsyncGenerator<>() {
			private final AtomicInteger index = new AtomicInteger();

			@Override
			public Data<NodeOutput> next() {
				int step = index.getAndIncrement();
				if (step == 0) {
					return Data.of(
							CompletableFuture.supplyAsync(() -> NodeOutput.of("node-1", "", new OverAllState(), new EmptyUsage())));
				}
				if (step == 1) {
					return Data.done(Map.of("result", "ok"));
				}
				return Data.done();
			}
		};

		Flux<GraphResponse<NodeOutput>> flux = invokeToFlux(generator);

		List<GraphResponse<NodeOutput>> responses = flux.collectList().block(Duration.ofSeconds(1));

		assertNotNull(responses);
		assertEquals(2, responses.size());

		GraphResponse<NodeOutput> first = responses.get(0);
		assertFalse(first.isDone());
		NodeOutput output = first.getOutput().getNow(null);
		assertNotNull(output);
		assertEquals("node-1", output.node());

		GraphResponse<NodeOutput> second = responses.get(1);
		assertTrue(second.isDone());
		Map<?, ?> resultValue = (Map<?, ?>) second.resultValue().orElseThrow();
		assertEquals("ok", resultValue.get("result"));
	}

	@Test
	void toFluxPropagatesErrors() throws Exception {
		AsyncGenerator<NodeOutput> generator = new AsyncGenerator<>() {
			private final AtomicInteger index = new AtomicInteger();

			@Override
			public Data<NodeOutput> next() {
				int step = index.getAndIncrement();
				if (step == 0) {
					return Data.of(CompletableFuture.supplyAsync(() -> {
						throw new IllegalStateException("boom");
					}));
				}
				return Data.done();
			}
		};

		Flux<GraphResponse<NodeOutput>> flux = invokeToFlux(generator);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> flux.collectList().block(Duration.ofSeconds(1)));
		assertEquals("boom", exception.getMessage());
	}

	@SuppressWarnings("unchecked")
	private Flux<GraphResponse<NodeOutput>> invokeToFlux(AsyncGenerator<NodeOutput> generator) throws Exception {
		return (Flux<GraphResponse<NodeOutput>>) TO_FLUX.invoke(this.action, generator);
	}

	private static Method initToFluxMethod() {
		try {
			Method method = A2aNodeActionWithConfig.class.getDeclaredMethod("toFlux", AsyncGenerator.class);
			method.setAccessible(true);
			return method;
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static AgentCardWrapper createAgentCardWrapper() {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.name()).thenReturn("test-agent");
		return new AgentCardWrapper(agentCard);
	}

	// ==================== Tests for Issue #3608 fixes ====================

	private static final Method EXTRACT_RESPONSE_TEXT = initExtractResponseTextMethod();

	/**
	 * Test that extractResponseText returns empty string for "submitted" state.
	 * This is a fix for Issue #3608 - "Agent State: submitted" should not be returned.
	 */
	@Test
	void extractResponseText_withSubmittedState_returnsEmptyString() throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", "submitted");
		result.put("status", status);

		String response = invokeExtractResponseText(result);
		assertEquals("", response, "submitted state should return empty string, not 'Agent State: submitted'");
	}

	/**
	 * Test that extractResponseText returns empty string for "canceled" state.
	 */
	@Test
	void extractResponseText_withCanceledState_returnsEmptyString() throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", "canceled");
		result.put("status", status);

		String response = invokeExtractResponseText(result);
		assertEquals("", response, "canceled state should return empty string");
	}

	/**
	 * Test that extractResponseText returns empty string for known ignorable states.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"completed", "processing", "failed", "submitted", "canceled"})
	void extractResponseText_withIgnorableStates_returnsEmptyString(String state) throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", state);
		result.put("status", status);

		String response = invokeExtractResponseText(result);
		assertEquals("", response, state + " state should return empty string");
	}

	/**
	 * Test that extractResponseText returns "Agent State: xxx" for unknown states.
	 */
	@Test
	void extractResponseText_withUnknownState_returnsAgentStateMessage() throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", "unknown_state");
		result.put("status", status);

		String response = invokeExtractResponseText(result);
		assertEquals("Agent State: unknown_state", response, "Unknown state should return 'Agent State: xxx'");
	}

	/**
	 * Test that extractResponseText correctly extracts text from "working" state.
	 */
	@Test
	void extractResponseText_withWorkingState_extractsMessageText() throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", "working");
		Map<String, Object> message = new HashMap<>();
		message.put("parts", List.of(Map.of("text", "Processing your request...")));
		status.put("message", message);
		result.put("status", status);

		String response = invokeExtractResponseText(result);
		assertEquals("Processing your request...", response);
	}

	/**
	 * Test that extractResponseText correctly extracts text from artifact-update.
	 */
	@Test
	void extractResponseText_withArtifactUpdate_extractsArtifactText() throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "artifact-update");
		Map<String, Object> artifact = new HashMap<>();
		artifact.put("parts", List.of(Map.of("text", "This is the artifact response")));
		result.put("artifact", artifact);

		String response = invokeExtractResponseText(result);
		assertEquals("This is the artifact response", response);
	}

	private String invokeExtractResponseText(Map<String, Object> result) throws Exception {
		return (String) EXTRACT_RESPONSE_TEXT.invoke(this.action, result);
	}

	private static Method initExtractResponseTextMethod() {
		try {
			Method method = A2aNodeActionWithConfig.class.getDeclaredMethod("extractResponseText", Map.class);
			method.setAccessible(true);
			return method;
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
