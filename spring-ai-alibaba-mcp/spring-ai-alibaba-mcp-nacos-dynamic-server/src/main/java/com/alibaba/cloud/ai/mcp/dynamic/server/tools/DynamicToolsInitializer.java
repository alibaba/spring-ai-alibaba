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

package com.alibaba.cloud.ai.mcp.dynamic.server.tools;

import com.alibaba.cloud.ai.mcp.dynamic.server.callback.DynamicNacosToolCallback;
import com.alibaba.cloud.ai.mcp.dynamic.server.callback.DynamicNacosToolCallbackV3;
import com.alibaba.cloud.ai.mcp.dynamic.server.definition.DynamicNacosToolDefinition;
import com.alibaba.cloud.ai.mcp.dynamic.server.definition.DynamicNacosToolDefinitionV3;
import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
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

public class DynamicToolsInitializer {

	private static final Logger logger = LoggerFactory.getLogger(DynamicToolsInitializer.class);

	private static final String TOOLS_CONFIG_SUFFIX = "-mcp-tools.json";

	private final NamingService namingService;

	private final ConfigService configService;

	private final WebClient webClient;

	private final NacosMcpRegistryProperties nacosMcpRegistryProperties;

	public DynamicToolsInitializer(NamingService namingService, ConfigService configService, WebClient webClient,
			NacosMcpRegistryProperties nacosMcpRegistryProperties) {
		this.namingService = namingService;
		this.configService = configService;
		this.webClient = webClient;
		this.nacosMcpRegistryProperties = nacosMcpRegistryProperties;
	}

	public List<ToolCallback> initializeTools() {
		String version = NacosHelper.fetchNacosVersion(webClient, nacosMcpRegistryProperties.getServerAddr());
		logger.info("Nacos server version: {}", version);
		if (version != null && NacosHelper.compareVersion(version, "3.0.0") >= 0) {
			logger.info("Nacos version >= 3.0.0, use new logic (not implemented yet)");
			return handleHighVersion();
		}
		return handleLowVersion();
	}

	private List<ToolCallback> handleHighVersion() {
		// 3.0.0及以上版本的新逻辑，分页获取所有 pageItems 并组装 ToolCallback
		List<Map<String, Object>> mcpServersAllPages = NacosHelper.fetchNacosMcpServersAllPages(webClient,
				nacosMcpRegistryProperties.getServerAddr(), nacosMcpRegistryProperties.getUsername(),
				nacosMcpRegistryProperties.getPassword());
		List<ToolCallback> allTools = new ArrayList<>();
		for (Map<String, Object> mcpServerInfo : mcpServersAllPages) {
			List<ToolCallback> tools = parseMcpServerInfo(mcpServerInfo, webClient,
					nacosMcpRegistryProperties.getServerAddr(), nacosMcpRegistryProperties.getUsername(),
					nacosMcpRegistryProperties.getPassword());
			if (CollectionUtils.isNotEmpty(tools)) {
				allTools.addAll(tools);
			}
		}
		logger.info("Initial tools loading completed (high version) - Found {} tools", allTools.size());
		return allTools;
	}

	/**
	 * 将 mcp server 的 tools转为 Toolcallback。
	 */
	@SuppressWarnings("unchecked")
	private List<ToolCallback> parseMcpServerInfo(Map<String, Object> mcpServerInfo, WebClient webClient,
			String serverAddr, String username, String password) {
		Object mcpName = mcpServerInfo.get("name");
		String url = NacosHelper.getServerUrl(serverAddr);
		String mcpServerDetail = null;
		try {
			mcpServerDetail = webClient.get()
				.uri(url + "/nacos/v3/admin/ai/mcp?mcpName=" + mcpName)
				.header("userName", username)
				.header("password", password)
				.retrieve()
				.bodyToMono(String.class)
				.block();
			logger.info("Nacos mcp server info (name {}): {}", mcpName, mcpServerDetail);
			Map<String, Object> serverInfoMap = JacksonUtils.toObj(mcpServerDetail, Map.class);
			if (serverInfoMap != null && serverInfoMap.containsKey("data")) {
				Map<String, Object> data = (Map<String, Object>) serverInfoMap.get("data");
				if (data != null && data.containsKey("toolSpec")) {
					// 解析工具信息
					Object toolSpec = data.get("toolSpec");
					Object remoteServerConfig = data.get("remoteServerConfig");
					Object localeServerConfig = data.get("localeServerConfig");
					String protocol = (String) data.get("protocol");
					if (toolSpec != null) {
						Map<String, Object> toolSpecMap = JacksonUtils.toObj(JacksonUtils.toJson(toolSpec), Map.class);
						List<Map<String, Object>> tools = (List<Map<String, Object>>) toolSpecMap.get("tools");
						List<ToolCallback> toolCallbacks = new ArrayList<>();
						for (Map<String, Object> tool : tools) {
							String toolName = (String) tool.get("name");
							String toolDescription = (String) tool.get("description");
							Object inputSchema = tool.get("inputSchema");
							ToolDefinition toolDefinition = DynamicNacosToolDefinitionV3.builder()
								.name(toolName)
								.description(toolDescription)
								.inputSchema(inputSchema)
								.protocol(protocol)
								.remoteServerConfig(remoteServerConfig)
								.localServerConfig(localeServerConfig)
								.build();
							toolCallbacks.add(new DynamicNacosToolCallbackV3(toolDefinition));
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
		String serviceGroup = nacosMcpRegistryProperties.getServiceGroup();
		try {
			List<String> allServices = NacosHelper.listAllServices(namingService, serviceGroup);
			for (String serviceName : allServices) {
				try {
					String toolConfig = configService.getConfig(serviceName + TOOLS_CONFIG_SUFFIX, serviceGroup, 5000);
					if (toolConfig != null) {
						DynamicNacosToolsInfo toolsInfo = JacksonUtils.toObj(toolConfig, DynamicNacosToolsInfo.class);
						List<DynamicNacosToolDefinition> toolsInNacos = toolsInfo.getTools();
						if (!CollectionUtils.isEmpty(toolsInNacos)) {
							for (DynamicNacosToolDefinition toolDefinition : toolsInNacos) {
								toolDefinition.setServiceName(serviceName);
								allTools.add(new DynamicNacosToolCallback(toolDefinition));
							}
						}
					}
				}
				catch (Exception e) {
					logger.error("Failed to initialize tools for service: {}", serviceName, e);
				}
			}
			logger.info("Initial tools loading completed - Found {} tools", allTools.size());
		}
		catch (Exception e) {
			logger.error("Failed to initialize tools", e);
		}
		return allTools;
	}

}
