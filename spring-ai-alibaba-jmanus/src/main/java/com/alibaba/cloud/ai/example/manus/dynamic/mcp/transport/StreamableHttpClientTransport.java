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

	/**
	 * Constructor for StreamableHttpClientTransport.
	 * @param webClientBuilder WebClient builder with base URL configured
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 * @param streamEndpoint Endpoint path for streaming (e.g., "/stream", "/streamable")
	 */
	public StreamableHttpClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			String streamEndpoint) {
		this.webClient = webClientBuilder.defaultHeader("Accept", "application/json, text/event-stream").build();
		this.objectMapper = objectMapper;
		this.fullUrl = streamEndpoint;
		logger.info("=== StreamableHttpClientTransport initialized with fullUrl: {} ===", this.fullUrl);
	}

	/**
	 * Constructor with default stream endpoint.
	 * @param webClientBuilder WebClient builder with base URL configured
	 * @param objectMapper ObjectMapper for JSON serialization/deserialization
	 */
	public StreamableHttpClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
		this(webClientBuilder, objectMapper, "/stream");
	}

	@Override
	public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> requestHandler) {
		logger.info("=== Connecting StreamableHttpClientTransport to: {} ===", fullUrl);

		if (connected.get()) {
			return Mono.error(new IllegalStateException("Transport is already connected"));
		}

		this.requestHandler = requestHandler;

		// 启动输出消息处理循环
		outgoingMessages.asFlux()
			.doOnNext(message -> logger.info("=== 发送消息: {} ===", message))
			.flatMap(this::sendHttpRequest)
			.doOnNext(response -> {
				logger.info("=== 收到HTTP响应: {} ===", response);
				// 将响应推送到输入流
				incomingMessages.tryEmitNext(response);
			})
			.doOnError(error -> logger.error("=== 输出流处理出错: {} ===", error.getMessage(), error))
			.subscribe();

		// 启动输入消息处理循环
		incomingMessages.asFlux()
			.doOnNext(message -> logger.info("=== 处理输入消息: {} ===", message))
			.doOnNext(this::handleIncomingMessage)
			.doOnError(error -> logger.error("=== 输入流处理出错: {} ===", error.getMessage(), error))
			.subscribe();

		connected.set(true);
		logger.info("=== StreamableHttpClientTransport connected to: {} ===", fullUrl);
		return Mono.empty();
	}

	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
		logger.info("=== Sending message via StreamableHttpClientTransport: {} ===", message);

		if (!connected.get()) {
			logger.warn("=== Not connected, message will not be sent ===");
			return Mono.empty();
		}

		try {
			// 反射兼容所有JSON-RPC消息类型
			Map<String, Object> jsonRpc = new HashMap<>();
			jsonRpc.put("jsonrpc", "2.0");
			final String[] messageIdHolder = { null };
			for (String field : new String[] { "id", "method", "params", "result", "error" }) {
				try {
					Method m = message.getClass().getMethod(field);
					Object v = m.invoke(message);
					if (v != null) {
						jsonRpc.put(field, v);
						if ("id".equals(field)) {
							messageIdHolder[0] = String.valueOf(v);
						}
					}
				}
				catch (NoSuchMethodException ignore) {
				}
			}
			String jsonMessage = objectMapper.writeValueAsString(jsonRpc);
			logger.info("=== 序列化消息: {} ===", jsonMessage);

			// 如果是请求消息（有ID），创建响应等待器
			Mono<Void> result = Mono.empty();
			final String messageId = messageIdHolder[0];
			if (messageId != null) {
				logger.info("=== 创建请求响应等待器: {} ===", messageId);
				Sinks.One<String> responseSink = Sinks.one();
				pendingRequests.put(messageId, responseSink);

				// 等待响应（但不阻塞sendMessage的返回）
				result = responseSink.asMono()
					.doOnNext(response -> logger.info("=== 收到请求 {} 的响应: {} ===", messageId, response))
					.then();
			}

			// 将消息发送到输出流
			outgoingMessages.tryEmitNext(jsonMessage);
			logger.info("=== 消息已推送到输出流 ===");

			return result;
		}
		catch (Exception e) {
			logger.error("=== Failed to send message ===", e);
			return Mono.error(e);
		}
	}

	private Mono<String> sendHttpRequest(String jsonMessage) {
		logger.info("=== 发送HTTP请求: {} ===", jsonMessage);
		return webClient.post()
			.uri(fullUrl)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Accept", "application/json, text/event-stream")
			.bodyValue(jsonMessage)
			.retrieve()
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(30));
	}

	private void handleIncomingMessage(String responseJson) {
		try {
			logger.info("=== 处理输入消息: {} ===", responseJson);
			// 解析响应
			Map<String, Object> data = objectMapper.readValue(responseJson, Map.class);
			String responseId = (String) data.get("id");

			// 使用官方推荐的反序列化方法
			McpSchema.JSONRPCMessage messageObj = null;
			try {
				messageObj = McpSchema.deserializeJsonRpcMessage(objectMapper, responseJson);
			}
			catch (Exception e) {
				logger.warn("=== 反序列化为 JSONRPCMessage 失败: {} ===", e.getMessage());
			}

			if (responseId != null && pendingRequests.containsKey(responseId)) {
				// 这是对某个请求的响应
				logger.info("=== 找到请求 {} 的响应等待器 ===", responseId);
				Sinks.One<String> responseSink = pendingRequests.remove(responseId);
				responseSink.tryEmitValue(responseJson);
				logger.info("=== 收到请求 {} 的响应: {} ===", responseId, responseJson);

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
				logger.info("=== 这是服务端主动消息或未知响应: {} ===", responseJson);
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
		}
		catch (Exception e) {
			logger.error("=== 处理输入消息失败: {} ===", e.getMessage(), e);
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

}
