package com.alibaba.cloud.ai.mcp.nacos;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import org.springframework.ai.autoconfigure.mcp.server.McpServerProperties;
import org.springframework.ai.autoconfigure.mcp.server.MpcServerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author Sunrisea
 */
@EnableConfigurationProperties({ NacosMcpRegistryProperties.class, McpServerProperties.class })
@AutoConfiguration(after = MpcServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class NacosMcpRegistryAutoConfigration {

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	public NacosMcpRegister nacosMcpRegisterSync(McpSyncServer mcpSyncServer,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, ServerMcpTransport mcpServerTransport) {
		McpAsyncServer mcpAsyncServer = mcpSyncServer.getAsyncServer();
		if (mcpServerTransport instanceof StdioServerTransport) {
			return new NacosMcpRegister(mcpAsyncServer, nacosMcpRegistryProperties, "stdio");
		}
		else {
			return new NacosMcpRegister(mcpAsyncServer, nacosMcpRegistryProperties, "sse");
		}
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	public NacosMcpRegister nacosMcpRegisterAsync(McpAsyncServer mcpAsyncServer,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, ServerMcpTransport mcpServerTransport) {
		if (mcpServerTransport instanceof StdioServerTransport) {
			return new NacosMcpRegister(mcpAsyncServer, nacosMcpRegistryProperties, "stdio");
		}
		else {
			return new NacosMcpRegister(mcpAsyncServer, nacosMcpRegistryProperties, "sse");
		}
	}

}
