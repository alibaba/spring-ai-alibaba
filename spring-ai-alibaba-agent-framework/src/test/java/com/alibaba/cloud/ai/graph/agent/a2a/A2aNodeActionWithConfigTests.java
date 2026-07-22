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
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;

import org.apache.http.client.methods.HttpPost;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;

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
					// Use completedFuture to avoid timing issues with async execution
					return Data.of(
							CompletableFuture.completedFuture(NodeOutput.of("node-1", "", new OverAllState(), new EmptyUsage())));
				}
				if (step == 1) {
					return Data.done(Map.of("result", "ok"));
				}
				return Data.done();
			}
		};

		Flux<GraphResponse<NodeOutput>> flux = invokeToFlux(generator);

		List<GraphResponse<NodeOutput>> responses = flux.collectList().block(Duration.ofSeconds(5));

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
	void toFluxDrainsCompletedFuturesWithoutRecursiveStackGrowth() throws Exception {
		int count = 20_000;
		AsyncGenerator<NodeOutput> generator = new AsyncGenerator<>() {
			private final AtomicInteger index = new AtomicInteger();

			@Override
			public Data<NodeOutput> next() {
				int step = index.getAndIncrement();
				if (step < count) {
					return Data.of(NodeOutput.of("node-" + step, "", new OverAllState(), new EmptyUsage()));
				}
				return Data.done(Map.of("result", "ok"));
			}
		};

		Flux<GraphResponse<NodeOutput>> flux = invokeToFlux(generator);

		List<GraphResponse<NodeOutput>> responses = flux.collectList().block(Duration.ofSeconds(5));

		assertNotNull(responses);
		assertEquals(count + 1, responses.size());
		assertEquals("node-0", responses.get(0).getOutput().getNow(null).node());
		assertEquals("node-" + (count - 1), responses.get(count - 1).getOutput().getNow(null).node());
		assertTrue(responses.get(count).isDone());
	}

	@Test
	void toFluxPropagatesErrors() throws Exception {
		AsyncGenerator<NodeOutput> generator = new AsyncGenerator<>() {
			private final AtomicInteger index = new AtomicInteger();

			@Override
			public Data<NodeOutput> next() {
				int step = index.getAndIncrement();
				if (step == 0) {
					// Use a completed exceptionally future to avoid timing issues
					CompletableFuture<NodeOutput> failedFuture = new CompletableFuture<>();
					failedFuture.completeExceptionally(new IllegalStateException("boom"));
					return Data.of(failedFuture);
				}
				return Data.done();
			}
		};

		Flux<GraphResponse<NodeOutput>> flux = invokeToFlux(generator);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> flux.collectList().block(Duration.ofSeconds(5)));
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
		return createAgentCardWrapper("1.0");
	}

	private static AgentCardWrapper createAgentCardWrapper(String protocolVersion) {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.name()).thenReturn("test-agent");
		when(agentCard.supportedInterfaces())
			.thenReturn(List.of(new AgentInterface("JSONRPC", "http://localhost:8080/a2a", null, protocolVersion)));
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

	@Test
	void extractResponseText_withV1TaskResult_extractsArtifactText() throws Exception {
		Map<String, Object> task = Map.of("status", Map.of("state", "TASK_STATE_COMPLETED"), "artifacts",
				List.of(Map.of("parts", List.of(Map.of("text", "A2A v1 response")))));

		String response = invokeExtractResponseText(Map.of("task", task));

		assertEquals("A2A v1 response", response);
	}

	@Test
	void extractResponseText_withV1ArtifactUpdate_extractsArtifactText() throws Exception {
		Map<String, Object> update = Map.of("artifact",
				Map.of("parts", List.of(Map.of("text", "A2A v1 streaming response"))));

		String response = invokeExtractResponseText(Map.of("artifactUpdate", update));

		assertEquals("A2A v1 streaming response", response);
	}

	@Test
	void buildSendRequests_useV1MethodAndMessageShape() throws Exception {
		assertV1RequestShape("buildSendMessageRequest", "SendMessage");
		assertV1RequestShape("buildSendStreamingMessageRequest", "SendStreamingMessage");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0.3", "0.3.0" })
	void buildSendRequests_useLegacyMethodAndMessageShapeForProtocolVersion03(String protocolVersion) throws Exception {
		A2aNodeActionWithConfig legacyAction = new A2aNodeActionWithConfig(createAgentCardWrapper(protocolVersion), "",
				false, "messages", "instruction", true);

		assertLegacyRequestShape(legacyAction, "buildSendMessageRequest", "message/send");
		assertLegacyRequestShape(legacyAction, "buildSendStreamingMessageRequest", "message/stream");
	}

	@Test
	void createHttpPost_setsA2aVersionHeaderForBothSendPaths() throws Exception {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.supportedInterfaces())
			.thenReturn(List.of(new AgentInterface("JSONRPC", "http://localhost:8080/a2a", null, "1.0")));
		AgentCardWrapper agentCardWrapper = new AgentCardWrapper(agentCard);
		Method method = A2aNodeActionWithConfig.class.getDeclaredMethod("createHttpPost", AgentCardWrapper.class,
				String.class, boolean.class);
		method.setAccessible(true);

		HttpPost nonStreamingPost = (HttpPost) method.invoke(this.action, agentCardWrapper, agentCardWrapper.url(), false);
		HttpPost streamingPost = (HttpPost) method.invoke(this.action, agentCardWrapper, agentCardWrapper.url(), true);

		assertEquals("1.0", nonStreamingPost.getFirstHeader(A2AHeaders.A2A_VERSION).getValue());
		assertEquals("1.0", streamingPost.getFirstHeader(A2AHeaders.A2A_VERSION).getValue());
		assertNull(nonStreamingPost.getFirstHeader("Accept"));
		assertEquals("text/event-stream", streamingPost.getFirstHeader("Accept").getValue());
	}

	@SuppressWarnings("unchecked")
	private void assertV1RequestShape(String methodName, String expectedMethod) throws Exception {
		Method method = A2aNodeActionWithConfig.class.getDeclaredMethod(methodName, OverAllState.class,
				RunnableConfig.class);
		method.setAccessible(true);
		String payload = (String) method.invoke(this.action, new OverAllState(), RunnableConfig.builder().build());
		Map<String, Object> request = JSON.parseObject(payload, new TypeReference<Map<String, Object>>() {
		});
		Map<String, Object> params = (Map<String, Object>) request.get("params");
		Map<String, Object> message = (Map<String, Object>) params.get("message");
		List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");

		assertEquals(expectedMethod, request.get("method"));
		assertEquals(expectedMethod, JSONRPCUtils.parseRequestBody(payload, null).getMethod());
		assertEquals("ROLE_USER", message.get("role"));
		assertFalse(message.containsKey("kind"));
		assertFalse(parts.get(0).containsKey("kind"));
		assertEquals("instruction", parts.get(0).get("text"));
	}

	@SuppressWarnings("unchecked")
	private void assertLegacyRequestShape(A2aNodeActionWithConfig target, String methodName, String expectedMethod)
			throws Exception {
		Method method = A2aNodeActionWithConfig.class.getDeclaredMethod(methodName, OverAllState.class,
				RunnableConfig.class);
		method.setAccessible(true);
		String payload = (String) method.invoke(target, new OverAllState(), RunnableConfig.builder().build());
		Map<String, Object> request = JSON.parseObject(payload, new TypeReference<Map<String, Object>>() {
		});
		Map<String, Object> params = (Map<String, Object>) request.get("params");
		Map<String, Object> message = (Map<String, Object>) params.get("message");
		List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");

		assertEquals(expectedMethod, request.get("method"));
		assertEquals("user", message.get("role"));
		assertEquals("message", message.get("kind"));
		assertEquals("text", parts.get(0).get("kind"));
		assertEquals("instruction", parts.get(0).get("text"));
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

	// ==================== Tests for gh-4760: A2A streaming output metadata ====================

	private static final Method BUILD_STREAMING_OUTPUT = initBuildStreamingOutputMethod();

	/**
	 * The studio chat UI renders streaming text from the {@code chunk} / {@code message} fields of
	 * {@link StreamingOutput}. Before gh-4760, A2A streaming events were built with the plain
	 * {@code (text, node, agentName, state)} constructor, leaving {@code message}, {@code chunk} and
	 * {@code outputType} unset, so the SSE payload serialized to {@code {}} and the remote agent's
	 * reply was never displayed. The streaming output must now carry the text in both {@code chunk}
	 * and {@code message} and be tagged {@link OutputType#AGENT_MODEL_STREAMING}.
	 */
	@Test
	void buildStreamingOutput_populatesMessageChunkAndOutputType() throws Exception {
		OverAllState state = new OverAllState();

		StreamingOutput<?> output = (StreamingOutput<?>) BUILD_STREAMING_OUTPUT.invoke(this.action, "hello world",
				state);

		// chunk + message are the fields the studio chat UI reads to render streaming text
		assertEquals("hello world", output.chunk(), "chunk must carry the streamed text");
		assertInstanceOf(AssistantMessage.class, output.message(), "message must be an assistant message");
		assertEquals("hello world", ((AssistantMessage) output.message()).getText());
		// outputType restores metadata for downstream consumers of getOutputType()
		assertEquals(OutputType.AGENT_MODEL_STREAMING, output.getOutputType());
	}

	private static Method initBuildStreamingOutputMethod() {
		try {
			Method method = A2aNodeActionWithConfig.class.getDeclaredMethod("buildStreamingOutput", String.class,
					OverAllState.class);
			method.setAccessible(true);
			return method;
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
