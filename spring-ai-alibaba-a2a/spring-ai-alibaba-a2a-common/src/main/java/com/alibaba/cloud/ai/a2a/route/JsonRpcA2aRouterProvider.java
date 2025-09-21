/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.route;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

import com.alibaba.cloud.ai.a2a.server.JsonRpcA2aRequestHandler;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author xiweng.yy
 */
public class JsonRpcA2aRouterProvider implements A2aRouterProvider<JsonRpcA2aRequestHandler> {

	private static final Logger log = LoggerFactory.getLogger(JsonRpcA2aRouterProvider.class);

	public static final String DEFAULT_WELL_KNOWN_URL = "/.well-known/agent.json";

	public static final String DEFAULT_MESSAGE_URL = "/a2a";

	private final String messageUrl;

	private final String wellKnownUrl;

	public JsonRpcA2aRouterProvider() {
		this(DEFAULT_WELL_KNOWN_URL, DEFAULT_MESSAGE_URL);
	}

	public JsonRpcA2aRouterProvider(String wellKnownUrl, String messageUrl) {
		this.wellKnownUrl = wellKnownUrl;
		this.messageUrl = messageUrl;
	}

	@Override
	public RouterFunction<ServerResponse> getRouter(JsonRpcA2aRequestHandler a2aRequestHandler) {
		return RouterFunctions.route()
				.GET(this.wellKnownUrl, new AgentCardHandler(a2aRequestHandler))
				.POST(this.messageUrl, new MessageHandler(a2aRequestHandler))
				.build();
	}

	private class AgentCardHandler implements HandlerFunction<ServerResponse> {

		private final JsonRpcA2aRequestHandler a2aRequestHandler;

		public AgentCardHandler(JsonRpcA2aRequestHandler a2aRequestHandler) {
			this.a2aRequestHandler = a2aRequestHandler;
		}

		@Override
		public ServerResponse handle(ServerRequest request) throws Exception {
			try {
				return ServerResponse.ok().body(a2aRequestHandler.getAgentCard());
			}
			catch (Exception e) {
				log.error("Failed to get Agent Card: {}", e.getMessage());
				return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	private class MessageHandler implements HandlerFunction<ServerResponse> {

		private final JsonRpcA2aRequestHandler a2aRequestHandler;

		private MessageHandler(JsonRpcA2aRequestHandler a2aRequestHandler) {
			this.a2aRequestHandler = a2aRequestHandler;
		}

		@Override
		public ServerResponse handle(ServerRequest request) throws Exception {
			try {
				String bodyString = request.body(String.class);
				Object result = a2aRequestHandler.onHandler(bodyString, request.headers());
				if (result instanceof Flux<?>) {
					return buildSseResponse((Flux<?>) result);
				}
				else {
					return buildJsonRpcResponse(result);
				}
			}
			catch (Exception e) {
				log.error("Failed to handle request: {}", e.getMessage());
				return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}

		private ServerResponse buildJsonRpcResponse(Object result) {
			return ServerResponse.ok().body(result);
		}

		private ServerResponse buildSseResponse(Flux<?> result) {
			return ServerResponse.sse(sseBuilder -> {
				sseBuilder.onComplete(() -> {
					log.debug("Agent SSE connection completed.");
				});
				sseBuilder.onTimeout(() -> {
					log.debug("Agent SSE connection timeout.");
				});
				result.subscribe((Consumer<Object>) o -> {
					if (o instanceof JSONRPCResponse) {
						try {
							String sseBody = Utils.OBJECT_MAPPER.writeValueAsString(o);
							if (log.isDebugEnabled()) {
								log.debug("send sse body to agent: {}", sseBody);
							}
							sseBuilder.data(sseBody);
							if (((JSONRPCResponse<?>) o).getResult() instanceof TaskStatusUpdateEvent) {
								TaskStatusUpdateEvent event = (TaskStatusUpdateEvent) ((JSONRPCResponse<?>) o).getResult();
								if (event.isFinal()) {
									sseBuilder.complete();
								}
							}
						}
						catch (IOException e) {
							sseBuilder.error(e);
						}
					}
				});
			}, Duration.ZERO);
		}

	}

}
