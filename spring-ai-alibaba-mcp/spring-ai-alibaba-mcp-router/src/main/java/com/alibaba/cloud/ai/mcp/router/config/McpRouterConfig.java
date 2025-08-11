/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.config;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.router.core.McpRouterWatcher;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.McpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.SimpleMcpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.nacos.NacosMcpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.service.McpRouterManagementService;
import com.alibaba.cloud.ai.mcp.router.service.McpRouterService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Properties;

/**
 * MCP Router 配置类 提供核心组件的配置和 Bean 定义
 */
@Configuration
@EnableConfigurationProperties({ NacosMcpProperties.class, McpRouterProperties.class })
@EnableScheduling
public class McpRouterConfig {

	//
	// /**
	// * 配置 RestClient
	// */
	// @Bean
	// @ConditionalOnMissingBean
	// public RestClient restClient() {
	// return RestClient.builder()
	// .defaultHeader("Accept", "application/json")
	// .defaultHeader("Content-Type", "application/json")
	// .build();
	// }

	/**
	 * 配置 MCP 服务发现
	 */
	@Bean
	@ConditionalOnMissingBean
	public McpServiceDiscovery mcpServiceDiscovery(NacosMcpOperationService nacosMcpOperationService) {
		return new NacosMcpServiceDiscovery(nacosMcpOperationService);
	}

	/**
	 * 配置 MCP Server 向量存储
	 */
	@Bean
	@ConditionalOnMissingBean
	public McpServerVectorStore mcpServerVectorStore(EmbeddingModel embeddingModel) {
		return new SimpleMcpServerVectorStore(embeddingModel);
	}

	// 配置可以在这里添加
	@Bean
	public NacosMcpOperationService nacosMcpOperationService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return new NacosMcpOperationService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public ToolCallbackProvider routerTools(McpRouterService routerService) {
		return MethodToolCallbackProvider.builder().toolObjects(routerService).build();
	}

	@Bean(initMethod = "startScheduledPolling", destroyMethod = "stop")
	public McpRouterWatcher mcpRouterWatcher(McpRouterManagementService managementService,
			McpRouterProperties mcpRouterProperties) {
		return new McpRouterWatcher(managementService, mcpRouterProperties.getServiceNames());
	}

}
