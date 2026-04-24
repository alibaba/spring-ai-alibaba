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

import java.util.ArrayList;
import java.util.List;

/**
 * POJO schema for AgentScope structured output of routing decisions. Used with
 * {@link io.agentscope.core.ReActAgent#call(java.util.List, Class)} and
 * {@link io.agentscope.core.message.Msg#getStructuredData(Class)}. Has a no-arg constructor
 * and public fields so AgentScope/Jackson can deserialize; converted to
 * {@link com.alibaba.cloud.ai.graph.agent.flow.node.RoutingNode.RoutingDecision} in
 * {@link AgentScopeRoutingNode}.
 */
public final class RoutingDecisionSchema {

	public List<AgentRoutingSchema> agents;

	public RoutingDecisionSchema() {
		this.agents = new ArrayList<>();
	}

	/**
	 * Single agent entry for schema; matches JSON {"agent": "...", "query": "..."}.
	 */
	public static final class AgentRoutingSchema {
		public String agent;
		public String query;

		public AgentRoutingSchema() {
		}
	}
}
