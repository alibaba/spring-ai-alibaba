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
import com.alibaba.cloud.ai.mcp.dynamic.server.definition.DynamicNacosToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

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
		// TODO: 3.0.0及以上版本的新逻辑
		return new ArrayList<>();
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