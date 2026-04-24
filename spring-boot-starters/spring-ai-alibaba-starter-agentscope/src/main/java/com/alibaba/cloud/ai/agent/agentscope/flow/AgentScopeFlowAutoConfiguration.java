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
package com.alibaba.cloud.ai.agent.agentscope.flow;

import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategyRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Registers AgentScope-specific flow strategies (e.g. AgentScope routing with Model) with the
 * framework's {@link FlowGraphBuildingStrategyRegistry} so that {@link AgentScopeRoutingAgent}
 * can build its graph.
 */
@AutoConfiguration
public class AgentScopeFlowAutoConfiguration {

	@Bean
	public AgentScopeRoutingStrategyRegistrar agentScopeRoutingStrategyRegistrar() {
		return new AgentScopeRoutingStrategyRegistrar();
	}

	/**
	 * Registers {@link AgentScopeRoutingGraphBuildingStrategy} when the bean is created.
	 */
	public static class AgentScopeRoutingStrategyRegistrar {

		public AgentScopeRoutingStrategyRegistrar() {
			FlowGraphBuildingStrategyRegistry registry = FlowGraphBuildingStrategyRegistry.getInstance();
			if (!registry.hasStrategy(AgentScopeRoutingGraphBuildingStrategy.AGENT_SCOPE_ROUTING_TYPE)) {
				registry.registerStrategy(
						AgentScopeRoutingGraphBuildingStrategy.AGENT_SCOPE_ROUTING_TYPE,
						AgentScopeRoutingGraphBuildingStrategy::new);
			}
		}
	}
}
