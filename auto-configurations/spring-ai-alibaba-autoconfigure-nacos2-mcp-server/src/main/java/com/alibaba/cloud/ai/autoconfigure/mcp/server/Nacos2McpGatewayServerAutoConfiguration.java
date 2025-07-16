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

package com.alibaba.cloud.ai.autoconfigure.mcp.server;

import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.properties.NacosMcpGatewayProperties;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.provider.NacosMcpGatewayAsyncGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.provider.NacosMcpGatewaySyncGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.provider.NacosMcpGatewayToolsProvider;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.provider.NacosMcpGatewayToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.tools.NacosMcpGatewayToolsInitializer;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.utils.SpringBeanUtils;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.watcher.NacosMcpGatewayToolsWatcher;
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
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
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
@EnableConfigurationProperties({ NacosMcpProperties.class, NacosMcpGatewayProperties.class, McpServerProperties.class })
@AutoConfiguration(after = McpServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class Nacos2McpGatewayServerAutoConfiguration implements ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(Nacos2McpGatewayServerAutoConfiguration.class);

	@Resource
	private NacosMcpProperties nacosMcpProperties;

	@Resource
	private NacosMcpGatewayProperties nacosMcpGatewayProperties;

	@Override
	public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
		SpringBeanUtils.getInstance().setApplicationContext(applicationContext);
	}

	@Bean
	public ToolCallbackProvider callbackProvider(final NacosMcpGatewayToolsInitializer toolsInitializer) {
		return NacosMcpGatewayToolCallbackProvider.builder().toolCallbacks(toolsInitializer.initializeTools()).build();
	}

	@Bean
	public NacosMcpGatewayToolsInitializer nacosMcpGatewayToolsInitializer(final ConfigService configService,
			final WebClient webClient) {
		return new NacosMcpGatewayToolsInitializer(configService, webClient, nacosMcpProperties,
				nacosMcpGatewayProperties);
	}

	@Bean(destroyMethod = "stop")
	public NacosMcpGatewayToolsWatcher nacosInstanceWatcher(final NamingService namingService,
			final ConfigService configService, final NacosMcpGatewayToolsProvider nacosMcpGatewayToolsProvider,
			final WebClient webClient) {
		return new NacosMcpGatewayToolsWatcher(namingService, configService, nacosMcpProperties,
				nacosMcpGatewayProperties, nacosMcpGatewayToolsProvider, webClient);
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@ConditionalOnMissingBean(NacosMcpGatewayToolsProvider.class)
	public NacosMcpGatewayToolsProvider nacosMcpGatewayAsyncToolsProvider(final McpAsyncServer mcpAsyncServer) {
		return new NacosMcpGatewayAsyncGatewayToolsProvider(mcpAsyncServer);
	}

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@ConditionalOnMissingBean(NacosMcpGatewayToolsProvider.class)
	public NacosMcpGatewayToolsProvider nacosMcpGatewaySyncToolsProvider(final McpSyncServer mcpSyncServer) {
		return new NacosMcpGatewaySyncGatewayToolsProvider(mcpSyncServer);
	}

	@Bean
	public ConfigService configService() throws NacosException {
		return NacosFactory.createConfigService(nacosMcpProperties.getNacosProperties());
	}

	@Bean
	public NamingService namingService() throws NacosException {
		return NamingFactory.createNamingService(nacosMcpProperties.getNacosProperties());
	}

	@Bean
	public WebClient webClient() {
		// 配置连接池
		ConnectionProvider provider = ConnectionProvider.builder("http-pool")
			.maxConnections(nacosMcpGatewayProperties.getMaxConnections())
			.pendingAcquireTimeout(Duration.ofMillis(nacosMcpGatewayProperties.getAcquireTimeout()))
			.maxIdleTime(Duration.ofSeconds(nacosMcpGatewayProperties.getMaxIdleTime()))
			.maxLifeTime(Duration.ofSeconds(nacosMcpGatewayProperties.getMaxLifeTime()))
			.build();

		// 配置 HTTP 客户端
		HttpClient httpClient = HttpClient.create(provider)
			// TCP 连接超时
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nacosMcpGatewayProperties.getConnectionTimeout())
			// 响应超时
			.responseTimeout(Duration.ofMillis(nacosMcpGatewayProperties.getReadTimeout()))
			.doOnConnected(conn -> conn
				// 读取超时
				.addHandlerLast(
						new ReadTimeoutHandler(nacosMcpGatewayProperties.getReadTimeout(), TimeUnit.MILLISECONDS))
				// 写入超时
				.addHandlerLast(
						new WriteTimeoutHandler(nacosMcpGatewayProperties.getWriteTimeout(), TimeUnit.MILLISECONDS)));

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
