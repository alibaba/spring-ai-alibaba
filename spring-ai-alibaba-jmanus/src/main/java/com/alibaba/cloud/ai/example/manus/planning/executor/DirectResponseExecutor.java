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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DirectResponseExecutor - Specialized executor for handling direct response plans This
 * executor records the plan execution start and marks the execution as successful, but
 * delegates the actual direct response generation to the PlanningCoordinator.
 */
public class DirectResponseExecutor extends AbstractPlanExecutor {

	private static final Logger log = LoggerFactory.getLogger(DirectResponseExecutor.class);

	/**
	 * Constructor for DirectResponseExecutor
	 * @param agents List of dynamic agent entities
	 * @param recorder Plan execution recorder
	 * @param agentService Agent service
	 * @param llmService LLM service
	 * @param manusProperties Manus properties
	 */
	public DirectResponseExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties) {
		super(agents, recorder, agentService, llmService, manusProperties);
	}

	/**
	 * Execute direct response plan - records plan execution start and marks as successful
	 * The actual direct response generation is handled by PlanningCoordinator
	 * @param context Execution context containing user request and plan information
	 */
	@Override
	public void executeAllSteps(ExecutionContext context) {
		log.info("Executing direct response plan for planId: {}", context.getCurrentPlanId());

		BaseAgent lastExecutor = null;

		try {
			// Record plan execution start
			recorder.recordPlanExecutionStart(context);

			log.info("Direct response executor completed successfully for planId: {}", context.getCurrentPlanId());
			context.setSuccess(true);
		}
		catch (Exception e) {
			log.error("Error during direct response execution for planId: {}", context.getCurrentPlanId(), e);
			context.setSuccess(false);
			// Set error message as result summary
			context.setResultSummary("Direct response execution failed: " + e.getMessage());
		}
		finally {
			performCleanup(context, lastExecutor);
		}
	}

}
