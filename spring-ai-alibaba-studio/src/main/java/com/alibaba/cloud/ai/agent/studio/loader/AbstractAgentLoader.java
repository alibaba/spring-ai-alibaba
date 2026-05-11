/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.agent.studio.loader;

import com.alibaba.cloud.ai.graph.agent.Agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Base implementation of {@link AgentLoader} that delegates to a name-to-agent map
 * supplied by subclasses via {@link #loadAgentMap()}. No dependency on
 * {@link ApplicationContext} in the base class: subclasses can scan the context,
 * manually register agents, or combine both.
 *
 * <p>Example – manual registration (no context):
 *
 * <pre>{@code
 * @Component
 * public class MyAgentLoader extends AbstractAgentLoader {
 *     private final Agent chatAgent;
 *     private final Agent codeAgent;
 *     public MyAgentLoader(@Qualifier("chatAgent") Agent chatAgent,
 *                         @Qualifier("codeAgent") Agent codeAgent) {
 *         this.chatAgent = chatAgent;
 *         this.codeAgent = codeAgent;
 *     }
 *     @Override
 *     protected Map<String, Agent> loadAgentMap() {
 *         return Map.of("chat", chatAgent, "code", codeAgent);
 *     }
 * }
 * }</pre>
 *
 * <p>Example – scan context (use static helper):
 *
 * <pre>{@code
 * @Component
 * public class MyAgentLoader extends AbstractAgentLoader {
 *     private final ApplicationContext context;
 *     public MyAgentLoader(ApplicationContext context) {
 *         this.context = context;
 *     }
 *     @Override
 *     protected Map<String, Agent> loadAgentMap() {
 *         return discoverFromContext(context);
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractAgentLoader implements AgentLoader {

	private static final Logger log = LoggerFactory.getLogger(AbstractAgentLoader.class);

	private volatile Map<String, Agent> agentMap;

	/**
	 * No-arg constructor for subclasses that provide the agent map via {@link #loadAgentMap()}.
	 */
	protected AbstractAgentLoader() {
	}

	/**
	 * Supplies the map of agent name to agent instance. Called once and cached.
	 * Subclasses can build the map from context, from injected beans, or both.
	 *
	 * @return map of agent name to agent (must not be null; can be empty)
	 */
	protected abstract Map<String, Agent> loadAgentMap();

	private Map<String, Agent> getAgentMap() {
		if (agentMap == null) {
			synchronized (this) {
				if (agentMap == null) {
					agentMap = loadAgentMap();
				}
			}
		}
		return agentMap;
	}

	/**
	 * Helper to build a name-to-agent map from all {@link Agent} beans in the given
	 * context. Duplicate names keep the first and log a warning. Use this in
	 * {@link #loadAgentMap()} when you want context scanning.
	 *
	 * @param context the Spring application context
	 * @return map of agent name to agent instance
	 */
	protected static Map<String, Agent> discoverFromContext(ApplicationContext context) {
		Map<String, Agent> beans = context.getBeansOfType(Agent.class);
		Map<String, Agent> result = new LinkedHashMap<>();
		for (Agent agent : beans.values()) {
			String name = agent.name();
			if (result.putIfAbsent(name, agent) != null) {
				log.warn("Duplicate agent name '{}', keeping first. Consider using unique agent names for Studio.", name);
			}
		}
		return result;
	}

	@Override
	@Nonnull
	public List<String> listAgents() {
		return List.copyOf(getAgentMap().keySet());
	}

	@Override
	public Agent loadAgent(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Agent name cannot be null or empty");
		}
		Agent agent = getAgentMap().get(name);
		if (agent == null) {
			throw new NoSuchElementException("Agent not found: " + name);
		}
		return agent;
	}
}
