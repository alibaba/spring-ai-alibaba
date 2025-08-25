/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.a2a;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.a2a.spec.AgentCard;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.fastjson.JSON;

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
		String requestPayload = this.streaming ? buildSendStreamingMessageRequest(state, this.inputKeyFromParent)
				: buildSendMessageRequest(state, this.inputKeyFromParent);

		String resultText = sendMessageToServer(this.agentCard, requestPayload);

		Map<String, Object> resultMap = JSON.parseObject(resultText, Map.class);
		Map<String, Object> result = (Map<String, Object>) resultMap.get("result");
		
		String responseText = extractResponseText(result);
		return Map.of(this.outputKeyToParent, responseText);
	}

	private String extractResponseText(Map<String, Object> result) {
		if (result.containsKey("artifacts")) {
			List<Object> artifacts = (List<Object>) result.get("artifacts");
			StringBuilder responseBuilder = new StringBuilder();
			for (Object artifact : artifacts) {
				if (artifact instanceof Map) {
					List<Object> parts = (List<Object>) ((Map<String, Object>) artifact).get("parts");
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
			return responseBuilder.toString();
		} else {
			List<Object> parts = (List<Object>) result.get("parts");
			Map<String, Object> lastPart = (Map<String, Object>) parts.get(parts.size() - 1);
			return (String) lastPart.get("text");
		}
	}

	/**
	 * Build the JSON-RPC request payload to send to the A2A server.
	 *
	 * @param state    Parent state
	 * @param inputKey Input key to retrieve user input from the state
	 * @return JSON string payload (e.g., JSON-RPC params)
	 */
	private String buildSendMessageRequest(OverAllState state, String inputKey) {
		Object textValue = state.value(inputKey)
				.orElseThrow(() -> new IllegalArgumentException("Input key '" + inputKey + "' not found in state: " + state));
		String text = String.valueOf(textValue);

		String id = UUID.randomUUID().toString();
		String messageId = UUID.randomUUID().toString().replace("-", "");

		Map<String, Object> part = Map.of("kind", "text", "text", text);

		Map<String, Object> message = new HashMap<>();
		message.put("kind", "message");
		message.put("messageId", messageId);
		message.put("parts", List.of(part));
		message.put("role", "user");

		Map<String, Object> params = Map.of("message", message);

		Map<String, Object> root = new HashMap<>();
		root.put("id", id);
		root.put("jsonrpc", "2.0");
		root.put("method", "message/send");
		root.put("params", params);

		try {
			return objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to build JSON-RPC payload", e);
		}
	}

	/**
	 * Build the JSON-RPC streaming request payload (method: message/stream).
	 *
	 * @param state    Parent state
	 * @param inputKey Input key to retrieve user input from the state
	 * @return JSON string payload for streaming
	 */
	private String buildSendStreamingMessageRequest(OverAllState state, String inputKey) {
		Object textValue = state.value(inputKey)
				.orElseThrow(() -> new IllegalArgumentException("Input key '" + inputKey + "' not found in state: " + state));
		String text = String.valueOf(textValue);

		String id = UUID.randomUUID().toString();
		String messageId = UUID.randomUUID().toString().replace("-", "");

		Map<String, Object> part = Map.of("kind", "text", "text", text);

		Map<String, Object> message = new HashMap<>();
		message.put("kind", "message");
		message.put("messageId", messageId);
		message.put("parts", List.of(part));
		message.put("role", "user");

		Map<String, Object> params = Map.of("message", message);

		Map<String, Object> root = new HashMap<>();
		root.put("id", id);
		root.put("jsonrpc", "2.0");
		root.put("method", "message/stream");
		root.put("params", params);

		try {
			return objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to build JSON-RPC streaming payload", e);
		}
	}

	/**
	 * Send the request to the remote A2A server and return the non-streaming response.
	 *
	 * @param agentCard      Agent card (source for server URL/metadata)
	 * @param requestPayload JSON string payload built by buildSendMessageRequest
	 * @return Response body as string
	 */
	private String sendMessageToServer(AgentCard agentCard, String requestPayload) throws Exception {
		String baseUrl = resolveAgentBaseUrl(agentCard);
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