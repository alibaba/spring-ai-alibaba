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

package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.config.McpAssignNodeProperties;
import com.alibaba.cloud.ai.example.deepresearch.util.mcp.McpClientUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Function;

/**
 * MCP Provider Factory Service
 * Responsible for creating and managing MCP providers
 *
 * @author Makoto
 */
@Service
@ConditionalOnProperty(prefix = McpAssignNodeProperties.MCP_ASSIGN_NODE_PREFIX, name = "enabled", havingValue = "true")
public class McpProviderFactory {

	private final Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> mcpConfigProvider;

	private final McpAsyncClientConfigurer mcpAsyncClientConfigurer;

	private final McpClientCommonProperties commonProperties;

	private final WebClient.Builder webClientBuilderTemplate;

	private final ObjectMapper objectMapper;

	@Autowired
	public McpProviderFactory(
			@Qualifier("agent2mcpConfigWithRuntime") Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> mcpConfigProvider,
			McpAsyncClientConfigurer mcpAsyncClientConfigurer, McpClientCommonProperties commonProperties,
			WebClient.Builder webClientBuilderTemplate, ObjectMapper objectMapper) {
		this.mcpConfigProvider = mcpConfigProvider;
		this.mcpAsyncClientConfigurer = mcpAsyncClientConfigurer;
		this.commonProperties = commonProperties;
		this.webClientBuilderTemplate = webClientBuilderTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Core method: Directly creates an MCP provider
	 * @param state State object
	 * @param agentName Agent name
	 * @return MCP tool callback provider
	 */
	public AsyncMcpToolCallbackProvider createProvider(OverAllState state, String agentName) {
		return McpClientUtil.createMcpProvider(state, agentName, mcpConfigProvider, mcpAsyncClientConfigurer,
				commonProperties, webClientBuilderTemplate, objectMapper);
	}

}
