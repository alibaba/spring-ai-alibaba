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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.streaming.StreamHttpNodeParam.StreamMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
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

import static com.alibaba.cloud.ai.graph.streaming.StreamHttpNodeParam.StreamFormat.*;

public class StreamHttpNode implements AsyncNodeAction {

	private static final Logger logger = LoggerFactory.getLogger(StreamHttpNode.class);

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private static final Pattern SSE_DATA_PATTERN = Pattern.compile("^data: (.*)$", Pattern.MULTILINE);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final StreamHttpNodeParam param;

	public StreamHttpNode(StreamHttpNodeParam param) {
		this.param = param;
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
		try {
			// 获取流式数据并转换为AsyncGenerator
			Flux<Map<String, Object>> streamFlux = executeStreaming(state);
			
			// 将Flux转换为AsyncGenerator，供图框架处理流式数据
			AsyncGenerator<Map<String, Object>> generator = createAsyncGenerator(streamFlux);
			
			// 返回包含AsyncGenerator的结果Map
			String outputKey = param.getOutputKey() != null ? param.getOutputKey() : "stream_output";
			return CompletableFuture.completedFuture(Map.of(outputKey, generator));
		}
		catch (Exception e) {
			logger.error("StreamHttpNode initialization failed: url={}, method={}, error={}", param.getUrl(),
					param.getMethod(), e.getMessage(), e);
			// 返回包含错误信息的AsyncGenerator而不是直接返回Map
			String outputKey = param.getOutputKey() != null ? param.getOutputKey() : "stream_output";
			Flux<Map<String, Object>> errorFlux = Flux.just(createErrorOutput(e));
			AsyncGenerator<Map<String, Object>> errorGenerator = createAsyncGenerator(errorFlux);
			return CompletableFuture.completedFuture(Map.of(outputKey, errorGenerator));
		}
	}

	/**
	 * 执行流式HTTP请求 - 保持原有的流式逻辑
	 * Package-private for testing
	 */
	Flux<Map<String, Object>> executeStreaming(OverAllState state) throws Exception {
		String finalUrl = replaceVariables(param.getUrl(), state);
		Map<String, String> finalHeaders = replaceVariables(param.getHeaders(), state);
		Map<String, String> finalQueryParams = replaceVariables(param.getQueryParams(), state);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(finalUrl);
		finalQueryParams.forEach(uriBuilder::queryParam);
		URI finalUri = uriBuilder.build().toUri();

		validateUrl(finalUri.toString()); // 添加URL安全验证，在URI构建之后

		WebClient.RequestBodySpec requestSpec = param.getWebClient()
			.method(param.getMethod())
			.uri(finalUri)
			.headers(headers -> headers.setAll(finalHeaders));

		applyAuth(requestSpec);
		initBody(requestSpec, state);

		// 直接返回处理后的结果，将HTTP错误转换为错误数据项
		return requestSpec.exchangeToFlux(response -> {
			if (!response.statusCode().is2xxSuccessful()) {
				// 处理HTTP错误：将错误转换为包含错误信息的Map，作为数据项发射出去
				return response.bodyToMono(String.class)
					.defaultIfEmpty("HTTP Error") // 如果响应体为空，使用默认错误信息
					.map(errorBody -> {
						// 创建错误信息Map
						WebClientResponseException exception = new WebClientResponseException(
								response.statusCode().value(), "HTTP " + response.statusCode() + ": " + errorBody,
								null, null, null);
						return createErrorOutput(exception);
					})
					.flux(); // 转换为Flux
			}

			// 处理成功响应
			Flux<DataBuffer> dataBufferFlux = response.bodyToFlux(DataBuffer.class);
			return processStreamResponse(dataBufferFlux, state);
		})
			.retryWhen(Retry.backoff(param.getRetryConfig().getMaxRetries(),
					Duration.ofMillis(param.getRetryConfig().getMaxRetryInterval())))
			.timeout(param.getReadTimeout())
			// 处理网络超时、连接错误等其他异常
			.onErrorResume(throwable -> {
				logger.error("StreamHttpNode execution failed: url={}, method={}, error={}", finalUrl,
						param.getMethod(), throwable.getMessage(), throwable);
				return Flux.just(createErrorOutput(throwable));
			});
	}

	/**
	 * 将Flux转换为AsyncGenerator，供图框架处理流式数据
	 */
	private AsyncGenerator<Map<String, Object>> createAsyncGenerator(Flux<Map<String, Object>> flux) {
		return new AsyncGenerator<Map<String, Object>>() {
			private boolean completed = false;
			private final java.util.concurrent.BlockingQueue<AsyncGenerator.Data<Map<String, Object>>> queue = 
				new java.util.concurrent.LinkedBlockingQueue<>();
			
			{
				// 异步处理Flux数据
				flux.subscribe(
					data -> queue.offer(AsyncGenerator.Data.of(CompletableFuture.completedFuture(data))),
					error -> queue.offer(AsyncGenerator.Data.error(error)),
					() -> {
						completed = true;
						queue.offer(AsyncGenerator.Data.done());
					}
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
			.buffer(param.getBufferTimeout()) // 使用配置的缓冲超时时间避免内存累积
			.map(chunks -> String.join("", chunks))
			.flatMap(this::parseStreamChunk)
			.filter(data -> {
				if (data instanceof String) {
					return !((String) data).isEmpty();
				}
				return data != null; // 对于Map对象，只要不为null就保留
			})
			.map(this::wrapOutput)
			.transform(flux -> {
				if (param.getStreamMode() == StreamMode.AGGREGATE) {
					return flux.collectList().map(this::aggregateResults).flux();
				}
				return flux;
			})
			.onErrorResume(error -> {
				// 处理数据处理层面的错误
				logger.error("Error processing stream response", error);
				return Flux.just(createErrorOutput(error));
			});
	}

	/**
	 * 解析流数据块
	 */
	private Flux<Object> parseStreamChunk(String chunk) {
		return switch (param.getStreamFormat()) {
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
					logger.warn("Invalid JSON line: {}, error: {}", line, e.getMessage());
					// 返回包含错误信息的Map对象，用于直接处理
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
		String[] parts = chunk.split(Pattern.quote(param.getDelimiter()));
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
			// 如果已经是Map对象（如错误处理的结果），直接使用
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
			// 对于其他类型的数据，直接使用
			result.put("data", data);
		}

		result.put("timestamp", System.currentTimeMillis());
		result.put("streaming", true);

		if (StringUtils.hasLength(param.getOutputKey())) {
			Map<String, Object> keyedResult = new HashMap<>();
			keyedResult.put(param.getOutputKey(), result);
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
			if (param.getOutputKey() != null && result.containsKey(param.getOutputKey())) {
				Map<String, Object> keyedData = (Map<String, Object>) result.get(param.getOutputKey());
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

		if (StringUtils.hasLength(param.getOutputKey())) {
			Map<String, Object> keyedResult = new HashMap<>();
			keyedResult.put(param.getOutputKey(), aggregated);
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

		if (StringUtils.hasLength(param.getOutputKey())) {
			Map<String, Object> keyedResult = new HashMap<>();
			keyedResult.put(param.getOutputKey(), errorResult);
			return keyedResult;
		}

		return errorResult;
	}

	/**
	 * 替换变量占位符
	 */
	private String replaceVariables(String template, OverAllState state) {
		if (template == null)
			return null;

		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			Object value = state.value(key).orElse("");
			String replacement = value != null ? value.toString() : "";
			// 不进行编码，让UriComponentsBuilder处理
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}

		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * 替换Map中的变量占位符
	 */
	private Map<String, String> replaceVariables(Map<String, String> map, OverAllState state) {
		Map<String, String> result = new HashMap<>();
		map.forEach((k, v) -> result.put(k, replaceVariables(v, state)));
		return result;
	}

	/**
	 * 应用认证配置
	 */
	private void applyAuth(WebClient.RequestBodySpec requestSpec) {
		if (param.getAuthConfig() != null) {
			if (param.getAuthConfig().isBasic()) {
				requestSpec.headers(headers -> headers.setBasicAuth(param.getAuthConfig().getUsername(),
						param.getAuthConfig().getPassword()));
			}
			else if (param.getAuthConfig().isBearer()) {
				requestSpec.headers(headers -> headers.setBearerAuth(param.getAuthConfig().getToken()));
			}
		}
	}

	/**
	 * 初始化请求体
	 */
	private void initBody(WebClient.RequestBodySpec requestSpec, OverAllState state) throws GraphRunnerException {
		if (param.getBody() == null || !param.getBody().hasContent()) {
			return;
		}

		switch (param.getBody().getType()) {
			case NONE:
				break;
			case RAW_TEXT:
				if (param.getBody().getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("RAW_TEXT body must contain exactly one item");
				}
				String rawText = replaceVariables(param.getBody().getData().get(0).getValue(), state);
				requestSpec.headers(h -> h.setContentType(MediaType.TEXT_PLAIN));
				requestSpec.bodyValue(rawText);
				break;
			case JSON:
				if (param.getBody().getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("JSON body must contain exactly one item");
				}
				String jsonTemplate = replaceVariables(param.getBody().getData().get(0).getValue(), state);
				try {
					Object jsonObject = objectMapper.readValue(jsonTemplate, Object.class);
					requestSpec.headers(h -> h.setContentType(MediaType.APPLICATION_JSON));
					requestSpec.bodyValue(jsonObject);
				}
				catch (JsonProcessingException e) {
					throw RunnableErrors.nodeInterrupt.exception("Failed to parse JSON body: " + e.getMessage());
				}
				break;
			default:
				logger.warn("Body type {} not fully supported in streaming mode", param.getBody().getType());
		}
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
			if (isInternalAddress(host) && !param.isAllowInternalAddress()) {
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
			throw new StreamHttpException("stream-http", url, "URL validation failed: " + e.getMessage(), e);
		}
	}

	/**
	 * 检查是否为内网地址
	 */
	private boolean isInternalAddress(String host) {
		// 简单的内网地址检查
		return host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.")
				|| host.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*") || "localhost".equalsIgnoreCase(host);
	}

	/**
	 * 构建器模式的工厂方法
	 */
	public static StreamHttpNode create(StreamHttpNodeParam param) {
		return new StreamHttpNode(param);
	}

}
