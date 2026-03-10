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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.agentscope.core.model.Model;

/**
 * FlowAgent equivalent of {@link com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent}
 * that uses AgentScope {@link Model} instead of Spring AI ChatModel for routing and merge. Reuses
 * the same graph building pipeline via {@link AgentScopeRoutingGraphBuildingStrategy} and
 * framework components (TransparentNode, addSubAgentNode, key strategies); only the routing and
 * merge nodes use AgentScope Model.
 */
public class AgentScopeRoutingAgent extends FlowAgent {

	private final Model model;
	private final String fallbackAgent;
	private final String systemPrompt;
	private final String instruction;

	protected AgentScopeRoutingAgent(AgentScopeRoutingAgentBuilder builder) {
		super(builder.name, builder.description, builder.compileConfig, builder.subAgents,
				builder.stateSerializer, builder.executor, builder.hooks);
		this.model = builder.model;
		this.fallbackAgent = builder.fallbackAgent;
		this.systemPrompt = builder.systemPrompt;
		this.instruction = builder.instruction;
	}

	public static AgentScopeRoutingAgentBuilder builder() {
		return new AgentScopeRoutingAgentBuilder();
	}

	public Model getModel() {
		return model;
	}

	public String getFallbackAgent() {
		return fallbackAgent;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public String getInstruction() {
		return instruction;
	}

	@Override
	protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		config.customProperty("agentScopeModel", this.model);
		return FlowGraphBuilder.buildGraph(AgentScopeRoutingGraphBuildingStrategy.AGENT_SCOPE_ROUTING_TYPE, config);
	}

	public static final class AgentScopeRoutingAgentBuilder extends FlowAgentBuilder<AgentScopeRoutingAgent, AgentScopeRoutingAgentBuilder> {

		private Model model;
		private String fallbackAgent;
		private String systemPrompt;
		private String instruction;

		public AgentScopeRoutingAgentBuilder model(Model model) {
			this.model = model;
			return this;
		}

		public AgentScopeRoutingAgentBuilder fallbackAgent(String fallbackAgent) {
			this.fallbackAgent = fallbackAgent;
			return this;
		}

		public AgentScopeRoutingAgentBuilder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public AgentScopeRoutingAgentBuilder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		@Override
		protected AgentScopeRoutingAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			if (model == null) {
				throw new IllegalArgumentException("AgentScope Model must be provided for AgentScope routing agent");
			}
		}

		@Override
		public AgentScopeRoutingAgent doBuild() {
			validate();
			return new AgentScopeRoutingAgent(this);
		}
	}
}
