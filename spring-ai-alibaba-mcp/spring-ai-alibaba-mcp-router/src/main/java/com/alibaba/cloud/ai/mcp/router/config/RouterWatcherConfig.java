/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.config;

import com.alibaba.cloud.ai.mcp.router.core.McpRouterWatcher;
import com.alibaba.cloud.ai.mcp.router.service.McpRouterManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@AutoConfiguration(after = { McpRouterManagementService.class })
public class RouterWatcherConfig {

	@Bean(initMethod = "startScheduledPolling", destroyMethod = "stop")
	public McpRouterWatcher mcpRouterWatcher(McpRouterManagementService managementService,
			@Value("${mcp.router.discovery.service-names:}") String serviceNamesStr) {
		List<String> serviceNames = Arrays.stream(serviceNamesStr.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toList());
		return new McpRouterWatcher(managementService, serviceNames);
	}

}
