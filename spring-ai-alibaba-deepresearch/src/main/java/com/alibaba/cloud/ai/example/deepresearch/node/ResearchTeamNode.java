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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.*;

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

	private final ExecutorService executorService;

	public ResearchTeamNode(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.executorService = Executors.newFixedThreadPool(10);
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
				Thread.sleep(20000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		return updated;
	}

	public boolean areAllExecutionResultsPresent(Plan plan) {
		if (CollectionUtils.isEmpty(plan.getSteps())) {
			return false;
		}

		return plan.getSteps().stream().allMatch(step -> StringUtils.hasLength(step.getExecutionRes()));
	}

}
