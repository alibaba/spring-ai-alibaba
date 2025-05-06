package com.alibaba.cloud.ai.mcp.nacos.client;

import com.alibaba.cloud.ai.mcp.nacos.client.tool.LoadbalancedAsyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.nacos.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpAsyncClient;
import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpSyncClient;
import org.springframework.ai.mcp.client.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.util.Collection;
import java.util.List;

/**
 * @author yingzi
 * @date 2025/5/6:13:41
 */
@AutoConfiguration(
        after = {NacosMcpClientAutoConfiguration.class}
)
@EnableConfigurationProperties({McpClientCommonProperties.class})
@Conditional({McpToolCallbackAutoConfiguration.McpToolCallbackAutoconfigurationCondition.class})
public class NacosMcpToolCallbackAutoConfiguration {

    public NacosMcpToolCallbackAutoConfiguration() {
    }

    @Bean(name = "loadbalancedMcpToolCallbacks")
    @ConditionalOnProperty(
            prefix = "spring.ai.mcp.client",
            name = {"type"},
            havingValue = "SYNC",
            matchIfMissing = true
    )
    public ToolCallbackProvider loadbalancedMcpToolCallbacks(ObjectProvider<List<LoadbalancedMcpSyncClient>> loadbalancedMcpSyncClients) {
        List<LoadbalancedMcpSyncClient> mcpClients = loadbalancedMcpSyncClients.stream().flatMap(Collection::stream).toList();
        return new LoadbalancedSyncMcpToolCallbackProvider(mcpClients);
    }

    @Bean(name = "loadbalancedMcpAsyncToolCallbacks")
    @ConditionalOnProperty(
            prefix = "spring.ai.mcp.client",
            name = {"type"},
            havingValue = "ASYNC"
    )
    public ToolCallbackProvider loadbalancedMcpAsyncToolCallbacks(ObjectProvider<List<LoadbalancedMcpAsyncClient>> loadbalancedMcpAsyncClients) {
        List<LoadbalancedMcpAsyncClient> mcpClients = loadbalancedMcpAsyncClients.stream().flatMap(Collection::stream).toList();
        return new LoadbalancedAsyncMcpToolCallbackProvider(mcpClients);
    }

}
