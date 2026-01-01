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
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

public class SupervisorAgent extends FlowAgent {

	private final ChatModel chatModel;
	private final String systemPrompt;
	private final String instruction;

	protected SupervisorAgent(SupervisorAgentBuilder builder) {
		super(builder.name, builder.description, builder.compileConfig, builder.subAgents, builder.stateSerializer, builder.executor);
		this.chatModel = builder.chatModel;
		this.systemPrompt = builder.systemPrompt;
		this.instruction = builder.instruction;
	}

	public static SupervisorAgentBuilder builder() {
		return new SupervisorAgentBuilder();
	}

	@Override
	protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		config.setChatModel(this.chatModel);
		return FlowGraphBuilder.buildGraph(FlowAgentEnum.SUPERVISOR.getType(), config);
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public String getInstruction() {
		return instruction;
	}

	/**
	 * Builder for creating SupervisorAgent instances. Extends the common FlowAgentBuilder
	 * and adds LLM-specific configuration.
	 */
	public static class SupervisorAgentBuilder extends FlowAgentBuilder<SupervisorAgent, SupervisorAgentBuilder> {

		private ChatModel chatModel;
		private String systemPrompt;
		private String instruction;

		/**
		 * Sets the ChatModel for LLM-based supervisor routing decisions.
		 * @param chatModel the chat model to use for routing
		 * @return this builder instance for method chaining
		 */
		public SupervisorAgentBuilder model(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		public SupervisorAgentBuilder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public SupervisorAgentBuilder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		@Override
		protected SupervisorAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			if (chatModel == null) {
				throw new IllegalArgumentException("ChatModel must be provided for supervisor agent");
			}
		}

		@Override
		public SupervisorAgent doBuild() {
			validate();
			return new SupervisorAgent(this);
		}

	}

}

