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

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service for managing MCP node configurations
 * 
 * @author Makoto
 * @since 2025/1/14
 */
@Service
public class McpNodeConfigService {

	private static final Logger logger = LoggerFactory.getLogger(McpNodeConfigService.class);

	private final DeepResearchProperties deepResearchProperties;
	private final ResourceLoader resourceLoader;
	private final ObjectMapper objectMapper;

	private Map<String, DeepResearchProperties.NodeMcpConfig> nodeConfigs;

	public McpNodeConfigService(DeepResearchProperties deepResearchProperties, 
								ResourceLoader resourceLoader,
								ObjectMapper objectMapper) {
		this.deepResearchProperties = deepResearchProperties;
		this.resourceLoader = resourceLoader;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	public void loadConfiguration() {
		try {
			Resource resource = resourceLoader.getResource(deepResearchProperties.getMcpNodeConfigPath());
			if (resource.exists()) {
				TypeReference<Map<String, DeepResearchProperties.NodeMcpConfig>> typeRef = 
					new TypeReference<Map<String, DeepResearchProperties.NodeMcpConfig>>() {};
				nodeConfigs = objectMapper.readValue(resource.getInputStream(), typeRef);
				logger.info("Loaded MCP node configuration: {}", nodeConfigs.keySet());
			} else {
				logger.warn("MCP node configuration file not found: {}", deepResearchProperties.getMcpNodeConfigPath());
				nodeConfigs = Collections.emptyMap();
			}
		} catch (IOException e) {
			logger.error("Failed to load MCP node configuration", e);
			nodeConfigs = Collections.emptyMap();
		}
	}

	/**
	 * Get MCP server configurations for a specific node
	 * 
	 * @param nodeName the node name (e.g., "researcher", "coder")
	 * @return list of MCP server configurations
	 */
	public List<DeepResearchProperties.McpServerConfig> getMcpServersForNode(String nodeName) {
		DeepResearchProperties.NodeMcpConfig nodeConfig = nodeConfigs.get(nodeName);
		if (nodeConfig != null && nodeConfig.getMcpServers() != null) {
			return nodeConfig.getMcpServers();
		}
		return Collections.emptyList();
	}

	/**
	 * Check if a node has MCP server configurations
	 * 
	 * @param nodeName the node name
	 * @return true if the node has MCP configurations
	 */
	public boolean hasMcpServersForNode(String nodeName) {
		return !getMcpServersForNode(nodeName).isEmpty();
	}

	/**
	 * Get all configured node names
	 * 
	 * @return set of configured node names
	 */
	public java.util.Set<String> getConfiguredNodes() {
		return nodeConfigs.keySet();
	}
} 