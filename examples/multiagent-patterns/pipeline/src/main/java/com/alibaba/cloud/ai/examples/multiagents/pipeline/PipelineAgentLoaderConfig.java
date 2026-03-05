/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.multiagents.pipeline;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers pipeline agents (Sequential, Parallel, Loop) for Spring AI Alibaba Studio.
 */
@Configuration
public class PipelineAgentLoaderConfig {

	private static final String SEQUENTIAL_SQL_AGENT = "sequential_sql_agent";
	private static final String PARALLEL_RESEARCH_AGENT = "parallel_research_agent";
	private static final String LOOP_SQL_REFINEMENT_AGENT = "loop_sql_refinement_agent";

	@Bean
	public AgentLoader pipelineAgentLoader(
			SequentialAgent sequentialSqlAgent,
			ParallelAgent parallelResearchAgent,
			LoopAgent loopSqlRefinementAgent) {
		Map<String, Agent> agents = new ConcurrentHashMap<>();
		agents.put(SEQUENTIAL_SQL_AGENT, sequentialSqlAgent);
		agents.put(PARALLEL_RESEARCH_AGENT, parallelResearchAgent);
		agents.put(LOOP_SQL_REFINEMENT_AGENT, loopSqlRefinementAgent);

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
					throw new RuntimeException("Agent not found: " + name
							+ ". Available: " + String.join(", ", agents.keySet()));
				}
				return agent;
			}
		};
	}
}
