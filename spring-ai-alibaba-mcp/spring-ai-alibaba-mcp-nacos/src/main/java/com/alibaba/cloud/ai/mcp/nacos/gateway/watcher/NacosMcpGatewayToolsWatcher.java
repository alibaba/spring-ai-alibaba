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

package com.alibaba.cloud.ai.mcp.nacos.gateway.watcher;

import com.alibaba.cloud.ai.mcp.nacos.gateway.properties.NacosMcpGatewayProperties;
import com.alibaba.cloud.ai.mcp.nacos.gateway.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos.gateway.provider.NacosMcpGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NacosMcpGatewayToolsWatcher {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayToolsWatcher.class);

	private static final long POLLING_INTERVAL = 30L;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private final NacosMcpGatewayProperties nacosMcpGatewayProperties;

	private final NacosMcpOperationService nacosMcpOperationService;

	private final NacosMcpGatewayToolsProvider nacosMcpGatewayToolsProvider;

	private final Map<String, McpServerDetailInfo> serviceDetailInfoCache = new ConcurrentHashMap<>();

	public NacosMcpGatewayToolsWatcher(final NacosMcpGatewayProperties nacosMcpGatewayProperties,
			final NacosMcpOperationService nacosMcpOperationService,
			final NacosMcpGatewayToolsProvider nacosMcpGatewayToolsProvider) {
		this.nacosMcpGatewayProperties = nacosMcpGatewayProperties;
		this.nacosMcpOperationService = nacosMcpOperationService;
		this.nacosMcpGatewayToolsProvider = nacosMcpGatewayToolsProvider;
		// 启动定时任务
		this.startScheduledPolling();
	}

	private void startScheduledPolling() {
		scheduler.scheduleAtFixedRate(this::watch, POLLING_INTERVAL, POLLING_INTERVAL, TimeUnit.SECONDS);
		logger.info("Started scheduled service polling with interval: {} seconds", POLLING_INTERVAL);
	}

	public void stop() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		}
		catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
		logger.info("Stopped scheduled service polling");
	}

	private void watch() {
		handleChange();
	}

	private void cleanupStaleServices(Set<String> currentServices) {
		// 获取所有已缓存但不在当前服务列表中的服务
		Set<String> staleServices = new HashSet<>(serviceDetailInfoCache.keySet());
		staleServices.removeAll(currentServices);

		// 移除过期服务的所有工具
		for (String staleService : staleServices) {
			McpServerDetailInfo staleServerDetail = serviceDetailInfoCache.get(staleService);
			McpToolSpecification mcpToolSpec = staleServerDetail.getToolSpec();
			if (mcpToolSpec != null) {
				List<McpTool> toolsToRemove = mcpToolSpec.getTools();
				if (toolsToRemove == null) {
					continue;
				}
				for (McpTool tool : toolsToRemove) {
					String toolName = tool.getName();
					try {
						logger.info("Removing tool: {} for stale service: {}", toolName, staleService);
						nacosMcpGatewayToolsProvider.removeTool(staleServerDetail.getName() + "_tools_" + toolName);
					}
					catch (Exception e) {
						logger.error("Failed to remove tool: {} for service: {}", toolName, staleService, e);
					}
					nacosMcpGatewayToolsProvider.removeTool(staleServerDetail.getName() + "_tools_" + tool.getName());
				}
			}
			serviceDetailInfoCache.remove(staleService);
		}
	}

	private void handleChange() {
		List<String> serviceNames = nacosMcpGatewayProperties.getServiceNames();
		if (CollectionUtils.isEmpty(serviceNames)) {
			logger.warn("No service names configured, no tools will be watched");
			return;
		}
		Set<String> currentServices = new HashSet<>(serviceNames);
		for (String serviceName : serviceNames) {
			try {
				updateHighVersionServiceTools(serviceName);
			}
			catch (Exception e) {
				logger.error("Failed to update tools for service: {}", serviceName, e);
			}
		}
		cleanupStaleServices(currentServices);
	}

	private void compareToolsChange(McpServerDetailInfo oldMcpServerDetail, McpServerDetailInfo mcpServerDetail,
			Set<String> needToDeleteTools, Set<String> needToUpdateTools) {
		boolean isHaveOldTools = true;
		boolean isHaveNewTools = true;
		if (oldMcpServerDetail == null || oldMcpServerDetail.getToolSpec() == null
				|| oldMcpServerDetail.getToolSpec().getTools() == null
				|| oldMcpServerDetail.getToolSpec().getToolsMeta() == null) {
			isHaveOldTools = false;
		}
		if (mcpServerDetail == null || mcpServerDetail.getToolSpec() == null
				|| mcpServerDetail.getToolSpec().getTools() == null
				|| mcpServerDetail.getToolSpec().getToolsMeta() == null) {
			isHaveNewTools = false;
		}
		String oldProtocol = null;
		if (oldMcpServerDetail != null) {
			oldProtocol = oldMcpServerDetail.getProtocol();
		}
		if (!StringUtils.equals(oldProtocol, "http") && !StringUtils.equals(oldProtocol, "https")) {
			isHaveOldTools = false;
		}
		String newProtocol = mcpServerDetail.getProtocol();

		if (!StringUtils.equals(newProtocol, "http") && !StringUtils.equals(newProtocol, "https")) {
			isHaveNewTools = false;
		}

		if (!isHaveOldTools && isHaveNewTools) {
			List<McpTool> tools = mcpServerDetail.getToolSpec().getTools();
			Map<String, McpToolMeta> toolsMeta = mcpServerDetail.getToolSpec().getToolsMeta();
			for (McpTool tool : tools) {
				String toolName = tool.getName();
				if (toolsMeta != null && toolsMeta.get(toolName) != null && toolsMeta.get(toolName).isEnabled()) {
					needToUpdateTools.add(toolName);
				}
			}
			return;
		}
		if (isHaveOldTools && !isHaveNewTools) {
			List<McpTool> tools = oldMcpServerDetail.getToolSpec().getTools();
			Map<String, McpToolMeta> toolsMeta = oldMcpServerDetail.getToolSpec().getToolsMeta();
			for (McpTool tool : tools) {
				String toolName = tool.getName();
				if (toolsMeta != null && toolsMeta.get(toolName) != null && toolsMeta.get(toolName).isEnabled()) {
					needToDeleteTools.add(toolName);
				}
			}
			return;
		}
		if (!isHaveOldTools && !isHaveNewTools) {
			return;
		}

		List<McpTool> oldTools = oldMcpServerDetail.getToolSpec().getTools();
		List<McpTool> newTools = mcpServerDetail.getToolSpec().getTools();

		Map<String, McpToolMeta> oldToolsMeta = oldMcpServerDetail.getToolSpec().getToolsMeta();
		Map<String, McpToolMeta> newToolsMeta = mcpServerDetail.getToolSpec().getToolsMeta();

		Map<String, McpTool> oldAvailableToolMap = new HashMap<>();
		Map<String, McpTool> newAvailableToolMap = new HashMap<>();
		for (McpTool tool : oldTools) {
			if (oldToolsMeta != null && oldToolsMeta.get(tool.getName()) != null
					&& oldToolsMeta.get(tool.getName()).isEnabled()) {
				oldAvailableToolMap.put(tool.getName(), tool);
			}
		}
		for (McpTool tool : newTools) {
			if (newToolsMeta != null && newToolsMeta.get(tool.getName()) != null
					&& newToolsMeta.get(tool.getName()).isEnabled()) {
				newAvailableToolMap.put(tool.getName(), tool);
			}
		}

		String oldVersion = oldMcpServerDetail.getVersionDetail().getVersion();
		String newVersion = mcpServerDetail.getVersionDetail().getVersion();
		McpServiceRef oldServiceRef = oldMcpServerDetail.getRemoteServerConfig().getServiceRef();
		McpServiceRef newServiceRef = mcpServerDetail.getRemoteServerConfig().getServiceRef();
		boolean isSameService = StringUtils.equals(oldServiceRef.getServiceName(), newServiceRef.getServiceName())
				&& StringUtils.equals(oldServiceRef.getNamespaceId(), newServiceRef.getNamespaceId())
				&& StringUtils.equals(oldServiceRef.getGroupName(), newServiceRef.getGroupName());
		if (!StringUtils.equals(oldVersion, newVersion) || !isSameService
				|| !StringUtils.equals(oldProtocol, newProtocol)) {
			needToUpdateTools.addAll(newAvailableToolMap.keySet());
			needToDeleteTools.addAll(oldAvailableToolMap.keySet());
			needToDeleteTools.removeAll(needToUpdateTools);
			return;
		}

		for (String toolName : newAvailableToolMap.keySet()) {
			if (!oldAvailableToolMap.containsKey(toolName)) {
				needToUpdateTools.add(toolName);
				continue;
			}
			McpTool newTool = newAvailableToolMap.get(toolName);
			McpTool oldTool = oldAvailableToolMap.get(toolName);
			McpToolMeta newToolMeta = newToolsMeta.get(toolName);
			McpToolMeta oldToolMeta = oldToolsMeta.get(toolName);
			boolean isSameTool = StringUtils.equals(JacksonUtils.toJson(newTool), JacksonUtils.toJson(oldTool));
			boolean isSameMeta = StringUtils.equals(JacksonUtils.toJson(newToolMeta), JacksonUtils.toJson(oldToolMeta));
			if (!isSameTool || !isSameMeta) {
				needToUpdateTools.add(toolName);
			}
			oldAvailableToolMap.remove(toolName);
		}
		needToDeleteTools.addAll(oldAvailableToolMap.keySet());
	}

	private void updateHighVersionServiceTools(String mcpName) {
		try {
			McpServerDetailInfo mcpServerDetail = nacosMcpOperationService.getServerDetail(mcpName);
			if (mcpServerDetail == null) {
				logger.warn("No service detail info found for service: {},do not update", mcpName);
				return;
			}
			McpServerDetailInfo oldMcpServerDetail = serviceDetailInfoCache.get(mcpName);
			serviceDetailInfoCache.put(mcpName, mcpServerDetail);
			Set<String> needToDeleteTools = new HashSet<>();
			Set<String> needToUpdateTools = new HashSet<>();
			compareToolsChange(oldMcpServerDetail, mcpServerDetail, needToDeleteTools, needToUpdateTools);

			logger.info("Nacos mcp service info (name {}): {}", mcpName, mcpServerDetail);
			McpToolSpecification toolSpec = mcpServerDetail.getToolSpec();
			McpServerRemoteServiceConfig remoteServerConfig = mcpServerDetail.getRemoteServerConfig();
			String protocol = mcpServerDetail.getProtocol();

			if (!needToUpdateTools.isEmpty()) {
				List<McpTool> tools = toolSpec.getTools();
				Map<String, McpToolMeta> toolsMeta = toolSpec.getToolsMeta();
				for (McpTool tool : tools) {
					if (!needToUpdateTools.contains(tool.getName())) {
						return;
					}
					String toolName = tool.getName();
					String toolDescription = tool.getDescription();
					Map<String, Object> inputSchema = tool.getInputSchema();
					McpToolMeta metaInfo = toolsMeta.get(toolName);
					NacosMcpGatewayToolDefinition toolDefinition = NacosMcpGatewayToolDefinition.builder()
						.name(mcpServerDetail.getName() + "_tools_" + toolName)
						.description(toolDescription)
						.inputSchema(inputSchema)
						.protocol(protocol)
						.remoteServerConfig(remoteServerConfig)
						.toolsMeta(metaInfo)
						.build();
					nacosMcpGatewayToolsProvider.addTool(toolDefinition);
				}
			}
			if (!needToDeleteTools.isEmpty()) {
				for (String toolName : needToDeleteTools) {
					nacosMcpGatewayToolsProvider.removeTool(mcpServerDetail.getName() + "_tools_" + toolName);
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to update tools for high version service: {}", mcpName, e);
		}
	}

}
