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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.MultiCommandAction;
import com.alibaba.cloud.ai.graph.agent.Agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MultiCommandAction that reads the sub-agent list from state key
 * {@value #SUPERVISOR_NEXT_KEY}. The value is set by {@link MainAgentNodeAction} (a
 * List of sub-agent names); no parsing of AssistantMessage or JSON text is needed here.
 */
public class SupervisorNodeFromState implements MultiCommandAction {
	public static final Logger logger = LoggerFactory.getLogger(SupervisorNodeFromState.class);

	/**
	 * State key where {@link MainAgentNodeAction} sets the routing decision: a List of
	 * sub-agent names (or ["FINISH"] for end). Read directly here, no parsing needed.
	 */
	public static final String SUPERVISOR_NEXT_KEY = "supervisor_next";

	private final String routingKey;
	private final List<Agent> subAgents;
	private final String entryNode;

	public SupervisorNodeFromState(String routingKey, List<Agent> subAgents, String entryNode) {
		this.routingKey = routingKey != null ? routingKey : SUPERVISOR_NEXT_KEY;
		this.subAgents = subAgents != null ? subAgents : List.of();
		this.entryNode = entryNode;
	}

	public SupervisorNodeFromState(List<Agent> subAgents) {
		this(SUPERVISOR_NEXT_KEY, subAgents, "");
	}

	@Override
	public MultiCommand apply(OverAllState state, RunnableConfig config) throws Exception {
		Object value = state.value(routingKey).orElse(null);
		List<String> agentNames = toAgentNames(value);
		List<String> validNames = agentNames.stream()
				.filter(name -> subAgents.stream().anyMatch(a -> a.name().equals(name)))
				.collect(Collectors.toList());

		logger.info("SupervisorNodeFromState: routingKey='{}', value='{}', parsed agentNames={}, validAgentNames={}",
				routingKey, value, agentNames, validNames);

		if (validNames.isEmpty()) {
			logger.error("SupervisorNodeFromState: no valid sub-agent names in state key '{}', value: {}", routingKey, value);
			return new MultiCommand(List.of(entryNode), Map.of());
		}
		return new MultiCommand(validNames, Map.of());
	}

	/**
	 * Reads agent names from the state value. The value is set by MainAgentNodeAction as
	 * a List of sub-agent names; only List is expected, no AssistantMessage/JSON parsing.
	 */
	private static List<String> toAgentNames(Object value) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof List<?> list) {
			return list.stream()
					.filter(e -> e != null && !"FINISH".equalsIgnoreCase(String.valueOf(e).trim()))
					.map(e -> String.valueOf(e).trim())
					.collect(Collectors.toList());
		}
		return List.of();
	}
}
