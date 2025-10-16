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
 */

package com.alibaba.cloud.ai.mcp.router.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory MCP Session store implementation
 *
 * @author Libres-coder
 * @since 2025.10.16
 */
public class InMemoryMcpSessionStore implements McpSessionStore {

	private static final Logger logger = LoggerFactory.getLogger(InMemoryMcpSessionStore.class);

	private final Map<String, Object> sessionStore = new ConcurrentHashMap<>();

	@Override
	public void put(String serviceName, Object sessionData) {
		if (serviceName == null) {
			logger.warn("Service name cannot be null, session not stored");
			return;
		}
		sessionStore.put(serviceName, sessionData);
		logger.debug("Stored session for service: {}", serviceName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String serviceName) {
		if (serviceName == null) {
			logger.warn("Service name cannot be null");
			return null;
		}
		T sessionData = (T) sessionStore.get(serviceName);
		logger.debug("Retrieved session for service: {} -> {}", serviceName, sessionData != null);
		return sessionData;
	}

	@Override
	public void remove(String serviceName) {
		if (serviceName == null) {
			logger.warn("Service name cannot be null");
			return;
		}
		Object removed = sessionStore.remove(serviceName);
		logger.debug("Removed session for service: {}, existed: {}", serviceName, removed != null);
	}

	@Override
	public boolean contains(String serviceName) {
		if (serviceName == null) {
			return false;
		}
		return sessionStore.containsKey(serviceName);
	}

	@Override
	public void clear() {
		int size = sessionStore.size();
		sessionStore.clear();
		logger.info("Cleared all sessions, count: {}", size);
	}

	@Override
	public int size() {
		return sessionStore.size();
	}

	@Override
	public Map<String, Object> getAll() {
		// 返回快照，避免外部修改影响内部状态
		return new HashMap<>(sessionStore);
	}

}

