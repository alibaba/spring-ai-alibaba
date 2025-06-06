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
package com.alibaba.cloud.ai.mcp.nacos2.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author aias00
 */
@ConfigurationProperties(prefix = NacosMcpGatewayProperties.CONFIG_PREFIX)
public class NacosMcpGatewayProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.nacos.gateway";

	String serviceNamespace;

	String serviceGroup = "DEFAULT_GROUP";

	List<String> serviceNames;

	private int maxConnections = 500;

	private int acquireTimeout = 3000;

	private int connectionTimeout = 3000;

	private int readTimeout = 5000;

	private int writeTimeout = 5000;

	private int maxIdleTime = 20;

	private int maxLifeTime = 60;

	public List<String> getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(List<String> serviceNames) {
		this.serviceNames = serviceNames;
	}

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

	public String getServiceNamespace() {
		return serviceNamespace;
	}

	void setServiceNamespace(String serviceNamespace) {
		this.serviceNamespace = serviceNamespace;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(final int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getAcquireTimeout() {
		return acquireTimeout;
	}

	public void setAcquireTimeout(final int acquireTimeout) {
		this.acquireTimeout = acquireTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(final int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(final int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public int getWriteTimeout() {
		return writeTimeout;
	}

	public void setWriteTimeout(final int writeTimeout) {
		this.writeTimeout = writeTimeout;
	}

	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	public void setMaxIdleTime(final int maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
	}

	public int getMaxLifeTime() {
		return maxLifeTime;
	}

	public void setMaxLifeTime(final int maxLifeTime) {
		this.maxLifeTime = maxLifeTime;
	}

}
