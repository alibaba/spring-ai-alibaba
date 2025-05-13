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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author aias00
 */
@ConfigurationProperties(prefix = NacosMcpDynamicProperties.CONFIG_PREFIX)
public class NacosMcpDynamicProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.nacos.dynamic";

	String serviceNamespace;

	String serviceGroup = "DEFAULT_GROUP";

	List<String> serviceNames;

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

}
