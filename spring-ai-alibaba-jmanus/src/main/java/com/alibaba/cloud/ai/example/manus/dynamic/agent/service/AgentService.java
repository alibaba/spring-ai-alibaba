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

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.model.Tool;

public interface AgentService {

	List<AgentConfig> getAllAgents();

	AgentConfig getAgentById(String id);

	AgentConfig createAgent(AgentConfig agentConfig);

	AgentConfig updateAgent(AgentConfig agentConfig);

	void deleteAgent(String id);

	List<Tool> getAvailableTools();

	/**
	 * 创建并返回一个可用的BaseAgent对象 类似于PlanningFactory中的createPlanningCoordinator方法
	 * @param name 代理名称
	 * @param planId 计划ID，用于标识代理所属的计划
	 * @return 创建的BaseAgent对象
	 */
	BaseAgent createDynamicBaseAgent(String name, String planId, Map<String, Object> initialAgentSetting);

}
