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

package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/4/29:08:24
 */
@ConfigurationProperties(NacosMcpSseClientProperties.CONFIG_PREFIX)
public class NacosMcpSseClientProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.nacos.client.sse";

	private final Map<String, NacosSseParameters> connections = new HashMap<>();

	public Map<String, NacosSseParameters> getConnections() {
		return connections;
	}

	public static record NacosSseParameters(String serviceName, String version) {
	}

}
