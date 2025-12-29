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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for WebClient connection pooling.
 *
 * @author GitHub Copilot
 * @since 1.1.0.0-SNAPSHOT
 */
@ConfigurationProperties(prefix = WebClientConfigProperties.CONFIG_PREFIX)
public class WebClientConfigProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.webclient";

	/**
	 * Enable WebClient connection pool configuration.
	 */
	private boolean enabled = true;

	/**
	 * Maximum number of connections per host. Default is 500.
	 */
	private int maxConnections = 500;

	/**
	 * Maximum time a connection can be idle before being evicted. Default is 30 seconds.
	 * This helps prevent "Connection reset" errors when the server closes idle connections.
	 */
	private Duration maxIdleTime = Duration.ofSeconds(30);

	/**
	 * Maximum time a connection can live. Default is 5 minutes.
	 */
	private Duration maxLifeTime = Duration.ofMinutes(5);

	/**
	 * Interval for evicting idle connections in background. Default is 10 seconds.
	 */
	private Duration evictionInterval = Duration.ofSeconds(10);

	/**
	 * Enable pending acquire queue. Default is true.
	 */
	private boolean pendingAcquireQueue = true;

	/**
	 * Maximum pending acquire requests. Default is 1000.
	 */
	private int maxPendingAcquires = 1000;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public Duration getMaxIdleTime() {
		return maxIdleTime;
	}

	public void setMaxIdleTime(Duration maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
	}

	public Duration getMaxLifeTime() {
		return maxLifeTime;
	}

	public void setMaxLifeTime(Duration maxLifeTime) {
		this.maxLifeTime = maxLifeTime;
	}

	public Duration getEvictionInterval() {
		return evictionInterval;
	}

	public void setEvictionInterval(Duration evictionInterval) {
		this.evictionInterval = evictionInterval;
	}

	public boolean isPendingAcquireQueue() {
		return pendingAcquireQueue;
	}

	public void setPendingAcquireQueue(boolean pendingAcquireQueue) {
		this.pendingAcquireQueue = pendingAcquireQueue;
	}

	public int getMaxPendingAcquires() {
		return maxPendingAcquires;
	}

	public void setMaxPendingAcquires(int maxPendingAcquires) {
		this.maxPendingAcquires = maxPendingAcquires;
	}

}
