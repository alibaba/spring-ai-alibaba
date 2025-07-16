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

import com.alibaba.cloud.ai.mcp.nacos2.client.tool.LoadbalancedAsyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.nacos2.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.nacos2.client.transport.LoadbalancedMcpAsyncClient;
import com.alibaba.cloud.ai.mcp.nacos2.client.transport.LoadbalancedMcpSyncClient;
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
 * @since 2025/5/6:13:41
 */
@AutoConfiguration(after = { Nacos2McpClientAutoConfiguration.class })
@EnableConfigurationProperties({ McpClientCommonProperties.class })
@Conditional({ McpToolCallbackAutoConfiguration.McpToolCallbackAutoConfigurationCondition.class })
public class Nacos2McpToolCallbackAutoConfiguration {

	public Nacos2McpToolCallbackAutoConfiguration() {
	}

	@Bean(name = "loadbalancedMcpSyncToolCallbacks")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "SYNC",
			matchIfMissing = true)
	public ToolCallbackProvider loadbalancedMcpToolCallbacks(
			ObjectProvider<List<LoadbalancedMcpSyncClient>> loadbalancedMcpSyncClients) {
		List<LoadbalancedMcpSyncClient> mcpClients = loadbalancedMcpSyncClients.stream()
			.flatMap(Collection::stream)
			.toList();
		return new LoadbalancedSyncMcpToolCallbackProvider(mcpClients);
	}

	@Bean(name = "loadbalancedMcpAsyncToolCallbacks")
	@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = { "type" }, havingValue = "ASYNC")
	public ToolCallbackProvider loadbalancedMcpAsyncToolCallbacks(
			ObjectProvider<List<LoadbalancedMcpAsyncClient>> loadbalancedMcpAsyncClients) {
		List<LoadbalancedMcpAsyncClient> mcpClients = loadbalancedMcpAsyncClients.stream()
			.flatMap(Collection::stream)
			.toList();
		return new LoadbalancedAsyncMcpToolCallbackProvider(mcpClients);
	}

}
