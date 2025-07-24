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

package com.alibaba.cloud.ai.mcp.router.core;

import com.alibaba.cloud.ai.mcp.router.service.McpRouterManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class McpRouterWatcher extends AbstractRouterWatcher {

	private static final Logger logger = LoggerFactory.getLogger(McpRouterWatcher.class);

	private final McpRouterManagementService managementService;

	private final List<String> serviceNames;

	public McpRouterWatcher(McpRouterManagementService managementService, List<String> serviceNames) {
		this.managementService = managementService;
		this.serviceNames = serviceNames;
	}

	@Override
	protected void handleChange() {
		logger.debug("McpRouterWatcher polling...");
		for (String serviceName : serviceNames) {
			try {
				managementService.refreshService(serviceName);
				logger.info("Refreshed MCP service: {}", serviceName);
			}
			catch (Exception e) {
				logger.warn("Failed to refresh MCP service: {}", serviceName, e);
			}
		}
	}

}
