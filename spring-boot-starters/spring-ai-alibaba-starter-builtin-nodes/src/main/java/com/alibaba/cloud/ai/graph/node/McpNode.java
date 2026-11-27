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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.modern.client.transport.HttpClientMcpTransport;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequestCustomizer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.modern.McpSchemaConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
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

	private final Protocol protocol;

	private McpNode(Builder builder) {
		this.url = builder.url;
		this.tool = builder.tool;
		this.headers = builder.headers;
		this.params = builder.params;
		this.outputKey = builder.outputKey;
		this.inputParamKeys = builder.inputParamKeys;
		this.protocol = builder.protocol;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
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
		CallToolResult result;
		try {
			result = callTool(finalTool, finalParams);
		}
		catch (Exception e) {
			throw new McpNodeException("MCP call failed: " + e.getMessage(), e);
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

	private CallToolResult callTool(String toolName, Map<String, Object> arguments) {
		URI endpoint = URI.create(this.url);
		String origin = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
		String path = endpoint.getRawPath();
		String query = endpoint.getRawQuery() == null ? "" : "?" + endpoint.getRawQuery();
		var request = new McpSchema.CallToolRequest(toolName, arguments);
		if (this.protocol == Protocol.MODERN) {
			URI modernEndpoint = StringUtils.hasText(path) && !"/".equals(path)
					? endpoint : URI.create(origin + "/mcp" + query);
			var transport = HttpClientMcpTransport.builder(modernEndpoint)
					.requestCustomizer(McpHttpRequestCustomizer.fromSync((method, requestHeaders, context) ->
							this.headers.forEach(requestHeaders::set)))
					.build();
			var client = io.modelcontextprotocol.modern.client.McpClient.sync(transport).build();
			return McpSchemaConverter.convert(client.tool(toolName).arguments(arguments).call(), CallToolResult.class);
		}
		if (this.protocol == Protocol.STREAMABLE) {
			var transport = HttpClientStreamableHttpTransport.builder(origin)
					.endpoint((StringUtils.hasText(path) && !"/".equals(path) ? path : "/mcp") + query)
					.httpRequestCustomizer((builder, method, uri, body, context) -> this.headers.forEach(builder::header))
					.build();
			try (var client = McpClient.sync(transport).build()) {
				client.initialize();
				return client.callTool(request);
			}
		}

		String ssePath = path.endsWith("/sse") ? path : path.replaceFirst("/$", "") + "/sse";
		var transport = HttpClientSseClientTransport.builder(origin)
				.sseEndpoint(ssePath + query)
				.httpRequestCustomizer((builder, method, uri, body, context) -> this.headers.forEach(builder::header))
				.build();
		try (var client = McpClient.sync(transport).build()) {
			client.initialize();
			return client.callTool(request);
		}
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

	public enum Protocol {
		SSE,
		STREAMABLE,
		MODERN
	}

	public static class Builder {

		private Protocol protocol = Protocol.SSE;

		public Builder protocol(Protocol protocol) {
			this.protocol = Objects.requireNonNull(protocol);
			return this;
		}


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
