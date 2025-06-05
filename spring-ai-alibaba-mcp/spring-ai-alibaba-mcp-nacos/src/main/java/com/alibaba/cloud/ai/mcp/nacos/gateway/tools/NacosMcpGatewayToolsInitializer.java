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

package com.alibaba.cloud.ai.mcp.nacos.gateway.tools;

import com.alibaba.cloud.ai.mcp.nacos.gateway.callback.DynamicNacosToolCallback;
import com.alibaba.cloud.ai.mcp.nacos.gateway.properties.NacosMcpGatewayProperties;
import com.alibaba.cloud.ai.mcp.nacos.gateway.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.utils.StringUtils;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NacosMcpGatewayToolsInitializer {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayToolsInitializer.class);

	private final NacosMcpOperationService nacosMcpOperationService;

	private final NacosMcpGatewayProperties nacosMcpGatewayProperties;

	public NacosMcpGatewayToolsInitializer(NacosMcpOperationService nacosMcpOperationService,
			NacosMcpGatewayProperties nacosMcpGatewayProperties) {
		this.nacosMcpOperationService = nacosMcpOperationService;
		this.nacosMcpGatewayProperties = nacosMcpGatewayProperties;
	}

	public List<ToolCallback> initializeTools() {
		List<String> serviceNames = nacosMcpGatewayProperties.getServiceNames();
		if (CollectionUtils.isEmpty(serviceNames)) {
			logger.warn("No service names configured, no tools will be initialized");
			return new ArrayList<>();
		}
		List<ToolCallback> allTools = new ArrayList<>();
		for (String serviceName : serviceNames) {
			try {
				McpServerDetailInfo mcpServerDetailInfo = nacosMcpOperationService.getServerDetail(serviceName);
				if (mcpServerDetailInfo == null) {
					logger.warn("No service detail info found for service: {}", serviceName);
					continue;
				}
				boolean isProtocolSupported = StringUtils.equals(mcpServerDetailInfo.getProtocol(), "http")
						|| StringUtils.equals(mcpServerDetailInfo.getProtocol(), "https");
				if (!isProtocolSupported) {
					logger.warn("Protocol {} is not supported, no tools will be initialized for service: {}",
							mcpServerDetailInfo.getProtocol(), serviceName);
					continue;
				}
				List<ToolCallback> tools = parseToolsFromMcpServerDetailInfo(mcpServerDetailInfo);
				if (CollectionUtils.isNotEmpty(tools)) {
					allTools.addAll(tools);
				}

			}
			catch (Exception e) {
				logger.error("Failed to initialize tools for service: {}", serviceName, e);
			}
		}
		logger.info("Initial dynamic tools loading completed from nacos - Found {} tools", allTools.size());
		return allTools;
	}

	private List<ToolCallback> parseToolsFromMcpServerDetailInfo(McpServerDetailInfo mcpServerDetailInfo) {
		try {
			McpToolSpecification toolSpecification = mcpServerDetailInfo.getToolSpec();
			String protocol = mcpServerDetailInfo.getProtocol();
			McpServerRemoteServiceConfig mcpServerRemoteServiceConfig = mcpServerDetailInfo.getRemoteServerConfig();
			List<ToolCallback> toolCallbacks = new ArrayList<>();
			if (toolSpecification != null) {
				List<McpTool> toolsList = toolSpecification.getTools();
				Map<String, McpToolMeta> toolsMeta = toolSpecification.getToolsMeta();
				if (toolsList == null || toolsMeta == null) {
					return new ArrayList<>();
				}
				for (McpTool tool : toolsList) {
					String toolName = tool.getName();
					String toolDescription = tool.getDescription();
					Map<String, Object> inputSchema = tool.getInputSchema();
					McpToolMeta metaInfo = toolsMeta.get(toolName);
					boolean enabled = metaInfo == null || metaInfo.isEnabled();
					if (!enabled) {
						logger.info("Tool {} is disabled by metaInfo, skipping.", toolName);
						continue;
					}
					NacosMcpGatewayToolDefinition toolDefinition = NacosMcpGatewayToolDefinition.builder()
						.name(mcpServerDetailInfo.getName() + "_tools_" + toolName)
						.description(toolDescription)
						.inputSchema(inputSchema)
						.protocol(protocol)
						.remoteServerConfig(mcpServerRemoteServiceConfig)
						.toolsMeta(metaInfo)
						.build();
					toolCallbacks.add(new DynamicNacosToolCallback(toolDefinition));
				}
			}
			return toolCallbacks;
		}
		catch (Exception e) {
			logger.warn("Failed to get or parse nacos mcp service tools info (mcpName {})",
					mcpServerDetailInfo.getName() + mcpServerDetailInfo.getVersionDetail().getVersion(), e);
		}
		return null;
	}

}
