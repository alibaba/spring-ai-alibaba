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

package com.alibaba.cloud.ai.mcp.router.core.discovery;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Composite McpServiceDiscovery, support multiple discovery types. Queries multiple
 * McpServiceDiscovery implementations in order. Returns the first non-null result.
 *
 * @author digitzh
 */
public class CompositeMcpServiceDiscovery implements McpServiceDiscovery {

	private static final Logger log = LoggerFactory.getLogger(CompositeMcpServiceDiscovery.class);

	private final McpServiceDiscoveryFactory discoveryFactory;

	private final List<String> searchOrder;

	public CompositeMcpServiceDiscovery(McpServiceDiscoveryFactory discoveryFactory, List<String> searchOrder) {
		this.discoveryFactory = discoveryFactory;
		this.searchOrder = searchOrder;
		if (discoveryFactory == null) {
			throw new IllegalArgumentException("McpServiceDiscoveryFactory cannot be null");
		}
		if (searchOrder == null || searchOrder.isEmpty()) {
			throw new IllegalArgumentException("Search order cannot be null or empty");
		}
		log.info("Created composite MCP service discovery with search order: {}", searchOrder);
	}

	@Override
	public McpServerInfo getService(String serviceName) {
		if (serviceName == null || serviceName.trim().isEmpty()) {
			log.warn("Service name is null or empty");
			return null;
		}

		log.debug("Searching for service: {} with order: {}", serviceName, searchOrder);

		for (String discoveryType : searchOrder) {
			McpServiceDiscovery discovery = discoveryFactory.getDiscovery(discoveryType);
			if (discovery == null) {
				log.debug("No discovery implementation found for type: {}", discoveryType);
				continue;
			}

			try {
				McpServerInfo serverInfo = discovery.getService(serviceName);
				if (serverInfo != null) {
					log.info("Found service '{}' using discovery type: {}", serviceName, discoveryType);
					return serverInfo;
				}
				else {
					log.debug("Service '{}' not found in discovery type: {}", serviceName, discoveryType);
				}
			}
			catch (Exception e) {
				log.error("Error occurred while searching service '{}' in discovery type: {}", serviceName,
						discoveryType, e);
			}
		}

		log.warn("Service '{}' not found in any registered discovery implementations", serviceName);
		return null;
	}

	public List<String> getSearchOrder() {
		return List.copyOf(searchOrder);
	}

	public McpServiceDiscoveryFactory getDiscoveryFactory() {
		return discoveryFactory;
	}

}
