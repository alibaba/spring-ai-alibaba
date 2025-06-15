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

package com.alibaba.cloud.ai.example.deepresearch.tool;

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.alibaba.cloud.ai.example.deepresearch.agents.McpJsonAutoConfiguration;
import org.apache.commons.compress.utils.Lists;
import org.glassfish.jersey.internal.guava.Sets;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 *
 * Just support mcp-client autoconfigure for spring-ai.
 *
 * @author Allen Hu
 * @since 2025/6/14
 */
@Service
public class McpClientToolCallbackProvider {

	private final ToolCallbackProvider yamlBasedToolCallbackProvider;

	private final McpClientCommonProperties commonProperties;

	private final DeepResearchProperties deepResearchProperties;

	@Autowired(required = false)
	private McpJsonAutoConfiguration.JsonBasedMcpToolCallbackProvider jsonBasedMcpToolCallbackProvider;

	private final ApplicationContext applicationContext;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(McpClientToolCallbackProvider.class);

	public McpClientToolCallbackProvider(@Qualifier("mcpAsyncToolCallbacks") ToolCallbackProvider yamlBasedToolCallbackProvider,
			McpClientCommonProperties commonProperties, 
			DeepResearchProperties deepResearchProperties,
			ApplicationContext applicationContext) {
		this.yamlBasedToolCallbackProvider = yamlBasedToolCallbackProvider;
		this.commonProperties = commonProperties;
		this.deepResearchProperties = deepResearchProperties;
		this.applicationContext = applicationContext;
	}

	/**
	 * Find ToolCallback by agentName
	 * @param agentName Agent name
	 * @return ToolCallback
	 */

	//这边添加的就是一些调试信息，方便观察和排查问题，支持两种方式获取工具回调：
	public Set<ToolCallback> findToolCallbacks(String agentName) {
		Set<ToolCallback> defineCallback = Sets.newHashSet();
		
		logger.info("开始为代理 {} 查找工具回调", agentName);
		
		// 优先使用JSON配置的MCP工具回调
		if (jsonBasedMcpToolCallbackProvider != null) {
			try {
				ToolCallback[] agentCallbacks = jsonBasedMcpToolCallbackProvider.getToolCallbacksForAgent(agentName);
				if (agentCallbacks != null && agentCallbacks.length > 0) {
					defineCallback.addAll(List.of(agentCallbacks));
					logger.info("从JSON配置获取到 {} 个工具回调，代理: {}", agentCallbacks.length, agentName);
					return defineCallback;
				}
			} catch (Exception e) {
				// 如果获取失败，回退到原有逻辑
				logger.warn("无法从JSON配置获取代理特定的工具回调，回退到YAML配置方式", e);
			}
		} else {
			logger.warn("jsonBasedMcpToolCallbackProvider 为 null，无法使用JSON配置");
		}

		// 回退到原有的YAML配置方式
		Set<String> mcpClients = deepResearchProperties.getMcpClientMapping().get(agentName);
		if (mcpClients == null || mcpClients.isEmpty()) {
			logger.debug("代理 {} 没有配置MCP客户端映射", agentName);
			return defineCallback;
		}

		List<String> exceptMcpClientNames = Lists.newArrayList();
		for (String mcpClient : mcpClients) {
			// spring-ai-mcp-client
			String name = commonProperties.getName();
			// spring_ai_mcp_client_amap_maps
			String prefixedMcpClientName = McpToolUtils.prefixedToolName(name, mcpClient);
			exceptMcpClientNames.add(prefixedMcpClientName);
		}

		ToolCallback[] toolCallbacks = yamlBasedToolCallbackProvider.getToolCallbacks();
		for (ToolCallback toolCallback : toolCallbacks) {
			ToolDefinition toolDefinition = toolCallback.getToolDefinition();
			// spring_ai_mcp_client_amap_maps_maps_regeocode
			String name = toolDefinition.name();
			for (String exceptMcpClientName : exceptMcpClientNames) {
				if (name.startsWith(exceptMcpClientName)) {
					defineCallback.add(toolCallback);
				}
			}
		}
		
		logger.info("从YAML配置获取到 {} 个工具回调，代理: {}", defineCallback.size(), agentName);
		return defineCallback;
	}
}
