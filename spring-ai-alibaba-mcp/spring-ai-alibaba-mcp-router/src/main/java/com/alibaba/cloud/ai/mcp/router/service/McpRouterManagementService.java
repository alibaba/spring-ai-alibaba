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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Router 管理服务 参考 SimpleVectorStoreManagementService 实现模式
 */
@Service
public class McpRouterManagementService {

	private final McpServiceDiscovery mcpServiceDiscovery;

	private final McpServerVectorStore mcpServerVectorStore;

	@Autowired
	public McpRouterManagementService(McpServiceDiscovery mcpServiceDiscovery,
			McpServerVectorStore mcpServerVectorStore) {
		this.mcpServiceDiscovery = mcpServiceDiscovery;
		this.mcpServerVectorStore = mcpServerVectorStore;
	}

	/**
	 * 初始化 MCP 服务到向量库
	 * @param serviceNames 要初始化的服务名列表
	 * @return 是否成功
	 */
	public Boolean initializeServices(List<String> serviceNames) {
		try {
			// 先清空现有数据
			clearAllServices();

			// 批量添加服务
			for (String serviceName : serviceNames) {
				addService(serviceName);
			}

			return true;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to initialize MCP services: " + e.getMessage(), e);
		}
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
	 * 搜索 MCP 服务
	 * @param query 查询文本
	 * @param limit 返回数量限制
	 * @return 匹配的服务列表
	 */
	public List<McpServerInfo> searchServices(String query, int limit) {
		try {
			return mcpServerVectorStore.search(query, limit);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to search MCP services: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取所有 MCP 服务
	 * @return 服务列表
	 */
	public List<McpServerInfo> getAllServices() {
		return mcpServerVectorStore.getAllServers();
	}

	/**
	 * 获取指定服务
	 * @param serviceName 服务名
	 * @return 服务信息
	 */
	public McpServerInfo getService(String serviceName) {
		return mcpServerVectorStore.getServer(serviceName);
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

	/**
	 * 清空所有服务
	 */
	public void clearAllServices() {
		try {
			mcpServerVectorStore.clear();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to clear all services: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取服务统计信息
	 * @return 统计信息
	 */
	public Map<String, Object> getStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalServices", mcpServerVectorStore.size());
		stats.put("vectorStoreType", mcpServerVectorStore.getClass().getSimpleName());
		stats.put("discoveryType", mcpServiceDiscovery.getClass().getSimpleName());
		return stats;
	}

}
