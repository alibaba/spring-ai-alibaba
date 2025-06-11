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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;


/**
 * @author yingzi
 * @since 2025/5/18 16:59
 * @author sixiyida
 * @since 2025/6/11 15:15
 *
 */
public class ResearchTeamNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResearchTeamNode.class);

	private final ApplicationContext applicationContext;

	private final ThreadPoolTaskExecutor executorService;

	private final static Long TIME_SLEEP = 20000L;

	public ResearchTeamNode(ApplicationContext applicationContext, ThreadPoolTaskExecutor executorService) {
		this.applicationContext = applicationContext;
		this.executorService = executorService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("research_team node is running.");
		Map<String, Object> updated = new HashMap<>();

		Plan curPlan = StateUtil.getPlan(state);

		// 收集所有步骤
		List<Plan.Step> steps = curPlan.getSteps();

		// 为每个步骤创建异步任务
		for (int i = 0; i < steps.size(); i++) {
			final int stepIndex = i;
			Plan.Step step = steps.get(stepIndex);

			// 检查步骤是否未完成且未分配
			if (!StringUtils.hasText(step.getExecutionRes()) && !StringUtils.hasText(step.getExecutionStatus())) {
				// 如果是处理步骤，先检查所有研究步骤是否完成
				if (step.getStepType() == Plan.StepType.PROCESSING && !areAllResearchStepsCompleted(curPlan)) {
					continue;  // 如果研究步骤未完成，跳过这个处理步骤
				}

				// 为步骤分配一个研究者节点，使用step的索引作为节点ID
				String executorNodeId = String.valueOf(stepIndex);
				String assignedStatus = StateUtil.EXECUTION_STATUS_ASSIGNED_PREFIX + executorNodeId;
				step.setExecutionStatus(assignedStatus);
				CompletableFuture.runAsync(() -> {
					try {
						if (step.getStepType() == Plan.StepType.RESEARCH) {
							logger.info("Executing research step {}: {}", stepIndex, step.getTitle());
							// 为每个研究步骤创建新的Agent
							ChatClient researchAgent = applicationContext.getBean("researchAgent", ChatClient.class);
							ResearcherNode researcherNode = new ResearcherNode(researchAgent, executorNodeId);
							researcherNode.apply(state);
						}
						else {
							logger.info("Executing processing step {}: {}", stepIndex, step.getTitle());
							// 为每个处理步骤创建新的Agent
							ChatClient coderAgent = applicationContext.getBean("coderAgent", ChatClient.class);
							CoderNode coderNode = new CoderNode(coderAgent, executorNodeId);
							coderNode.apply(state);
						}
					}
					catch (Exception e) {
						logger.error("Error executing step {}: {}", stepIndex, step.getTitle(), e);
					}
				}, executorService);
			}
		}

		// 检查是否所有步骤都已完成
		if (areAllExecutionResultsPresent(curPlan)) {
			updated.put("research_team_next_node", "reporter");
			logger.info("All steps completed, moving to reporter node");
		}
		else {
			// 如果还有未完成的步骤，继续执行
			updated.put("research_team_next_node", "research_team");
			logger.info("Some steps are still pending, continuing execution");
			// 在TeamNode这里自旋等待
			try {
				Thread.sleep(TIME_SLEEP);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		return updated;
	}

	private boolean areAllResearchStepsCompleted(Plan plan) {
		if (CollectionUtils.isEmpty(plan.getSteps())) {
			return true;
		}
		
		return plan.getSteps().stream()
			.filter(step -> step.getStepType() == Plan.StepType.RESEARCH)
			.allMatch(step -> step.getExecutionStatus().startsWith(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX));
	}

	public boolean areAllExecutionResultsPresent(Plan plan) {
		if (CollectionUtils.isEmpty(plan.getSteps())) {
			return true;
		}

		return plan.getSteps().stream().allMatch(step -> StringUtils.hasLength(step.getExecutionRes()));
	}

}
