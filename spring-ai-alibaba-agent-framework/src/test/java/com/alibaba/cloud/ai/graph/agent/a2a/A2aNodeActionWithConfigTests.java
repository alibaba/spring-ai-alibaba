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
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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
		return createAgentCardWrapper(protocolVersion, "http://localhost:8080/a2a", null);
	}

	private static AgentCardWrapper createAgentCardWrapper(String protocolVersion, String url, String tenant) {
		return new AgentCardWrapper(createAgentCard(protocolVersion, url, tenant));
	}

	private static AgentCard createAgentCard(String protocolVersion, String url, String tenant) {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.name()).thenReturn("test-agent");
		when(agentCard.supportedInterfaces())
			.thenReturn(List.of(new AgentInterface("JSONRPC", url, tenant, protocolVersion)));
		return agentCard;
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
	 * Test that extractResponseText returns empty string for known ignorable states.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "completed", "processing", "submitted" })
	void extractResponseText_withIgnorableStates_returnsEmptyString(String state) throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", state);
		result.put("status", status);

		String response = invokeExtractResponseText(result);
		assertEquals("", response, state + " state should return empty string");
	}

	@Test
	void extractResponseText_withUnknownState_throws() {
		Map<String, Object> result = new HashMap<>();
		result.put("kind", "status-update");
		Map<String, Object> status = new HashMap<>();
		status.put("state", "unknown_state");
		result.put("status", status);

		Exception exception = assertThrows(Exception.class, () -> invokeExtractResponseText(result));
		assertTrue(rootMessage(exception).contains("Unsupported A2A task state: unknown_state"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "TASK_STATE_FAILED", "TASK_STATE_REJECTED", "TASK_STATE_CANCELED" })
	void extractResponseText_withTerminalFailureState_throws(String state) {
		Map<String, Object> result = Map.of("taskId", "task-1", "status",
				Map.of("state", state, "message", Map.of("parts", List.of(Map.of("text", "remote reason")))));

		Exception exception = assertThrows(Exception.class, () -> invokeExtractResponseText(result));

		assertTrue(rootMessage(exception).contains("remote reason"));
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

	@ParameterizedTest
	@ValueSource(strings = { "TASK_STATE_WORKING", "TASK_STATE_INPUT_REQUIRED", "TASK_STATE_AUTH_REQUIRED" })
	void extractResponseText_withActiveV1State_extractsAllMessageTextParts(String state) throws Exception {
		Map<String, Object> result = Map.of("taskId", "task-1", "status",
				Map.of("state", state, "message",
						Map.of("parts", List.of(Map.of("text", "first "), Map.of("data", Map.of("x", 1)),
								Map.of("text", "second")))));

		assertEquals("first second", invokeExtractResponseText(result));
	}

	@Test
	void extractResponseText_withMessage_extractsAllTextParts() throws Exception {
		Map<String, Object> result = Map.of("message",
				Map.of("parts", List.of(Map.of("text", "first "), Map.of("file", Map.of("name", "x")),
						Map.of("text", "second"))));

		assertEquals("first second", invokeExtractResponseText(result));
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
		Map<String, Object> task = Map.of("taskId", "task-1", "status", Map.of("state", "TASK_STATE_COMPLETED"), "artifacts",
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
	void completedV1TaskWithArtifacts_isReturnedByNonStreamingRequest() throws Exception {
		HttpServer server = startJsonServer(completedTaskResponse("final answer"));
		try {
			A2aNodeActionWithConfig target = new A2aNodeActionWithConfig(
					createAgentCardWrapper("1.0", serverUrl(server), null), "", false, "messages", "instruction",
					false);

			Map<String, Object> result = target.apply(new OverAllState(), RunnableConfig.builder().build());

			assertEquals("final answer", result.get("messages"));
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void completedV1TaskWithArtifacts_isReturnedByStreamingRequest() throws Exception {
		HttpServer server = startSseServer("data:" + completedTaskResponse("final answer") + "\n\n");
		try {
			List<GraphResponse<NodeOutput>> responses = invokeStreamingAction(streamingAction(server));

			assertEquals(List.of("final answer"), streamingChunks(responses));
			assertEquals("final answer", streamingResult(responses));
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void artifactReplacementSnapshots_emitOnlyDeltaAndDoNotDuplicateResult() throws Exception {
		String first = artifactUpdateResponse("hel", false, false);
		String second = artifactUpdateResponse("hello", false, true);
		HttpServer server = startSseServer("data:" + first + "\n\ndata:" + second + "\n\n");
		try {
			List<GraphResponse<NodeOutput>> responses = invokeStreamingAction(streamingAction(server));

			assertEquals(List.of("hel", "lo"), streamingChunks(responses));
			assertEquals("hello", streamingResult(responses));
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void taskArtifactSnapshotFollowedByReplacementUpdate_doesNotDuplicateResult() throws Exception {
		String task = taskSnapshotResponse("hel", "TASK_STATE_WORKING");
		String update = artifactUpdateResponse("hello", false, true);
		HttpServer server = startSseServer("data:" + task + "\n\ndata:" + update + "\n\n");
		try {
			List<GraphResponse<NodeOutput>> responses = invokeStreamingAction(streamingAction(server));

			assertEquals(List.of("hel", "lo"), streamingChunks(responses));
			assertEquals("hello", streamingResult(responses));
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void failedTaskSnapshotWithPartialArtifactsTerminatesStreamingWithError() throws Exception {
		String failedTask = """
				{"jsonrpc":"2.0","id":"request-1","result":{"task":{"taskId":"task-1","status":{"state":"TASK_STATE_FAILED","message":{"parts":[{"text":"remote reason"}]}},"artifacts":[{"artifactId":"artifact-1","parts":[{"text":"partial"}]}]}}}
				""".trim();

		assertStreamingFailure("data:" + failedTask + "\n\n", "remote reason");
	}

	@Test
	@SuppressWarnings("unchecked")
	void streamingApplyIsLazyAndEachSubscriptionGetsAnIndependentRequest() throws Exception {
		AtomicInteger requests = new AtomicInteger();
		String event = "data:" + completedTaskResponse("answer") + "\n\n";
		HttpServer server = startServer(event, "text/event-stream; charset=UTF-8",
				exchange -> requests.incrementAndGet());
		try {
			A2aNodeActionWithConfig target = streamingAction(server);
			Map<String, Object> result = target.apply(new OverAllState(), RunnableConfig.builder().build());
			Flux<GraphResponse<NodeOutput>> responses = (Flux<GraphResponse<NodeOutput>>) result.get("messages");

			Thread.sleep(100);
			assertEquals(0, requests.get(), "apply must not start HTTP work before subscription");
			assertEquals("answer", streamingResult(responses.collectList().block(Duration.ofSeconds(10))));
			assertEquals("answer", streamingResult(responses.collectList().block(Duration.ofSeconds(10))));
			assertEquals(2, requests.get(), "subscriptions must not share a single generator queue");
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void requestUsesOneEndpointSnapshotAcrossConcurrentCardRefresh() throws Exception {
		AtomicInteger refreshedRequests = new AtomicInteger();
		AtomicReference<String> requestVersion = new AtomicReference<>();
		AtomicReference<String> requestBody = new AtomicReference<>();
		HttpServer original = startServer(completedTaskResponse("answer"), "application/json; charset=UTF-8", exchange -> {
			requestVersion.set(exchange.getRequestHeaders().getFirst(A2AHeaders.A2A_VERSION));
			try {
				requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		});
		HttpServer refreshed = startServer(completedTaskResponse("wrong endpoint"), "application/json; charset=UTF-8",
				exchange -> refreshedRequests.incrementAndGet());
		CountDownLatch snapshotSelected = new CountDownLatch(1);
		CountDownLatch continueRequest = new CountDownLatch(1);
		AtomicBoolean firstSelection = new AtomicBoolean(true);
		AgentCardWrapper wrapper = new AgentCardWrapper(createAgentCard("0.3", serverUrl(original), null)) {
			@Override
			public AgentEndpoint endpoint() {
				AgentEndpoint endpoint = super.endpoint();
				if (firstSelection.compareAndSet(true, false)) {
					snapshotSelected.countDown();
					try {
						continueRequest.await();
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException(ex);
					}
				}
				return endpoint;
			}
		};
		A2aNodeActionWithConfig target = new A2aNodeActionWithConfig(wrapper, "", false, "messages", "instruction",
				false);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = new Thread(() -> {
			try {
				result.set(target.apply(new OverAllState(), RunnableConfig.builder().build()));
			}
			catch (Throwable ex) {
				failure.set(ex);
			}
		});
		try {
			worker.start();
			assertTrue(snapshotSelected.await(2, TimeUnit.SECONDS));
			wrapper.setAgentCard(createAgentCard("1.0", serverUrl(refreshed), "tenant-new"));
			continueRequest.countDown();
			worker.join(Duration.ofSeconds(5).toMillis());

			assertFalse(worker.isAlive());
			assertNull(failure.get());
			assertEquals("answer", result.get().get("messages"));
			assertEquals("0.3", requestVersion.get());
			assertEquals("message/send", JSON.parseObject(requestBody.get()).getString("method"));
			assertEquals(0, refreshedRequests.get());
		}
		finally {
			continueRequest.countDown();
			original.stop(0);
			refreshed.stop(0);
		}
	}

	@Test
	void buildSendRequests_useV1MethodAndMessageShape() throws Exception {
		assertV1RequestShape("buildSendMessageRequest", "SendMessage");
		assertV1RequestShape("buildSendStreamingMessageRequest", "SendStreamingMessage");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0.2.5", "0.3", "0.3.0" })
	void buildSendRequests_useLegacyMethodAndMessageShapeForProtocolVersion03(String protocolVersion) throws Exception {
		A2aNodeActionWithConfig legacyAction = new A2aNodeActionWithConfig(createAgentCardWrapper(protocolVersion), "",
				false, "messages", "instruction", true);

		assertLegacyRequestShape(legacyAction, "buildSendMessageRequest", "message/send");
		assertLegacyRequestShape(legacyAction, "buildSendStreamingMessageRequest", "message/stream");
	}

	@Test
	void buildSendRequests_includeSelectedInterfaceTenantOnlyForV1() throws Exception {
		A2aNodeActionWithConfig v1Action = new A2aNodeActionWithConfig(
				createAgentCardWrapper("1.0", "http://localhost:8080/a2a", "tenant-a"), "", false,
				"messages", "instruction", true);
		A2aNodeActionWithConfig legacyAction = new A2aNodeActionWithConfig(
				createAgentCardWrapper("0.3", "http://localhost:8080/a2a", "tenant-a"), "", false,
				"messages", "instruction", true);

		assertEquals("tenant-a", requestParams(v1Action, "buildSendMessageRequest").get("tenant"));
		assertFalse(requestParams(legacyAction, "buildSendMessageRequest").containsKey("tenant"));
	}

	@Test
	void createStreamingGenerator_propagatesJsonRpcSseErrors() throws Exception {
		assertStreamingFailure(
				"data:{\"jsonrpc\":\"2.0\",\"id\":\"request-1\",\"error\":{\"code\":-32603,\"message\":\"remote boom\"}}\n\n",
				"remote boom");
	}

	@Test
	void createStreamingGenerator_propagatesMalformedSseData() throws Exception {
		assertStreamingFailure("data: not-json\n\n", "not-json");
	}

	@Test
	void createStreamingGenerator_appliesBackpressureAndPreservesCompletion() throws Exception {
		String event = "data:{\"jsonrpc\":\"2.0\",\"id\":\"request-1\",\"result\":{\"message\":{\"parts\":[{\"text\":\"x\"}]}}}\n\n";
		HttpServer server = startSseServer(event.repeat(1_100));
		try {
			A2aNodeActionWithConfig target = streamingAction(server);
			AsyncGenerator<NodeOutput> generator = invokeCreateStreamingGenerator(target);
			Method queuedItems = generator.getClass().getDeclaredMethod("queuedItems");
			queuedItems.setAccessible(true);
			long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
			while ((int) queuedItems.invoke(generator) < 1_000 && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}
			assertEquals(1_000, queuedItems.invoke(generator),
					"the producer should block at the bounded queue capacity");

			class DemandSubscriber extends BaseSubscriber<GraphResponse<NodeOutput>> {
				private final List<GraphResponse<NodeOutput>> responses = new CopyOnWriteArrayList<>();

				private final CountDownLatch first = new CountDownLatch(1);

				private final CountDownLatch complete = new CountDownLatch(1);

				private final AtomicReference<Throwable> failure = new AtomicReference<>();

				@Override
				protected void hookOnSubscribe(Subscription subscription) {
					request(1);
				}

				@Override
				protected void hookOnNext(GraphResponse<NodeOutput> value) {
					this.responses.add(value);
					this.first.countDown();
				}

				@Override
				protected void hookOnComplete() {
					this.complete.countDown();
				}

				@Override
				protected void hookOnError(Throwable throwable) {
					this.failure.set(throwable);
					this.complete.countDown();
				}

				void requestMore(long amount) {
					request(amount);
				}
			}
			DemandSubscriber subscriber = new DemandSubscriber();
			invokeToFlux(target, generator).subscribe(subscriber);
			assertTrue(subscriber.first.await(2, TimeUnit.SECONDS));
			deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
			while ((int) queuedItems.invoke(generator) < 1_000 && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}
			assertEquals(1, subscriber.responses.size());
			assertEquals(1_000, queuedItems.invoke(generator),
					"downstream demand must keep the producer blocked instead of moving data to an unbounded buffer");
			assertTrue(producerThread(generator).isAlive());

			subscriber.requestMore(1_100);
			assertTrue(subscriber.complete.await(10, TimeUnit.SECONDS));
			assertNull(subscriber.failure.get());
			assertEquals(1_101, subscriber.responses.size());
			assertTrue(subscriber.responses.get(1_100).isDone());
		}
		finally {
			server.stop(0);
		}
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

	@SuppressWarnings("unchecked")
	private Map<String, Object> requestParams(A2aNodeActionWithConfig target, String methodName) throws Exception {
		Method method = A2aNodeActionWithConfig.class.getDeclaredMethod(methodName, OverAllState.class,
				RunnableConfig.class);
		method.setAccessible(true);
		String payload = (String) method.invoke(target, new OverAllState(), RunnableConfig.builder().build());
		Map<String, Object> request = JSON.parseObject(payload, new TypeReference<Map<String, Object>>() {
		});
		return (Map<String, Object>) request.get("params");
	}

	private void assertStreamingFailure(String responseBody, String expectedMessage) throws Exception {
		HttpServer server = startSseServer(responseBody);
		try {
			A2aNodeActionWithConfig target = streamingAction(server);
			AsyncGenerator<NodeOutput> generator = invokeCreateStreamingGenerator(target);

			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> invokeToFlux(target, generator).collectList().block(Duration.ofSeconds(10)));

			assertTrue(exceptionMessages(exception).contains(expectedMessage), exceptionMessages(exception));
			Thread producer = producerThread(generator);
			producer.join(Duration.ofSeconds(2).toMillis());
			assertFalse(producer.isAlive(), "the error terminal signal must not leave the producer blocked");
		}
		finally {
			server.stop(0);
		}
	}

	private static Thread producerThread(AsyncGenerator<NodeOutput> generator) throws Exception {
		var field = generator.getClass().getDeclaredField("producerThread");
		field.setAccessible(true);
		return (Thread) field.get(generator);
	}

	@SuppressWarnings("unchecked")
	private AsyncGenerator<NodeOutput> invokeCreateStreamingGenerator(A2aNodeActionWithConfig target) throws Exception {
		Method method = A2aNodeActionWithConfig.class.getDeclaredMethod("createStreamingGenerator", OverAllState.class,
				RunnableConfig.class);
		method.setAccessible(true);
		return (AsyncGenerator<NodeOutput>) method.invoke(target, new OverAllState(), RunnableConfig.builder().build());
	}

	@SuppressWarnings("unchecked")
	private Flux<GraphResponse<NodeOutput>> invokeToFlux(A2aNodeActionWithConfig target,
			AsyncGenerator<NodeOutput> generator) throws Exception {
		return (Flux<GraphResponse<NodeOutput>>) TO_FLUX.invoke(target, generator);
	}

	private static A2aNodeActionWithConfig streamingAction(HttpServer server) {
		return new A2aNodeActionWithConfig(createAgentCardWrapper("1.0", serverUrl(server), null), "", false, "messages",
				"instruction", true);
	}

	private static HttpServer startSseServer(String responseBody) throws IOException {
		return startServer(responseBody, "text/event-stream; charset=UTF-8");
	}

	private static HttpServer startJsonServer(String responseBody) throws IOException {
		return startServer(responseBody, "application/json; charset=UTF-8");
	}

	private static HttpServer startServer(String responseBody, String contentType) throws IOException {
		return startServer(responseBody, contentType, exchange -> {
		});
	}

	@Test
	void cancellingSlowSubscriberStopsBlockedProducerWithoutTerminalSignal() throws Exception {
		String event = "data:{\"jsonrpc\":\"2.0\",\"id\":\"request-1\",\"result\":{\"message\":{\"parts\":[{\"text\":\"x\"}]}}}\n\n";
		HttpServer server = startSseServer(event.repeat(1_100));
		try {
			A2aNodeActionWithConfig target = streamingAction(server);
			AsyncGenerator<NodeOutput> generator = invokeCreateStreamingGenerator(target);
			Method queuedItems = generator.getClass().getDeclaredMethod("queuedItems");
			queuedItems.setAccessible(true);
			long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
			while ((int) queuedItems.invoke(generator) < 1_000 && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}
			CountDownLatch first = new CountDownLatch(1);
			AtomicBoolean terminated = new AtomicBoolean();
			BaseSubscriber<GraphResponse<NodeOutput>> subscriber = new BaseSubscriber<>() {
				@Override
				protected void hookOnSubscribe(Subscription subscription) {
					request(1);
				}

				@Override
				protected void hookOnNext(GraphResponse<NodeOutput> value) {
					first.countDown();
				}

				@Override
				protected void hookOnComplete() {
					terminated.set(true);
				}

				@Override
				protected void hookOnError(Throwable throwable) {
					terminated.set(true);
				}
			};
			invokeToFlux(target, generator).subscribe(subscriber);
			assertTrue(first.await(2, TimeUnit.SECONDS));

			subscriber.dispose();
			Thread producer = producerThread(generator);
			producer.join(Duration.ofSeconds(2).toMillis());

			assertFalse(producer.isAlive());
			assertFalse(terminated.get(), "cancellation must not become an onError/onComplete signal");
			assertTrue((int) queuedItems.invoke(generator) <= 1);
		}
		finally {
			server.stop(0);
		}
	}

	private static HttpServer startServer(String responseBody, String contentType, Consumer<HttpExchange> observer)
			throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/a2a", exchange -> {
			observer.accept(exchange);
			byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", contentType);
			exchange.sendResponseHeaders(200, bytes.length);
			try (var output = exchange.getResponseBody()) {
				output.write(bytes);
			}
		});
		server.start();
		return server;
	}

	private static String serverUrl(HttpServer server) {
		return "http://localhost:" + server.getAddress().getPort() + "/a2a";
	}

	private static String completedTaskResponse(String text) {
		return taskSnapshotResponse(text, "TASK_STATE_COMPLETED");
	}

	private static String taskSnapshotResponse(String text, String state) {
		return """
				{"jsonrpc":"2.0","id":"request-1","result":{"task":{"taskId":"task-1","status":{"state":"%s"},"artifacts":[{"artifactId":"artifact-1","parts":[{"text":"%s"}]}]}}}
				""".formatted(state, text).trim();
	}

	private static String artifactUpdateResponse(String text, boolean append, boolean lastChunk) {
		return """
				{"jsonrpc":"2.0","id":"request-1","result":{"artifactUpdate":{"taskId":"task-1","artifact":{"artifactId":"artifact-1","parts":[{"text":"%s"}]},"append":%s,"lastChunk":%s}}}
				""".formatted(text, append, lastChunk).trim();
	}

	@SuppressWarnings("unchecked")
	private static List<GraphResponse<NodeOutput>> invokeStreamingAction(A2aNodeActionWithConfig target) throws Exception {
		Map<String, Object> result = target.apply(new OverAllState(), RunnableConfig.builder().build());
		return ((Flux<GraphResponse<NodeOutput>>) result.get("messages")).collectList().block(Duration.ofSeconds(10));
	}

	private static List<String> streamingChunks(List<GraphResponse<NodeOutput>> responses) {
		return responses.stream()
			.filter(response -> !response.isDone())
			.map(response -> ((StreamingOutput<?>) response.getOutput().join()).chunk())
			.toList();
	}

	private static String streamingResult(List<GraphResponse<NodeOutput>> responses) {
		GraphResponse<NodeOutput> completed = responses.get(responses.size() - 1);
		Map<?, ?> result = (Map<?, ?>) completed.resultValue().orElseThrow();
		return (String) result.get("messages");
	}

	private static String rootMessage(Throwable throwable) {
		Throwable root = throwable;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		return String.valueOf(root.getMessage());
	}

	private static String exceptionMessages(Throwable throwable) {
		StringBuilder messages = new StringBuilder();
		Throwable current = throwable;
		while (current != null) {
			messages.append(String.valueOf(current.getMessage())).append('\n');
			current = current.getCause();
		}
		return messages.toString();
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
