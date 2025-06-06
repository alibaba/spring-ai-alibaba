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

package com.alibaba.cloud.ai.mcp.nacos2.gateway.watcher;

import com.alibaba.cloud.ai.mcp.nacos2.gateway.properties.NacosMcpGatewayProperties;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.definition.NacosMcpGatewayToolDefinitionV3;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.provider.NacosMcpGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.tools.NacosMcpGatewayToolsInfo;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.tools.NacosHelper;
import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.nacos.api.config.ConfigChangeEvent;
import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.config.listener.impl.AbstractConfigChangeListener;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NacosMcpGatewayToolsWatcher extends AbstractConfigChangeListener implements EventListener {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayToolsWatcher.class);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private static final long POLLING_INTERVAL = 30L; // 轮询间隔，单位秒

	private static final String toolsConfigSuffix = "-mcp-tools.json";

	private final NamingService namingService;

	private final ConfigService configService;

	private final NacosMcpProperties nacosMcpProperties;

	private final NacosMcpGatewayProperties nacosMcpGatewayProperties;

	private final NacosMcpGatewayToolsProvider nacosMcpGatewayToolsProvider;

	private final WebClient webClient;

	// 缓存服务名称和其工具的映射关系
	private final Map<String, Set<String>> serviceToolsCache = new ConcurrentHashMap<>();

	private volatile String nacosVersion;

	public NacosMcpGatewayToolsWatcher(final NamingService namingService, final ConfigService configService,
			final NacosMcpProperties nacosMcpProperties, final NacosMcpGatewayProperties nacosMcpGatewayProperties,
			final NacosMcpGatewayToolsProvider nacosMcpGatewayToolsProvider, final WebClient webClient) {
		this.namingService = namingService;
		this.configService = configService;
		this.nacosMcpProperties = nacosMcpProperties;
		this.nacosMcpGatewayProperties = nacosMcpGatewayProperties;
		this.nacosMcpGatewayToolsProvider = nacosMcpGatewayToolsProvider;
		this.webClient = webClient;
		this.nacosVersion = NacosHelper.fetchNacosVersion(webClient, nacosMcpProperties.getServerAddr());
		logger.info("Fetched nacos server version at startup: {}", nacosVersion);
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

	private String getNacosVersion() {
		if (nacosVersion == null) {
			nacosVersion = NacosHelper.fetchNacosVersion(webClient, nacosMcpProperties.getServerAddr());
			logger.info("Fetched nacos server version on demand: {}", nacosVersion);
		}
		return nacosVersion;
	}

	private void watch() {
		String version = getNacosVersion();
		logger.info("Nacos server version: {}", version);
		if (version != null && NacosHelper.compareVersion(version, "3.0.0") >= 0) {
			logger.info("Nacos version >= 3.0.0, using new logic");
			handleHighVersion();
			return;
		}

		List<String> serviceNames = nacosMcpGatewayProperties.getServiceNames();
		if (CollectionUtils.isEmpty(serviceNames)) {
			logger.warn("No service names configured, no tools will be watched");
			return;
		}

		Set<String> currentServices = new HashSet<>(serviceNames);
		for (String serviceName : serviceNames) {
			try {
				updateServiceTools(serviceName);
				namingService.subscribe(serviceName, nacosMcpGatewayProperties.getServiceGroup(), this);
				configService.addListener(serviceName, nacosMcpGatewayProperties.getServiceGroup(), this);
			}
			catch (NacosException e) {
				logger.error("Failed to subscribe to service: {}", serviceName, e);
			}
			catch (Exception e) {
				logger.error("Unexpected error during service subscription: {}", serviceName, e);
			}
		}
		cleanupStaleServices(currentServices);
	}

	private void cleanupStaleServices(Set<String> currentServices) {
		// 获取所有已缓存但不在当前服务列表中的服务
		Set<String> staleServices = new HashSet<>(serviceToolsCache.keySet());
		staleServices.removeAll(currentServices);

		// 移除过期服务的所有工具
		for (String staleService : staleServices) {
			Set<String> toolsToRemove = serviceToolsCache.get(staleService);
			if (toolsToRemove != null) {
				for (String toolName : toolsToRemove) {
					try {
						logger.info("Removing tool: {} for stale service: {}", toolName, staleService);
						nacosMcpGatewayToolsProvider.removeTool(toolName);
					}
					catch (Exception e) {
						logger.error("Failed to remove tool: {} for service: {}", toolName, staleService, e);
					}
				}
			}
			serviceToolsCache.remove(staleService);
		}
	}

	private void updateServiceTools(String serviceName) {
		try {
			String toolConfig = configService.getConfig(serviceName + toolsConfigSuffix,
					nacosMcpGatewayProperties.getServiceGroup(), 5000);

			// 获取该服务当前的实例列表
			List<Instance> instances = namingService.getAllInstances(serviceName,
					nacosMcpGatewayProperties.getServiceGroup());

			// 检查是否有健康且启用的实例
			boolean hasHealthyEnabledInstance = NacosHelper.hasHealthyEnabledInstance(instances);

			// 如果没有实例、没有健康且启用的实例或配置为空，移除所有相关工具
			if (CollectionUtils.isEmpty(instances) || !hasHealthyEnabledInstance || toolConfig == null) {
				logger.info("Service {} has no healthy and enabled instances or no tool config, removing all tools",
						serviceName);
				removeServiceTools(serviceName);
				return;
			}

			// 解析工具配置
			NacosMcpGatewayToolsInfo toolsInfo = JacksonUtils.toObj(toolConfig, NacosMcpGatewayToolsInfo.class);
			List<NacosMcpGatewayToolDefinition> toolsInNacos = toolsInfo.getTools();

			if (CollectionUtils.isEmpty(toolsInNacos)) {
				removeServiceTools(serviceName);
				return;
			}

			// 更新工具缓存
			Set<String> currentTools = new HashSet<>();
			for (NacosMcpGatewayToolDefinition toolDefinition : toolsInNacos) {
				currentTools.add(toolDefinition.name());
				toolDefinition.setServiceName(serviceName);
				nacosMcpGatewayToolsProvider.addTool(toolDefinition);
			}

			// 获取之前的工具集合
			Set<String> previousTools = serviceToolsCache.getOrDefault(serviceName, new HashSet<>());

			// 移除不再存在的工具
			Set<String> toolsToRemove = new HashSet<>(previousTools);
			toolsToRemove.removeAll(currentTools);
			for (String toolName : toolsToRemove) {
				logger.info("Removing obsolete tool: {} for service: {}", toolName, serviceName);
				nacosMcpGatewayToolsProvider.removeTool(toolName);
			}

			// 更新缓存
			serviceToolsCache.put(serviceName, currentTools);

		}
		catch (NacosException e) {
			logger.error("Failed to update tools for service: {}", serviceName, e);
		}
		catch (Exception e) {
			logger.error("Unexpected error while updating tools for service: {}", serviceName, e);
		}
	}

	private void removeServiceTools(String serviceName) {
		Set<String> tools = serviceToolsCache.remove(serviceName);
		if (tools != null) {
			for (String toolName : tools) {
				try {
					logger.info("Removing tool: {} for service: {}", toolName, serviceName);
					nacosMcpGatewayToolsProvider.removeTool(toolName);
				}
				catch (Exception e) {
					logger.error("Failed to remove tool: {} for service: {}", toolName, serviceName, e);
				}
			}
		}
	}

	@Override
	public void onEvent(Event event) {
		if (event instanceof NamingEvent namingEvent) {
			String serviceName = namingEvent.getServiceName();
			logger.info("Received service instance change event for service: {}", serviceName);
			updateServiceTools(serviceName);
		}
	}

	@Override
	public void receiveConfigChange(final ConfigChangeEvent event) {
		for (ConfigChangeItem item : event.getChangeItems()) {
			String dataId = item.getKey();
			if (dataId != null && dataId.endsWith(toolsConfigSuffix)) {
				String serviceName = dataId.substring(0, dataId.length() - toolsConfigSuffix.length());
				logger.info("Received config change event for service: {}", serviceName);
				updateServiceTools(serviceName);
			}
		}
	}

	private void handleHighVersion() {
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

	private void updateHighVersionServiceTools(String mcpName) {
		try {
			String url = NacosHelper.getServerUrl(nacosMcpProperties.getServerAddr());
			String mcpServerDetail = webClient.get()
				.uri(url + "/nacos/v3/admin/ai/mcp?mcpName=" + mcpName)
				.header("userName", nacosMcpProperties.getUsername())
				.header("password", nacosMcpProperties.getPassword())
				.retrieve()
				.bodyToMono(String.class)
				.block();

			logger.info("Nacos mcp server info (name {}): {}", mcpName, mcpServerDetail);
			Map<String, Object> serverInfoMap = JacksonUtils.toObj(mcpServerDetail, Map.class);
			if (serverInfoMap != null && serverInfoMap.containsKey("data")) {
				Map<String, Object> data = (Map<String, Object>) serverInfoMap.get("data");
				if (data != null && data.containsKey("toolSpec")) {
					Object toolSpec = data.get("toolSpec");
					Object remoteServerConfig = data.get("remoteServerConfig");
					Object localeServerConfig = data.get("localeServerConfig");
					String protocol = (String) data.get("protocol");

					if (toolSpec != null) {
						Map<String, Object> toolSpecMap = JacksonUtils.toObj(JacksonUtils.toJson(toolSpec), Map.class);
						List<Map<String, Object>> tools = (List<Map<String, Object>>) toolSpecMap.get("tools");
						Map<String, Object> toolsMeta = (Map<String, Object>) toolSpecMap.get("toolsMeta");

						// Update tools cache
						Set<String> currentTools = new HashSet<>();
						for (Map<String, Object> tool : tools) {
							String toolName = (String) tool.get("name");
							currentTools.add(toolName);

							// Check if tool is enabled
							Object metaInfo = toolsMeta.getOrDefault(toolName, new Object());
							boolean enabled = false;
							if (metaInfo instanceof Map) {
								Object enabledObj = ((Map<?, ?>) metaInfo).get("enabled");
								if (enabledObj instanceof Boolean) {
									enabled = (Boolean) enabledObj;
								}
								else if (enabledObj instanceof String) {
									enabled = Boolean.parseBoolean((String) enabledObj);
								}
							}

							if (!enabled) {
								logger.info("Tool {} is disabled by metaInfo, skipping.", toolName);
								continue;
							}

							// Create and add tool definition
							ToolDefinition toolDefinition = NacosMcpGatewayToolDefinitionV3.builder()
								.name(toolName)
								.description((String) tool.get("description"))
								.inputSchema(tool.get("inputSchema"))
								.protocol(protocol)
								.remoteServerConfig(remoteServerConfig)
								.localServerConfig(localeServerConfig)
								.toolsMeta(metaInfo)
								.build();

							nacosMcpGatewayToolsProvider.addTool(toolDefinition);
						}

						// Get previous tools for this service
						Set<String> previousTools = serviceToolsCache.getOrDefault(mcpName, new HashSet<>());

						// Remove obsolete tools
						Set<String> toolsToRemove = new HashSet<>(previousTools);
						toolsToRemove.removeAll(currentTools);
						for (String toolName : toolsToRemove) {
							logger.info("Removing obsolete tool: {} for service: {}", toolName, mcpName);
							nacosMcpGatewayToolsProvider.removeTool(toolName);
						}

						// Update cache
						serviceToolsCache.put(mcpName, currentTools);
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to update tools for high version service: {}", mcpName, e);
		}
	}

}
