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
package com.alibaba.cloud.ai.manus.agent.service;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.model.Tool;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;

public interface AgentService {

	List<AgentConfig> getAllAgentsByNamespace(String namespace);

	AgentConfig getAgentById(String id);

	AgentConfig createAgent(AgentConfig agentConfig);

	AgentConfig updateAgent(AgentConfig agentConfig);

	void deleteAgent(String id);

	List<Tool> getAvailableTools();

	/**
	 * Get all dynamic agent entities for the current namespace
	 * @return List of DynamicAgentEntity objects
	 */
	List<DynamicAgentEntity> getAllAgents();

	/**
	 * Create and return a usable BaseAgent object, similar to the
	 * createPlanningCoordinator method in PlanningFactory
	 * @param name Agent name
	 * @param currentPlanId Plan ID, used to identify the plan the agent belongs to
	 * @param rootPlanId Root plan ID
	 * @param initialAgentSetting Initial agent settings
	 * @param expectedReturnInfo Expected return information
	 * @param step Execution step
	 * @param modelEntity Dynamic model entity (optional, can be null)
	 * @param availableToolKeys List of available tool keys (optional, can be null)
	 * @return Created BaseAgent object
	 */
	BaseAgent createDynamicBaseAgent(String name, String currentPlanId, String rootPlanId,
			Map<String, Object> initialAgentSetting, String expectedReturnInfo, ExecutionStep step,
			DynamicModelEntity modelEntity, List<String> selectedToolKeys);

}
