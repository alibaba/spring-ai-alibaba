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

package com.alibaba.cloud.ai.mcp.router.core;

import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.McpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class McpRouterWatcher extends AbstractRouterWatcher {

	private static final Logger logger = LoggerFactory.getLogger(McpRouterWatcher.class);

	private final McpServiceDiscovery mcpServiceDiscovery;

	private final McpServerVectorStore mcpServerVectorStore;

	private final List<String> serviceNames;

	public McpRouterWatcher(McpServiceDiscovery mcpServiceDiscovery, McpServerVectorStore mcpServerVectorStore,
			List<String> serviceNames) {
		this.serviceNames = serviceNames;
		this.mcpServiceDiscovery = mcpServiceDiscovery;
		this.mcpServerVectorStore = mcpServerVectorStore;
	}

	@Override
	protected void handleChange() {
		logger.debug("McpRouterWatcher polling...");
		if (serviceNames == null || serviceNames.isEmpty()) {
			logger.warn("No MCP services configured for refresh.");
			return;
		}
		for (String serviceName : serviceNames) {
			try {
				mcpServerVectorStore.removeServer(serviceName);
				// 从服务发现获取服务信息
				McpServerInfo serverInfo = mcpServiceDiscovery.getService(serviceName);
				if (serverInfo == null) {
					logger.warn("No MCP service found for: {}", serviceName);
					return;
				}

				// 添加到向量存储
				mcpServerVectorStore.addServer(serverInfo);
				logger.info("Refreshed MCP service: {}", serviceName);
			}
			catch (Exception e) {
				logger.warn("Failed to refresh MCP service: {}", serviceName, e);
			}
		}
	}

}
