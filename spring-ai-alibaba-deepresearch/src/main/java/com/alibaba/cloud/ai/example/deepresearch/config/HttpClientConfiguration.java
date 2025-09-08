/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP client configuration.
 * Provides Bean configuration for RestClient and other HTTP clients, supporting configurable parameters.
 *
 * @author hupei
 */
@Configuration
@EnableConfigurationProperties(HttpClientConfiguration.HttpClientProperties.class)
public class HttpClientConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientConfiguration.class);

	private final HttpClientProperties properties;

	public HttpClientConfiguration(HttpClientProperties properties) {
		this.properties = properties;
	}

	/**
	 * Configures RestClient Bean with configurable timeout and connection parameters.
	 */
	@Bean
	public RestClient restClient() {
		logger.info("Configuring RestClient with connect timeout: {}ms, read timeout: {}ms",
				properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());

		return RestClient.builder().requestFactory(clientHttpRequestFactory()).build();
	}

	/**
	 * Configure HTTP request factory using Apache HttpComponents for enhanced performance and configuration options.
	 */
	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setConnectTimeout(properties.getConnectTimeoutMs());
		factory.setReadTimeout(properties.getReadTimeoutMs());
		factory.setConnectionRequestTimeout(properties.getConnectionRequestTimeoutMs());

		logger.debug("HTTP client factory configured - Connect: {}ms, Read: {}ms, Request: {}ms",
				properties.getConnectTimeoutMs(), properties.getReadTimeoutMs(),
				properties.getConnectionRequestTimeoutMs());

		return factory;
	}

	/**
	 * RestClient.Builder Bean (For customizing RestClient in other components)
	 */
	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder().requestFactory(clientHttpRequestFactory());
	}

	/**
	 * HTTP client configuration properties
	 */
	@ConfigurationProperties(prefix = "spring.ai.alibaba.deepresearch.http-client")
	public static class HttpClientProperties {

		/**
		 * Connection timeout duration (in milliseconds)
		 */
		private int connectTimeoutMs = 30000;

		/**
		 * Read timeout duration (in milliseconds)
		 */
		private int readTimeoutMs = 60000;

		/**
		 * Connection request timeout duration (in milliseconds)
		 */
		private int connectionRequestTimeoutMs = 10000;

		/**
		 * Maximum number of connections
		 */
		private int maxConnections = 100;

		/**
		 * Maximum connections per route
		 */
		private int maxConnectionsPerRoute = 20;

		/**
		 * Connection keep-alive duration (in milliseconds)
		 */
		private int connectionTimeToLiveMs = 300000;

		/**
		 * 是否启用连接池
		 */
		private boolean poolingEnabled = true;

		/**
		 * Connection pool enabled
		 */
		private boolean retryEnabled = true;

		/**
		 * Whether to enable retry mechanism
		 */
		private int maxRetryAttempts = 3;

		// Getters and setters
		public int getConnectTimeoutMs() {
			return connectTimeoutMs;
		}

		public void setConnectTimeoutMs(int connectTimeoutMs) {
			this.connectTimeoutMs = connectTimeoutMs;
		}

		public int getReadTimeoutMs() {
			return readTimeoutMs;
		}

		public void setReadTimeoutMs(int readTimeoutMs) {
			this.readTimeoutMs = readTimeoutMs;
		}

		public int getConnectionRequestTimeoutMs() {
			return connectionRequestTimeoutMs;
		}

		public void setConnectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
			this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
		}

		public int getMaxConnections() {
			return maxConnections;
		}

		public void setMaxConnections(int maxConnections) {
			this.maxConnections = maxConnections;
		}

		public int getMaxConnectionsPerRoute() {
			return maxConnectionsPerRoute;
		}

		public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
			this.maxConnectionsPerRoute = maxConnectionsPerRoute;
		}

		public int getConnectionTimeToLiveMs() {
			return connectionTimeToLiveMs;
		}

		public void setConnectionTimeToLiveMs(int connectionTimeToLiveMs) {
			this.connectionTimeToLiveMs = connectionTimeToLiveMs;
		}

		public boolean isPoolingEnabled() {
			return poolingEnabled;
		}

		public void setPoolingEnabled(boolean poolingEnabled) {
			this.poolingEnabled = poolingEnabled;
		}

		public boolean isRetryEnabled() {
			return retryEnabled;
		}

		public void setRetryEnabled(boolean retryEnabled) {
			this.retryEnabled = retryEnabled;
		}

		public int getMaxRetryAttempts() {
			return maxRetryAttempts;
		}

		public void setMaxRetryAttempts(int maxRetryAttempts) {
			this.maxRetryAttempts = maxRetryAttempts;
		}

	}

}
