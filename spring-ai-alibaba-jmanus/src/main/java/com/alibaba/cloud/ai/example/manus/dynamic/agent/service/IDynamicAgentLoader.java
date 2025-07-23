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

/**
 * Dynamic Agent loader interface, providing Agent loading function
 */
public interface IDynamicAgentLoader {

	/**
	 * 加载Agent
	 * @param agentName Agent名称
	 * @param initialAgentSetting 初始Agent设置
	 * @return 动态Agent
	 */
	DynamicAgent loadAgent(String agentName, Map<String, Object> initialAgentSetting);

	/**
	 * 获取所有Agent
	 * @return Agent实体列表
	 */
	List<DynamicAgentEntity> getAllAgents();

}
