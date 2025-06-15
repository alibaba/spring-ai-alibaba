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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * MCP JSON配置属性类 支持从JSON文件读取MCP服务器配置
 *
 * @author Makoto
 * @since 2025/6/14
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.deepresearch.mcp")
public class McpJsonProperties {

	private String configFile = "classpath:mcp-config.json";

	/**
	 * 是否启用JSON配置
	 */
	private boolean enabled = true;

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * MCP服务器配置
	 */
	public static class McpServerConfig {

		private String url;

		private String description;

		private boolean enabled = true;

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

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			return "McpServerConfig{" + "url='" + url + '\'' + ", description='" + description + '\'' + ", enabled="
					+ enabled + '}';
		}

	}

	/**
	 * 代理配置
	 */
	public static class AgentConfig {

		@JsonProperty("mcp-servers")
		private List<McpServerConfig> mcpServers;

		public List<McpServerConfig> getMcpServers() {
			return mcpServers;
		}

		public void setMcpServers(List<McpServerConfig> mcpServers) {
			this.mcpServers = mcpServers;
		}

		public List<McpServerConfig> getEnabledMcpServers() {
			if (mcpServers == null) {
				return List.of();
			}
			return mcpServers.stream().filter(McpServerConfig::isEnabled).toList();
		}

		@Override
		public String toString() {
			return "AgentConfig{" + "mcpServers=" + mcpServers + '}';
		}

	}

	/**
	 * JSON配置结构
	 */
	public static class McpJsonConfig {

		private AgentConfig coder;

		private AgentConfig researcher;

		public AgentConfig getCoder() {
			return coder;
		}

		public void setCoder(AgentConfig coder) {
			this.coder = coder;
		}

		public AgentConfig getResearcher() {
			return researcher;
		}

		public void setResearcher(AgentConfig researcher) {
			this.researcher = researcher;
		}

		public java.util.Map<String, AgentConfig> getAllAgentConfigs() {
			java.util.Map<String, AgentConfig> configs = new java.util.HashMap<>();
			if (coder != null) {
				configs.put("coderAgent", coder);
			}
			if (researcher != null) {
				configs.put("researchAgent", researcher);
			}
			return configs;
		}

		@Override
		public String toString() {
			return "McpJsonConfig{" + "coder=" + coder + ", researcher=" + researcher + '}';
		}

	}

}
