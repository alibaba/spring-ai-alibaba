/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.examples.multiagents.routing.graph;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static Agent Loader for the routing-graph example.
 * Exposes the router agent for Spring AI Alibaba Studio.
 */
@Component
class AgentStaticLoader implements AgentLoader {

	private static final String ROUTER_AGENT_NAME = "router";

	private final Map<String, Agent> agents = new ConcurrentHashMap<>();

	public AgentStaticLoader(LlmRoutingAgent routerAgent) {
		agents.put(ROUTER_AGENT_NAME, routerAgent);
	}

	@Override
	@Nonnull
	public List<String> listAgents() {
		return agents.keySet().stream().toList();
	}

	@Override
	public Agent loadAgent(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Agent name cannot be null or empty");
		}
		Agent agent = agents.get(name);
		if (agent == null) {
			throw new NoSuchElementException("Agent not found: " + name);
		}
		return agent;
	}
}
