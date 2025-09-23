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
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Node: 多通道处理节点，支持 MCP 协议和 HTTP 流式处理
 * 作为图编排中的能力聚合和分发枢纽
 */
public class McpNode implements AsyncNodeAction {

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private static final Pattern SSE_DATA_PATTERN = Pattern.compile("^data: (.*)$", Pattern.MULTILINE);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final Logger log = LoggerFactory.getLogger(McpNode.class);

	// 处理模式枚举
	public enum McpProcessMode {
		/**
		 * MCP 同步模式 - 原有功能
		 */
		MCP_SYNC,
		/**
		 * HTTP 流式模式 - 新增能力
		 */
		HTTP_STREAM
	}

	/**
	 * 流格式枚举
	 */
	public enum StreamFormat {
		/**
		 * Server-Sent Events格式
		 */
		SSE,
		/**
		 * JSON Lines格式 (每行一个JSON对象)
		 */
		JSON_LINES,
		/**
		 * 纯文本流，按分隔符分割
		 */
		TEXT_STREAM
	}

	/**
	 * 流处理模式枚举
	 */
	public enum StreamMode {
		/**
		 * 分发模式：流中的每个元素都触发下游节点执行
		 */
		DISTRIBUTE,
		/**
		 * 聚合模式：收集完整流后再执行下游节点
		 */
		AGGREGATE
	}

	// 原有 MCP 配置
	private final String url;
	private final String tool;
	private final Map<String, String> headers;
	private final Map<String, Object> params;
	private final String outputKey;
	private final List<String> inputParamKeys;

	// 处理模式配置
	private final McpProcessMode processMode;

	// HTTP 流式处理配置
	private final HttpMethod httpMethod;
	private final Map<String, String> queryParams;
	private final StreamFormat streamFormat;
	private final StreamMode streamMode;
	private final Duration readTimeout;
	private final boolean allowInternalAddress;
	private final Duration bufferTimeout;
	private final String delimiter;
	private final WebClient webClient;

	// MCP 客户端（仅在 MCP_SYNC 模式使用）
	private HttpClientSseClientTransport transport;
	private McpSyncClient client;

	private McpNode(Builder builder) {
		this.url = builder.url;
		this.tool = builder.tool;
		this.headers = builder.headers;
		this.params = builder.params;
		this.outputKey = builder.outputKey;
		this.inputParamKeys = builder.inputParamKeys;

		// 处理模式配置
		this.processMode = builder.processMode;

		// HTTP 流式处理配置
		this.httpMethod = builder.httpMethod;
		this.queryParams = builder.queryParams;
		this.streamFormat = builder.streamFormat;
		this.streamMode = builder.streamMode;
		this.readTimeout = builder.readTimeout;
		this.allowInternalAddress = builder.allowInternalAddress;
		this.bufferTimeout = builder.bufferTimeout;
		this.delimiter = builder.delimiter;
		this.webClient = builder.webClient;
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
		try {
			// 根据处理模式路由到不同的处理逻辑
			return switch (processMode) {
				case MCP_SYNC -> handleMcpSync(state);
				case HTTP_STREAM -> handleHttpStream(state);
			};
		} catch (Exception e) {
			log.error("[McpNode] Execution failed: mode={}, error={}", processMode, e.getMessage(), e);
			return CompletableFuture.completedFuture(createErrorOutput(e));
		}
	}

	/**
	 * 处理 MCP 同步模式 - 保持原有逻辑
	 */
	private CompletableFuture<Map<String, Object>> handleMcpSync(OverAllState state) throws Exception {
		log.info(
				"[McpNode] Start executing MCP sync, original configuration: url={}, tool={}, headers={}, inputParamKeys={}",
				url, tool, headers, inputParamKeys);

		// Build transport and client
		String baseUrl = this.url;
		String sseEndpoint = "/sse";
		if (this.url.contains("/sse?")) {
			int idx = this.url.indexOf("/sse?");
			baseUrl = this.url.substring(0, idx);
			sseEndpoint = this.url.substring(idx); // e.g. /sse?key=xxx
		}
		HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(baseUrl)
			.sseEndpoint(sseEndpoint);
		if (this.headers != null && !this.headers.isEmpty()) {
			transportBuilder.customizeRequest(req -> this.headers.forEach(req::header));
		}
		this.transport = transportBuilder.build();
		this.client = McpClient.sync(this.transport).build();
		InitializeResult initializeResult = this.client.initialize();
		log.info("[McpNode] MCP Client initialized: {}", initializeResult);
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
		log.info("[McpNode] MCP sync result: {}", updatedState);
		return CompletableFuture.completedFuture(updatedState);
	}

	/**
	 * 处理 HTTP 流式模式
	 */
	private CompletableFuture<Map<String, Object>> handleHttpStream(OverAllState state) {
		try {
			// 获取流式数据并转换为AsyncGenerator
			Flux<Map<String, Object>> streamFlux = executeStreaming(state);

			// 将Flux转换为AsyncGenerator，供图框架处理流式数据
			AsyncGenerator<Map<String, Object>> generator = createAsyncGenerator(streamFlux);

			// 返回包含AsyncGenerator的结果Map
			String outputKey = this.outputKey != null ? this.outputKey : "stream_output";
			return CompletableFuture.completedFuture(Map.of(outputKey, generator));
		}
		catch (Exception e) {
			log.error("[McpNode] HTTP stream initialization failed: url={}, method={}, error={}", url,
					httpMethod, e.getMessage(), e);
			// 返回包含错误信息的AsyncGenerator而不是直接返回Map
			String outputKey = this.outputKey != null ? this.outputKey : "stream_output";
			Flux<Map<String, Object>> errorFlux = Flux.just(createErrorOutput(e));
			AsyncGenerator<Map<String, Object>> errorGenerator = createAsyncGenerator(errorFlux);
			return CompletableFuture.completedFuture(Map.of(outputKey, errorGenerator));
		}
	}

	/**
	 * 执行流式HTTP请求
	 */
	private Flux<Map<String, Object>> executeStreaming(OverAllState state) throws Exception {
		String finalUrl = replaceVariables(this.url, state);
		Map<String, String> finalHeaders = replaceVariables(this.headers, state);
		Map<String, String> finalQueryParams = replaceVariables(this.queryParams, state);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(finalUrl);
		finalQueryParams.forEach(uriBuilder::queryParam);
		URI finalUri = uriBuilder.build().toUri();

		validateUrl(finalUri.toString());

		WebClient.RequestBodySpec requestSpec = webClient
			.method(httpMethod)
			.uri(finalUri)
			.headers(headers -> headers.setAll(finalHeaders));

		// 处理请求体
		if (params != null && !params.isEmpty()) {
			Map<String, Object> finalParams = replaceVariablesObj(params, state);
			requestSpec.headers(h -> h.setContentType(MediaType.APPLICATION_JSON));
			requestSpec.bodyValue(finalParams);
		}

		// 直接返回处理后的结果，将HTTP错误转换为错误数据项
		return requestSpec.exchangeToFlux(response -> {
			if (!response.statusCode().is2xxSuccessful()) {
				// 处理HTTP错误
				return response.bodyToMono(String.class)
					.defaultIfEmpty("HTTP Error")
					.map(errorBody -> {
						WebClientResponseException exception = new WebClientResponseException(
								response.statusCode().value(), "HTTP " + response.statusCode() + ": " + errorBody,
								null, null, null);
						return createErrorOutput(exception);
					})
					.flux();
			}

			// 处理成功响应
			Flux<DataBuffer> dataBufferFlux = response.bodyToFlux(DataBuffer.class);
			return processStreamResponse(dataBufferFlux, state);
		})
			.retryWhen(Retry.backoff(3, Duration.ofMillis(1000))) // 默认重试配置
			.timeout(readTimeout)
			.onErrorResume(throwable -> {
				log.error("[McpNode] HTTP stream execution failed: url={}, method={}, error={}", finalUrl,
						httpMethod, throwable.getMessage(), throwable);
				return Flux.just(createErrorOutput(throwable));
			});
	}

	/**
	 * 将Flux转换为AsyncGenerator
	 */
	private AsyncGenerator<Map<String, Object>> createAsyncGenerator(Flux<Map<String, Object>> flux) {
		return new AsyncGenerator<Map<String, Object>>() {
			private final java.util.concurrent.BlockingQueue<AsyncGenerator.Data<Map<String, Object>>> queue =
				new java.util.concurrent.LinkedBlockingQueue<>();

			{
				// 异步处理Flux数据
				flux.subscribe(
					data -> queue.offer(AsyncGenerator.Data.of(CompletableFuture.completedFuture(data))),
					error -> queue.offer(AsyncGenerator.Data.error(error)),
					() -> queue.offer(AsyncGenerator.Data.done())
				);
			}

			@Override
			public AsyncGenerator.Data<Map<String, Object>> next() {
				try {
					return queue.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return AsyncGenerator.Data.error(e);
				}
			}
		};
	}

	/**
	 * 处理流式响应数据
	 */
	private Flux<Map<String, Object>> processStreamResponse(Flux<DataBuffer> responseFlux, OverAllState state) {
		return responseFlux.map(dataBuffer -> {
			byte[] bytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(bytes);
			return new String(bytes, StandardCharsets.UTF_8);
		})
			.buffer(bufferTimeout)
			.map(chunks -> String.join("", chunks))
			.flatMap(this::parseStreamChunk)
			.filter(data -> {
				if (data instanceof String) {
					return !((String) data).isEmpty();
				}
				return data != null;
			})
			.map(this::wrapOutput)
			.transform(flux -> {
				if (streamMode == StreamMode.AGGREGATE) {
					return flux.collectList().map(this::aggregateResults).flux();
				}
				return flux;
			})
			.onErrorResume(error -> {
				log.error("[McpNode] Error processing stream response", error);
				return Flux.just(createErrorOutput(error));
			});
	}

	/**
	 * 解析流数据块
	 */
	private Flux<Object> parseStreamChunk(String chunk) {
		return switch (streamFormat) {
			case SSE -> parseSSEChunk(chunk).cast(Object.class);
			case JSON_LINES -> parseJsonLinesChunk(chunk);
			case TEXT_STREAM -> parseTextStreamChunk(chunk).cast(Object.class);
		};
	}

	/**
	 * 解析SSE格式数据
	 */
	private Flux<String> parseSSEChunk(String chunk) {
		List<String> results = new ArrayList<>();
		Matcher matcher = SSE_DATA_PATTERN.matcher(chunk);

		while (matcher.find()) {
			String data = matcher.group(1).trim();
			if (!data.isEmpty() && !"[DONE]".equals(data)) {
				results.add(data);
			}
		}

		return Flux.fromIterable(results);
	}

	/**
	 * 解析JSON Lines格式数据
	 */
	private Flux<Object> parseJsonLinesChunk(String chunk) {
		String[] lines = chunk.split("\n");
		List<Object> results = new ArrayList<>();

		for (String line : lines) {
			line = line.trim();
			if (!line.isEmpty()) {
				try {
					objectMapper.readTree(line);
					results.add(line);
				}
				catch (JsonProcessingException e) {
					log.warn("[McpNode] Invalid JSON line: {}, error: {}", line, e.getMessage());
					Map<String, Object> errorMap = new HashMap<>();
					errorMap.put("_parsing_error", e.getMessage());
					errorMap.put("_raw_data", line);
					results.add(errorMap);
				}
			}
		}

		return Flux.fromIterable(results);
	}

	/**
	 * 解析文本流数据
	 */
	private Flux<String> parseTextStreamChunk(String chunk) {
		String[] parts = chunk.split(Pattern.quote(delimiter));
		List<String> results = new ArrayList<>();

		for (String part : parts) {
			part = part.trim();
			if (!part.isEmpty()) {
				results.add(part);
			}
		}

		return Flux.fromIterable(results);
	}

	/**
	 * 包装输出数据
	 */
	private Map<String, Object> wrapOutput(Object data) {
		Map<String, Object> result = new HashMap<>();

		if (data instanceof Map) {
			result.put("data", data);
		}
		else if (data instanceof String) {
			String stringData = (String) data;
			try {
				if (stringData.startsWith("{") || stringData.startsWith("[")) {
					JsonNode jsonNode = objectMapper.readTree(stringData);
					Object parsedData = objectMapper.convertValue(jsonNode, Object.class);
					result.put("data", parsedData);
				}
				else {
					result.put("data", stringData);
				}
			}
			catch (JsonProcessingException e) {
				result.put("data", stringData);
			}
		}
		else {
			result.put("data", data);
		}

		result.put("timestamp", System.currentTimeMillis());
		result.put("streaming", true);

		if (StringUtils.hasLength(outputKey)) {
			Map<String, Object> keyedResult = new HashMap<>();
			keyedResult.put(outputKey, result);
			return keyedResult;
		}

		return result;
	}

	/**
	 * 聚合模式下的结果汇总
	 */
	private Map<String, Object> aggregateResults(List<Map<String, Object>> results) {
		Map<String, Object> aggregated = new HashMap<>();
		List<Object> dataList = new ArrayList<>();

		for (Map<String, Object> result : results) {
			if (outputKey != null && result.containsKey(outputKey)) {
				Map<String, Object> keyedData = (Map<String, Object>) result.get(outputKey);
				dataList.add(keyedData.get("data"));
			}
			else {
				dataList.add(result.get("data"));
			}
		}

		aggregated.put("data", dataList);
		aggregated.put("count", results.size());
		aggregated.put("streaming", false);
		aggregated.put("aggregated", true);
		aggregated.put("timestamp", System.currentTimeMillis());

		if (StringUtils.hasLength(outputKey)) {
			Map<String, Object> keyedResult = new HashMap<>();
			keyedResult.put(outputKey, aggregated);
			return keyedResult;
		}

		return aggregated;
	}

	/**
	 * 创建错误输出
	 */
	private Map<String, Object> createErrorOutput(Throwable error) {
		Map<String, Object> errorResult = new HashMap<>();
		errorResult.put("error", error.getMessage());
		errorResult.put("timestamp", System.currentTimeMillis());
		errorResult.put("streaming", false);

		if (StringUtils.hasLength(outputKey)) {
			Map<String, Object> keyedResult = new HashMap<>();
			keyedResult.put(outputKey, errorResult);
			return keyedResult;
		}

		return errorResult;
	}

	/**
	 * URL安全验证
	 */
	private void validateUrl(String url) {
		try {
			URI uri = URI.create(url);
			String host = uri.getHost();

			if (host == null) {
				throw new IllegalArgumentException("Invalid URL: missing host");
			}

			// 检查内网地址访问权限
			if (isInternalAddress(host) && !allowInternalAddress) {
				throw new SecurityException(
						"Internal network access not allowed: " + host + ". Set allowInternalAddress=true to enable.");
			}

			// 验证协议
			String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				throw new IllegalArgumentException("Only HTTP/HTTPS protocols are supported: " + scheme);
			}

		}
		catch (IllegalArgumentException | SecurityException e) {
			throw new McpNodeException("URL validation failed: " + e.getMessage(), e);
		}
	}

	/**
	 * 检查是否为内网地址
	 */
	private boolean isInternalAddress(String host) {
		return host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.")
				|| host.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*") || "localhost".equalsIgnoreCase(host);
	}

	private String replaceVariables(String template, OverAllState state) {
		if (template == null)
			return null;
		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			Object value = state.value(key).orElse("");
			log.debug("[McpNode] replace param: {} -> {}", key, value);
			matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private Map<String, String> replaceVariables(Map<String, String> map, OverAllState state) {
		if (map == null) return new HashMap<>();
		Map<String, String> result = new HashMap<>();
		map.forEach((k, v) -> result.put(k, replaceVariables(v, state)));
		return result;
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

		// 原有 MCP 配置
		private String url;
		private String tool;
		private Map<String, String> headers = new HashMap<>();
		private Map<String, Object> params = new HashMap<>();
		private String outputKey;
		private List<String> inputParamKeys;

		// 处理模式配置
		private McpProcessMode processMode = McpProcessMode.MCP_SYNC; // 默认保持原有行为

		// HTTP 流式处理配置
		private HttpMethod httpMethod = HttpMethod.GET;
		private Map<String, String> queryParams = new HashMap<>();
		private StreamFormat streamFormat = StreamFormat.SSE;
		private StreamMode streamMode = StreamMode.DISTRIBUTE;
		private Duration readTimeout = Duration.ofMinutes(5);
		private boolean allowInternalAddress = false;
		private Duration bufferTimeout = Duration.ofMillis(100);
		private String delimiter = "\n";
		private WebClient webClient = WebClient.create();

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

		// 处理模式配置
		public Builder processMode(McpProcessMode processMode) {
			this.processMode = processMode;
			return this;
		}

		// HTTP 流式处理配置方法
		public Builder httpMethod(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		public Builder queryParam(String name, String value) {
			this.queryParams.put(name, value);
			return this;
		}

		public Builder queryParams(Map<String, String> queryParams) {
			this.queryParams.putAll(queryParams);
			return this;
		}

		public Builder streamFormat(StreamFormat streamFormat) {
			this.streamFormat = streamFormat;
			return this;
		}

		public Builder streamMode(StreamMode streamMode) {
			this.streamMode = streamMode;
			return this;
		}

		public Builder readTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}

		public Builder allowInternalAddress(boolean allowInternalAddress) {
			this.allowInternalAddress = allowInternalAddress;
			return this;
		}

		public Builder bufferTimeout(Duration bufferTimeout) {
			this.bufferTimeout = bufferTimeout;
			return this;
		}

		public Builder delimiter(String delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		public Builder webClient(WebClient webClient) {
			this.webClient = webClient;
			return this;
		}

		/**
		 * 便捷方法：启用HTTP流式模式
		 */
		public Builder enableHttpStream() {
			this.processMode = McpProcessMode.HTTP_STREAM;
			return this;
		}

		/**
		 * 便捷方法：启用HTTP流式模式并设置基本参数
		 */
		public Builder enableHttpStream(HttpMethod method, StreamFormat format) {
			this.processMode = McpProcessMode.HTTP_STREAM;
			this.httpMethod = method;
			this.streamFormat = format;
			return this;
		}

		public McpNode build() {
			// 验证配置
			if (url == null || url.trim().isEmpty()) {
				throw new IllegalArgumentException("URL cannot be null or empty");
			}

			if (processMode == McpProcessMode.MCP_SYNC && (tool == null || tool.trim().isEmpty())) {
				throw new IllegalArgumentException("Tool name is required for MCP_SYNC mode");
			}

			return new McpNode(this);
		}

	}

	public static class McpNodeException extends RuntimeException {

		public McpNodeException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
