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

package com.alibaba.cloud.ai.example.deepresearch.config;

import com.google.common.collect.Maps;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * @author Allen Hu
 * @since 2025/5/24
 */
@ConfigurationProperties(prefix = DeepResearchProperties.PREFIX)
public class DeepResearchProperties {

	public static final String PREFIX = "spring.ai.alibaba.deepreserch";

	/**
	 * McpClient mapping for Agent name. key=Agent name, value=McpClient Name
	 */
	private Map<String, Set<String>> mcpClientMapping = Maps.newHashMap();

	private String mcpNodeConfigPath = "classpath:mcp-node-config.json";

	public Map<String, Set<String>> getMcpClientMapping() {
		return mcpClientMapping;
	}

	public void setMcpClientMapping(Map<String, Set<String>> mcpClientMapping) {
		this.mcpClientMapping = mcpClientMapping;
	}

	public String getMcpNodeConfigPath() {
		return mcpNodeConfigPath;
	}

	public void setMcpNodeConfigPath(String mcpNodeConfigPath) {
		this.mcpNodeConfigPath = mcpNodeConfigPath;
	}

	/**
	 * MCP Server configuration for nodes
	 */
	public static class McpServerConfig {
		private String url;
		private String description;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	/**
	 * Node MCP configuration
	 */
	public static class NodeMcpConfig {
		private List<McpServerConfig> mcpServers;

		public List<McpServerConfig> getMcpServers() {
			return mcpServers;
		}

		public void setMcpServers(List<McpServerConfig> mcpServers) {
			this.mcpServers = mcpServers;
		}
	}

}
