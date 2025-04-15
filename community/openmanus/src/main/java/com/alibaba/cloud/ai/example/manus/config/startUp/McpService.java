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
package com.alibaba.cloud.ai.example.manus.config.startUp;

import com.alibaba.cloud.ai.example.manus.config.entity.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.config.repository.McpConfigRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpService implements InitializingBean {

	@Autowired
	private McpConfigRepository mcpConfigRepository;

	private final Map<String, Map<McpAsyncClient, AsyncMcpToolCallbackProvider>> toolCallbackMap = new ConcurrentHashMap<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		// https://mcp.higress.ai/mcp-stock-helper
		for (McpConfigEntity mcpConfigEntity : mcpConfigRepository.findAll()) {
			addClient(mcpConfigEntity);
		}
	}

	private void addClient(McpConfigEntity mcpConfigEntity) throws IOException {
		if (toolCallbackMap.containsKey(mcpConfigEntity.getConnectionConfig())) {
			return;
		}

		McpClientTransport transport = null;
		switch (mcpConfigEntity.getConnectionType()) {
			case SSE -> {
				WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(mcpConfigEntity.getConnectionConfig());
				transport = new WebFluxSseClientTransport(webClientBuilder, new ObjectMapper());
			}
			case STUDIO -> {
				try (JsonParser jsonParser = new ObjectMapper().createParser(mcpConfigEntity.getConnectionConfig())) {
					ServerParameters serverParameters = jsonParser.readValueAs(ServerParameters.class);
					transport = new StdioClientTransport(serverParameters);
				}
			}
		}
		if (transport != null) {
			McpAsyncClient mcpAsyncClient = McpClient.async(transport)
				.clientInfo(new McpSchema.Implementation(mcpConfigEntity.getMcpServerName(), "1.0.0"))
				.build();
			mcpAsyncClient.initialize().block();
			toolCallbackMap.computeIfAbsent(mcpConfigEntity.getConnectionConfig(), k -> new ConcurrentHashMap<>())
				.put(mcpAsyncClient, new AsyncMcpToolCallbackProvider(mcpAsyncClient));
		}
	}

	public void addMcpServer(McpConfigEntity mcpConfigEntity) throws IOException {
		// TODO Check connection config structure
		addClient(mcpConfigEntity);
		mcpConfigRepository.save(mcpConfigEntity);
	}

	public void removeMcpServer(String mcpServerName) {
		McpConfigEntity mcpConfigEntity = mcpConfigRepository.findByMcpServerName(mcpServerName);
		if (mcpConfigEntity != null) {
			Map<McpAsyncClient, AsyncMcpToolCallbackProvider> map = toolCallbackMap
				.remove(mcpConfigEntity.getConnectionConfig());
			if (map != null) {
				map.keySet().forEach(McpAsyncClient::close);
			}
		}
		mcpConfigRepository.deleteByMcpServerName(mcpServerName);
	}

	public List<McpConfigEntity> getMcpServers() {
		return mcpConfigRepository.findAll();
	}

	public List<ToolCallback> getFunctionCallbacks() {
		return toolCallbackMap.values()
			.stream()
			.flatMap(map -> map.values().stream())
			.map(AsyncMcpToolCallbackProvider::getToolCallbacks)
			.map(List::of)
			.flatMap(List::stream)
			.toList();
	}

}
