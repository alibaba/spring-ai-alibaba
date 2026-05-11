/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.core.route;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.a2a.core.server.JsonRpcA2aRequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Router for multi-agent A2A request handling.
 * <p>
 * Routes incoming requests to the appropriate agent handler based on the agent name
 * extracted from the URL path.
 *
 * @author xiweng.yy
 */
public class MultiAgentRequestRouter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiAgentRequestRouter.class);

	private final Map<String, JsonRpcA2aRequestHandler> handlers = new ConcurrentHashMap<>();

	/**
	 * Register a handler for an agent.
	 * @param agentName the agent name (used as URL path segment)
	 * @param handler the request handler for this agent
	 */
	public void registerHandler(String agentName, JsonRpcA2aRequestHandler handler) {
		handlers.put(agentName, handler);
		LOGGER.info("Registered A2A handler for agent: {}", agentName);
	}

	/**
	 * Get the handler for the specified agent.
	 * @param agentName the agent name
	 * @return the request handler, or null if not found
	 */
	public JsonRpcA2aRequestHandler getHandler(String agentName) {
		return handlers.get(agentName);
	}

	/**
	 * Get all registered agent names.
	 * @return an unmodifiable set of agent names
	 */
	public Set<String> getAgentNames() {
		return Collections.unmodifiableSet(handlers.keySet());
	}

	/**
	 * Check if a handler exists for the specified agent.
	 * @param agentName the agent name
	 * @return true if a handler exists
	 */
	public boolean hasHandler(String agentName) {
		return handlers.containsKey(agentName);
	}

	/**
	 * Get the number of registered handlers.
	 * @return the count of registered handlers
	 */
	public int size() {
		return handlers.size();
	}

}
