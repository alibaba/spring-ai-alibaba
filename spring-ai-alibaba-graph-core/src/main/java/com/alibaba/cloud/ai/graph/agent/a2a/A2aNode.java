/*
 * Copyright 2024-2025 the original author or authors.
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

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.AsyncGeneratorQueue;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.spec.AgentCard;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class A2aNode implements NodeAction {

	private final AgentCard agentCard;

	private final String inputKeyFromParent;

	private final String outputKeyToParent;

	private final boolean streaming;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public A2aNode(AgentCard agentCard, String inputKeyFromParent, String outputKeyToParent) {
		this(agentCard, inputKeyFromParent, outputKeyToParent, false);
	}

	public A2aNode(AgentCard agentCard, String inputKeyFromParent, String outputKeyToParent, boolean streaming) {
		this.agentCard = agentCard;
		this.inputKeyFromParent = inputKeyFromParent;
		this.outputKeyToParent = outputKeyToParent;
		this.streaming = streaming;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (streaming) {
			AsyncGenerator<NodeOutput> generator = createStreamingGenerator(state);
			return Map.of(StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages",
					generator);
		}
		else {
			String requestPayload = buildSendMessageRequest(state, this.inputKeyFromParent);
			String resultText = sendMessageToServer(this.agentCard, requestPayload);
			Map<String, Object> resultMap = autoDetectAndParseResponse(resultText);
			Map<String, Object> result = (Map<String, Object>) resultMap.get("result");
			String responseText = extractResponseText(result);
			return Map.of(this.outputKeyToParent, responseText);
		}
	}

	/**
	 * Create a streaming generator.
	 */
	private AsyncGenerator<NodeOutput> createStreamingGenerator(OverAllState state) throws Exception {
		final String requestPayload = buildSendStreamingMessageRequest(state, this.inputKeyFromParent);
		final BlockingQueue<AsyncGenerator.Data<NodeOutput>> queue = new LinkedBlockingQueue<>(1000);
		final String outputKey = StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages";
		final StringBuilder accumulated = new StringBuilder();

		return AsyncGeneratorQueue.of(queue, q -> {
			String baseUrl = resolveAgentBaseUrl(this.agentCard);
			if (baseUrl == null || baseUrl.isBlank()) {
				StreamingOutput errorOutput = new StreamingOutput("Error: AgentCard.url is empty", "a2aNode", state);
				queue.add(AsyncGenerator.Data.of(errorOutput));
				return;
			}

			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				HttpPost post = new HttpPost(baseUrl);
				post.setHeader("Content-Type", "application/json");
				post.setEntity(new StringEntity(requestPayload, ContentType.APPLICATION_JSON));

				try (CloseableHttpResponse response = httpClient.execute(post)) {
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != 200) {
						StreamingOutput errorOutput = new StreamingOutput("HTTP request failed, status: " + statusCode,
								"a2aNode", state);
						queue.add(AsyncGenerator.Data.of(errorOutput));
						return;
					}

					HttpEntity entity = response.getEntity();
					if (entity == null) {
						StreamingOutput errorOutput = new StreamingOutput("Empty HTTP entity", "a2aNode", state);
						queue.add(AsyncGenerator.Data.of(errorOutput));
						return;
					}

					String contentType = entity.getContentType() != null ? entity.getContentType().getValue() : "";
					boolean isEventStream = contentType.contains("text/event-stream");

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
								try {
									Map<String, Object> parsed = JSON.parseObject(jsonContent,
											new TypeReference<Map<String, Object>>() {
											});
									Map<String, Object> result = (Map<String, Object>) parsed.get("result");
									if (result != null) {
										String text = extractResponseText(result);
										if (text != null && !text.isEmpty()) {
											accumulated.append(text);
											queue.add(AsyncGenerator.Data
												.of(new StreamingOutput(text, "a2aNode", state)));
										}
									}
								}
								catch (Exception ignore) {
								}
							}
						}
					}
					else {
						// Non-SSE: read the full body and emit a single output
						String body = EntityUtils.toString(entity, "UTF-8");
						try {
							Map<String, Object> resultMap = JSON.parseObject(body,
									new TypeReference<Map<String, Object>>() {
									});
							Map<String, Object> result = (Map<String, Object>) resultMap.get("result");
							String text = extractResponseText(result);
							if (text != null && !text.isEmpty()) {
								accumulated.append(text);
								queue.add(AsyncGenerator.Data.of(new StreamingOutput(text, "a2aNode", state)));
							}
						}
						catch (Exception ex) {
							queue.add(AsyncGenerator.Data
								.of(new StreamingOutput("Error: " + ex.getMessage(), "a2aNode", state)));
						}
					}
				}
			}
			catch (Exception e) {
				StreamingOutput errorOutput = new StreamingOutput("Error: " + e.getMessage(), "a2aNode", state);
				queue.add(AsyncGenerator.Data.of(errorOutput));
			}
			finally {
				queue.add(AsyncGenerator.Data.done(Map.of(outputKey, accumulated.toString())));
			}
		});
	}

	/**
	 * Check whether the given text looks like an SSE response.
	 */
	private boolean isSSEResponse(String responseText) {
		return responseText.contains("data: ");
	}

	/**
	 * Create a streaming generator for SSE-formatted text.
	 */
	private AsyncGenerator<NodeOutput> createSseStreamingGenerator(String sseResponseText, OverAllState state) {
		// Use the new real-time streaming method
		return createRealTimeSseStreamingGenerator(sseResponseText, state);
	}

	/**
	 * Create a real-time SSE streaming generator (recommended). This method starts
	 * processing SSE data immediately and pushes chunks as they arrive.
	 */
	private AsyncGenerator<NodeOutput> createRealTimeSseStreamingGenerator(String sseResponseText, OverAllState state) {
		BlockingQueue<AsyncGenerator.Data<NodeOutput>> queue = new LinkedBlockingQueue<>(1000);
		final String outputKey = StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages";
		final StringBuilder accumulated = new StringBuilder();

		// Start async processing immediately; do not wait for the entire content
		return AsyncGeneratorQueue.of(queue, executor -> {
			try {
				// Process SSE response line by line to achieve true streaming
				String[] lines = sseResponseText.split("\n");

				for (String line : lines) {
					line = line.trim();
					if (line.startsWith("data: ")) {
						try {
							String jsonContent = line.substring(6); // remove "data: "
																	// prefix

							// End marker
							if ("[DONE]".equals(jsonContent)) {
								break;
							}

							Map<String, Object> parsed = JSON.parseObject(jsonContent,
									new TypeReference<Map<String, Object>>() {
									});
							Map<String, Object> result = (Map<String, Object>) parsed.get("result");

							if (result != null) {
								StreamingOutput streamingOutput = createStreamingOutputFromResult(result, state);
								if (streamingOutput != null) {
									queue.add(AsyncGenerator.Data.of(streamingOutput));
								}
							}
						}
						catch (Exception e) {
							// Ignore parse errors and continue
							continue;
						}
					}
				}

				// Signal completion with final result value
				queue.add(AsyncGenerator.Data.done(Map.of(outputKey, accumulated.toString())));

			}
			catch (Exception e) {
				// On error, emit an error message and signal completion
				StreamingOutput errorOutput = new StreamingOutput("Error: " + e.getMessage(), "a2aNode", state);
				queue.add(AsyncGenerator.Data.of(errorOutput));
				queue.add(AsyncGenerator.Data.done(Map.of(outputKey, accumulated.toString())));
			}
		});
	}

	/**
	 * Create a single-output streaming generator (for non-SSE responses).
	 */
	private AsyncGenerator<NodeOutput> createSingleStreamingGenerator(String responseText, OverAllState state) {
		BlockingQueue<AsyncGenerator.Data<NodeOutput>> queue = new LinkedBlockingQueue<>(10);
		final String outputKey = StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages";
		final StringBuilder accumulated = new StringBuilder();

		try {
			Map<String, Object> resultMap = JSON.parseObject(responseText, new TypeReference<Map<String, Object>>() {
			});
			Map<String, Object> result = (Map<String, Object>) resultMap.get("result");
			String responseText2 = extractResponseText(result);

			if (responseText2 != null && !responseText2.isEmpty()) {
				accumulated.append(responseText2);
				StreamingOutput streamingOutput = new StreamingOutput(responseText2, "a2aNode", state);
				queue.add(AsyncGenerator.Data.of(streamingOutput));
			}
		}
		catch (Exception e) {
			// On parse failure, emit an error message
			StreamingOutput errorOutput = new StreamingOutput("Error: " + e.getMessage(), "a2aNode", state);
			queue.add(AsyncGenerator.Data.of(errorOutput));
		}

		// Signal completion with final result value
		queue.add(AsyncGenerator.Data.done(Map.of(outputKey, accumulated.toString())));

		return new AsyncGeneratorQueue.Generator<>(queue);
	}

	/**
	 * Create a StreamingOutput from the parsed result map.
	 */
	private StreamingOutput createStreamingOutputFromResult(Map<String, Object> result, OverAllState state) {
		String text = extractResponseText(result);
		if (text != null && !text.isEmpty()) {
			return new StreamingOutput(text, "a2aNode", state);
		}
		return null;
	}

	/**
	 * Get the streaming generator (similar to LlmNode.stream).
	 */
	public AsyncGenerator<NodeOutput> stream(OverAllState state) throws Exception {
		if (!this.streaming) {
			throw new IllegalStateException("Streaming is not enabled for this A2aNode");
		}
		return createStreamingGenerator(state);
	}

	/**
	 * Get the non-streaming result (similar to LlmNode.call).
	 */
	public String call(OverAllState state) throws Exception {
		String requestPayload = buildSendMessageRequest(state, this.inputKeyFromParent);
		String resultText = sendMessageToServer(this.agentCard, requestPayload);
		Map<String, Object> resultMap = autoDetectAndParseResponse(resultText);
		Map<String, Object> result = (Map<String, Object>) resultMap.get("result");
		return extractResponseText(result);
	}

	/**
	 * Auto-detect response format and parse accordingly.
	 * @param responseText The raw response text
	 * @return Parsed result map
	 */
	private Map<String, Object> autoDetectAndParseResponse(String responseText) {
		if (responseText.contains("data: ")) {
			return parseStreamingResponse(responseText);
		}
		else {
			// Standard JSON response
			return JSON.parseObject(responseText, new TypeReference<Map<String, Object>>() {
			});
		}
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
			if (line.startsWith("data: ")) {
				String jsonContent = line.substring(6); // remove "data: " prefix
				try {
					Map<String, Object> parsed = JSON.parseObject(jsonContent,
							new TypeReference<Map<String, Object>>() {
							});
					Map<String, Object> result = (Map<String, Object>) parsed.get("result");
					if (result != null) {
						if (result.containsKey("artifact") || lastResult == null) {
							lastResult = result;
						}
					}
				}
				catch (Exception e) {
					continue;
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

	private String extractResponseText(Map<String, Object> result) {
		if (result == null) {
			throw new IllegalStateException("Result is null, cannot extract response text");
		}

		if ("status-update".equals(result.get("kind"))) {
			Map<String, Object> status = (Map<String, Object>) result.get("status");
			if (status != null) {
				String state = (String) status.get("state");
				if ("completed".equals(state)) {
					return "";
				}
				else if ("processing".equals(state)) {
					return "";
				}
				else if ("failed".equals(state)) {
					return "";
				}
				else if ("working".equals(state)) {
					Map<String, Object> message = (Map<String, Object>) status.get("message");
					if (message != null && message.containsKey("parts")) {
						List<Object> parts = (List<Object>) message.get("parts");
						if (parts != null && !parts.isEmpty()) {
							Map<String, Object> lastPart = (Map<String, Object>) parts.get(parts.size() - 1);
							if (lastPart != null) {
								String text = (String) lastPart.get("text");
								if (text != null) {
									return text;
								}
							}
						}
					}
					return "";
				}
				else {
					return "Agent State: " + state;
				}
			}
			return "";
		}

		if ("artifact-update".equals(result.get("kind"))) {
			Map<String, Object> artifact = (Map<String, Object>) result.get("artifact");
			if (artifact != null && artifact.containsKey("parts")) {
				List<Object> parts = (List<Object>) artifact.get("parts");
				if (parts != null && !parts.isEmpty()) {
					StringBuilder responseBuilder = new StringBuilder();
					for (Object part : parts) {
						if (part instanceof Map) {
							String text = (String) ((Map<String, Object>) part).get("text");
							if (text != null) {
								responseBuilder.append(text);
							}
						}
					}
					String response = responseBuilder.toString();
					if (!response.isEmpty()) {
						return response;
					}
				}
			}
			return "";
		}
		if (result.containsKey("artifacts")) {
			List<Object> artifacts = (List<Object>) result.get("artifacts");
			if (artifacts != null && !artifacts.isEmpty()) {
				StringBuilder responseBuilder = new StringBuilder();
				for (Object artifact : artifacts) {
					if (artifact instanceof Map) {
						List<Object> parts = (List<Object>) ((Map<String, Object>) artifact).get("parts");
						if (parts != null) {
							for (Object part : parts) {
								if (part instanceof Map) {
									String text = (String) ((Map<String, Object>) part).get("text");
									if (text != null) {
										responseBuilder.append(text);
									}
								}
							}
						}
					}
				}
				String response = responseBuilder.toString();
				if (!response.isEmpty()) {
					return response;
				}
			}
		}
		if (result.containsKey("parts")) {
			List<Object> parts = (List<Object>) result.get("parts");
			if (parts != null && !parts.isEmpty()) {
				Map<String, Object> lastPart = (Map<String, Object>) parts.get(parts.size() - 1);
				if (lastPart != null) {
					String text = (String) lastPart.get("text");
					if (text != null) {
						return text;
					}
				}
			}
		}
		if (result.containsKey("message")) {
			Map<String, Object> message = (Map<String, Object>) result.get("message");
			if (message != null && message.containsKey("parts")) {
				List<Object> parts = (List<Object>) message.get("parts");
				if (parts != null && !parts.isEmpty()) {
					Map<String, Object> lastPart = (Map<String, Object>) parts.get(parts.size() - 1);
					if (lastPart != null) {
						String text = (String) lastPart.get("text");
						if (text != null) {
							return text;
						}
					}
				}
			}
		}
		throw new IllegalStateException("No valid text content found in result: " + result);
	}

	/**
	 * Build the JSON-RPC request payload to send to the A2A server.
	 * @param state Parent state
	 * @param inputKey Input key to retrieve user input from the state
	 * @return JSON string payload (e.g., JSON-RPC params)
	 */
	private String buildSendMessageRequest(OverAllState state, String inputKey) {
		Object textValue = state.value(inputKey)
			.orElseThrow(
					() -> new IllegalArgumentException("Input key '" + inputKey + "' not found in state: " + state));
		String text = String.valueOf(textValue);

		String id = UUID.randomUUID().toString();
		String messageId = UUID.randomUUID().toString().replace("-", "");

		Map<String, Object> part = Map.of("kind", "text", "text", text);

		Map<String, Object> message = new HashMap<>();
		message.put("kind", "message");
		message.put("messageId", messageId);
		message.put("parts", List.of(part));
		message.put("role", "user");

		Map<String, String> metadata = new HashMap<>();
		metadata.put("userId",
				state.data().containsKey("userId") ? String.valueOf(state.data().get("userId")) : "default_user");
		metadata.put("sessionId", state.data().containsKey("sessionId") ? String.valueOf(state.data().get("sessionId"))
				: "default_session");
		message.put("metadata", metadata);

		Map<String, Object> params = Map.of("message", message);

		Map<String, Object> root = new HashMap<>();
		root.put("id", id);
		root.put("jsonrpc", "2.0");
		root.put("method", "message/send");
		root.put("params", params);

		try {
			return objectMapper.writeValueAsString(root);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to build JSON-RPC payload", e);
		}
	}

	/**
	 * Build the JSON-RPC streaming request payload (method: message/stream).
	 * @param state Parent state
	 * @param inputKey Input key to retrieve user input from the state
	 * @return JSON string payload for streaming
	 */
	private String buildSendStreamingMessageRequest(OverAllState state, String inputKey) {
		Object textValue = state.value(inputKey)
			.orElseThrow(
					() -> new IllegalArgumentException("Input key '" + inputKey + "' not found in state: " + state));
		String text = String.valueOf(textValue);

		String id = UUID.randomUUID().toString();
		String messageId = UUID.randomUUID().toString().replace("-", "");

		Map<String, Object> part = Map.of("kind", "text", "text", text);

		Map<String, Object> message = new HashMap<>();
		message.put("kind", "message");
		message.put("messageId", messageId);
		message.put("parts", List.of(part));
		message.put("role", "user");

		Map<String, String> metadata = new HashMap<>();
		metadata.put("userId",
				state.data().containsKey("userId") ? String.valueOf(state.data().get("userId")) : "default_user");
		metadata.put("sessionId", state.data().containsKey("sessionId") ? String.valueOf(state.data().get("sessionId"))
				: "default_session");
		message.put("metadata", metadata);

		Map<String, Object> params = Map.of("message", message);

		Map<String, Object> root = new HashMap<>();
		root.put("id", id);
		root.put("jsonrpc", "2.0");
		root.put("method", "message/stream");
		root.put("params", params);

		try {
			return objectMapper.writeValueAsString(root);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to build JSON-RPC streaming payload", e);
		}
	}

	/**
	 * Send the request to the remote A2A server and return the non-streaming response.
	 * @param agentCard Agent card (source for server URL/metadata)
	 * @param requestPayload JSON string payload built by buildSendMessageRequest
	 * @return Response body as string
	 */
	private String sendMessageToServer(AgentCard agentCard, String requestPayload) throws Exception {
		String baseUrl = resolveAgentBaseUrl(agentCard);
		System.out.println(baseUrl);
		System.out.println(requestPayload);
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("AgentCard.url is empty");
		}

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(baseUrl);
			post.setHeader("Content-Type", "application/json");
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

	/**
	 * Resolve base URL from the AgentCard.
	 */
	private String resolveAgentBaseUrl(AgentCard agentCard) {
		return agentCard.url();
	}

}
