package com.alibaba.cloud.ai.agent.nacos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolsInitializer;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.mcp.gateway.nacos.properties.NacosMcpGatewayProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.commons.collections.CollectionUtils;
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

	public static void registry(LlmNode llmNode,ToolNode toolNode,NacosOptions nacosOptions, String agentId)  {

		try {
			nacosOptions.getNacosConfigService()
					.addListener(String.format("mcp-servers-%s.json", agentId), "nacos-ai-agent", new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							McpServersVO mcpServersVO = JSON.parseObject(configInfo, McpServersVO.class);
							List<ToolCallback> toolCallbacks = convert(nacosOptions, mcpServersVO);
							if (toolCallbacks!=null){
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

	public static McpServersVO getMcpServersVO(NacosOptions nacosOptions, String agentId) {
		try {
			String config = nacosOptions.getNacosConfigService()
					.getConfig(String.format("mcp-servers-%s.json", agentId), "nacos-ai-agent", 3000L);
			return JSON.parseObject(config, McpServersVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<ToolCallback> convert(NacosOptions nacosOptions, McpServersVO mcpServersVO) {

		NacosMcpGatewayProperties nacosMcpGatewayProperties=new NacosMcpGatewayProperties();
		nacosMcpGatewayProperties.setServiceNames(mcpServersVO.getMcpServers().stream().map(McpServersVO.McpServerVO::getMcpServerName).toList());
		NacosMcpGatewayToolsInitializer nacosMcpGatewayToolsInitializer = new NacosMcpGatewayToolsInitializer(
				nacosOptions.mcpOperationService, nacosMcpGatewayProperties);
		List<ToolCallback> toolCallbacks = nacosMcpGatewayToolsInitializer.initializeTools();

		return toolCallbacks;
	}

}
