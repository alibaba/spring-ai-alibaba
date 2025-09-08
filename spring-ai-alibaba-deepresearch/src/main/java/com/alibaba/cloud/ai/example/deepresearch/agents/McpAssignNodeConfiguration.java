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

package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.config.McpAssignNodeProperties;
import com.alibaba.cloud.ai.example.deepresearch.util.mcp.McpConfigMergeUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Allocate the specified MCP to the target node
 *
 * @author Makoto
 */
@ConditionalOnProperty(prefix = McpAssignNodeProperties.MCP_ASSIGN_NODE_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties({ McpAssignNodeProperties.class, McpClientCommonProperties.class })
@Configuration
public class McpAssignNodeConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpAssignNodeConfiguration.class);

	@Autowired
	private McpAssignNodeProperties mcpAssignNodeProperties;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Parse the JSON configuration file
	 */
	@Bean(name = "agent2mcpConfig")
	public Map<String, McpAssignNodeProperties.McpServerConfig> agent2mcpConfig() {
		try {
			Resource resource = resourceLoader.getResource(mcpAssignNodeProperties.getConfigLocation());
			if (!resource.exists()) {
				return new HashMap<>();
			}

			try (InputStream inputStream = resource.getInputStream()) {
				TypeReference<Map<String, McpAssignNodeProperties.McpServerConfig>> typeRef = new TypeReference<>() {
				};
				return objectMapper.readValue(inputStream, typeRef);
			}
		}
		catch (IOException e) {
			logger.error("读取MCP配置失败", e);
			return new HashMap<>();
		}
	}

	/**
	 * MCP configuration provider with runtime configuration support
	 */
	@Bean(name = "agent2mcpConfigWithRuntime")
	public Function<OverAllState, Map<String, McpAssignNodeProperties.McpServerConfig>> agent2mcpConfigWithRuntime(
			@Qualifier("agent2mcpConfig") Map<String, McpAssignNodeProperties.McpServerConfig> staticConfig) {

		return state -> {
			// Fetch the runtime MCP configuration
			Map<String, Object> runtimeMcpSettings = state.value("mcp_settings", Map.class)
				.orElse(Collections.emptyMap());
			return McpConfigMergeUtil.mergeAgent2McpConfigs(staticConfig, runtimeMcpSettings, objectMapper);
		};
	}

	// The MCP client creation logic has been moved to be handled internally within each node.
	// The configuration class is now solely responsible for providing configuration data,
	// while the actual client instantiation is performed during node runtime.

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

}