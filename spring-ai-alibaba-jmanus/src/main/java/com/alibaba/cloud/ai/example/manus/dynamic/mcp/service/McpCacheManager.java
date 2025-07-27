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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.config.McpProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.repository.McpConfigRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

/**
 * MCP缓存管理器
 */
@Component
public class McpCacheManager {

	private static final Logger logger = LoggerFactory.getLogger(McpCacheManager.class);

	private final McpConnectionFactory connectionFactory;

	private final McpConfigRepository mcpConfigRepository;

	private final McpProperties mcpProperties;

	private final LoadingCache<String, Map<String, McpServiceEntity>> toolCallbackMapCache;

	public McpCacheManager(McpConnectionFactory connectionFactory, McpConfigRepository mcpConfigRepository,
			McpProperties mcpProperties) {
		this.connectionFactory = connectionFactory;
		this.mcpConfigRepository = mcpConfigRepository;
		this.mcpProperties = mcpProperties;

		this.toolCallbackMapCache = buildCache();
	}

	/**
	 * 构建缓存
	 * @return 加载缓存
	 */
	private LoadingCache<String, Map<String, McpServiceEntity>> buildCache() {
		return CacheBuilder.newBuilder()
			.expireAfterAccess(mcpProperties.getCacheExpireAfterAccess().toMinutes(), TimeUnit.MINUTES)
			.removalListener((RemovalListener<String, Map<String, McpServiceEntity>>) notification -> {
				Map<String, McpServiceEntity> mcpServiceEntityMap = notification.getValue();
				if (mcpServiceEntityMap == null) {
					return;
				}
				for (McpServiceEntity mcpServiceEntity : mcpServiceEntityMap.values()) {
					try {
						mcpServiceEntity.getMcpAsyncClient().close();
					}
					catch (Throwable t) {
						logger.error("Failed to close MCP client", t);
					}
				}
			})
			.build(new CacheLoader<>() {
				@Override
				public Map<String, McpServiceEntity> load(String key) throws Exception {
					return loadMcpServices(mcpConfigRepository.findAll());
				}
			});
	}

	/**
	 * 加载MCP服务
	 * @param mcpConfigEntities MCP配置实体列表
	 * @return MCP服务实体映射
	 * @throws IOException 加载失败时抛出异常
	 */
	private Map<String, McpServiceEntity> loadMcpServices(List<McpConfigEntity> mcpConfigEntities) throws IOException {
		Map<String, McpServiceEntity> toolCallbackMap = new ConcurrentHashMap<>();

		if (mcpConfigEntities == null || mcpConfigEntities.isEmpty()) {
			logger.info("No MCP server configurations found");
			return toolCallbackMap;
		}

		logger.info("Loading {} MCP server configurations", mcpConfigEntities.size());

		for (McpConfigEntity mcpConfigEntity : mcpConfigEntities) {
			String serverName = mcpConfigEntity.getMcpServerName();

			try {
				McpServiceEntity mcpServiceEntity = connectionFactory.createConnection(mcpConfigEntity);

				if (mcpServiceEntity != null) {
					toolCallbackMap.put(serverName, mcpServiceEntity);
					logger.info("Successfully loaded MCP server: {} with type: {}", serverName,
							mcpConfigEntity.getConnectionType());
				}
				else {
					logger.warn("Failed to create MCP service entity for server: {}", serverName);
				}
			}
			catch (Exception e) {
				logger.error("Failed to load MCP server configuration for: {}, error: {}", serverName, e.getMessage(),
						e);
			}
		}

		logger.info("Successfully loaded {} out of {} MCP servers", toolCallbackMap.size(), mcpConfigEntities.size());
		return toolCallbackMap;
	}

	/**
	 * 获取或加载MCP服务
	 * @param planId 计划ID
	 * @return MCP服务实体映射
	 */
	public Map<String, McpServiceEntity> getOrLoadServices(String planId) {
		try {
			return toolCallbackMapCache.get(planId != null ? planId : "DEFAULT");
		}
		catch (Exception e) {
			logger.error("Failed to get or load MCP services for plan: {}", planId, e);
			return new ConcurrentHashMap<>();
		}
	}

	/**
	 * 获取MCP服务实体列表
	 * @param planId 计划ID
	 * @return MCP服务实体列表
	 */
	public List<McpServiceEntity> getServiceEntities(String planId) {
		try {
			return new ArrayList<>(getOrLoadServices(planId).values());
		}
		catch (Exception e) {
			logger.error("Failed to get MCP service entities for plan: {}", planId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 清除缓存
	 * @param planId 计划ID
	 */
	public void invalidateCache(String planId) {
		toolCallbackMapCache.invalidate(planId != null ? planId : "DEFAULT");
		logger.info("Invalidated cache for plan: {}", planId);
	}

	/**
	 * 清除所有缓存
	 */
	public void invalidateAllCache() {
		toolCallbackMapCache.invalidateAll();
		logger.info("Invalidated all MCP service caches");
	}

	/**
	 * 刷新缓存
	 * @param planId 计划ID
	 */
	public void refreshCache(String planId) {
		toolCallbackMapCache.refresh(planId != null ? planId : "DEFAULT");
		logger.info("Refreshed cache for plan: {}", planId);
	}

	/**
	 * 获取缓存统计信息
	 * @return 缓存统计信息
	 */
	public String getCacheStats() {
		return toolCallbackMapCache.stats().toString();
	}

}
