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

package com.alibaba.cloud.ai.autoconfigure.mcp.router;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.router.config.McpRouterProperties;
import com.alibaba.cloud.ai.mcp.router.core.McpRouterWatcher;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.McpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.SimpleMcpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.nacos.NacosMcpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.service.McpProxyService;
import com.alibaba.cloud.ai.mcp.router.service.McpRouterService;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 * @author aias00
 */
@EnableConfigurationProperties({ McpRouterProperties.class, NacosMcpProperties.class, McpServerProperties.class })
@ConditionalOnProperty(prefix = McpRouterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class NacosMcpRouterAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(NacosMcpRouterAutoConfiguration.class);

	@Value("${spring.ai.dashscope.api-key:default_api_key}")
	private String apiKey;

	@Bean
	@ConditionalOnMissingBean
	public EmbeddingModel embeddingModel() {
		if (apiKey == null || apiKey.isEmpty() || "default_api_key".equals(apiKey)) {
			throw new IllegalArgumentException("Environment variable DASHSCOPE_API_KEY is not set.");
		}
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();

		return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
				DashScopeEmbeddingOptions.builder().withModel("text-embedding-v2").build());
	}

	@Bean
	@ConditionalOnMissingBean(NacosMcpOperationService.class)
	public NacosMcpOperationService nacosMcpOperationService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return new NacosMcpOperationService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

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

	/**
	 * 配置 MCP 代理服务
	 */
	@Bean
	@ConditionalOnMissingBean
	public McpProxyService mcpProxyService(NacosMcpOperationService nacosMcpOperationService) {
		return new McpProxyService(nacosMcpOperationService);
	}

	/**
	 * 配置 MCP 路由服务
	 */
	@Bean
	@ConditionalOnMissingBean
	public McpRouterService mcpRouterService(McpServiceDiscovery mcpServiceDiscovery,
			McpServerVectorStore mcpServerVectorStore, NacosMcpOperationService nacosMcpOperationService,
			McpProxyService mcpProxyService) {
		return new McpRouterService(mcpServiceDiscovery, mcpServerVectorStore, nacosMcpOperationService,
				mcpProxyService);
	}

	/**
	 * 配置 MCP 路由工具回调提供者
	 */
	@Bean
	public ToolCallbackProvider routerTools(McpRouterService routerService) {
		return MethodToolCallbackProvider.builder().toolObjects(routerService).build();
	}

	/**
	 * 配置 MCP 路由监视器
	 */
	@Bean(initMethod = "startScheduledPolling", destroyMethod = "stop")
	public McpRouterWatcher mcpRouterWatcher(McpServiceDiscovery mcpServiceDiscovery,
			McpServerVectorStore mcpServerVectorStore, McpRouterProperties mcpRouterProperties) {
		return new McpRouterWatcher(mcpServiceDiscovery, mcpServerVectorStore, mcpRouterProperties.getServiceNames());
	}

}
