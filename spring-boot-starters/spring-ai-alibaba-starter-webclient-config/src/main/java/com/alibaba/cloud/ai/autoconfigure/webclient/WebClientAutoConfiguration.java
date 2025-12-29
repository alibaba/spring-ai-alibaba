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
package com.alibaba.cloud.ai.autoconfigure.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration for WebClient with optimized connection pooling settings.
 *
 * This configuration provides a WebClient.Builder bean with a properly configured
 * connection pool that prevents "Connection reset" errors by:
 * <ul>
 *   <li>Evicting idle connections before they're closed by the server</li>
 *   <li>Setting maximum connection lifetime</li>
 *   <li>Periodically cleaning up stale connections</li>
 * </ul>
 *
 * @author GitHub Copilot
 * @since 1.1.0.0-SNAPSHOT
 */
@AutoConfiguration
@ConditionalOnClass({ WebClient.class, HttpClient.class, ConnectionProvider.class })
@EnableConfigurationProperties(WebClientConfigProperties.class)
@ConditionalOnProperty(prefix = WebClientConfigProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class WebClientAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(WebClientAutoConfiguration.class);

	/**
	 * Creates a WebClient.Builder bean with optimized connection pooling.
	 *
	 * This builder configures the connection pool to prevent stale connection issues
	 * by evicting connections that have been idle for too long.
	 *
	 * @param properties the WebClient configuration properties
	 * @return configured WebClient.Builder
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebClient.Builder webClientBuilder(WebClientConfigProperties properties) {
		log.info("Configuring WebClient with connection pool settings: " +
				"maxConnections={}, maxIdleTime={}, maxLifeTime={}, evictionInterval={}",
				properties.getMaxConnections(), properties.getMaxIdleTime(),
				properties.getMaxLifeTime(), properties.getEvictionInterval());

		// Create connection provider with eviction settings
		ConnectionProvider connectionProvider = ConnectionProvider.builder("spring-ai-connection-pool")
				.maxConnections(properties.getMaxConnections())
				.maxIdleTime(properties.getMaxIdleTime())
				.maxLifeTime(properties.getMaxLifeTime())
				.evictInBackground(properties.getEvictionInterval())
				.pendingAcquireMaxCount(properties.isPendingAcquireQueue() ? properties.getMaxPendingAcquires() : -1)
				.build();

		// Create HttpClient with the connection provider
		HttpClient httpClient = HttpClient.create(connectionProvider)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
				.responseTimeout(Duration.ofSeconds(60))
				.doOnConnected(conn ->
						conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
								.addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
				);

		// Create ReactorClientHttpConnector with the HttpClient
		ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

		// Return WebClient.Builder with the connector
		return WebClient.builder().clientConnector(connector);
	}

}
