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

import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties,
			LevelBasedExecutorPool levelBasedExecutorPool) {
		super(agents, recorder, agentService, llmService, manusProperties, levelBasedExecutorPool);
	}

	/**
	 * Execute direct response plan asynchronously - records plan execution start and
	 * marks as successful The actual direct response generation is handled by
	 * PlanningCoordinator
	 * @param context Execution context containing user request and plan information
	 * @return CompletableFuture containing the execution result
	 */
	@Override
	public CompletableFuture<PlanExecutionResult> executeAllStepsAsync(ExecutionContext context) {
		return CompletableFuture.<PlanExecutionResult>supplyAsync(() -> {
			log.info("Executing direct response plan for planId: {}", context.getCurrentPlanId());

			BaseAgent lastExecutor = null;

			try {
				// Record plan execution start
				recorder.recordPlanExecutionStart(context.getCurrentPlanId(), context.getPlan().getTitle(),
						context.getUserRequest(), context.getPlan().getAllSteps(), context.getParentPlanId(),
						context.getRootPlanId(), context.getToolCallId());

				log.info("Direct response executor completed successfully for planId: {}", context.getCurrentPlanId());
				context.setSuccess(true);

				// Create successful result
				PlanExecutionResult result = new PlanExecutionResult();
				result.setSuccess(true);
				result.setFinalResult(context.getPlan().getResult());
				return result;
			}
			catch (Exception e) {
				log.error("Error during direct response execution for planId: {}", context.getCurrentPlanId(), e);
				context.setSuccess(false);
				// Create failed result
				PlanExecutionResult result = new PlanExecutionResult();
				result.setSuccess(false);
				result.setErrorMessage("Direct response execution failed: " + e.getMessage());
				return result;
			}
			finally {
				performCleanup(context, lastExecutor);
			}
		});
	}

}
