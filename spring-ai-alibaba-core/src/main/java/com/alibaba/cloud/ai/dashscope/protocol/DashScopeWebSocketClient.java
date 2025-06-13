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
package com.alibaba.cloud.ai.dashscope.protocol;

import com.alibaba.cloud.ai.dashscope.api.ApiUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Headers;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Dispatcher;
import okhttp3.Protocol;
import okhttp3.ConnectionPool;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author kevinlin09
 */
public class DashScopeWebSocketClient extends WebSocketListener {

	private final Logger logger = LoggerFactory.getLogger(DashScopeWebSocketClient.class);

	private final DashScopeWebSocketClientOptions options;

	private WebSocket webSocketClient;

	private AtomicBoolean isOpen;

	FluxSink<ByteBuffer> emitter;

	FluxSink<ByteBuffer> binary_emitter;

	FluxSink<String> text_emitter;

	public DashScopeWebSocketClient(DashScopeWebSocketClientOptions options) {
		this.options = options;
		this.isOpen = new AtomicBoolean(false);
	}

	public Flux<ByteBuffer> streamBinaryOut(String text) {
		Flux<ByteBuffer> flux = Flux.<ByteBuffer>create(emitter -> {
			this.binary_emitter = emitter;
		}, FluxSink.OverflowStrategy.BUFFER);

		sendText(text);

		return flux;
	}

	public Flux<String> streamTextOut(Flux<ByteBuffer> binary) {
		Flux<String> flux = Flux.<String>create(emitter -> {
			this.text_emitter = emitter;
		}, FluxSink.OverflowStrategy.BUFFER);

		binary.subscribe(this::sendBinary);

		return flux;
	}

	public void sendText(String text) {
		if (!isOpen.get()) {
			establishWebSocketClient();
		}

		boolean success = webSocketClient.send(text);

		if (!success) {
			logger.error("send text failed");
		}
	}

	public void sendBinary(ByteBuffer binary) {
		// TODO
		// if (!isOpen.get()) {
		// establishWebSocketClient();
		// }

		boolean success = webSocketClient.send(ByteString.of(binary));

		if (!success) {
			logger.error("send binary failed");
		}
	}

	private void establishWebSocketClient() {
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.valueOf(Constants.DEFAULT_HTTP_LOGGING_LEVEL));
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(Constants.DEFAULT_MAXIMUM_ASYNC_REQUESTS);
		dispatcher.setMaxRequestsPerHost(Constants.DEFAULT_MAXIMUM_ASYNC_REQUESTS_PER_HOST);

		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
		clientBuilder.connectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT)
			.readTimeout(Constants.DEFAULT_READ_TIMEOUT)
			.writeTimeout(Constants.DEFAULT_WRITE_TIMEOUT)
			.addInterceptor(logging)
			.dispatcher(dispatcher)
			.protocols(Collections.singletonList(Protocol.HTTP_1_1))
			.connectionPool(new ConnectionPool(Constants.DEFAULT_CONNECTION_POOL_SIZE,
					Constants.DEFAULT_CONNECTION_IDLE_TIMEOUT.getSeconds(), TimeUnit.SECONDS));
		OkHttpClient httpClient = clientBuilder.build();

		try {
			webSocketClient = httpClient.newWebSocket(buildConnectionRequest(), this);
		}
		catch (Throwable ex) {
			logger.error("create websocket failed: msg={}", ex.getMessage());
		}
	}

	private Request buildConnectionRequest() {
		Builder bd = new Request.Builder();
		bd.headers(
				Headers.of(ApiUtils.getMapContentHeaders(options.getApiKey(), false, options.getWorkSpaceId(), null)));
		return bd.url(options.getUrl()).build();
	}

	private String getRequestBody(Response response) {
		String responseBody = "";
		if (response != null && response.body() != null) {
			try {
				responseBody = response.body().string();
			}
			catch (IOException ex) {
				logger.error("get response body failed: {}", ex.getMessage());
			}
		}
		return responseBody;
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		logger.info("receive ws event onOpen: handle={}, body={}", webSocket, getRequestBody(response));
		isOpen.set(true);
	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		logger.info("receive ws event onClosed: handle={}, code={}, reason={}", webSocket, code, reason);
		isOpen.set(false);
		emittersComplete("closed");
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		logger.info("receive ws event onClosing: handle={}, code={}, reason={}", webSocket.toString(), code, reason);
		emittersComplete("closing");
		webSocket.close(code, reason);
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		String failureMessage = String.format("msg=%s, cause=%s, body=%s", t.getMessage(), t.getCause(),
				getRequestBody(response));
		logger.error("receive ws event onFailure: handle={}, {}", webSocket, failureMessage);
		isOpen.set(false);
		emittersError("failure", new Exception(failureMessage, t));
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		logger.debug("receive ws event onMessage(text): handle={}, text={}", webSocket, text);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			EventMessage message = objectMapper.readValue(text, EventMessage.class);
			switch (message.header.event) {
				case TASK_STARTED:
					logger.info("task started: text={}", text);
					break;
				case TASK_FINISHED:
					logger.info("task finished: text={}", text);
					emittersComplete("finished");
					break;
				case TASK_FAILED:
					logger.error("task failed: text={}", text);
					emittersError("task failed", new Exception());
					break;
				case RESULT_GENERATED:
					if (this.text_emitter != null) {
						text_emitter.next(text);
					}
					break;
				default:
					logger.error("task error: text={}", text);
					emittersError("unsupported event", new Exception());
			}
		}
		catch (Exception e) {
			logger.error("parse message failed: text={}, msg={}", text, e.getMessage());
		}
	}

	@Override
	public void onMessage(WebSocket webSocket, ByteString bytes) {
		logger.debug("receive ws event onMessage(bytes): handle={}, size={}", webSocket, bytes.size());
		if (this.binary_emitter != null) {
			binary_emitter.next(bytes.asByteBuffer());
		}
	}

	private void emittersComplete(String event) {
		if (this.binary_emitter != null && !this.binary_emitter.isCancelled()) {
			logger.info("binary emitter handling: complete on {}", event);
			this.binary_emitter.complete();
		}
		if (this.text_emitter != null && !this.text_emitter.isCancelled()) {
			logger.info("text emitter handling: complete on {}", event);
			this.text_emitter.complete();
			logger.info("done");
		}
	}

	private void emittersError(String event, Throwable t) {
		if (this.binary_emitter != null && !this.binary_emitter.isCancelled()) {
			logger.info("binary emitter handling: error on {}", event);
			this.binary_emitter.error(t);
		}
		if (this.text_emitter != null && !this.text_emitter.isCancelled()) {
			logger.info("text emitter handling: error on {}", event);
			this.text_emitter.error(t);
		}
	}

	public static class Constants {

		private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(120);

		private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(60);

		private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(300);

		private static final Duration DEFAULT_CONNECTION_IDLE_TIMEOUT = Duration.ofSeconds(300);

		private static final Integer DEFAULT_CONNECTION_POOL_SIZE = 32;

		private static final Integer DEFAULT_MAXIMUM_ASYNC_REQUESTS = 32;

		private static final Integer DEFAULT_MAXIMUM_ASYNC_REQUESTS_PER_HOST = 32;

		private static final String DEFAULT_HTTP_LOGGING_LEVEL = "NONE";

	}

	// @formatter:off
	public enum EventType {

		// receive
		@JsonProperty("task-started")
		TASK_STARTED("task-started"),

		@JsonProperty("result-generated")
		RESULT_GENERATED("result-generated"),

		@JsonProperty("task-finished")
		TASK_FINISHED("task-finished"),

		@JsonProperty("task-failed")
		TASK_FAILED("task-failed"),

		// send
		@JsonProperty("run-task")
		RUN_TASK("run-task"),

		@JsonProperty("continue-task")
		CONTINUE_TASK("continue-task"),

		@JsonProperty("finish-task")
		FINISH_TASK("finish-task");

		private final String value;

		private EventType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EventMessage(
		@JsonProperty("header") EventMessageHeader header,
		@JsonProperty("payload") EventMessagePayload payload
	) {
		public record EventMessageHeader (
			@JsonProperty("task_id") String taskId,
			@JsonProperty("event") EventType event,
			@JsonProperty("error_code") String code,
			@JsonProperty("error_message") String message
		){}
		public record EventMessagePayload(
			@JsonProperty("output") JsonNode output,
			@JsonProperty("usage")  JsonNode usage
		){}
	}
	// @formatter:on

}
