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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author aias00
 */
@ConfigurationProperties(prefix = McpGatewayProperties.CONFIG_PREFIX)
public class McpGatewayProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.gateway";

	private Boolean enabled = true;

	private String registry = "nacos";

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRegistry() {
		return registry;
	}

	public void setRegistry(final String registry) {
		this.registry = registry;
	}

}
