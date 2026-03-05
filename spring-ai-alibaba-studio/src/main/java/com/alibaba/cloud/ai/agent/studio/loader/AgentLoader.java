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

import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * Interface for loading agents used by Spring AI Alibaba Studio.
 *
 * <p><strong>Default behavior:</strong> If you do not define an {@code AgentLoader} bean,
 * Studio automatically uses {@link ContextScanningAgentLoader}, which discovers all
 * {@link Agent} beans from the Spring {@link org.springframework.context.ApplicationContext}
 * and exposes them by their {@link Agent#name()}. You can therefore use Studio without
 * implementing this interface: just define your agents as {@code @Bean}s. All {@link Agent}
 * subtypes are supported, including {@link com.alibaba.cloud.ai.graph.agent.ReactAgent},
 * {@link com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent},
 * {@link com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent},
 * {@link com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent},
 * {@link com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent}, and
 * {@link com.alibaba.cloud.ai.graph.agent.agentscope.AgentScopeAgent}.
 *
 * <p><strong>Custom loader:</strong> To control which agents are visible or how they are
 * named, define your own {@code AgentLoader} bean. You can extend {@link AbstractAgentLoader}
 * and override {@link AbstractAgentLoader#loadAgentMap()} for custom discovery (e.g. using
 * {@link AbstractAgentLoader#discoverFromContext(org.springframework.context.ApplicationContext)}),
 * or implement this interface directly.
 *
 * <p><strong>Thread safety:</strong> Implementations must be thread-safe; they are used as
 * singleton beans and accessed concurrently by multiple HTTP requests.
 *
 * <p>Example – custom loader (manual map or context scan):
 *
 * <pre>{@code
 * @Component
 * public class MyAgentLoader extends AbstractAgentLoader {
 *   private final Agent myAgent;
 *   public MyAgentLoader(@Qualifier("myAgent") Agent myAgent) { this.myAgent = myAgent; }
 *   @Override
 *   protected Map<String, Agent> loadAgentMap() {
 *     return Map.of("my_agent", myAgent);
 *   }
 * }
 * }</pre>
 */
public interface AgentLoader {

	/**
	 * Returns a list of available agent names.
	 *
	 * @return ImmutableList of agent names. Must not return null - return an empty list if no agents
	 *     are available.
	 */
	@Nonnull
	List<String> listAgents();

	/**
	 * Loads the Agent instance for the specified agent name.
	 *
	 * @param name the name of the agent to load
	 * @return Agent instance for the given name (any subtype: ReactAgent, SequentialAgent, etc.)
	 * @throws java.util.NoSuchElementException if the agent doesn't exist
	 * @throws IllegalStateException if the agent exists but fails to load
	 */
	Agent loadAgent(String name);
}
