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

	// Assume fullUrl is already the complete URL in configuration file
	// No longer concatenate baseUrl/streamEndpoint/query in code, only use fullUrl
	// Please comment or delete other related concatenation logic
	private final String fullUrl;

	private final AtomicBoolean connected = new AtomicBoolean(false);

	private volatile Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> requestHandler;

	// Stream for bidirectional communication
	private final Sinks.Many<String> outgoingMessages = Sinks.many().multicast().onBackpressureBuffer();

	private final Sinks.Many<String> incomingMessages = Sinks.many().multicast().onBackpressureBuffer();

	// For tracking request-response mapping
	private final Map<String, Sinks.One<String>> pendingRequests = new ConcurrentHashMap<>();

	// Add Session ID support
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
		logger.info("=== Starting StreamableHttpClientTransport initialization ===");
		logger.info("=== Received streamEndpoint: {} ===", streamEndpoint);

		this.webClient = webClientBuilder.defaultHeader("Accept", "application/json, text/event-stream").build();
		logger.info("=== WebClient built with default Accept header: application/json, text/event-stream ===");

		this.objectMapper = objectMapper;
		logger.info("=== ObjectMapper configured ===");

		this.fullUrl = streamEndpoint;
		logger.info("=== Complete URL configured: {} ===", this.fullUrl);

		logger.info("=== StreamableHttpClientTransport initialization completed ===");
		logger.info("=== Final configuration: fullUrl={} ===", this.fullUrl);
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
		logger.info("=== Starting StreamableHttpClientTransport connection ===");
		logger.info("=== Target URL: {} ===", fullUrl);
		logger.info("=== Current connection status: {} ===", connected.get() ? "Connected" : "Disconnected");

		if (connected.get()) {
			logger.error("=== Transport already connected, cannot connect again ===");
			return Mono.error(new IllegalStateException("Transport is already connected"));
		}

		logger.info("=== Setting request handler ===");
		this.requestHandler = requestHandler;

		logger.info("=== Starting output message processing loop ===");
		// Start output message processing loop
		outgoingMessages.asFlux()
			.doOnNext(message -> logger.info("=== Message received from output stream: {} ===", message))
			.flatMap(this::sendHttpRequest)
			.doOnNext(response -> {
				logger.info("=== HTTP response received: {} ===", response);
				// Push response to input stream
				incomingMessages.tryEmitNext(response);
				logger.info("=== Response pushed to input stream ===");
			})
			.doOnError(error -> {
				logger.error("=== Output stream processing error ===");
				logger.error("=== Error type: {} ===", error.getClass().getSimpleName());
				logger.error("=== Error message: {} ===", error.getMessage());
				logger.error("=== Error stack trace: ===", error);
			})
			.subscribe();

		logger.info("=== Starting input message processing loop ===");
		// Start input message processing loop
		incomingMessages.asFlux()
			.doOnNext(message -> logger.info("=== Message received from input stream: {} ===", message))
			.doOnNext(this::handleIncomingMessage)
			.doOnError(error -> {
				logger.error("=== Input stream processing error ===");
				logger.error("=== Error type: {} ===", error.getClass().getSimpleName());
				logger.error("=== Error message: {} ===", error.getMessage());
				logger.error("=== Error stack trace: ===", error);
			})
			.subscribe();

		connected.set(true);
		logger.info("=== StreamableHttpClientTransport connection successful ===");
		logger.info("=== Connection status set to: {} ===", connected.get());
		return Mono.empty();
	}

	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
		logger.info("=== Starting to send message ===");
		logger.info("=== Message type: {} ===", message.getClass().getSimpleName());
		logger.info("=== Message content: {} ===", message);

		if (!connected.get()) {
			logger.warn("=== Not connected, message will not be sent ===");
			return Mono.empty();
		}

		try {
			// Reflection compatibility for all JSON-RPC message types
			Map<String, Object> jsonRpc = new HashMap<>();
			jsonRpc.put("jsonrpc", "2.0");
			final String[] messageIdHolder = { null };

			logger.info("=== Starting to serialize message fields ===");
			for (String field : new String[] { "id", "method", "params", "result", "error" }) {
				try {
					Method m = message.getClass().getMethod(field);
					Object v = m.invoke(message);
					if (v != null) {
						jsonRpc.put(field, v);
						logger.info("=== Field {}: {} ===", field, v);
						if ("id".equals(field)) {
							messageIdHolder[0] = String.valueOf(v);
							logger.info("=== Message ID: {} ===", messageIdHolder[0]);
						}
					}
					else {
						logger.debug("=== Field {}: null ===", field);
					}
				}
				catch (NoSuchMethodException ignore) {
					logger.debug("=== Field {} does not exist ===", field);
				}
			}

			String jsonMessage = objectMapper.writeValueAsString(jsonRpc);
			logger.info("=== Serialized JSON message: {} ===", jsonMessage);
			logger.info("=== JSON message length: {} bytes ===", jsonMessage.getBytes().length);

			// If it's a request message (with ID), create response waiter
			Mono<Void> result = Mono.empty();
			final String messageId = messageIdHolder[0];
			if (messageId != null) {
				logger.info("=== Creating request response waiter, message ID: {} ===", messageId);
				Sinks.One<String> responseSink = Sinks.one();
				pendingRequests.put(messageId, responseSink);
				logger.info("=== Current number of pending requests: {} ===", pendingRequests.size());

				// Wait for response (but don't block sendMessage return)
				result = responseSink.asMono()
					.doOnNext(response -> logger.info("=== Received response for request {}: {} ===", messageId,
							response))
					.then();
			}
			else {
				logger.info("=== This is a notification message (no ID), no need to wait for response ===");
			}

			// Send message to output stream
			logger.info("=== Pushing message to output stream ===");
			outgoingMessages.tryEmitNext(jsonMessage);
			logger.info("=== Message pushed to output stream ===");

			return result;
		}
		catch (Exception e) {
			logger.error("=== Failed to send message ===", e);
			logger.error("=== Failed message content: {} ===", message);
			return Mono.error(e);
		}
	}

	private Mono<String> sendHttpRequest(String jsonMessage) {
		logger.info("=== Starting HTTP request ===");
		logger.info("=== Request URL: {} ===", fullUrl);
		logger.info("=== Request method: POST ===");
		logger.info("=== Current Session ID: {} ===", sessionId);

		// Build request headers
		String acceptHeader = "application/json, text/event-stream";
		logger.info("=== Request headers: Content-Type=application/json, Accept={} ===", acceptHeader);
		logger.info("=== Request body: {} ===", jsonMessage);
		logger.info("=== Request body length: {} bytes ===", jsonMessage.getBytes().length);

		// Build WebClient request
		WebClient.RequestBodySpec requestSpec = webClient.post()
			.uri(fullUrl)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Accept", acceptHeader);

		// If there is Session ID, add to request header
		if (sessionId != null) {
			requestSpec.header("MCP-Session-ID", sessionId);
			logger.info("=== Adding Session ID to request header: {} ===", sessionId);
		}
		else {
			logger.info("=== No Session ID currently, skipping Session ID header ===");
		}

		return requestSpec.bodyValue(jsonMessage).exchangeToMono(clientResponse -> {
			logger.info("=== Received HTTP response ===");
			logger.info("=== Response status: {} ===", clientResponse.statusCode());
			logger.info("=== Response headers: {} ===", clientResponse.headers().asHttpHeaders());

			// Extract Session ID from response headers
			extractSessionIdFromHeaders(clientResponse.headers().asHttpHeaders());

			if (clientResponse.statusCode().is2xxSuccessful()) {
				logger.info("=== HTTP request successful ===");
				return clientResponse.bodyToMono(String.class).doOnNext(response -> {
					logger.info("=== Response body: {} ===", response);
					logger.info("=== Response body length: {} bytes ===",
							response != null ? response.getBytes().length : 0);
				});
			}
			else {
				logger.error("=== HTTP request failed, status code: {} ===", clientResponse.statusCode());
				return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
					logger.error("=== Error response body: {} ===", errorBody);
					return Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
							clientResponse.statusCode().value(), clientResponse.statusCode().toString(),
							clientResponse.headers().asHttpHeaders(), errorBody.getBytes(), null));
				});
			}
		}).timeout(Duration.ofSeconds(30)).doOnError(error -> {
			logger.error("=== HTTP request failed ===");
			logger.error("=== Error type: {} ===", error.getClass().getSimpleName());
			logger.error("=== Error message: {} ===", error.getMessage());

			if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
				org.springframework.web.reactive.function.client.WebClientResponseException wcre = (org.springframework.web.reactive.function.client.WebClientResponseException) error;
				logger.error("=== HTTP status code: {} ===", wcre.getStatusCode());
				logger.error("=== HTTP status text: {} ===", wcre.getStatusText());
				logger.error("=== Response headers: {} ===", wcre.getHeaders());
				logger.error("=== Response body: {} ===", wcre.getResponseBodyAsString());
			}
		});
	}

	private void handleIncomingMessage(String responseJson) {
		try {
			logger.info("=== Starting to process input message ===");
			logger.info("=== Raw response content: {} ===", responseJson);
			logger.info("=== Raw response length: {} bytes ===",
					responseJson != null ? responseJson.getBytes().length : 0);

			// Auto-detect format and parse
			String jsonContent = parseResponseFormat(responseJson);
			if (jsonContent == null) {
				logger.error("=== Unable to parse response format, raw content: {} ===", responseJson);
				return;
			}

			logger.info("=== Parsed JSON content: {} ===", jsonContent);
			logger.info("=== Parsed JSON length: {} bytes ===", jsonContent.getBytes().length);

			// Parse response
			Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);
			// Safely handle id field, which could be String or Integer
			Object idObj = data.get("id");
			String responseId = idObj != null ? String.valueOf(idObj) : null;
			// Safely handle method field
			Object methodObj = data.get("method");
			String method = methodObj != null ? String.valueOf(methodObj) : null;
			// Safely handle jsonrpc field
			Object jsonrpcObj = data.get("jsonrpc");
			String jsonrpc = jsonrpcObj != null ? String.valueOf(jsonrpcObj) : null;

			logger.info("=== Parsed response fields ===");
			logger.info("=== jsonrpc: {} ===", jsonrpc);
			logger.info("=== method: {} ===", method);
			logger.info("=== id: {} ===", responseId);
			logger.info("=== Complete parsed data: {} ===", data);

			// Use officially recommended deserialization method
			McpSchema.JSONRPCMessage messageObj = null;
			try {
				// Pre-process JSON to handle type conversion issues
				String processedJsonContent = preprocessJsonForDeserialization(jsonContent);
				messageObj = McpSchema.deserializeJsonRpcMessage(objectMapper, processedJsonContent);
				logger.info("=== Successfully deserialized to JSONRPCMessage ===");
			}
			catch (Exception e) {
				logger.warn("=== Failed to deserialize to JSONRPCMessage: {} ===", e.getMessage());
				logger.warn("=== JSON content that failed deserialization: {} ===", jsonContent);
			}

			if (responseId != null && pendingRequests.containsKey(responseId)) {
				// This is a response to some request
				logger.info("=== Found response waiter for request {} ===", responseId);
				Sinks.One<String> responseSink = pendingRequests.remove(responseId);
				responseSink.tryEmitValue(jsonContent);
				logger.info("=== Received response for request {}: {} ===", responseId, jsonContent);

				if (requestHandler != null && messageObj != null) {
					logger.info("=== Processing response through requestHandler (type-safe mode) ===");
					try {
						requestHandler.apply(Mono.just(messageObj))
							.subscribe(result -> logger.info("=== requestHandler processing completed: {} ===", result),
									error -> logger.error("=== requestHandler processing error: {} ===",
											String.valueOf(error), error),
									() -> logger.info("=== requestHandler processing stream completed ==="));
					}
					catch (Exception e) {
						logger.error("=== requestHandler call failed: {} ===", e.getMessage(), e);
					}
				}
				else if (requestHandler != null) {
					logger.warn("=== Failed to deserialize to JSONRPCMessage, skipping requestHandler processing ===");
				}
			}
			else {
				logger.info("=== This is server-initiated message or unknown response: {} ===", jsonContent);
				logger.info("=== Currently pending request IDs: {} ===", pendingRequests.keySet());
				// For server-initiated messages, also process through requestHandler
				if (requestHandler != null && messageObj != null) {
					try {
						requestHandler.apply(Mono.just(messageObj)).subscribe();
					}
					catch (Exception e) {
						logger.error("=== Failed to process server message: {} ===", e.getMessage(), e);
					}
				}
				else if (requestHandler != null) {
					logger.warn("=== Failed to deserialize to JSONRPCMessage, skipping requestHandler processing ===");
				}
			}

			logger.info("=== Input message processing completed ===");
		}
		catch (Exception e) {
			logger.error("=== Failed to process input message: {} ===", e.getMessage(), e);
			logger.error("=== Failed raw response: {} ===", responseJson);
		}
	}

	/**
	 * Parse response format, supports SSE and JSON formats
	 * @param rawResponse Raw response content
	 * @return Parsed JSON content, return null if parsing fails
	 */
	private String parseResponseFormat(String rawResponse) {
		logger.info("=== Starting response format parsing ===");
		logger.info("=== Raw response: {} ===", rawResponse);

		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			logger.warn("=== Raw response is empty or null ===");
			return null;
		}

		String trimmedResponse = rawResponse.trim();
		logger.info("=== Response after trimming: {} ===", trimmedResponse);
		logger.info("=== Response length: {} characters ===", trimmedResponse.length());

		// Detect if it's SSE format
		boolean isSse = isSseFormat(trimmedResponse);
		logger.info("=== Format detection result: {} ===", isSse ? "SSE format" : "JSON format");

		if (isSse) {
			logger.info("=== Detected SSE format, starting parsing ===");
			String result = parseSseFormat(trimmedResponse);
			logger.info("=== SSE parsing result: {} ===", result);
			return result;
		}
		else {
			logger.info("=== Detected JSON format, using directly ===");
			String result = parseJsonFormat(trimmedResponse);
			logger.info("=== JSON parsing result: {} ===", result);
			return result;
		}
	}

	/**
	 * Detect if response is in SSE format
	 * @param response Response content
	 * @return true if SSE format, false otherwise
	 */
	private boolean isSseFormat(String response) {
		logger.debug("=== Starting SSE format detection ===");
		logger.debug("=== Detection content: {} ===", response);

		// Normalize line breaks, handle \r\n and \n cases
		String normalizedResponse = response.replace("\r\n", "\n");

		// SSE format characteristics: contains event: and data: fields, or starts with
		// event:
		boolean startsWithEvent = normalizedResponse.startsWith("event:");
		boolean containsEvent = normalizedResponse.contains("event:");
		boolean containsData = normalizedResponse.contains("data:");

		logger.debug("=== SSE format detection results ===");
		logger.debug("=== Starts with event:: {} ===", startsWithEvent);
		logger.debug("=== Contains event:: {} ===", containsEvent);
		logger.debug("=== Contains data:: {} ===", containsData);

		boolean isSse = startsWithEvent || (containsEvent && containsData);
		logger.debug("=== Final SSE format determination: {} ===", isSse);

		return isSse;
	}

	/**
	 * Parse SSE format
	 * @param sseResponse SSE format response
	 * @return Extracted JSON content
	 */
	private String parseSseFormat(String sseResponse) {
		logger.info("=== Starting SSE format parsing ===");
		logger.info("=== SSE raw content: {} ===", sseResponse);

		try {
			// Normalize line breaks, handle \r\n and \n cases
			String normalizedResponse = sseResponse.replace("\r\n", "\n");

			// Split by lines
			String[] lines = normalizedResponse.split("\n");
			logger.info("=== SSE line count: {} ===", lines.length);

			StringBuilder jsonContent = new StringBuilder();
			boolean inDataSection = false;

			for (int i = 0; i < lines.length; i++) {
				String line = lines[i].trim();
				logger.debug("=== Processing line {}: {} ===", i + 1, line);

				if (line.isEmpty()) {
					logger.debug("=== Skipping empty line ===");
					continue;
				}

				if (line.startsWith("data:")) {
					// Extract content after data:
					String data = line.substring(5).trim();
					logger.info("=== Found data field: {} ===", data);
					if (!data.isEmpty()) {
						jsonContent.append(data);
						inDataSection = true;
						logger.info("=== Added to JSON content ===");
					}
				}
				else if (inDataSection && !line.startsWith("event:") && !line.startsWith("id:")
						&& !line.startsWith("retry:")) {
					// If already in data section and not SSE control field, might be part
					// of multi-line JSON
					logger.info("=== Adding multi-line JSON content: {} ===", line);
					jsonContent.append(line);
				}
				else {
					logger.debug("=== Skipping SSE control field: {} ===", line);
				}
			}

			String result = jsonContent.toString().trim();
			logger.info("=== SSE parsing completed, result: {} ===", result);

			if (result.isEmpty()) {
				logger.warn("=== SSE format parsing failed, no data content found ===");
				return null;
			}

			logger.info("=== SSE format parsing successful ===");
			return result;

		}
		catch (Exception e) {
			logger.error("=== SSE format parsing exception: {} ===", e.getMessage(), e);
			logger.error("=== Failed SSE content: {} ===", sseResponse);
			return null;
		}
	}

	/**
	 * Parse JSON format (return directly or perform basic validation)
	 * @param jsonResponse JSON format response
	 * @return JSON content
	 */
	private String parseJsonFormat(String jsonResponse) {
		logger.info("=== Starting JSON format parsing ===");
		logger.info("=== JSON raw content: {} ===", jsonResponse);

		try {
			// Check if contains SSE format prefix, if so try parsing SSE format first
			if (jsonResponse.contains("event:") || jsonResponse.contains("data:")) {
				logger.warn("=== Detected SSE format prefix but misidentified as JSON format, trying SSE parsing ===");
				String sseResult = parseSseFormat(jsonResponse);
				if (sseResult != null) {
					logger.info("=== SSE parsing successful, returning result: {} ===", sseResult);
					return sseResult;
				}
				else {
					logger.error("=== SSE parsing failed, raw content may have format error ===");
					return null;
				}
			}
			// Try parsing JSON to validate format
			Object parsed = objectMapper.readValue(jsonResponse, Object.class);
			logger.info("=== JSON format validation successful, parsed result type: {} ===",
					parsed.getClass().getSimpleName());
			return jsonResponse;
		}
		catch (Exception e) {
			logger.error("=== JSON format validation failed: {} ===", e.getMessage(), e);
			logger.error("=== Failed JSON content: {} ===", jsonResponse);
			return null;
		}
	}

	/**
	 * Create response message instance
	 */
	private McpSchema.JSONRPCMessage createResponseMessage(Map<String, Object> data) {
		try {
			logger.info("=== Attempting to create response message instance ===");

			// Convert to JSON string
			String json = objectMapper.writeValueAsString(data);
			logger.info("=== Converted to JSON string: {} ===", json);

			// Try using readValue method
			try {
				McpSchema.JSONRPCMessage message = objectMapper.readValue(json, McpSchema.JSONRPCMessage.class);
				logger.info("=== Successfully created via readValue ===");
				return message;
			}
			catch (Exception e) {
				logger.warn("=== readValue failed: {} ===", e.getMessage());
			}

			// Try using unmarshalFrom method
			try {
				TypeReference<McpSchema.JSONRPCMessage> typeRef = new TypeReference<McpSchema.JSONRPCMessage>() {
				};
				McpSchema.JSONRPCMessage message = unmarshalFrom(data, typeRef);
				logger.info("=== Successfully created via unmarshalFrom ===");
				return message;
			}
			catch (Exception e) {
				logger.warn("=== unmarshalFrom failed: {} ===", e.getMessage());
			}

			logger.warn("=== All creation methods failed, returning null ===");
			return null;
		}
		catch (Exception e) {
			logger.error("Failed to unmarshal data", e);
			logger.warn("=== unmarshalFrom failed: {} ===", e.getMessage());
			logger.warn("=== All creation methods failed, returning null ===");
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
	 * Extract Session ID from response headers
	 * @param headers HTTP response headers
	 */
	private void extractSessionIdFromHeaders(org.springframework.http.HttpHeaders headers) {
		logger.info("=== Starting Session ID extraction from response headers ===");
		logger.info("=== Response headers: {} ===", headers);

		// Try to get Session ID from different header fields
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
				logger.info("=== Successfully extracted Session ID: {} ===", this.sessionId);
			}
		}
		else {
			logger.warn("=== Session ID not found in response headers ===");
		}
	}

	/**
	 * Get current Session ID
	 * @return Current Session ID, returns null if not set
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Set Session ID
	 * @param sessionId Session ID to set
	 */
	public void setSessionId(String sessionId) {
		synchronized (sessionIdLock) {
			this.sessionId = sessionId;
			logger.info("=== Manually set Session ID: {} ===", this.sessionId);
		}
	}

	/**
	 * Pre-process JSON content to handle type conversion issues before deserialization
	 * @param jsonContent Original JSON content
	 * @return Processed JSON content with proper type handling
	 */
	private String preprocessJsonForDeserialization(String jsonContent) {
		try {
			logger.debug("=== Pre-processing JSON for deserialization ===");
			logger.debug("=== Original JSON: {} ===", jsonContent);

			// Parse the JSON to a Map
			Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);

			// Handle id field - convert Integer to String if needed
			Object idObj = data.get("id");
			if (idObj instanceof Integer) {
				data.put("id", String.valueOf(idObj));
				logger.debug("=== Converted id from Integer to String: {} ===", idObj);
			}

			// Handle method field - ensure it's a String
			Object methodObj = data.get("method");
			if (methodObj != null && !(methodObj instanceof String)) {
				data.put("method", String.valueOf(methodObj));
				logger.debug("=== Converted method to String: {} ===", methodObj);
			}

			// Handle jsonrpc field - ensure it's a String
			Object jsonrpcObj = data.get("jsonrpc");
			if (jsonrpcObj != null && !(jsonrpcObj instanceof String)) {
				data.put("jsonrpc", String.valueOf(jsonrpcObj));
				logger.debug("=== Converted jsonrpc to String: {} ===", jsonrpcObj);
			}

			// Convert back to JSON string
			String processedJson = objectMapper.writeValueAsString(data);
			logger.debug("=== Processed JSON: {} ===", processedJson);

			return processedJson;
		}
		catch (Exception e) {
			logger.warn("=== Failed to pre-process JSON: {} ===", e.getMessage());
			logger.warn("=== Returning original JSON content ===");
			return jsonContent;
		}
	}

}
