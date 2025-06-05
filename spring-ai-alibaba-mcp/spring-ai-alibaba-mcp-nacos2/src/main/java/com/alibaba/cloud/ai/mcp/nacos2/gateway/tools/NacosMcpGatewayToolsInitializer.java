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

package com.alibaba.cloud.ai.mcp.nacos2.gateway.tools;

import com.alibaba.cloud.ai.mcp.nacos2.gateway.callback.NacosMcpGatewayToolCallback;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.callback.NacosMcpGatewayToolCallbackV3;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.properties.NacosMcpGatewayProperties;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.definition.NacosMcpGatewayToolDefinitionV3;
import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NacosMcpGatewayToolsInitializer {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayToolsInitializer.class);

	private static final String TOOLS_CONFIG_SUFFIX = "-mcp-tools.json";

	private final ConfigService configService;

	private final WebClient webClient;

	private final NacosMcpProperties nacosMcpProperties;

	private final NacosMcpGatewayProperties nacosMcpGatewayProperties;

	public NacosMcpGatewayToolsInitializer(ConfigService configService, WebClient webClient,
			NacosMcpProperties nacosMcpProperties, NacosMcpGatewayProperties nacosMcpGatewayProperties) {
		this.configService = configService;
		this.webClient = webClient;
		this.nacosMcpProperties = nacosMcpProperties;
		this.nacosMcpGatewayProperties = nacosMcpGatewayProperties;
	}

	public List<ToolCallback> initializeTools() {
		String version = NacosHelper.fetchNacosVersion(webClient, nacosMcpProperties.getServerAddr());
		logger.info("Nacos server version: {}", version);
		if (version != null && NacosHelper.compareVersion(version, "3.0.0") >= 0) {
			logger.info("Nacos version >= 3.0.0, use new logic");
			return handleHighVersion();
		}
		return handleLowVersion();
	}

	private List<ToolCallback> handleHighVersion() {
		List<String> serviceNames = nacosMcpGatewayProperties.getServiceNames();
		if (CollectionUtils.isEmpty(serviceNames)) {
			logger.warn("No service names configured, no tools will be initialized");
			return new ArrayList<>();
		}

		List<ToolCallback> allTools = new ArrayList<>();
		for (String serviceName : serviceNames) {
			try {
				String url = NacosHelper.getServerUrl(nacosMcpProperties.getServerAddr());
				String mcpServerDetail = webClient.get()
					.uri(url + "/nacos/v3/admin/ai/mcp?mcpName=" + serviceName)
					.header("userName", nacosMcpProperties.getUsername())
					.header("password", nacosMcpProperties.getPassword())
					.retrieve()
					.bodyToMono(String.class)
					.block();

				if (mcpServerDetail != null) {
					List<ToolCallback> tools = parseMcpServerInfo(JacksonUtils.toObj(mcpServerDetail, Map.class));
					if (CollectionUtils.isNotEmpty(tools)) {
						allTools.addAll(tools);
					}
				}
			}
			catch (Exception e) {
				logger.error("Failed to initialize tools for service: {}", serviceName, e);
			}
		}
		logger.info("Initial tools loading completed (high version) - Found {} tools", allTools.size());
		return allTools;
	}

	/**
	 * 将 mcp server 的 tools转为 Toolcallback。
	 */
	@SuppressWarnings("unchecked")
	private List<ToolCallback> parseMcpServerInfo(Map<String, Object> mcpServerInfo) {
		Object mcpName = mcpServerInfo.get("name");
		try {
			if (mcpServerInfo.containsKey("data")) {
				Map<String, Object> data = (Map<String, Object>) mcpServerInfo.get("data");
				if (data != null && data.containsKey("toolSpec")) {
					// 解析工具信息
					Object toolSpec = data.get("toolSpec");
					Object remoteServerConfig = data.get("remoteServerConfig");
					Object localeServerConfig = data.get("localeServerConfig");
					String protocol = (String) data.get("protocol");
					if (toolSpec != null) {
						Map<String, Object> toolSpecMap = JacksonUtils.toObj(JacksonUtils.toJson(toolSpec), Map.class);
						List<Map<String, Object>> tools = (List<Map<String, Object>>) toolSpecMap.get("tools");
						Map<String, Object> toolsMeta = (Map<String, Object>) toolSpecMap.get("toolsMeta");
						List<ToolCallback> toolCallbacks = new ArrayList<>();
						for (Map<String, Object> tool : tools) {
							String toolName = (String) tool.get("name");
							String toolDescription = (String) tool.get("description");
							Object inputSchema = tool.get("inputSchema");
							Object metaInfo = toolsMeta.getOrDefault(toolName, new Object());

							// 判断 metaInfo.enabled 是否为 true
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

							ToolDefinition toolDefinition = NacosMcpGatewayToolDefinitionV3.builder()
								.name(toolName)
								.description(toolDescription)
								.inputSchema(inputSchema)
								.protocol(protocol)
								.remoteServerConfig(remoteServerConfig)
								.localServerConfig(localeServerConfig)
								.toolsMeta(metaInfo)
								.build();
							toolCallbacks.add(new NacosMcpGatewayToolCallbackV3(toolDefinition));
						}
						return toolCallbacks;
					}

				}
			}
		}
		catch (Exception e) {
			logger.warn("Failed to get or parse nacos mcp server info (mcpName {})", mcpName, e);
		}
		return null;
	}

	private List<ToolCallback> handleLowVersion() {
		List<ToolCallback> allTools = new ArrayList<>();
		String serviceGroup = nacosMcpGatewayProperties.getServiceGroup();
		List<String> serviceNames = nacosMcpGatewayProperties.getServiceNames();

		if (CollectionUtils.isEmpty(serviceNames)) {
			logger.warn("No service names configured, no tools will be initialized");
			return allTools;
		}

		for (String serviceName : serviceNames) {
			try {
				String toolConfig = configService.getConfig(serviceName + TOOLS_CONFIG_SUFFIX, serviceGroup, 5000);
				if (toolConfig != null) {
					NacosMcpGatewayToolsInfo toolsInfo = JacksonUtils.toObj(toolConfig, NacosMcpGatewayToolsInfo.class);
					List<NacosMcpGatewayToolDefinition> toolsInNacos = toolsInfo.getTools();
					if (!CollectionUtils.isEmpty(toolsInNacos)) {
						for (NacosMcpGatewayToolDefinition toolDefinition : toolsInNacos) {
							toolDefinition.setServiceName(serviceName);
							allTools.add(new NacosMcpGatewayToolCallback(toolDefinition));
						}
					}
				}
			}
			catch (Exception e) {
				logger.error("Failed to initialize tools for service: {}", serviceName, e);
			}
		}
		logger.info("Initial tools loading completed - Found {} tools", allTools.size());
		return allTools;
	}

}
