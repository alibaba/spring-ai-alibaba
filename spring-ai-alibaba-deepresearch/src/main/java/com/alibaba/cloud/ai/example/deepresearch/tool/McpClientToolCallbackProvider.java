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
import org.apache.commons.compress.utils.Lists;
import org.glassfish.jersey.internal.guava.Sets;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
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

	private final ToolCallbackProvider toolCallbackProvider;

	private final McpClientCommonProperties commonProperties;

	private final DeepResearchProperties deepResearchProperties;

	public McpClientToolCallbackProvider(ToolCallbackProvider toolCallbackProvider,
			McpClientCommonProperties commonProperties, DeepResearchProperties deepResearchProperties) {
		this.toolCallbackProvider = toolCallbackProvider;
		this.commonProperties = commonProperties;
		this.deepResearchProperties = deepResearchProperties;
	}

	/**
	 * Find ToolCallback by agentName
	 * @param agentName Agent name
	 * @return ToolCallback
	 */
	public Set<ToolCallback> findToolCallbacks(String agentName) {
		Set<ToolCallback> defineCallback = Sets.newHashSet();
		Set<String> mcpClients = deepResearchProperties.getMcpClientMapping().get(agentName);
		if (mcpClients == null || mcpClients.isEmpty()) {
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

		ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
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
		return defineCallback;
	}

}
