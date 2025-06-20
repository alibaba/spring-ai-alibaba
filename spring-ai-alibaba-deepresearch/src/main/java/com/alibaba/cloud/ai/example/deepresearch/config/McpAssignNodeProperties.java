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
 * MCP代理节点分配配置属性
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = McpAssignNodeProperties.MCP_ASSIGN_NODE_PREFIX)
public class McpAssignNodeProperties {

	public static final String MCP_ASSIGN_NODE_PREFIX = DeepResearchProperties.PREFIX + ".mcp";

	/**
	 * 是否启用MCP代理节点分配
	 */
	private boolean enabled = true;

	private String configLocation = "classpath:mcp-config.json";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getConfigLocation() {
		return configLocation;
	}

	public void setConfigLocation(String configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * MCP服务器配置
	 */
	public static record McpServerConfig(@JsonProperty("mcp-servers") List<McpServerInfo> mcpServers) {
	}

	/**
	 * MCP服务器信息
	 */
	public static record McpServerInfo(String url, String sseEndpoint, String description, boolean enabled) {
	}

}
