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
package com.alibaba.cloud.ai.graph.agent.flow.agent;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.util.StringUtils;

public class LlmRoutingAgent extends FlowAgent {

	public static final String FALLBACK_AGENT_KEY = "fallbackAgent";
	private final ChatModel chatModel;
	private final String fallbackAgent;
	private final String systemPrompt;
	private final String instruction;

	protected LlmRoutingAgent(LlmRoutingAgentBuilder builder) {
		super(builder.name, builder.description, builder.compileConfig, builder.subAgents, builder.stateSerializer, builder.executor, builder.hooks);
		this.chatModel = builder.chatModel;
		this.fallbackAgent = builder.fallbackAgent;
		this.systemPrompt = builder.systemPrompt;
		this.instruction = builder.instruction;
	}

	public static LlmRoutingAgentBuilder builder() {
		return new LlmRoutingAgentBuilder();
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
		config.setChatModel(this.chatModel);
		config.customProperty(FALLBACK_AGENT_KEY, fallbackAgent);
		return FlowGraphBuilder.buildGraph(FlowAgentEnum.ROUTING.getType(), config);
	}

	/**
	 * Builder for creating LlmRoutingAgent instances. Extends the common FlowAgentBuilder
	 * and adds LLM-specific configuration.
	 */
	public static class LlmRoutingAgentBuilder extends FlowAgentBuilder<LlmRoutingAgent, LlmRoutingAgentBuilder> {

		private ChatModel chatModel;
		private String fallbackAgent;
		private String systemPrompt;
		private String instruction;

		/**
		 * Sets the ChatModel for LLM-based routing decisions.
		 * @param chatModel the chat model to use for routing
		 * @return this builder instance for method chaining
		 */
		public LlmRoutingAgentBuilder model(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		/**
		 * Sets the name of a configured sub-agent to use when the routing model
		 * cannot produce a valid routing decision after all retry attempts.
		 *
		 * <p>The fallback agent must be one of the agents configured through
		 * {@link FlowAgentBuilder#subAgents}. This property is optional.</p>
		 *
		 * @param fallbackAgent the name of an existing sub-agent
		 * @return this builder
		 */
		public LlmRoutingAgentBuilder fallbackAgent(String fallbackAgent) {
			this.fallbackAgent = fallbackAgent;
			return this;
		}

		public LlmRoutingAgentBuilder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public LlmRoutingAgentBuilder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		@Override
		protected LlmRoutingAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			if (chatModel == null) {
				throw new IllegalArgumentException("ChatModel must be provided for LLM routing agent");
			}
			if (StringUtils.hasText(fallbackAgent)) {
				boolean fallbackAgentExists = subAgents.stream()
						.map(Agent::name)
						.anyMatch(fallbackAgent::equals);

				if (!fallbackAgentExists) {
					throw new IllegalArgumentException("Fallback agent '" + fallbackAgent + "' must be one of the configured sub-agents");
				}
			}
		}

		@Override
		public LlmRoutingAgent doBuild() {
			validate();
			return new LlmRoutingAgent(this);
		}

	}

}
