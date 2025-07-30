/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Streamable HTTP Client Transport for MCP protocol.
 * <p>
 * This transport implements a streamable HTTP connection using WebClient and reactive
 * streams. It supports bidirectional communication by maintaining separate input and
 * output message flows.
 * </p>
 */
public class StreamableHttpClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(StreamableHttpClientTransport.class);

	private final WebClient webClient;

	private final ObjectMapper objectMapper;

	// 假设 fullUrl 已经是配置文件里的完整URL
	// 代码中不再拼接 baseUrl/streamEndpoint/query，只用 fullUrl
	// 其它相关拼接逻辑请注释或删除
	private final String fullUrl;

	private final AtomicBoolean connected = new AtomicBoolean(false);

	private volatile Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> requestHandler;

	// 用于双向通信的流
	private final Sinks.Many<String> outgoingMessages = Sinks.many().multicast().onBackpressureBuffer();

	private final Sinks.Many<String> incomingMessages = Sinks.many().multicast().onBackpressureBuffer();

	// 用于跟踪请求-响应映射
	private final Map<String, Sinks.One<String>> pendingRequests = new ConcurrentHashMap<>();

	// 添加Session ID支持
	private volatile String sessionId = null;

	private final Object sessionIdLock = new Object();

	/**
	 * Constructor for StreamableHttpClientTransport.
	 * @param webClientBuilder WebClient builder with base URL configured
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 * @param streamEndpoint Endpoint path for streaming (e.g., "/stream", "/streamable")
	 */
	public StreamableHttpClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			String streamEndpoint) {
		logger.info("=== 开始初始化 StreamableHttpClientTransport ===");
		logger.info("=== 传入的 streamEndpoint: {} ===", streamEndpoint);

		this.webClient = webClientBuilder.defaultHeader("Accept", "application/json, text/event-stream").build();
		logger.info("=== WebClient 已构建，默认 Accept 头: application/json, text/event-stream ===");

		this.objectMapper = objectMapper;
		logger.info("=== ObjectMapper 已设置 ===");

		this.fullUrl = streamEndpoint;
		logger.info("=== 完整URL已设置: {} ===", this.fullUrl);

		logger.info("=== StreamableHttpClientTransport 初始化完成 ===");
		logger.info("=== 最终配置: fullUrl={} ===", this.fullUrl);
	}

	public StreamableHttpClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			String streamEndpoint, ExchangeFilterFunction traceFilter) {
		if (traceFilter != null) {
			this.webClient = webClientBuilder.filter(traceFilter)
				.defaultHeader("Accept", "application/json, text/event-stream")
				.build();
		}
		else {
			this.webClient = webClientBuilder.defaultHeader("Accept", "application/json, text/event-stream").build();
		}
		this.objectMapper = objectMapper;
		this.fullUrl = streamEndpoint;
		logger.info("=== StreamableHttpClientTransport initialized with fullUrl: {} and tracing: {} ===", this.fullUrl,
				traceFilter != null);
	}

	/**
	 * Constructor with default stream endpoint.
	 * @param webClientBuilder WebClient builder with base URL configured
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 */
	public StreamableHttpClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
		this(webClientBuilder, objectMapper, "/stream");
	}

	public StreamableHttpClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			ExchangeFilterFunction traceFilter) {
		this(webClientBuilder, objectMapper, "/stream", traceFilter);
	}

	@Override
	public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> requestHandler) {
		logger.info("=== 开始连接 StreamableHttpClientTransport ===");
		logger.info("=== 目标URL: {} ===", fullUrl);
		logger.info("=== 当前连接状态: {} ===", connected.get() ? "已连接" : "未连接");

		if (connected.get()) {
			logger.error("=== 传输已连接，无法重复连接 ===");
			return Mono.error(new IllegalStateException("Transport is already connected"));
		}

		logger.info("=== 设置请求处理器 ===");
		this.requestHandler = requestHandler;

		logger.info("=== 启动输出消息处理循环 ===");
		// 启动输出消息处理循环
		outgoingMessages.asFlux()
			.doOnNext(message -> logger.info("=== 从输出流获取消息: {} ===", message))
			.flatMap(this::sendHttpRequest)
			.doOnNext(response -> {
				logger.info("=== 收到HTTP响应: {} ===", response);
				// 将响应推送到输入流
				incomingMessages.tryEmitNext(response);
				logger.info("=== 响应已推送到输入流 ===");
			})
			.doOnError(error -> {
				logger.error("=== 输出流处理出错 ===");
				logger.error("=== 错误类型: {} ===", error.getClass().getSimpleName());
				logger.error("=== 错误消息: {} ===", error.getMessage());
				logger.error("=== 错误堆栈: ===", error);
			})
			.subscribe();

		logger.info("=== 启动输入消息处理循环 ===");
		// 启动输入消息处理循环
		incomingMessages.asFlux()
			.doOnNext(message -> logger.info("=== 从输入流获取消息: {} ===", message))
			.doOnNext(this::handleIncomingMessage)
			.doOnError(error -> {
				logger.error("=== 输入流处理出错 ===");
				logger.error("=== 错误类型: {} ===", error.getClass().getSimpleName());
				logger.error("=== 错误消息: {} ===", error.getMessage());
				logger.error("=== 错误堆栈: ===", error);
			})
			.subscribe();

		connected.set(true);
		logger.info("=== StreamableHttpClientTransport 连接成功 ===");
		logger.info("=== 连接状态已设置为: {} ===", connected.get());
		return Mono.empty();
	}

	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
		logger.info("=== 开始发送消息 ===");
		logger.info("=== 消息类型: {} ===", message.getClass().getSimpleName());
		logger.info("=== 消息内容: {} ===", message);

		if (!connected.get()) {
			logger.warn("=== 未连接，消息将不会发送 ===");
			return Mono.empty();
		}

		try {
			// 反射兼容所有JSON-RPC消息类型
			Map<String, Object> jsonRpc = new HashMap<>();
			jsonRpc.put("jsonrpc", "2.0");
			final String[] messageIdHolder = { null };

			logger.info("=== 开始序列化消息字段 ===");
			for (String field : new String[] { "id", "method", "params", "result", "error" }) {
				try {
					Method m = message.getClass().getMethod(field);
					Object v = m.invoke(message);
					if (v != null) {
						jsonRpc.put(field, v);
						logger.info("=== 字段 {}: {} ===", field, v);
						if ("id".equals(field)) {
							messageIdHolder[0] = String.valueOf(v);
							logger.info("=== 消息ID: {} ===", messageIdHolder[0]);
						}
					}
					else {
						logger.debug("=== 字段 {}: null ===", field);
					}
				}
				catch (NoSuchMethodException ignore) {
					logger.debug("=== 字段 {} 不存在 ===", field);
				}
			}

			String jsonMessage = objectMapper.writeValueAsString(jsonRpc);
			logger.info("=== 序列化后的JSON消息: {} ===", jsonMessage);
			logger.info("=== JSON消息长度: {} 字节 ===", jsonMessage.getBytes().length);

			// 如果是请求消息（有ID），创建响应等待器
			Mono<Void> result = Mono.empty();
			final String messageId = messageIdHolder[0];
			if (messageId != null) {
				logger.info("=== 创建请求响应等待器，消息ID: {} ===", messageId);
				Sinks.One<String> responseSink = Sinks.one();
				pendingRequests.put(messageId, responseSink);
				logger.info("=== 当前等待中的请求数量: {} ===", pendingRequests.size());

				// 等待响应（但不阻塞sendMessage的返回）
				result = responseSink.asMono()
					.doOnNext(response -> logger.info("=== 收到请求 {} 的响应: {} ===", messageId, response))
					.then();
			}
			else {
				logger.info("=== 这是通知消息（无ID），不需要等待响应 ===");
			}

			// 将消息发送到输出流
			logger.info("=== 推送消息到输出流 ===");
			outgoingMessages.tryEmitNext(jsonMessage);
			logger.info("=== 消息已推送到输出流 ===");

			return result;
		}
		catch (Exception e) {
			logger.error("=== 发送消息失败 ===", e);
			logger.error("=== 失败的消息内容: {} ===", message);
			return Mono.error(e);
		}
	}

	private Mono<String> sendHttpRequest(String jsonMessage) {
		logger.info("=== 发送HTTP请求开始 ===");
		logger.info("=== 请求URL: {} ===", fullUrl);
		logger.info("=== 请求方法: POST ===");
		logger.info("=== 当前Session ID: {} ===", sessionId);

		// 构建请求头
		String acceptHeader = "application/json, text/event-stream";
		logger.info("=== 请求头: Content-Type=application/json, Accept={} ===", acceptHeader);
		logger.info("=== 请求体: {} ===", jsonMessage);
		logger.info("=== 请求体长度: {} 字节 ===", jsonMessage.getBytes().length);

		// 构建WebClient请求
		WebClient.RequestBodySpec requestSpec = webClient.post()
			.uri(fullUrl)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Accept", acceptHeader);

		// 如果有Session ID，添加到请求头
		if (sessionId != null) {
			requestSpec.header("MCP-Session-ID", sessionId);
			logger.info("=== 添加Session ID到请求头: {} ===", sessionId);
		}
		else {
			logger.info("=== 当前没有Session ID，跳过Session ID头 ===");
		}

		return requestSpec.bodyValue(jsonMessage).exchangeToMono(clientResponse -> {
			logger.info("=== 收到HTTP响应 ===");
			logger.info("=== 响应状态: {} ===", clientResponse.statusCode());
			logger.info("=== 响应头: {} ===", clientResponse.headers().asHttpHeaders());

			// 从响应头中提取Session ID
			extractSessionIdFromHeaders(clientResponse.headers().asHttpHeaders());

			if (clientResponse.statusCode().is2xxSuccessful()) {
				logger.info("=== HTTP请求成功 ===");
				return clientResponse.bodyToMono(String.class).doOnNext(response -> {
					logger.info("=== 响应体: {} ===", response);
					logger.info("=== 响应体长度: {} 字节 ===", response != null ? response.getBytes().length : 0);
				});
			}
			else {
				logger.error("=== HTTP请求失败，状态码: {} ===", clientResponse.statusCode());
				return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
					logger.error("=== 错误响应体: {} ===", errorBody);
					return Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
							clientResponse.statusCode().value(), clientResponse.statusCode().toString(),
							clientResponse.headers().asHttpHeaders(), errorBody.getBytes(), null));
				});
			}
		}).timeout(Duration.ofSeconds(30)).doOnError(error -> {
			logger.error("=== HTTP请求失败 ===");
			logger.error("=== 错误类型: {} ===", error.getClass().getSimpleName());
			logger.error("=== 错误消息: {} ===", error.getMessage());

			if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
				org.springframework.web.reactive.function.client.WebClientResponseException wcre = (org.springframework.web.reactive.function.client.WebClientResponseException) error;
				logger.error("=== HTTP状态码: {} ===", wcre.getStatusCode());
				logger.error("=== HTTP状态文本: {} ===", wcre.getStatusText());
				logger.error("=== 响应头: {} ===", wcre.getHeaders());
				logger.error("=== 响应体: {} ===", wcre.getResponseBodyAsString());
			}
		});
	}

	private void handleIncomingMessage(String responseJson) {
		try {
			logger.info("=== 开始处理输入消息 ===");
			logger.info("=== 原始响应内容: {} ===", responseJson);
			logger.info("=== 原始响应长度: {} 字节 ===", responseJson != null ? responseJson.getBytes().length : 0);

			// 自动检测格式并解析
			String jsonContent = parseResponseFormat(responseJson);
			if (jsonContent == null) {
				logger.error("=== 无法解析响应格式，原始内容: {} ===", responseJson);
				return;
			}

			logger.info("=== 解析后的JSON内容: {} ===", jsonContent);
			logger.info("=== 解析后的JSON长度: {} 字节 ===", jsonContent.getBytes().length);

			// 解析响应
			Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);
			String responseId = (String) data.get("id");
			String method = (String) data.get("method");
			String jsonrpc = (String) data.get("jsonrpc");

			logger.info("=== 解析的响应字段 ===");
			logger.info("=== jsonrpc: {} ===", jsonrpc);
			logger.info("=== method: {} ===", method);
			logger.info("=== id: {} ===", responseId);
			logger.info("=== 完整解析数据: {} ===", data);

			// 使用官方推荐的反序列化方法
			McpSchema.JSONRPCMessage messageObj = null;
			try {
				messageObj = McpSchema.deserializeJsonRpcMessage(objectMapper, jsonContent);
				logger.info("=== 成功反序列化为 JSONRPCMessage ===");
			}
			catch (Exception e) {
				logger.warn("=== 反序列化为 JSONRPCMessage 失败: {} ===", e.getMessage());
				logger.warn("=== 反序列化失败的JSON内容: {} ===", jsonContent);
			}

			if (responseId != null && pendingRequests.containsKey(responseId)) {
				// 这是对某个请求的响应
				logger.info("=== 找到请求 {} 的响应等待器 ===", responseId);
				Sinks.One<String> responseSink = pendingRequests.remove(responseId);
				responseSink.tryEmitValue(jsonContent);
				logger.info("=== 收到请求 {} 的响应: {} ===", responseId, jsonContent);

				if (requestHandler != null && messageObj != null) {
					logger.info("=== 通过 requestHandler 处理响应（类型安全模式） ===");
					try {
						requestHandler.apply(Mono.just(messageObj))
							.subscribe(
									result -> logger.info("=== requestHandler 处理完成: {} ===", result), error -> logger
										.error("=== requestHandler 处理出错: {} ===", String.valueOf(error), error),
									() -> logger.info("=== requestHandler 处理流完成 ==="));
					}
					catch (Exception e) {
						logger.error("=== requestHandler 调用失败: {} ===", e.getMessage(), e);
					}
				}
				else if (requestHandler != null) {
					logger.warn("=== 反序列化为 JSONRPCMessage 失败，跳过 requestHandler 处理 ===");
				}
			}
			else {
				logger.info("=== 这是服务端主动消息或未知响应: {} ===", jsonContent);
				logger.info("=== 当前等待中的请求ID: {} ===", pendingRequests.keySet());
				// 对于服务端主动消息，也通过 requestHandler 处理
				if (requestHandler != null && messageObj != null) {
					try {
						requestHandler.apply(Mono.just(messageObj)).subscribe();
					}
					catch (Exception e) {
						logger.error("=== 处理服务端消息失败: {} ===", e.getMessage(), e);
					}
				}
				else if (requestHandler != null) {
					logger.warn("=== 反序列化为 JSONRPCMessage 失败，跳过 requestHandler 处理 ===");
				}
			}

			logger.info("=== 输入消息处理完成 ===");
		}
		catch (Exception e) {
			logger.error("=== 处理输入消息失败: {} ===", e.getMessage(), e);
			logger.error("=== 失败的原始响应: {} ===", responseJson);
		}
	}

	/**
	 * 解析响应格式，支持SSE和JSON格式
	 * @param rawResponse 原始响应内容
	 * @return 解析后的JSON内容，如果解析失败返回null
	 */
	private String parseResponseFormat(String rawResponse) {
		logger.info("=== 开始解析响应格式 ===");
		logger.info("=== 原始响应: {} ===", rawResponse);

		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			logger.warn("=== 原始响应为空或null ===");
			return null;
		}

		String trimmedResponse = rawResponse.trim();
		logger.info("=== 去除首尾空格后的响应: {} ===", trimmedResponse);
		logger.info("=== 响应长度: {} 字符 ===", trimmedResponse.length());

		// 检测是否为SSE格式
		boolean isSse = isSseFormat(trimmedResponse);
		logger.info("=== 格式检测结果: {} ===", isSse ? "SSE格式" : "JSON格式");

		if (isSse) {
			logger.info("=== 检测到SSE格式，开始解析 ===");
			String result = parseSseFormat(trimmedResponse);
			logger.info("=== SSE解析结果: {} ===", result);
			return result;
		}
		else {
			logger.info("=== 检测到JSON格式，直接使用 ===");
			String result = parseJsonFormat(trimmedResponse);
			logger.info("=== JSON解析结果: {} ===", result);
			return result;
		}
	}

	/**
	 * 检测是否为SSE格式
	 * @param response 响应内容
	 * @return true if SSE format, false otherwise
	 */
	private boolean isSseFormat(String response) {
		logger.debug("=== 开始检测SSE格式 ===");
		logger.debug("=== 检测内容: {} ===", response);

		// 标准化换行符，处理\r\n和\n的情况
		String normalizedResponse = response.replace("\r\n", "\n");

		// SSE格式特征：包含event:和data:字段，或者以event:开头
		boolean startsWithEvent = normalizedResponse.startsWith("event:");
		boolean containsEvent = normalizedResponse.contains("event:");
		boolean containsData = normalizedResponse.contains("data:");

		logger.debug("=== SSE格式检测结果 ===");
		logger.debug("=== 以event:开头: {} ===", startsWithEvent);
		logger.debug("=== 包含event:: {} ===", containsEvent);
		logger.debug("=== 包含data:: {} ===", containsData);

		boolean isSse = startsWithEvent || (containsEvent && containsData);
		logger.debug("=== 最终SSE格式判断: {} ===", isSse);

		return isSse;
	}

	/**
	 * 解析SSE格式
	 * @param sseResponse SSE格式的响应
	 * @return 提取的JSON内容
	 */
	private String parseSseFormat(String sseResponse) {
		logger.info("=== 开始解析SSE格式 ===");
		logger.info("=== SSE原始内容: {} ===", sseResponse);

		try {
			// 标准化换行符，处理\r\n和\n的情况
			String normalizedResponse = sseResponse.replace("\r\n", "\n");

			// 按行分割
			String[] lines = normalizedResponse.split("\n");
			logger.info("=== SSE行数: {} ===", lines.length);

			StringBuilder jsonContent = new StringBuilder();
			boolean inDataSection = false;

			for (int i = 0; i < lines.length; i++) {
				String line = lines[i].trim();
				logger.debug("=== 处理第{}行: {} ===", i + 1, line);

				if (line.isEmpty()) {
					logger.debug("=== 跳过空行 ===");
					continue;
				}

				if (line.startsWith("data:")) {
					// 提取data:后面的内容
					String data = line.substring(5).trim();
					logger.info("=== 找到data字段: {} ===", data);
					if (!data.isEmpty()) {
						jsonContent.append(data);
						inDataSection = true;
						logger.info("=== 已添加到JSON内容 ===");
					}
				}
				else if (inDataSection && !line.startsWith("event:") && !line.startsWith("id:")
						&& !line.startsWith("retry:")) {
					// 如果已经在data部分，且不是SSE控制字段，则可能是多行JSON的一部分
					logger.info("=== 添加多行JSON内容: {} ===", line);
					jsonContent.append(line);
				}
				else {
					logger.debug("=== 跳过SSE控制字段: {} ===", line);
				}
			}

			String result = jsonContent.toString().trim();
			logger.info("=== SSE解析完成，结果: {} ===", result);

			if (result.isEmpty()) {
				logger.warn("=== SSE格式解析失败，未找到data内容 ===");
				return null;
			}

			logger.info("=== SSE格式解析成功 ===");
			return result;

		}
		catch (Exception e) {
			logger.error("=== SSE格式解析异常: {} ===", e.getMessage(), e);
			logger.error("=== 解析失败的SSE内容: {} ===", sseResponse);
			return null;
		}
	}

	/**
	 * 解析JSON格式（直接返回，或进行基本验证）
	 * @param jsonResponse JSON格式的响应
	 * @return JSON内容
	 */
	private String parseJsonFormat(String jsonResponse) {
		logger.info("=== 开始解析JSON格式 ===");
		logger.info("=== JSON原始内容: {} ===", jsonResponse);

		try {
			// 检查是否包含SSE格式的前缀，如果是则先尝试解析SSE格式
			if (jsonResponse.contains("event:") || jsonResponse.contains("data:")) {
				logger.warn("=== 检测到SSE格式前缀，但被误判为JSON格式，尝试解析SSE ===");
				String sseResult = parseSseFormat(jsonResponse);
				if (sseResult != null) {
					logger.info("=== SSE解析成功，返回结果: {} ===", sseResult);
					return sseResult;
				}
				else {
					logger.error("=== SSE解析失败，原始内容可能格式错误 ===");
					return null;
				}
			}
			// 尝试解析JSON以验证格式
			Object parsed = objectMapper.readValue(jsonResponse, Object.class);
			logger.info("=== JSON格式验证成功，解析结果类型: {} ===", parsed.getClass().getSimpleName());
			return jsonResponse;
		}
		catch (Exception e) {
			logger.error("=== JSON格式验证失败: {} ===", e.getMessage(), e);
			logger.error("=== 验证失败的JSON内容: {} ===", jsonResponse);
			return null;
		}
	}

	/**
	 * 创建响应消息实例
	 */
	private McpSchema.JSONRPCMessage createResponseMessage(Map<String, Object> data) {
		try {
			logger.info("=== 尝试创建响应消息实例 ===");

			// 转换为JSON字符串
			String json = objectMapper.writeValueAsString(data);
			logger.info("=== 转换为JSON字符串: {} ===", json);

			// 尝试使用 unmarshalFrom 方法
			try {
				McpSchema.JSONRPCMessage message = objectMapper.readValue(json, McpSchema.JSONRPCMessage.class);
				logger.info("=== 通过 readValue 创建成功 ===");
				return message;
			}
			catch (Exception e) {
				logger.warn("=== readValue 失败: {} ===", e.getMessage());
			}

			// 尝试使用 unmarshalFrom 方法
			try {
				TypeReference<McpSchema.JSONRPCMessage> typeRef = new TypeReference<McpSchema.JSONRPCMessage>() {
				};
				McpSchema.JSONRPCMessage message = unmarshalFrom(data, typeRef);
				logger.info("=== 通过 unmarshalFrom 创建成功 ===");
				return message;
			}
			catch (Exception e) {
				logger.warn("=== unmarshalFrom 失败: {} ===", e.getMessage());
			}

			logger.warn("=== 所有创建方法都失败，返回 null ===");
			return null;
		}
		catch (Exception e) {
			logger.error("Failed to unmarshal data", e);
			logger.warn("=== unmarshalFrom 失败: {} ===", e.getMessage());
			logger.warn("=== 所有创建方法都失败，返回 null ===");
			return null;
		}
	}

	@Override
	public <T> T unmarshalFrom(Object data, TypeReference<T> typeReference) {
		try {
			String json = objectMapper.writeValueAsString(data);
			return objectMapper.readValue(json, typeReference);
		}
		catch (Exception e) {
			logger.error("Failed to unmarshal data", e);
			throw new RuntimeException("Failed to unmarshal data", e);
		}
	}

	@Override
	public void close() {
		logger.info("Closing StreamableHttpClientTransport");
		connected.set(false);
		outgoingMessages.tryEmitComplete();
		incomingMessages.tryEmitComplete();
	}

	@Override
	public Mono<Void> closeGracefully() {
		logger.info("Gracefully closing StreamableHttpClientTransport");
		return Mono.fromRunnable(this::close);
	}

	/**
	 * 从响应头中提取Session ID
	 * @param headers HTTP响应头
	 */
	private void extractSessionIdFromHeaders(org.springframework.http.HttpHeaders headers) {
		logger.info("=== 开始从响应头提取Session ID ===");
		logger.info("=== 响应头: {} ===", headers);

		// 尝试从不同的头字段中获取Session ID
		String newSessionId = headers.getFirst("mcp-session-id");
		if (newSessionId == null) {
			newSessionId = headers.getFirst("MCP-Session-ID");
		}
		if (newSessionId == null) {
			newSessionId = headers.getFirst("session-id");
		}
		if (newSessionId == null) {
			newSessionId = headers.getFirst("Session-ID");
		}

		if (newSessionId != null && !newSessionId.trim().isEmpty()) {
			synchronized (sessionIdLock) {
				this.sessionId = newSessionId.trim();
				logger.info("=== 成功提取Session ID: {} ===", this.sessionId);
			}
		}
		else {
			logger.warn("=== 响应头中未找到Session ID ===");
		}
	}

	/**
	 * 获取当前Session ID
	 * @return 当前Session ID，如果未设置则返回null
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * 设置Session ID
	 * @param sessionId 要设置的Session ID
	 */
	public void setSessionId(String sessionId) {
		synchronized (sessionIdLock) {
			this.sessionId = sessionId;
			logger.info("=== 手动设置Session ID: {} ===", this.sessionId);
		}
	}

}
