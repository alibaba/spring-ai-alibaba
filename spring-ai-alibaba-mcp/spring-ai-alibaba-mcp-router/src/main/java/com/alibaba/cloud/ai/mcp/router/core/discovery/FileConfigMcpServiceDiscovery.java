package com.alibaba.cloud.ai.mcp.router.core.discovery;

import com.alibaba.cloud.ai.mcp.router.config.McpRouterProperties;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.springframework.stereotype.Component;

// FileConfigMcpServiceDiscovery.java
@Component
public class FileConfigMcpServiceDiscovery implements McpServiceDiscovery {

	private final McpRouterProperties properties;

	public FileConfigMcpServiceDiscovery(McpRouterProperties properties) {
		this.properties = properties;
	}

	@Override
	public McpServerInfo getService(String serviceName) {
		return properties.getServices()
			.stream()
			.filter(config -> config.getName().equals(serviceName))
			.findFirst()
			.orElse(null);
	}

}
