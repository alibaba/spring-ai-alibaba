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

package com.alibaba.cloud.ai.examples.multiagents.supervisor;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static Agent Loader for the supervisor personal assistant.
 *
 * <p>Exposes the supervisor agent (and optionally calendar/email agents) through the
 * AgentLoader interface for Spring AI Alibaba Studio. The main entry for Studio is
 * the "personal_assistant" supervisor agent.
 */
@Component
class AgentStaticLoader implements AgentLoader {

	private static final String SUPERVISOR_AGENT_NAME = "personal_assistant";

	private final Map<String, Agent> agents = new ConcurrentHashMap<>();

	public AgentStaticLoader(@Qualifier("supervisorAgent") ReactAgent supervisorAgent) {
		this.agents.put(SUPERVISOR_AGENT_NAME, supervisorAgent);
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
