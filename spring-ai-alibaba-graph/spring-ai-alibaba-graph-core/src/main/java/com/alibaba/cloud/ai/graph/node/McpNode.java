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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Node: Node for calling MCP Server
 */
public class McpNode implements NodeAction {

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private static final Logger log = LoggerFactory.getLogger(McpNode.class);

	private final String url;

	private final String tool;

	private final Map<String, String> headers;

	private final Map<String, Object> params;

	private final String outputKey;

	private final List<String> inputParamKeys;

	private HttpClientSseClientTransport transport;

	private McpSyncClient client;

	private McpNode(Builder builder) {
		this.url = builder.url;
		this.tool = builder.tool;
		this.headers = builder.headers;
		this.params = builder.params;
		this.outputKey = builder.outputKey;
		this.inputParamKeys = builder.inputParamKeys;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		log.info(
				"[McpNode] Start executing apply, original configuration: url={}, tool={}, headers={}, inputParamKeys={}",
				url, tool, headers, inputParamKeys);

		// Build transport and client
		HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(this.url);
		if (this.headers != null && !this.headers.isEmpty()) {
			transportBuilder.customizeRequest(req -> this.headers.forEach(req::header));
		}
		this.transport = transportBuilder.build();
		this.client = McpClient.sync(this.transport).build();
		this.client.initialize();
		// Variable replacement
		String finalTool = replaceVariables(tool, state);
		Map<String, Object> finalParams = new HashMap<>();
		// 1. First read from inputParamKeys
		if (inputParamKeys != null) {
			for (String key : inputParamKeys) {
				Object value = state.value(key).orElse(null);
				if (value != null) {
					finalParams.put(key, value);
				}
			}
		}
		// 2. Then use params (after variable replacement) to overwrite
		Map<String, Object> replacedParams = replaceVariablesObj(params, state);
		if (replacedParams != null) {
			finalParams.putAll(replacedParams);
		}
		log.info("[McpNode] after replace params: url={}, tool={}, headers={}, params={}", url, finalTool, headers,
				finalParams);

		// Directly use the already initialized client
		CallToolResult result;
		try {
			McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(finalTool, finalParams);
			log.info("[McpNode] CallToolRequest: {}", request);
			result = client.callTool(request);
			log.info("[McpNode] tool call result: {}", result);
		}
		catch (Exception e) {
			log.error("[McpNode] MCP call fail:", e);
			throw new McpNodeException("MCP call fail: " + e.getMessage(), e);
		}

		// Result handling
		Map<String, Object> updatedState = new HashMap<>();
		// updatedState.put("mcp_result", result.content());
		updatedState.put("messages", result.content());
		if (StringUtils.hasLength(this.outputKey)) {
			Object content = result.content();
			if (content instanceof List<?> list && !CollectionUtils.isEmpty(list)) {
				Object first = list.get(0);
				// Compatible with the text field of TextContent
				if (first instanceof TextContent textContent) {
					updatedState.put(this.outputKey, textContent.text());
				}
				else if (first instanceof Map<?, ?> map && map.containsKey("text")) {
					updatedState.put(this.outputKey, map.get("text"));
				}
				else {
					updatedState.put(this.outputKey, first);
				}
			}
			else {
				updatedState.put(this.outputKey, content);
			}
		}
		log.info("[McpNode] update state: {}", updatedState);
		return updatedState;
	}

	private String replaceVariables(String template, OverAllState state) {
		if (template == null)
			return null;
		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			Object value = state.value(key).orElse("");
			log.info("[McpNode] replace param: {} -> {}", key, value);
			matcher.appendReplacement(result, value.toString());
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private Map<String, Object> replaceVariablesObj(Map<String, Object> map, OverAllState state) {
		if (map == null)
			return null;
		Map<String, Object> result = new HashMap<>();
		map.forEach((k, v) -> {
			if (v instanceof String) {
				result.put(k, replaceVariables((String) v, state));
			}
			else {
				result.put(k, v);
			}
		});
		return result;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String url;

		private String tool;

		private Map<String, String> headers = new HashMap<>();

		private Map<String, Object> params = new HashMap<>();

		private String outputKey;

		private List<String> inputParamKeys;

		public Builder url(String url) {
			this.url = url;
			return this;
		}

		public Builder tool(String tool) {
			this.tool = tool;
			return this;
		}

		public Builder header(String name, String value) {
			this.headers.put(name, value);
			return this;
		}

		public Builder param(String name, Object value) {
			this.params.put(name, value);
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder inputParamKeys(List<String> inputParamKeys) {
			this.inputParamKeys = inputParamKeys;
			return this;
		}

		public McpNode build() {
			return new McpNode(this);
		}

	}

	public static class McpNodeException extends RuntimeException {

		public McpNodeException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
