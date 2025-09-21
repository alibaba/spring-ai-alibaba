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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory class for creating and managing {@link McpServiceDiscovery} implementations.
 *
 * @author digitzh
 */
public class McpServiceDiscoveryFactory {

	private static final Logger log = LoggerFactory.getLogger(McpServiceDiscoveryFactory.class);

	/**
	 * Stores the registered {@link McpServiceDiscovery} implementations, keyed by their
	 * type.
	 */
	private final ConcurrentMap<String, McpServiceDiscovery> discoveryMap = new ConcurrentHashMap<>();

	/**
	 * Registers a {@link McpServiceDiscovery} implementation for the specified type.
	 * @param type of the service discovery (file, database, nacos等)
	 * @param discovery implementation of the service discovery
	 */
	public void registerDiscovery(String type, McpServiceDiscovery discovery) {
		if (!StringUtils.hasText(type)) {
			throw new IllegalArgumentException("Discovery type cannot be null or empty");
		}
		if (discovery == null) {
			throw new IllegalArgumentException("Discovery implementation cannot be null");
		}

		McpServiceDiscovery existing = discoveryMap.put(type, discovery);
		if (existing != null) {
			log.warn("Replaced existing MCP service discovery for type: {}", type);
		}
		else {
			log.info("Registered MCP service discovery for type: {}", type);
		}
	}

	public McpServiceDiscovery getDiscovery(String type) {
		return discoveryMap.get(type);
	}

	public List<McpServiceDiscovery> getAllDiscoveries() {
		return new ArrayList<>(discoveryMap.values());
	}

	public List<String> getRegisteredTypes() {
		return new ArrayList<>(discoveryMap.keySet());
	}

	public boolean hasDiscovery(String type) {
		return discoveryMap.containsKey(type);
	}

	public McpServiceDiscovery removeDiscovery(String type) {
		McpServiceDiscovery removed = discoveryMap.remove(type);
		if (removed != null) {
			log.info("Removed MCP service discovery for type: {}", type);
		}
		return removed;
	}

	public void clear() {
		discoveryMap.clear();
		log.info("Cleared all MCP service discovery implementations");
	}

	public int size() {
		return discoveryMap.size();
	}

}
