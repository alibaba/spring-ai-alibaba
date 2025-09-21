/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.config;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = McpRouterProperties.CONFIG_PREFIX)
public class McpRouterProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.router";

	private boolean enabled = true;

	/**
	 * MCP router service names
	 */
	private List<String> serviceNames = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(final List<String> serviceNames) {
		this.serviceNames = serviceNames;
	}

	/**
	 * MCP router service configurations
	 */
	private List<McpServerInfo> services = new ArrayList<>();

	public List<McpServerInfo> getServices() {
		return services;
	}

	public void setServices(List<McpServerInfo> services) {
		this.services = services;
	}

	/**
	 * MCP router service discovery search order 支持多种发现方式同时使用，按顺序查找服务 例如: ["file",
	 * "database", "nacos"]
	 */
	private List<String> discoveryOrder = List.of("nacos");

	public List<String> getDiscoveryOrder() {
		return discoveryOrder;
	}

	public void setDiscoveryOrder(List<String> discoveryOrder) {
		this.discoveryOrder = discoveryOrder;
	}

}
