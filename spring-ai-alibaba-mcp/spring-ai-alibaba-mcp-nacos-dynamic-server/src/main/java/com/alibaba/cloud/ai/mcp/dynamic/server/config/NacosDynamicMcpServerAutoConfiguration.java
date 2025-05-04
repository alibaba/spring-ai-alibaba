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

package com.alibaba.cloud.ai.mcp.dynamic.server.config;

import com.alibaba.cloud.ai.mcp.dynamic.server.properties.McpDynamicServerProperties;
import com.alibaba.cloud.ai.mcp.dynamic.server.provider.DynamicMcpAsyncToolsProvider;
import com.alibaba.cloud.ai.mcp.dynamic.server.provider.DynamicMcpSyncToolsProvider;
import com.alibaba.cloud.ai.mcp.dynamic.server.provider.DynamicMcpToolsProvider;
import com.alibaba.cloud.ai.mcp.dynamic.server.provider.DynamicToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.dynamic.server.tools.DynamicToolsInitializer;
import com.alibaba.cloud.ai.mcp.dynamic.server.utils.SpringBeanUtils;
import com.alibaba.cloud.ai.mcp.dynamic.server.watcher.DynamicNacosToolsWatcher;
import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.mcp.server.McpServerProperties;
import org.springframework.ai.autoconfigure.mcp.server.MpcServerAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author aias00
 */
@EnableConfigurationProperties({ McpDynamicServerProperties.class, NacosMcpRegistryProperties.class,
		McpServerProperties.class })
@AutoConfiguration(after = MpcServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class NacosDynamicMcpServerAutoConfiguration implements ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(NacosDynamicMcpServerAutoConfiguration.class);

	@Resource
	private McpDynamicServerProperties mcpDynamicServerProperties;

	@Resource
	private NacosMcpRegistryProperties nacosMcpRegistryProperties;

	@Override
	public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
		SpringBeanUtils.getInstance().setApplicationContext(applicationContext);
	}

	@Bean
	public ToolCallbackProvider callbackProvider(final DynamicToolsInitializer toolsInitializer) {
		return DynamicToolCallbackProvider.builder().toolCallbacks(toolsInitializer.initializeTools()).build();
	}

	@Bean
	public DynamicToolsInitializer dynamicToolsInitializer(final NamingService namingService,
			final ConfigService configService) {
		return new DynamicToolsInitializer(namingService, configService, nacosMcpRegistryProperties);
	}

	@Bean(destroyMethod = "stop")
	public DynamicNacosToolsWatcher nacosInstanceWatcher(final NamingService namingService,
			final ConfigService configService, final DynamicMcpToolsProvider dynamicMcpToolsProvider) {
		return new DynamicNacosToolsWatcher(namingService, configService, nacosMcpRegistryProperties,
				dynamicMcpToolsProvider);
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@ConditionalOnMissingBean(DynamicMcpToolsProvider.class)
	public DynamicMcpToolsProvider dynamicMcpAsyncToolsProvider(final McpAsyncServer mcpAsyncServer) {
		return new DynamicMcpAsyncToolsProvider(mcpAsyncServer);
	}

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@ConditionalOnMissingBean(DynamicMcpToolsProvider.class)
	public DynamicMcpToolsProvider dynamicMcpSyncToolsProvider(final McpSyncServer mcpSyncServer) {
		return new DynamicMcpSyncToolsProvider(mcpSyncServer);
	}

	@Bean
	public ConfigService configService() throws NacosException {
		return NacosFactory.createConfigService(nacosMcpRegistryProperties.getNacosProperties());
	}

	@Bean
	public NamingService namingService() throws NacosException {
		return NamingFactory.createNamingService(nacosMcpRegistryProperties.getNacosProperties());
	}

	@Bean
	public WebClient webClient() {
		// 配置连接池
		ConnectionProvider provider = ConnectionProvider.builder("http-pool")
			.maxConnections(mcpDynamicServerProperties.getMaxConnections())
			.pendingAcquireTimeout(Duration.ofMillis(mcpDynamicServerProperties.getAcquireTimeout()))
			.maxIdleTime(Duration.ofSeconds(mcpDynamicServerProperties.getMaxIdleTime()))
			.maxLifeTime(Duration.ofSeconds(mcpDynamicServerProperties.getMaxLifeTime()))
			.build();

		// 配置 HTTP 客户端
		HttpClient httpClient = HttpClient.create(provider)
			// TCP 连接超时
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, mcpDynamicServerProperties.getConnectionTimeout())
			// 响应超时
			.responseTimeout(Duration.ofMillis(mcpDynamicServerProperties.getReadTimeout()))
			.doOnConnected(conn -> conn
				// 读取超时
				.addHandlerLast(
						new ReadTimeoutHandler(mcpDynamicServerProperties.getReadTimeout(), TimeUnit.MILLISECONDS))
				// 写入超时
				.addHandlerLast(
						new WriteTimeoutHandler(mcpDynamicServerProperties.getWriteTimeout(), TimeUnit.MILLISECONDS)));

		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.filter(logRequest())
			.filter(logResponse())
			.build();
	}

	private ExchangeFilterFunction logRequest() {
		Logger logger = LoggerFactory.getLogger(WebClient.class);
		return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
			logger.info("Request: {} {}", clientRequest.method(), clientRequest.url());
			clientRequest.headers()
				.forEach((name, values) -> values.forEach(value -> log.debug("Request Header: {}={}", name, value)));
			return Mono.just(clientRequest);
		});
	}

	private ExchangeFilterFunction logResponse() {
		Logger logger = LoggerFactory.getLogger(WebClient.class);
		return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
			logger.info("Response Status: {}", clientResponse.statusCode());
			clientResponse.headers()
				.asHttpHeaders()
				.forEach((name, values) -> values.forEach(value -> log.debug("Response Header: {}={}", name, value)));
			return Mono.just(clientResponse);
		});
	}

}
