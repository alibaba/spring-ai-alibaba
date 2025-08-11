/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.router.service;

import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.McpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.springframework.stereotype.Service;

/**
 * MCP Router 管理服务 参考 SimpleVectorStoreManagementService 实现模式
 */
@Service
public class McpRouterManagementService {

	private final McpServiceDiscovery mcpServiceDiscovery;

	private final McpServerVectorStore mcpServerVectorStore;

	public McpRouterManagementService(McpServiceDiscovery mcpServiceDiscovery,
			McpServerVectorStore mcpServerVectorStore) {
		this.mcpServiceDiscovery = mcpServiceDiscovery;
		this.mcpServerVectorStore = mcpServerVectorStore;
	}

	/**
	 * 添加单个 MCP 服务
	 * @param serviceName 服务名
	 * @return 是否成功
	 */
	public Boolean addService(String serviceName) {
		try {
			// 从服务发现获取服务信息
			McpServerInfo serverInfo = mcpServiceDiscovery.getService(serviceName);
			if (serverInfo == null) {
				return false;
			}

			// 添加到向量存储
			return mcpServerVectorStore.addServer(serverInfo);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to add MCP service: " + e.getMessage(), e);
		}
	}

	/**
	 * 移除 MCP 服务
	 * @param serviceName 服务名
	 * @return 是否成功
	 */
	public Boolean removeService(String serviceName) {
		try {
			return mcpServerVectorStore.removeServer(serviceName);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to remove MCP service: " + e.getMessage(), e);
		}
	}

	/**
	 * 刷新服务信息
	 * @param serviceName 服务名
	 * @return 是否成功
	 */
	public Boolean refreshService(String serviceName) {
		try {
			// 先移除旧数据
			removeService(serviceName);
			// 重新添加
			return addService(serviceName);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to refresh MCP service: " + e.getMessage(), e);
		}
	}

}
