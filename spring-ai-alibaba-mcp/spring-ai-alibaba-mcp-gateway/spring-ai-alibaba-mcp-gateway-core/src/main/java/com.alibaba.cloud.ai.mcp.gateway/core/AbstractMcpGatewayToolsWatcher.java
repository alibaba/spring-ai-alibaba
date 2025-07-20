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

package com.alibaba.cloud.ai.mcp.gateway.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MCP Gateway 工具监听器抽象基类 提供了通用的工具监听和更新功能
 */
public abstract class AbstractMcpGatewayToolsWatcher {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractMcpGatewayToolsWatcher.class);

	protected static final long DEFAULT_POLLING_INTERVAL = 30L;

	protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	protected final McpGatewayToolManager toolManager;

	protected final McpGatewayServiceRegistry serviceRegistry;

	protected final ConcurrentHashMap<String, McpServiceDetail> serviceDetailInfoCache = new ConcurrentHashMap<>();

	public AbstractMcpGatewayToolsWatcher(McpGatewayToolManager toolManager,
			McpGatewayServiceRegistry serviceRegistry) {
		// 验证参数
		if (toolManager == null) {
			throw new IllegalArgumentException("McpGatewayToolManager cannot be null");
		}
		if (serviceRegistry == null) {
			throw new IllegalArgumentException("McpGatewayServiceRegistry cannot be null");
		}

		this.toolManager = toolManager;
		this.serviceRegistry = serviceRegistry;

		// 启动定时任务
		this.startScheduledPolling();
	}

	/**
	 * 启动定时轮询
	 */
	protected void startScheduledPolling() {
		long pollingInterval = getPollingInterval();
		scheduler.scheduleAtFixedRate(this::watch, pollingInterval, pollingInterval, TimeUnit.SECONDS);
		logger.info("Started scheduled service polling with interval: {} seconds", pollingInterval);
	}

	/**
	 * 停止监听器
	 */
	public void stop() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
					if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
						logger.error("Scheduler did not terminate");
					}
				}
			}
			catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
				logger.error("Interrupted while waiting for scheduler to terminate", e);
			}
		}

		// 清理缓存
		serviceDetailInfoCache.clear();
		logger.info("Stopped scheduled service polling and cleared cache");
	}

	/**
	 * 监听服务变更
	 */
	protected void watch() {
		try {
			handleChange();
		}
		catch (Exception e) {
			logger.error("Error occurred during service watching", e);
		}
	}

	/**
	 * 处理服务变更
	 */
	protected void handleChange() {
		List<String> serviceNames = getServiceNames();
		if (serviceNames == null || serviceNames.isEmpty()) {
			logger.warn("No service names configured, no tools will be watched");
			return;
		}

		Set<String> currentServices = new java.util.HashSet<>(serviceNames);
		for (String serviceName : serviceNames) {
			try {
				updateServiceTools(serviceName);
			}
			catch (Exception e) {
				logger.error("Failed to update tools for service: {}", serviceName, e);
			}
		}
		cleanupStaleServices(currentServices);
	}

	/**
	 * 更新服务工具
	 * @param serviceName 服务名称
	 */
	protected void updateServiceTools(String serviceName) {
		try {
			McpServiceDetail mcpServiceDetail = serviceRegistry.getServiceDetail(serviceName);
			if (mcpServiceDetail == null) {
				logger.warn("No service detail info found for service: {}, do not update", serviceName);
				return;
			}

			// 验证必要字段
			if (mcpServiceDetail.getToolSpec() == null || mcpServiceDetail.getServiceRef() == null) {
				logger.warn("Service {} missing required configuration, skipping update", serviceName);
				return;
			}

			// 原子性更新缓存
			McpServiceDetail oldMcpServiceDetail = serviceDetailInfoCache.put(serviceName, mcpServiceDetail);
			Set<String> needToDeleteTools = new java.util.HashSet<>();
			Set<String> needToUpdateTools = new java.util.HashSet<>();
			compareToolsChange(oldMcpServiceDetail, mcpServiceDetail, needToDeleteTools, needToUpdateTools);

			logger.info("MCP service info (name {}): {}", serviceName, mcpServiceDetail);

			// 先删除需要删除的工具
			if (!needToDeleteTools.isEmpty()) {
				for (String toolName : needToDeleteTools) {
					try {
						toolManager.removeTool(mcpServiceDetail.getName() + "_tools_" + toolName);
						logger.info("Removed tool: {} for service: {}", toolName, serviceName);
					}
					catch (Exception e) {
						logger.error("Failed to remove tool: {} for service: {}", toolName, serviceName, e);
					}
				}
			}

			// 再添加需要更新的工具
			if (!needToUpdateTools.isEmpty()) {
				List<McpServiceDetail.McpTool> tools = mcpServiceDetail.getToolSpec().getTools();
				java.util.Map<String, McpServiceDetail.McpToolMeta> toolsMeta = mcpServiceDetail.getToolSpec()
					.getToolsMeta();
				if (tools == null || toolsMeta == null) {
					logger.warn("Service {} has null tools or toolsMeta, skipping update", serviceName);
					return;
				}
				for (McpServiceDetail.McpTool tool : tools) {
					if (!needToUpdateTools.contains(tool.getName())) {
						continue; // 跳过不需要更新的工具
					}
					String toolName = tool.getName();
					String toolDescription = tool.getDescription();
					java.util.Map<String, Object> inputSchema = tool.getInputSchema();
					McpServiceDetail.McpToolMeta metaInfo = toolsMeta.get(toolName);
					McpGatewayToolDefinition toolDefinition = createToolDefinition(mcpServiceDetail, tool, metaInfo);
					try {
						toolManager.addTool(toolDefinition);
						logger.info("Added/Updated tool: {} for service: {}", toolName, serviceName);
					}
					catch (Exception e) {
						logger.error("Failed to add/update tool: {} for service: {}", toolName, serviceName, e);
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to update tools for service: {}", serviceName, e);
		}
	}

	/**
	 * 清理过期服务
	 * @param currentServices 当前服务集合
	 */
	protected void cleanupStaleServices(Set<String> currentServices) {
		// 获取所有已缓存但不在当前服务列表中的服务
		Set<String> staleServices = new java.util.HashSet<>(serviceDetailInfoCache.keySet());
		staleServices.removeAll(currentServices);

		// 移除过期服务的所有工具
		for (String staleService : staleServices) {
			McpServiceDetail staleServiceDetail = serviceDetailInfoCache.get(staleService);
			McpServiceDetail.McpToolSpecification mcpToolSpec = staleServiceDetail.getToolSpec();
			if (mcpToolSpec != null) {
				List<McpServiceDetail.McpTool> toolsToRemove = mcpToolSpec.getTools();
				if (toolsToRemove == null) {
					continue;
				}
				for (McpServiceDetail.McpTool tool : toolsToRemove) {
					String toolName = tool.getName();
					try {
						logger.info("Removing tool: {} for stale service: {}", toolName, staleService);
						toolManager.removeTool(staleServiceDetail.getName() + "_tools_" + toolName);
					}
					catch (Exception e) {
						logger.error("Failed to remove tool: {} for service: {}", toolName, staleService, e);
					}
				}
			}
			serviceDetailInfoCache.remove(staleService);
		}
	}

	/**
	 * 比较工具变更
	 * @param oldServiceDetail 旧的服务详情
	 * @param newServiceDetail 新的服务详情
	 * @param needToDeleteTools 需要删除的工具
	 * @param needToUpdateTools 需要更新的工具
	 */
	protected abstract void compareToolsChange(McpServiceDetail oldServiceDetail, McpServiceDetail newServiceDetail,
			Set<String> needToDeleteTools, Set<String> needToUpdateTools);

	/**
	 * 创建工具定义
	 * @param serviceDetail 服务详情
	 * @param tool 工具信息
	 * @param metaInfo 工具元数据
	 * @return 工具定义
	 */
	protected abstract McpGatewayToolDefinition createToolDefinition(McpServiceDetail serviceDetail,
			McpServiceDetail.McpTool tool, McpServiceDetail.McpToolMeta metaInfo);

	/**
	 * 获取服务名称列表
	 * @return 服务名称列表
	 */
	protected abstract List<String> getServiceNames();

	/**
	 * 获取轮询间隔
	 * @return 轮询间隔（秒）
	 */
	protected long getPollingInterval() {
		return DEFAULT_POLLING_INTERVAL;
	}

}
