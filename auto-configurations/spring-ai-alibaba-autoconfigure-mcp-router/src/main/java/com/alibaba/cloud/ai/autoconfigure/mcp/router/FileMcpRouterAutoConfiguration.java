// FileMcpRouterAutoConfiguration.java
package com.alibaba.cloud.ai.autoconfigure.mcp.router;

import com.alibaba.cloud.ai.mcp.router.config.McpRouterProperties;
import com.alibaba.cloud.ai.mcp.router.core.discovery.FileConfigMcpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpRouterProperties.class)
@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileMcpRouterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FileMcpRouterAutoConfiguration.class);

    @Value("${spring.ai.dashscope.api-key:default_api_key}")
    private String apiKey;

    @Bean
    @ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "discovery-type", havingValue = "file")
    public McpServiceDiscovery fileConfigMcpServiceDiscovery(McpRouterProperties properties) {
        return new FileConfigMcpServiceDiscovery(properties);
    }
}
