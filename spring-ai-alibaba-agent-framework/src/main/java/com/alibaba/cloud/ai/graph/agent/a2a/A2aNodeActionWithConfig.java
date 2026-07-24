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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;

import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.spec.Message;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import static java.lang.String.format;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;

public class A2aNodeActionWithConfig implements NodeActionWithConfig {

	private static final String LEGACY_SEND_MESSAGE_METHOD = "message/send";

	private static final String LEGACY_SEND_STREAMING_MESSAGE_METHOD = "message/stream";

	private static final int STREAM_QUEUE_CAPACITY = 1000;

	private final String agentName;

	private final AgentCardWrapper agentCard;

	private final boolean includeContents;

	private final String outputKeyToParent;

	private final boolean streaming;

	private final String instruction;

	private boolean shareState;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private CompileConfig parentCompileConfig;


	public A2aNodeActionWithConfig(AgentCardWrapper agentCard, String agentName, boolean includeContents, String outputKeyToParent, String instruction, boolean streaming) {
		this.agentName = agentName;
		this.agentCard = agentCard;
		this.includeContents = includeContents;
		this.outputKeyToParent = outputKeyToParent;
		this.streaming = streaming;
		this.instruction = instruction;
		this.shareState = false;
	}

	public A2aNodeActionWithConfig(AgentCardWrapper agentCard, String agentName, boolean includeContents, String outputKeyToParent, String instruction, boolean streaming, boolean shareState, CompileConfig compileConfig) {
		this(agentCard, agentName, includeContents, outputKeyToParent, instruction, streaming);
		this.parentCompileConfig = compileConfig;
		this.shareState = shareState;
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		RunnableConfig subGraphRunnableConfig = getSubGraphRunnableConfig(config);
		if (streaming) {
			Flux<GraphResponse<NodeOutput>> flux = Flux.defer(() -> {
				try {
					return toFlux(createStreamingGenerator(state, subGraphRunnableConfig));
				}
				catch (Exception ex) {
					return Flux.error(ex);
				}
			});
			return Map.of(StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages", flux);
		}
		else {
			AgentCardWrapper.AgentEndpoint endpoint = this.agentCard.endpoint();
			String requestPayload = buildSendRequest(state, subGraphRunnableConfig, false, endpoint);
			String resultText = sendMessageToServer(endpoint, requestPayload);
			Map<String, Object> resultMap = autoDetectAndParseResponse(resultText);
			Map<String, Object> result = responseResult(resultMap);
			String responseText = extractResponseText(result);
			return Map.of(this.outputKeyToParent, responseText);
		}
	}

	private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
		if (shareState) {
			return config;
		}
		return RunnableConfig.builder(config)
				.threadId(config.threadId()
						.map(threadId -> format("%s_%s", threadId, subGraphId()))
						.orElseGet(this::subGraphId))
				.nextNode(null)
				.checkPointId(null)
				.build();
	}

	public String subGraphId() {
		return format("subgraph_%s", agentCard.name());
	}

	/**
	 * Converts this AsyncGenerator to a Project Reactor Flux. This method provides
	 * forward compatibility for converting AsyncGenerator to reactive streams.
	 * @return a Flux that emits the elements from this AsyncGenerator
	 */
	private <E> Flux<GraphResponse<E>> toFlux(AsyncGenerator<E> generator) {
		return Flux.<GraphResponse<E>>generate(sink -> {
			AsyncGenerator.Data<E> data;
			try {
				data = generator.next();
			}
			catch (Exception ex) {
				sink.error(ex);
				return;
			}

			if (data.isDone()) {
				data.resultValue().ifPresent(result -> sink.next(GraphResponse.done(result)));
				sink.complete();
				return;
			}

			var future = data.getData();
			if (future == null) {
				sink.error(new IllegalStateException("AsyncGenerator data is null without completion signal"));
				return;
			}

			try {
				sink.next(GraphResponse.of(future.join()));
			}
			catch (Exception ex) {
				sink.error(unwrapCompletionException(ex));
			}
		})
			.subscribeOn(Schedulers.boundedElastic())
			.doFinally(signalType -> {
				if (generator instanceof StreamingQueueGenerator<?> streamingGenerator) {
					streamingGenerator.cancel();
				}
			});
	}

	private Throwable unwrapCompletionException(Throwable throwable) {
		if (throwable instanceof java.util.concurrent.CompletionException completionException
				&& completionException.getCause() != null) {
			return completionException.getCause();
		}
		return throwable;
	}

	/**
	 * Create a streaming generator.
	 */
	private AsyncGenerator<NodeOutput> createStreamingGenerator(OverAllState state, RunnableConfig config) throws Exception {
		final AgentCardWrapper.AgentEndpoint endpoint = this.agentCard.endpoint();
		final String requestPayload = buildSendRequest(state, config, true, endpoint);
		final String outputKey = StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages";
		final StreamingTextAccumulator accumulator = new StreamingTextAccumulator();
		StreamingQueueGenerator<NodeOutput> generator = new StreamingQueueGenerator<>();

		generator.start(queue -> {
			String baseUrl = resolveAgentBaseUrl(endpoint);
			if (baseUrl == null || baseUrl.isBlank()) {
				throw new IllegalStateException("AgentCard.url is empty");
			}

			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				queue.registerCloseable(httpClient);
				try {
					HttpPost post = createHttpPost(endpoint, baseUrl, true);
					post.setEntity(new StringEntity(requestPayload, ContentType.APPLICATION_JSON));

					try (CloseableHttpResponse response = httpClient.execute(post)) {
						int statusCode = response.getStatusLine().getStatusCode();
						if (statusCode != 200) {
							throw new IllegalStateException("HTTP request failed, status: " + statusCode);
						}

						HttpEntity entity = response.getEntity();
						if (entity == null) {
							throw new IllegalStateException("Empty HTTP entity");
						}

						String contentType = entity.getContentType() != null ? entity.getContentType().getValue() : "";
						boolean isEventStream = contentType.toLowerCase(Locale.ROOT).contains("text/event-stream");

						if (isEventStream) {
							try (BufferedReader reader = new BufferedReader(
									new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
								String line;
								while ((line = reader.readLine()) != null) {
									String trimmed = line.trim();
									if (!trimmed.startsWith("data:")) {
										continue;
									}
									String jsonContent = trimmed.substring(5).trim();
									if ("[DONE]".equals(jsonContent)) {
										break;
									}
									Map<String, Object> parsed = parseSseData(jsonContent);
									Map<String, Object> result = responseResult(parsed);
									String text = accumulator.add(result);
									if (!text.isEmpty()) {
										queue.emit(buildStreamingOutput(text, state));
									}
								}
							}
						}
						else {
							// Non-SSE: read the full body and emit a single output
							String body = EntityUtils.toString(entity, "UTF-8");
							Map<String, Object> resultMap = JSON.parseObject(body,
									new TypeReference<Map<String, Object>>() {
									});
							Map<String, Object> result = responseResult(resultMap);
							String text = accumulator.add(result);
							if (!text.isEmpty()) {
								queue.emit(buildStreamingOutput(text, state));
							}
						}
					}
					queue.complete(Map.of(outputKey, accumulator.result()));
				}
				finally {
					queue.clearCloseable(httpClient);
				}
			}
		});
		return generator;
	}

	@FunctionalInterface
	private interface StreamingProducer<E> {

		void produce(StreamingQueueGenerator<E> generator) throws Exception;

	}

	private static final class StreamingQueueGenerator<E> implements AsyncGenerator<E> {

		private final ArrayBlockingQueue<Data<E>> queue = new ArrayBlockingQueue<>(STREAM_QUEUE_CAPACITY);

		private final AtomicBoolean cancelled = new AtomicBoolean();

		private final AtomicBoolean terminalEnqueued = new AtomicBoolean();

		private volatile Data<E> terminal;

		private volatile Thread producerThread;

		private volatile Closeable closeable;

		void start(StreamingProducer<E> producer) {
			Thread thread = new Thread(() -> {
				try {
					producer.produce(this);
					complete(null);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					if (!this.cancelled.get()) {
						fail(ex);
					}
				}
				catch (Exception ex) {
					if (!this.cancelled.get()) {
						fail(ex);
					}
				}
			}, "a2a-stream-" + UUID.randomUUID());
			thread.setDaemon(true);
			this.producerThread = thread;
			thread.start();
		}

		void emit(E value) throws InterruptedException {
			enqueue(Data.of(value));
		}

		void complete(Object result) throws InterruptedException {
			if (this.terminalEnqueued.compareAndSet(false, true)) {
				enqueue(Data.done(result));
			}
		}

		void registerCloseable(Closeable closeable) throws IOException {
			this.closeable = closeable;
			if (this.cancelled.get()) {
				closeable.close();
				throw new IOException("A2A stream was cancelled");
			}
		}

		void clearCloseable(Closeable closeable) {
			if (this.closeable == closeable) {
				this.closeable = null;
			}
		}

		private void fail(Throwable error) {
			if (!this.terminalEnqueued.compareAndSet(false, true)) {
				return;
			}
			try {
				enqueue(Data.error(error));
				enqueue(Data.done());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		private void enqueue(Data<E> data) throws InterruptedException {
			if (this.cancelled.get()) {
				throw new InterruptedException("A2A stream was cancelled");
			}
			this.queue.put(data);
		}

		@Override
		public Data<E> next() {
			Data<E> completed = this.terminal;
			if (completed != null) {
				return completed;
			}
			try {
				Data<E> data = this.queue.take();
				if (data.isDone()) {
					this.terminal = data;
				}
				return data;
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return Data.error(ex);
			}
		}

		void cancel() {
			if (!this.cancelled.compareAndSet(false, true)) {
				return;
			}
			Closeable activeCloseable = this.closeable;
			if (activeCloseable != null) {
				try {
					activeCloseable.close();
				}
				catch (IOException ignored) {
				}
			}
			Thread thread = this.producerThread;
			if (thread != null) {
				thread.interrupt();
			}
			this.queue.clear();
			Data<E> done = Data.done();
			this.terminalEnqueued.set(true);
			this.terminal = done;
			this.queue.offer(done);
		}

		int queuedItems() {
			return this.queue.size();
		}

	}

	/**
	 * Build a {@link StreamingOutput} for an A2A remote response chunk.
	 *
	 * <p>
	 * The plain {@code (originData, node, agentName, state)} constructor leaves {@code message},
	 * {@code chunk} and {@code outputType} unset, so the serialized streaming event is empty
	 * ({@code data:{}}). The studio chat UI renders streaming text from the {@code chunk} /
	 * {@code message} fields, so A2A remote-agent replies were never displayed. Wrapping the text
	 * in an {@link AssistantMessage} populates both {@code message} and {@code chunk}, and tagging
	 * the event {@link OutputType#AGENT_MODEL_STREAMING} restores the metadata expected by
	 * downstream consumers of {@link StreamingOutput#getOutputType()}. See gh-4760.
	 */
	private StreamingOutput<?> buildStreamingOutput(String text, OverAllState state) {
		return new StreamingOutput<>(new AssistantMessage(text), text, "a2aNode", agentName, state,
				OutputType.AGENT_MODEL_STREAMING);
	}

//	/**
//	 * Get the streaming generator (similar to LlmNode.stream).
//	 */
//	public Flux<NodeOutput> stream(OverAllState state) throws Exception {
//		if (!this.streaming) {
//			throw new IllegalStateException("Streaming is not enabled for this A2aNode");
//		}
//		AsyncGenerator<NodeOutput> generator = createStreamingGenerator(state);
//		Flux<GraphResponse<NodeOutput>> graphResponseFlux = toFlux(generator);
//
//		// Convert Flux<GraphResponse<NodeOutput>> to Flux<NodeOutput>
//		return graphResponseFlux.filter(graphResponse -> !graphResponse.isDone()) // Filter out completion signals
//			.map(graphResponse -> {
//				try {
//					return graphResponse.getOutput().join();
//				}
//				catch (Exception e) {
//					throw new RuntimeException("Error extracting output from GraphResponse", e);
//				}
//			});
//	}

//	/**
//	 * Get the non-streaming result (similar to LlmNode.call).
//	 */
//	public String call(OverAllState state) throws Exception {
//		String requestPayload = buildSendMessageRequest(state, this.inputKeyFromParent);
//		String resultText = sendMessageToServer(this.agentCard, requestPayload);
//		Map<String, Object> resultMap = autoDetectAndParseResponse(resultText);
//		Map<String, Object> result = (Map<String, Object>) resultMap.get("result");
//		return extractResponseText(result);
//	}

	/**
	 * Auto-detect response format and parse accordingly.
	 * @param responseText The raw response text
	 * @return Parsed result map
	 */
	private Map<String, Object> autoDetectAndParseResponse(String responseText) {
		if (responseText.lines().map(String::trim).anyMatch(line -> line.startsWith("data:"))) {
			return parseStreamingResponse(responseText);
		}
		else {
			// Standard JSON response
			return JSON.parseObject(responseText, new TypeReference<Map<String, Object>>() {
			});
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> responseResult(Map<String, Object> response) {
		if (response == null) {
			throw new IllegalStateException("A2A response is empty");
		}
		Object error = response.get("error");
		if (error != null) {
			throw new IllegalStateException("A2A request failed: " + JSON.toJSONString(error));
		}
		Object result = response.get("result");
		if (!(result instanceof Map<?, ?>)) {
			throw new IllegalStateException("A2A response does not contain a result");
		}
		return (Map<String, Object>) result;
	}

	/**
	 * Parse streaming response in Server-Sent Events (SSE) format.
	 * @param responseText The raw SSE response text
	 * @return Parsed result map
	 */
	private Map<String, Object> parseStreamingResponse(String responseText) {
		String[] lines = responseText.split("\n");
		Map<String, Object> lastResult = null;
		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("data:")) {
				String jsonContent = line.substring(5).trim();
				if ("[DONE]".equals(jsonContent)) {
					break;
				}
				Map<String, Object> parsed = parseSseData(jsonContent);
				Map<String, Object> result = responseResult(parsed);
				if (result.containsKey("artifact") || lastResult == null) {
					lastResult = result;
				}
			}
		}

		if (lastResult == null) {
			throw new IllegalStateException("Failed to parse any valid result from streaming response");
		}
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("result", lastResult);
		return resultMap;
	}

	private Map<String, Object> parseSseData(String jsonContent) {
		try {
			return JSON.parseObject(jsonContent, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Malformed A2A SSE data: " + jsonContent, ex);
		}
	}

	private String extractResponseText(Map<String, Object> result) {
		if (result == null) {
			throw new IllegalStateException("Result is null, cannot extract response text");
		}

		Map<String, Object> unwrapped = unwrapResponse(result);
		if (unwrapped != result) {
			return extractResponseText(unwrapped);
		}
		throwIfTerminalFailure(result);

		if ("artifact-update".equals(result.get("kind")) || result.containsKey("artifact")) {
			Map<String, Object> artifact = (Map<String, Object>) result.get("artifact");
			return artifact != null ? extractTextParts(artifact.get("parts")) : "";
		}

		if (result.containsKey("artifacts")) {
			String artifactsText = extractArtifacts(result.get("artifacts"));
			if (!artifactsText.isEmpty()) {
				return artifactsText;
			}
		}

		if (result.containsKey("parts")) {
			return extractTextParts(result.get("parts"));
		}

		if (result.containsKey("status")) {
			Map<String, Object> status = (Map<String, Object>) result.get("status");
			if (status == null) {
				throw new IllegalStateException("A2A task status is missing");
			}
			String state = status.get("state") instanceof String value ? value : null;
			if (state == null || state.isBlank()) {
				throw new IllegalStateException("A2A task state is missing");
			}
			String normalizedState = normalizeTaskState(state);
			String statusMessage = "";
			if (status.get("message") instanceof Map<?, ?> message) {
				statusMessage = extractTextParts(message.get("parts"));
			}
			return switch (normalizedState) {
				case "completed", "working", "input_required", "auth_required", "processing" -> statusMessage;
				case "submitted" -> "";
				case "canceled", "failed", "rejected" -> throw new IllegalStateException(
						"A2A task " + normalizedState + (statusMessage.isEmpty() ? "" : ": " + statusMessage));
				case "unrecognized" -> throw new IllegalStateException("A2A task state is unrecognized");
				default -> throw new IllegalStateException("Unsupported A2A task state: " + state);
			};
		}

		if (result.containsKey("artifacts")) {
			return "";
		}
		throw new IllegalStateException("No valid text content found in result: " + result);
	}

	private void throwIfTerminalFailure(Map<String, Object> result) {
		if (result.get("status") instanceof Map<?, ?> status) {
			String state = status.get("state") instanceof String value ? value : "";
			String normalizedState = normalizeTaskState(state);
			if (List.of("canceled", "failed", "rejected").contains(normalizedState)) {
				String statusMessage = status.get("message") instanceof Map<?, ?> message
						? extractTextParts(message.get("parts")) : "";
				throw new IllegalStateException(
						"A2A task " + normalizedState + (statusMessage.isEmpty() ? "" : ": " + statusMessage));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> unwrapResponse(Map<String, Object> result) {
		for (String wrapperKey : List.of("task", "message", "statusUpdate", "artifactUpdate")) {
			Object wrappedResult = result.get(wrapperKey);
			if (wrappedResult instanceof Map<?, ?>) {
				return (Map<String, Object>) wrappedResult;
			}
		}
		return result;
	}

	private String extractArtifacts(Object artifactsValue) {
		if (!(artifactsValue instanceof List<?> artifacts)) {
			return "";
		}
		StringBuilder response = new StringBuilder();
		for (Object artifactValue : artifacts) {
			if (artifactValue instanceof Map<?, ?> artifact) {
				response.append(extractTextParts(artifact.get("parts")));
			}
		}
		return response.toString();
	}

	private String normalizeTaskState(String state) {
		String normalized = state.startsWith("TASK_STATE_") ? state.substring("TASK_STATE_".length()) : state;
		return normalized.toLowerCase(Locale.ROOT).replace('-', '_');
	}

	private String extractTextParts(Object partsValue) {
		if (!(partsValue instanceof List<?> parts)) {
			return "";
		}
		StringBuilder response = new StringBuilder();
		for (Object partValue : parts) {
			if (partValue instanceof Map<?, ?> part && part.get("text") instanceof String text) {
				response.append(text);
			}
		}
		return response.toString();
	}

	private final class StreamingTextAccumulator {

		private final StringBuilder messages = new StringBuilder();

		private final Map<String, String> artifacts = new LinkedHashMap<>();

		String add(Map<String, Object> result) {
			Map<String, Object> event = unwrapResponse(result);
			throwIfTerminalFailure(event);
			if (event.get("artifact") instanceof Map<?, ?> artifact) {
				return updateArtifact(artifact, Boolean.TRUE.equals(event.get("append")), "__default__");
			}
			if (event.get("artifacts") instanceof List<?> artifactSnapshots) {
				StringBuilder delta = new StringBuilder();
				for (int index = 0; index < artifactSnapshots.size(); index++) {
					if (artifactSnapshots.get(index) instanceof Map<?, ?> artifact) {
						delta.append(updateArtifact(artifact, false, "__default__" + index));
					}
				}
				return delta.toString();
			}

			String text = extractResponseText(result);
			this.messages.append(text);
			return text;
		}

		private String updateArtifact(Map<?, ?> artifact, boolean append, String fallbackId) {
			String text = extractTextParts(artifact.get("parts"));
			String artifactId = artifact.get("artifactId") instanceof String id ? id : fallbackId;
			String previous = this.artifacts.getOrDefault(artifactId, "");
			String updated = append ? previous + text : text;
			this.artifacts.put(artifactId, updated);
			if (append || previous.isEmpty()) {
				return text;
			}
			if (updated.startsWith(previous)) {
				return updated.substring(previous.length());
			}
			return updated.equals(previous) ? "" : updated;
		}

		String result() {
			StringBuilder result = new StringBuilder(this.messages);
			this.artifacts.values().forEach(result::append);
			return result.toString();
		}

	}

	/**
	 * Build the JSON-RPC request payload to send to the A2A server.
	 * @param state Parent state
	 * @return JSON string payload (e.g., JSON-RPC params)
	 */
	private String buildSendMessageRequest(OverAllState state, RunnableConfig config) {
		return buildSendRequest(state, config, false);
	}

	/**
	 * Build the JSON-RPC streaming request payload (method: SendStreamingMessage).
	 * @param state Parent state
	 * @return JSON string payload for streaming
	 */
	private String buildSendStreamingMessageRequest(OverAllState state, RunnableConfig config) {
		return buildSendRequest(state, config, true);
	}

	private String buildSendRequest(OverAllState state, RunnableConfig config, boolean streaming) {
		return buildSendRequest(state, config, streaming, this.agentCard.endpoint());
	}

	private String buildSendRequest(OverAllState state, RunnableConfig config, boolean streaming,
			AgentCardWrapper.AgentEndpoint endpoint) {
		Object textValue = getEffectiveInstruction(state);
		String text = String.valueOf(textValue);
		boolean legacyProtocol = isLegacyProtocolVersion(endpoint.protocolVersion());

		String id = UUID.randomUUID().toString();
		String messageId = UUID.randomUUID().toString().replace("-", "");

		Map<String, Object> part = new HashMap<>();
		part.put("text", text);
		if (legacyProtocol) {
			part.put("kind", "text");
		}

		Map<String, Object> message = new HashMap<>();
		message.put("messageId", messageId);
		message.put("parts", List.of(part));
		message.put("role", legacyProtocol ? "user" : Message.Role.ROLE_USER.name());
		if (legacyProtocol) {
			message.put("kind", "message");
		}

		Map<String, Object> params = new HashMap<>();
		params.put("message", message);

		Map<String, Object> metadata = new HashMap<>();
		config.threadId().ifPresent(threadId -> metadata.put("threadId", threadId));
		// FIXME, the key 'userId' should be configurable
		config.metadata("userId").ifPresent(userId -> metadata.put("userId", userId));
		params.put("metadata", metadata);
		if (!legacyProtocol && endpoint.tenant() != null && !endpoint.tenant().isBlank()) {
			params.put("tenant", endpoint.tenant());
		}

		Map<String, Object> root = new HashMap<>();
		root.put("id", id);
		root.put("jsonrpc", "2.0");
		root.put("method", requestMethod(streaming, legacyProtocol));
		root.put("params", params);

		try {
			return objectMapper.writeValueAsString(root);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to build JSON-RPC payload", e);
		}
	}

	private String requestMethod(boolean streaming, boolean legacyProtocol) {
		if (legacyProtocol) {
			return streaming ? LEGACY_SEND_STREAMING_MESSAGE_METHOD : LEGACY_SEND_MESSAGE_METHOD;
		}
		return streaming ? SEND_STREAMING_MESSAGE_METHOD : SEND_MESSAGE_METHOD;
	}

	private boolean isLegacyProtocolVersion(String protocolVersion) {
		return protocolVersion != null && protocolVersion.startsWith("0.");
	}

	private String getEffectiveInstruction(OverAllState state) {
		if (StringUtils.hasLength(this.instruction)) {
			PromptTemplate template = PromptTemplate.builder().template(this.instruction).build();
			return template.render(state.data());
		} else if (!shareState || (shareState && state.value("messages").isEmpty())) {
			throw new IllegalStateException("Instruction is empty and shareState is false");
		}
		return "";
	}

	/**
	 * Send the request to the remote A2A server and return the non-streaming response.
	 * @param endpoint Immutable endpoint selected from one agent-card snapshot
	 * @param requestPayload JSON string payload built by buildSendMessageRequest
	 * @return Response body as string
	 */
	private String sendMessageToServer(AgentCardWrapper.AgentEndpoint endpoint, String requestPayload) throws Exception {
		String baseUrl = resolveAgentBaseUrl(endpoint);
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("AgentCard.url is empty");
		}

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost post = createHttpPost(endpoint, baseUrl, false);
			post.setEntity(new StringEntity(requestPayload, ContentType.APPLICATION_JSON));

			try (CloseableHttpResponse response = httpClient.execute(post)) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					throw new IllegalStateException("HTTP request failed, status: " + statusCode);
				}
				HttpEntity entity = response.getEntity();
				if (entity == null) {
					throw new IllegalStateException("Empty HTTP entity");
				}
				return EntityUtils.toString(entity, "UTF-8");
			}
		}
	}

	private HttpPost createHttpPost(AgentCardWrapper agentCard, String baseUrl, boolean streaming) {
		return createHttpPost(agentCard.endpoint(), baseUrl, streaming);
	}

	private HttpPost createHttpPost(AgentCardWrapper.AgentEndpoint endpoint, String baseUrl, boolean streaming) {
		HttpPost post = new HttpPost(baseUrl);
		post.setHeader("Content-Type", "application/json");
		post.setHeader(A2AHeaders.A2A_VERSION, endpoint.protocolVersion());
		if (streaming) {
			post.setHeader("Accept", "text/event-stream");
		}
		return post;
	}

	/**
	 * Resolve base URL from the AgentCard.
	 */
	private String resolveAgentBaseUrl(AgentCardWrapper.AgentEndpoint endpoint) {
		return endpoint.url();
	}

}
