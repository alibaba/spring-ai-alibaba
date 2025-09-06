/*
 * Copyright 2024-2025 the original author or authors.
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

public class LlmRoutingAgent extends FlowAgent {

	private final ChatModel chatModel;

	protected LlmRoutingAgent(LlmRoutingAgentBuilder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
		this.chatModel = builder.chatModel;
		this.graph = initGraph();
	}

	@Override
	protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		config.setChatModel(this.chatModel);
		return FlowGraphBuilder.buildGraph(FlowAgentEnum.ROUTING.getType(), config);
	}

	public static LlmRoutingAgentBuilder builder() {
		return new LlmRoutingAgentBuilder();
	}

	/**
	 * Builder for creating LlmRoutingAgent instances. Extends the common FlowAgentBuilder
	 * and adds LLM-specific configuration.
	 */
	public static class LlmRoutingAgentBuilder extends FlowAgentBuilder<LlmRoutingAgent, LlmRoutingAgentBuilder> {

		private ChatModel chatModel;

		/**
		 * Sets the ChatModel for LLM-based routing decisions.
		 * @param chatModel the chat model to use for routing
		 * @return this builder instance for method chaining
		 */
		public LlmRoutingAgentBuilder model(ChatModel chatModel) {
			this.chatModel = chatModel;
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
		}

		@Override
		public LlmRoutingAgent build() throws GraphStateException {
			validate();
			return new LlmRoutingAgent(this);
		}

	}

}
