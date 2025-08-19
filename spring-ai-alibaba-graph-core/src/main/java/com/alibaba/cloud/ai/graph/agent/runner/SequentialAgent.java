/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.runner;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class SequentialAgent extends BaseAgent {

	private final List<BaseAgent> agents;

	protected SequentialAgent(Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.subAgents);
		this.agents = builder.agents;
	}

	@Override
	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent) {
		return node_async((state) -> Map.of());
	}

	public List<BaseAgent> getAgents() {
		return agents;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		// Base agent properties
		private String name;
		private String description;
		private String outputKey;
		private List<? extends BaseAgent> subAgents;

		// SequentialFlowAgent specific properties
		private List<BaseAgent> agents;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder subAgents(List<? extends BaseAgent> subAgents) {
			this.subAgents = subAgents;
			return this;
		}

		public Builder agents(List<BaseAgent> agents) {
			this.agents = agents;
			return this;
		}

		public SequentialAgent build() throws GraphStateException {
			// Validation
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Name must be provided");
			}
			if (agents == null || agents.isEmpty()) {
				throw new IllegalArgumentException("At least one agent must be provided for sequential flow");
			}

			return new SequentialAgent(this);
		}
	}
}
