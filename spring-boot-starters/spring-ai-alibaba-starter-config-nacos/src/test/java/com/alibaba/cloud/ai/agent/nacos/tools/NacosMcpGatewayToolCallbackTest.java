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

package com.alibaba.cloud.ai.agent.nacos.tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.utils.NacosMcpProtocol;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.modern.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NacosMcpGatewayToolCallbackTest {

	@Test
	void shouldApplyRequestAndInitializationTimeoutsWhenConfigured() {
		NacosMcpGatewayToolCallback callback = new NacosMcpGatewayToolCallback(toolDefinition(),
				mock(NacosMcpOperationService.class), new McpServersVO.McpServerVO(), Duration.ofSeconds(90));
		McpClient.SyncSpec clientSpec = mock(McpClient.SyncSpec.class, RETURNS_SELF);

		callback.configureTimeouts(clientSpec);

		verify(clientSpec).requestTimeout(Duration.ofSeconds(90));
		verify(clientSpec).initializationTimeout(Duration.ofSeconds(90));
	}

	@Test
	void shouldNotOverrideSdkTimeoutsWhenUnset() {
		NacosMcpGatewayToolCallback callback = new NacosMcpGatewayToolCallback(toolDefinition(),
				mock(NacosMcpOperationService.class), new McpServersVO.McpServerVO(), null);
		McpClient.SyncSpec clientSpec = mock(McpClient.SyncSpec.class, RETURNS_SELF);

		callback.configureTimeouts(clientSpec);

		verifyNoInteractions(clientSpec);
	}

	@ParameterizedTest
	@ValueSource(strings = { "mcp-modern", "mcp-streamable", "mcp-sse" })
	void callsModernAndLegacyToolsUsingTheRegistryProtocol(String protocol) throws Exception {
		boolean modern = "mcp-modern".equals(protocol);
		boolean sse = "mcp-sse".equals(protocol);
		var received = new AtomicReference<Invocation>();
		var eventStream = new AtomicReference<HttpExchange>();
		var methods = new ArrayList<String>();
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		if (sse) {
			server.createContext("/custom-mcp", exchange -> {
				exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
				exchange.sendResponseHeaders(200, 0);
				eventStream.set(exchange);
				sendEvent(exchange, "endpoint", "/message");
			});
			server.createContext("/message", exchange -> respond(exchange, methods, received, eventStream.get()));
		}
		else {
			server.createContext("/custom-mcp", exchange -> respond(exchange, methods, received, null));
		}
		server.start();
		try {
			var ref = new McpServiceRef();
			ref.setServiceName(modern ? NacosMcpProtocol.modernServiceName("orders") : "orders");
			ref.setTransportProtocol("http");
			var remote = new McpServerRemoteServiceConfig();
			remote.setServiceRef(ref);
			remote.setExportPath("/custom-mcp");
			var endpoint = new McpEndpointInfo();
			endpoint.setAddress("127.0.0.1");
			endpoint.setPort(server.getAddress().getPort());
			var registry = mock(NacosMcpOperationService.class);
			when(registry.selectEndpoint(ref)).thenReturn(endpoint);

			var tool = new McpTool();
			tool.setName("query");
			tool.setDescription("Query orders");
			tool.setInputSchema(Map.of("type", "object"));
			var specification = new McpToolSpecification();
			specification.setTools(List.of(tool));
			specification.setToolsMeta(Map.of());
			var detail = new McpServerDetailInfo();
			detail.setName("orders");
			detail.setProtocol(modern ? "mcp-streamable" : protocol);
			detail.setRemoteServerConfig(remote);
			detail.setToolSpec(specification);
			when(registry.getServerDetail("orders")).thenReturn(detail);

			var cfg = new McpServersVO.McpServerVO();
			cfg.setMcpServerName("orders");
			cfg.setHeaders(Map.of("Authorization", "Bearer fixture"));
			cfg.setQueryParams(Map.of("tenant", "test tenant"));
			var tools = new NacosMcpGatewayToolsInitializer(registry, List.of(cfg), Duration.ofSeconds(3)).initializeTools();
			assertEquals(1, tools.size());
			assertEquals("order matched", tools.get(0).call("{\"order\":42}"), methods::toString);
			assertEquals(new Invocation("query", 42, "Bearer fixture", "tenant=test tenant"), received.get());
			assertEquals(modern ? List.of("tools/list", "tools/call") : List.of("initialize", "notifications/initialized", "tools/call"),
					methods);
		}
		finally {
			server.stop(0);
		}
	}

	private static void sendEvent(HttpExchange stream, String event, String data) throws IOException {
		var body = stream.getResponseBody();
		body.write(("event: " + event + "\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
		body.flush();
	}

	private static void respond(HttpExchange exchange, List<String> methods, AtomicReference<Invocation> received,
			HttpExchange eventStream) throws IOException {
		try (exchange) {
			if (!"POST".equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}
			var mapper = new ObjectMapper();
			var request = mapper.readTree(exchange.getRequestBody());
			String method = request.path("method").asText();
			methods.add(method);
			if ("notifications/initialized".equals(method)) {
				exchange.sendResponseHeaders(202, -1);
				return;
			}
			var params = request.path("params");
			Map<String, Object> result;
			if ("initialize".equals(method)) {
				result = Map.of("protocolVersion", params.path("protocolVersion").asText(),
						"capabilities", Map.of("tools", Map.of()), "serverInfo", Map.of("name", "fixture", "version", "1"));
			}
			else if ("server/discover".equals(method)) {
				result = Map.of("supportedVersions", List.of(McpSchema.LATEST_PROTOCOL_VERSION),
						"capabilities", Map.of("tools", Map.of()));
			}
			else if ("tools/list".equals(method)) {
				result = Map.of("tools", List.of(Map.of("name", "query", "description", "Query orders",
						"inputSchema", Map.of("type", "object"))), "ttlMs", 0, "cacheScope", "private");
			}
			else if ("tools/call".equals(method)) {
				received.set(new Invocation(params.path("name").asText(), params.path("arguments").path("order").asInt(),
						exchange.getRequestHeaders().getFirst("Authorization"),
						(eventStream == null ? exchange : eventStream).getRequestURI().getQuery()));
				result = Map.of("content", List.of(Map.of("type", "text", "text", "order matched")), "isError", false);
			}
			else {
				exchange.sendResponseHeaders(400, -1);
				return;
			}
			var response = Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", result);
			if (eventStream != null) {
				exchange.sendResponseHeaders(202, -1);
				sendEvent(eventStream, "message", mapper.writeValueAsString(response));
			}
			else {
				var body = mapper.writeValueAsBytes(response);
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		}
	}

	private record Invocation(String tool, int order, String authorization, String query) {
	}

	private static NacosMcpGatewayToolDefinition toolDefinition() {
		return NacosMcpGatewayToolDefinition.builder()
				.name("order-service_tools_query")
				.description("Query orders")
				.inputSchema(Map.of("type", "object", "properties", Map.of()))
				.build();
	}

}
