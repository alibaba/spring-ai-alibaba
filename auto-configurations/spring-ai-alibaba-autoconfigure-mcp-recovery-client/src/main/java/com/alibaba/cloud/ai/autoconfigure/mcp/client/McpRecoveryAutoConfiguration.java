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

package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import com.alibaba.cloud.ai.mcp.client.McpAsyncRecovery;
import com.alibaba.cloud.ai.mcp.client.McpSyncRecovery;
import com.alibaba.cloud.ai.mcp.client.config.McpRecoveryProperties;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpSyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpSseClientProperties;
import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author yingzi
 * @since 2025/7/14
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties({ McpSseClientProperties.class, McpClientCommonProperties.class,
		McpRecoveryProperties.class })
@ConditionalOnProperty(prefix = McpRecoveryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
public class McpRecoveryAutoConfiguration {

	@Bean(name = "mcpSyncRecovery")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	public McpSyncRecovery mcpSyncRecovery(McpRecoveryProperties mcpRecoveryProperties,
			McpSseClientProperties mcpSseClientProperties, McpClientCommonProperties mcpClientCommonProperties,
			McpSyncClientConfigurer mcpSyncClientConfigurer) {
		McpSyncRecovery mcpSyncRecovery = new McpSyncRecovery(mcpRecoveryProperties, mcpSseClientProperties,
				mcpClientCommonProperties, mcpSyncClientConfigurer);
		mcpSyncRecovery.init();
		mcpSyncRecovery.startScheduledPolling();
		mcpSyncRecovery.startReconnectTask();

		return mcpSyncRecovery;
	}

	@Bean(name = "mcpAsyncRecovery")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public McpAsyncRecovery mcpAsyncRecovery(McpRecoveryProperties mcpRecoveryProperties,
			McpSseClientProperties mcpSseClientProperties, McpClientCommonProperties mcpClientCommonProperties,
			McpAsyncClientConfigurer mcpAsyncClientConfigurer) {
		McpAsyncRecovery mcpAsyncRecovery = new McpAsyncRecovery(mcpRecoveryProperties, mcpSseClientProperties,
				mcpClientCommonProperties, mcpAsyncClientConfigurer);
		mcpAsyncRecovery.init();
		mcpAsyncRecovery.startScheduledPolling();
		mcpAsyncRecovery.startReconnectTask();

		return mcpAsyncRecovery;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	McpSyncClientConfigurer mcpSyncClientConfigurer(ObjectProvider<McpSyncClientCustomizer> customizerProvider) {
		return new McpSyncClientConfigurer(customizerProvider.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	McpAsyncClientConfigurer mcpAsyncClientConfigurer(ObjectProvider<McpAsyncClientCustomizer> customizerProvider) {
		return new McpAsyncClientConfigurer(customizerProvider.orderedStream().toList());
	}

}
