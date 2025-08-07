package com.alibaba.cloud.ai.mcp.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = McpRouterProperties.CONFIG_PREFIX)
public class McpRouterProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.router";

	/**
	 * 是否启用 MCP 路由器
	 */
	private boolean enabled = true;

	/**
	 * MCP 路由器服务名称
	 */
	private List<String> serviceNames = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(final List<String> serviceNames) {
		this.serviceNames = serviceNames;
	}

}
