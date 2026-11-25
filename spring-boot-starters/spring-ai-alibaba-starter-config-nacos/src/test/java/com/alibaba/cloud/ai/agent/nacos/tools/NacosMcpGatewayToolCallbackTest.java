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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.client.McpClient;
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
	@ValueSource(strings = {"mcp-sse", "mcp-streamable"})
	void invokesRemoteToolsWithArgumentsAndHeaders(String protocol) throws Exception {
		boolean sse = protocol.equals("mcp-sse");
		var mapper = new ObjectMapper();
		var events = new LinkedBlockingQueue<String>();
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		var executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/gateway", exchange -> {
			if (!sse || !"GET".equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				exchange.close();
				return;
			}
			exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
			exchange.sendResponseHeaders(200, 0);
			try (var body = exchange.getResponseBody()) {
				body.write("event: endpoint\ndata: /rpc\n\n".getBytes(StandardCharsets.UTF_8));
				body.flush();
				for (int n = 0; n < 2; n++) {
					String event = events.poll(5, TimeUnit.SECONDS);
					if (event == null) {
						break;
					}
					body.write(("event: message\ndata: " + event + "\n\n").getBytes(StandardCharsets.UTF_8));
					body.flush();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
		server.createContext("/rpc", exchange -> {
			try {
				if (!"POST".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				var request = mapper.readTree(exchange.getRequestBody());
				if (!request.has("id")) {
					exchange.sendResponseHeaders(202, -1);
					return;
				}
				Map<String, ?> result;
				if ("initialize".equals(request.path("method").asText())) {
					result = Map.of("protocolVersion", request.path("params").path("protocolVersion").asText(),
							"capabilities", Map.of("tools", Map.of()), "serverInfo", Map.of("name", "fixture", "version", "1"));
				}
				else {
					var params = request.path("params");
					String text = request.path("method").asText() + " " + params.path("name").asText() + " "
							+ params.path("arguments").path("ticket").asText() + " "
							+ exchange.getRequestHeaders().getFirst("X-Fixture");
					result = Map.of("content", List.of(Map.of("type", "text", "text", text)));
				}
				String response = mapper.writeValueAsString(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", result));
				if (sse) {
					events.add(response);
					exchange.sendResponseHeaders(202, -1);
				}
				else {
					byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
					exchange.getResponseHeaders().add("Content-Type", "application/json");
					exchange.sendResponseHeaders(200, bytes.length);
					exchange.getResponseBody().write(bytes);
				}
			}
			finally {
				exchange.close();
			}
		});
		server.start();
		try {
			var ref = new McpServiceRef();
			ref.setServiceName("order-service");
			var remote = new McpServerRemoteServiceConfig();
			remote.setServiceRef(ref);
			remote.setExportPath(sse ? "/gateway" : "/rpc");
			var endpoint = new McpEndpointInfo();
			endpoint.setAddress("127.0.0.1");
			endpoint.setPort(server.getAddress().getPort());
			var nacos = mock(NacosMcpOperationService.class);
			when(nacos.selectEndpoint(ref)).thenReturn(endpoint);
			var definition = NacosMcpGatewayToolDefinition.builder().name("order-service_tools_query")
					.description("Query orders").inputSchema(Map.of("type", "object"))
					.protocol(protocol).remoteServerConfig(remote).build();
			var configuration = new McpServersVO.McpServerVO();
			configuration.setHeaders(Map.of("X-Fixture", "authenticated-fixture"));
			var callback = new NacosMcpGatewayToolCallback(definition, nacos, configuration, Duration.ofSeconds(5));

			assertEquals("tools/call query 123 authenticated-fixture", callback.call("{\"ticket\":\"123\"}"));
		}
		finally {
			server.stop(0);
			executor.shutdownNow();
		}
	}

	private static NacosMcpGatewayToolDefinition toolDefinition() {
		return NacosMcpGatewayToolDefinition.builder()
				.name("order-service_tools_query")
				.description("Query orders")
				.inputSchema(Map.of("type", "object", "properties", Map.of()))
				.build();
	}

}
