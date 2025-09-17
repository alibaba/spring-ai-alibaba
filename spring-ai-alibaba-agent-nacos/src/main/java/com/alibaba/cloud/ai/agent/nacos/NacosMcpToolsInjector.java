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

package com.alibaba.cloud.ai.agent.nacos;

import java.util.List;

import com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolsInitializer;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.mcp.gateway.nacos.properties.NacosMcpGatewayProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.ToolCallback;

public class NacosMcpToolsInjector {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpToolsInjector.class);

	public static List<ToolCallback> loadMcpTools(NacosOptions nacosOptions, String agentId) {
		McpServersVO mcpServersVO = getMcpServersVO(nacosOptions, agentId);
		if (mcpServersVO != null) {
			return convert(nacosOptions, mcpServersVO);
		}
		return null;
	}

	public static void registry(LlmNode llmNode, ToolNode toolNode, NacosOptions nacosOptions, String agentName) {

		try {
			nacosOptions.getNacosConfigService()
					.addListener("mcp-servers.json", "ai-agent-" + agentName, new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							McpServersVO mcpServersVO = JSON.parseObject(configInfo, McpServersVO.class);
							List<ToolCallback> toolCallbacks = convert(nacosOptions, mcpServersVO);
							if (toolCallbacks != null) {
								toolNode.setToolCallbacks(toolCallbacks);
								llmNode.setToolCallbacks(toolCallbacks);
							}

						}
					});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}

	}

	public static McpServersVO getMcpServersVO(NacosOptions nacosOptions, String agentName) {
		try {
			String config = nacosOptions.getNacosConfigService()
					.getConfig("mcp-servers.json", "ai-agent-" + agentName, 3000L);
			return JSON.parseObject(config, McpServersVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<ToolCallback> convert(NacosOptions nacosOptions, McpServersVO mcpServersVO) {

		NacosMcpGatewayProperties nacosMcpGatewayProperties = new NacosMcpGatewayProperties();
		nacosMcpGatewayProperties.setServiceNames(mcpServersVO.getMcpServers().stream()
				.map(McpServersVO.McpServerVO::getMcpServerName).toList());
		NacosMcpGatewayToolsInitializer nacosMcpGatewayToolsInitializer = new NacosMcpGatewayToolsInitializer(
				nacosOptions.mcpOperationService, nacosMcpGatewayProperties, mcpServersVO.getMcpServers());
		List<ToolCallback> toolCallbacks = nacosMcpGatewayToolsInitializer.initializeTools();

		return toolCallbacks;
	}

}
