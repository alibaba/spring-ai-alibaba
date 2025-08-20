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
package com.alibaba.cloud.ai.graph.agent.a2a;

import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.a2a.spec.AgentCard;

public class A2aRemoteAgent extends BaseAgent {
	private AgentCard agentCard;

	// Private constructor for Builder pattern
	private A2aRemoteAgent(Builder builder) {
		super(builder.name, builder.description, builder.outputKey);
		this.agentCard = builder.agentCard;
	}

	@Override
	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent) throws GraphStateException {
		return AsyncNodeAction.node_async(new A2aNode(agentCard, inputKeyFromParent, outputKeyToParent));
	}

	@Override
	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		return Optional.empty();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		// BaseAgent properties
		private String name;
		private String description;
		private String outputKey;

		// A2aRemoteAgent specific properties
		private AgentCard agentCard;

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

		public Builder agentCard(AgentCard agentCard) {
			this.agentCard = agentCard;
			return this;
		}

		public A2aRemoteAgent build() {
			// Validation
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Name must be provided");
			}
			if (description == null || description.trim().isEmpty()) {
				throw new IllegalArgumentException("Description must be provided");
			}

			return new A2aRemoteAgent(this);
		}
	}

}
