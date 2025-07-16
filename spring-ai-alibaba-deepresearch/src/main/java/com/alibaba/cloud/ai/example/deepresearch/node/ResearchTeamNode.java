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

import java.util.HashMap;
import java.util.Map;

/**
 * @author sixiyida
 * @since 2025/6/12 09:14
 */

public class ResearchTeamNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResearchTeamNode.class);

	private static final long TIME_SLEEP = 10000;

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		// 智能等待：根据是否有反思任务调整等待时间
		if (state.value("research_team_next_node").isPresent()) {
			Plan curPlan = StateUtil.getPlan(state);
			if (hasActiveReflectionTasks(curPlan)) {
				Thread.sleep(5000);
				logger.debug("decline waiting time for reflection tasks");
			}
			else {
				Thread.sleep(TIME_SLEEP);
			}
		}

		logger.info("research_team node is running.");
		String nextStep = "reporter";
		Map<String, Object> updated = new HashMap<>();

		Plan curPlan = StateUtil.getPlan(state);
		// 判断steps里的每个step都有执行结果
		if (!areAllExecutionResultsPresent(curPlan)) {
			nextStep = "parallel_executor";
		}
		updated.put("research_team_next_node", nextStep);
		logger.info("research_team node -> {} node", nextStep);
		return updated;
	}

	public boolean areAllExecutionResultsPresent(Plan plan) {
		if (CollectionUtils.isEmpty(plan.getSteps())) {
			return false;
		}

		return plan.getSteps()
			.stream()
			.allMatch(step -> step.getExecutionStatus() != null
					&& step.getExecutionStatus().startsWith(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX));
	}

	/**
	 * 检查是否有活跃的反思任务
	 */
	private boolean hasActiveReflectionTasks(Plan plan) {
		if (CollectionUtils.isEmpty(plan.getSteps())) {
			return false;
		}

		return plan.getSteps()
			.stream()
			.anyMatch(step -> step.getExecutionStatus() != null
					&& (step.getExecutionStatus().contains("waiting_reflecting")
							|| step.getExecutionStatus().contains("waiting_processing")));
	}

}
