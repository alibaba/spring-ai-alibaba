/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.core.route;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

import com.alibaba.cloud.ai.a2a.core.server.JsonRpcA2aRequestHandler;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Router provider for multi-agent A2A server.
 * <p>
 * Provides routing for multiple agents, each accessible at its own URL path:
 * <ul>
 *   <li>Agent card: /.well-known/agent.json/{agentName}</li>
 *   <li>Message endpoint: /a2a/{agentName}</li>
 * </ul>
 *
 * @author xiweng.yy
 */
public class MultiAgentJsonRpcRouterProvider {

	private static final Logger log = LoggerFactory.getLogger(MultiAgentJsonRpcRouterProvider.class);

	public static final String MULTI_AGENT_WELL_KNOWN_URL = "/.well-known/agent.json/{agentName}";

	public static final String MULTI_AGENT_MESSAGE_URL = "/a2a/{agentName}";

	private final String messageUrl;

	private final String wellKnownUrl;

	private final MultiAgentRequestRouter router;

	public MultiAgentJsonRpcRouterProvider(MultiAgentRequestRouter router) {
		this(MULTI_AGENT_WELL_KNOWN_URL, MULTI_AGENT_MESSAGE_URL, router);
	}

	public MultiAgentJsonRpcRouterProvider(String wellKnownUrl, String messageUrl, MultiAgentRequestRouter router) {
		this.wellKnownUrl = wellKnownUrl;
		this.messageUrl = messageUrl;
		this.router = router;
	}

	public RouterFunction<ServerResponse> getRouter() {
		return RouterFunctions.route()
			.GET(this.wellKnownUrl, new MultiAgentCardHandler(router))
			.POST(this.messageUrl, new MultiAgentMessageHandler(router))
			.build();
	}

	private static class MultiAgentCardHandler implements HandlerFunction<ServerResponse> {

		private final MultiAgentRequestRouter router;

		public MultiAgentCardHandler(MultiAgentRequestRouter router) {
			this.router = router;
		}

		@Override
		public ServerResponse handle(ServerRequest request) throws Exception {
			String agentName = request.pathVariable("agentName");
			JsonRpcA2aRequestHandler handler = router.getHandler(agentName);
			if (handler == null) {
				log.warn("Agent not found: {}", agentName);
				return ServerResponse.status(HttpStatus.NOT_FOUND)
					.body("Agent not found: " + agentName);
			}
			try {
				return ServerResponse.ok().body(handler.getAgentCard());
			}
			catch (Exception e) {
				log.error("Failed to get Agent Card for {}: {}", agentName, e.getMessage());
				return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}

	}

	private static class MultiAgentMessageHandler implements HandlerFunction<ServerResponse> {

		private final MultiAgentRequestRouter router;

		private MultiAgentMessageHandler(MultiAgentRequestRouter router) {
			this.router = router;
		}

		@Override
		public ServerResponse handle(ServerRequest request) throws Exception {
			String agentName = request.pathVariable("agentName");
			JsonRpcA2aRequestHandler handler = router.getHandler(agentName);
			if (handler == null) {
				log.warn("Agent not found: {}", agentName);
				return ServerResponse.status(HttpStatus.NOT_FOUND)
					.body("Agent not found: " + agentName);
			}
			try {
				String bodyString = request.body(String.class);
				Object result = handler.onHandler(bodyString, request.headers());
				if (result instanceof Flux<?>) {
					return buildSseResponse((Flux<?>) result);
				}
				else {
					return buildJsonRpcResponse(result);
				}
			}
			catch (Exception e) {
				log.error("Failed to handle request for {}: {}", agentName, e.getMessage());
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
								TaskStatusUpdateEvent event = (TaskStatusUpdateEvent) ((JSONRPCResponse<?>) o)
									.getResult();
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
