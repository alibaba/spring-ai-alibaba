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

package com.alibaba.cloud.ai.autoconfigure.mcp.client.config;

import com.alibaba.cloud.ai.autoconfigure.mcp.client.McpAsyncRecovery;
import com.alibaba.cloud.ai.autoconfigure.mcp.client.McpSyncRecovery;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;

/**
 * @author yingzi
 * @since 2025/7/14
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties({ McpSseClientProperties.class, McpClientCommonProperties.class,
		McpRecoveryAutoProperties.class })
@ConditionalOnProperty(prefix = McpRecoveryAutoProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
public class McpRecoveryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ThreadPoolTaskScheduler pingScheduler(ThreadPoolTaskScheduler pingScheduler) {
		return pingScheduler;
	}

	@Bean
	@ConditionalOnMissingBean
	public ExecutorService reconnectExecutor(ExecutorService reconnectExecutor) {
		return reconnectExecutor;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	public McpSyncRecovery mcpSyncRecovery(McpRecoveryAutoProperties mcpRecoveryAutoProperties,
			ThreadPoolTaskScheduler pingScheduler, ExecutorService reconnectExecutor,
			ApplicationContext applicationContext) {
		McpSyncRecovery mcpSyncRecovery = new McpSyncRecovery(mcpRecoveryAutoProperties, pingScheduler,
				reconnectExecutor, applicationContext);
		mcpSyncRecovery.init();
		mcpSyncRecovery.startScheduledPolling();
		mcpSyncRecovery.startReconnectTask();

		return mcpSyncRecovery;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public McpAsyncRecovery mcpAsyncRecovery(McpRecoveryAutoProperties mcpRecoveryAutoProperties,
			ThreadPoolTaskScheduler pingScheduler, ExecutorService reconnectExecutor,
			ApplicationContext applicationContext) {
		McpAsyncRecovery mcpAsyncRecovery = new McpAsyncRecovery(mcpRecoveryAutoProperties, pingScheduler,
				reconnectExecutor, applicationContext);
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
