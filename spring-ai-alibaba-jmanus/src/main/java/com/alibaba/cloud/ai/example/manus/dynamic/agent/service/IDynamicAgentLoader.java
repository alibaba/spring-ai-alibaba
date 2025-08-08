/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.DynamicAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

/**
 * Dynamic Agent loader interface, providing Agent loading function
 */
public interface IDynamicAgentLoader {

	/**
	 * Load Agent
	 * @param agentName Agent name
	 * @param initialAgentSetting Initial Agent settings
	 * @return Dynamic Agent
	 */
	DynamicAgent loadAgent(String agentName, Map<String, Object> initialAgentSetting);

	/**
	 * Get all Agents
	 * @return List of Agent entities
	 */
	List<DynamicAgentEntity> getAllAgents();

	default List<DynamicAgentEntity> getAgents(ExecutionContext context) {
		return getAllAgents();
	}

}
