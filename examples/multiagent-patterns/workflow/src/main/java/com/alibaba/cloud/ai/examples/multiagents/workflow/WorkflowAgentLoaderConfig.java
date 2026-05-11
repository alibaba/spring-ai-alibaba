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

package com.alibaba.cloud.ai.examples.multiagents.workflow;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides AgentLoader for the workflow example. Registers RAG and/or SQL agents
 * when enabled via workflow.rag.enabled and workflow.sql.enabled.
 */
@Configuration
public class WorkflowAgentLoaderConfig {

	private static final String GENERATE_QUERY_NAME = "generate_query";
	private static final String RAG_AGENT_NAME = "rag_agent";

	@Bean
	public AgentLoader agentStaticLoader(
			@Autowired(required = false) @Qualifier("generateQueryAgent") ReactAgent generateQueryAgent,
			@Autowired(required = false) @Qualifier("ragAgent") ReactAgent ragAgent) {
		Map<String, Agent> agents = new ConcurrentHashMap<>();
		if (generateQueryAgent != null) {
			agents.put(GENERATE_QUERY_NAME, generateQueryAgent);
		}
		if (ragAgent != null) {
			agents.put(RAG_AGENT_NAME, ragAgent);
		}
		return new AgentLoader() {
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
					throw new NoSuchElementException("Agent not found: " + name + ". Enable workflow.sql.enabled and/or workflow.rag.enabled in application.yml.");
				}
				return agent;
			}
		};
	}
}
