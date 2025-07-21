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

package com.alibaba.cloud.ai.mcp.gateway.core;

/**
 * MCP 端点信息
 */
public class McpEndpointInfo {

	private String address;

	private int port;

	private String serviceName;

	private String namespaceId;

	private String groupName;

	private boolean healthy;

	public McpEndpointInfo() {
	}

	public McpEndpointInfo(String address, int port, String serviceName, String namespaceId, String groupName,
			boolean healthy) {
		this.address = address;
		this.port = port;
		this.serviceName = serviceName;
		this.namespaceId = namespaceId;
		this.groupName = groupName;
		this.healthy = healthy;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public boolean isHealthy() {
		return healthy;
	}

	public void setHealthy(boolean healthy) {
		this.healthy = healthy;
	}

	@Override
	public String toString() {
		return "McpEndpointInfo{" + "address='" + address + '\'' + ", port=" + port + ", serviceName='" + serviceName
				+ '\'' + ", namespaceId='" + namespaceId + '\'' + ", groupName='" + groupName + '\'' + ", healthy="
				+ healthy + '}';
	}

}
