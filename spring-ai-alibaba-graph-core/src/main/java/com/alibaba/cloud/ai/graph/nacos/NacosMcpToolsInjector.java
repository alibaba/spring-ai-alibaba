package com.alibaba.cloud.ai.graph.nacos;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;

import org.springframework.ai.tool.ToolCallback;

public class NacosMcpToolsInjector {

	public static List<ToolCallback> getTools(NacosConfigService nacosConfigService, String agentId) {
		try {
			String config = nacosConfigService.getConfig(String.format("agent-%s-mcp-servers.json", agentId), "nacos-ai-agent", 3000L);
			ToolsVO toolsVO = JSON.parseObject(config, ToolsVO.class);
			return null;
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}
}
