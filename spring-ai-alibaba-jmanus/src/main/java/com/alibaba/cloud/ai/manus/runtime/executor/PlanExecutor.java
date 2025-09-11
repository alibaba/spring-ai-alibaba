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
package com.alibaba.cloud.ai.manus.runtime.executor;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;

import java.util.List;

/**
 * Basic implementation class responsible for executing plans Now uses level-based
 * executor pools for different hierarchy depths
 */
public class PlanExecutor extends AbstractPlanExecutor {

	/**
	 * Constructor for PlanExecutor
	 * @param agents List of dynamic agent entities
	 * @param recorder Plan execution recorder
	 * @param agentService Agent service
	 * @param llmService LLM service
	 * @param manusProperties Manus properties
	 * @param levelBasedExecutorPool Level-based executor pool for depth-based execution
	 */
	public PlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder, AgentService agentService,
			ILlmService llmService, ManusProperties manusProperties, LevelBasedExecutorPool levelBasedExecutorPool) {
		super(agents, recorder, agentService, llmService, manusProperties, levelBasedExecutorPool);

	}

}
