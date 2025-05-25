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

import com.alibaba.cloud.ai.example.deepresearch.model.Plan;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author yingzi
 * @date 2025/5/18 16:59
 */

public class ResearchTeamNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResearchTeamNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("ResearchTeam node is running.");
		String nextStep = "planner";
		Map<String, Object> updated = new HashMap<>();

		Optional<Plan> currentPlanOpt = state.value("current_plan", Plan.class);
		if (currentPlanOpt.isEmpty() || !currentPlanOpt.get().getSteps().isEmpty()) {
			updated.put("research_team_next_node", nextStep);
			return updated;
		}

		Plan curPlan = currentPlanOpt.get();
		// 判断steps里的每个step是否都执行完毕
		if (areAllExecutionResultsPresent(curPlan)) {
			updated.put("research_team_next_node", nextStep);
			return updated;
		}

		for (Plan.Step step : curPlan.getSteps()) {
			if (!StringUtils.hasLength(step.getExecutionRes())) {
				if (step.getStepType() == Plan.StepType.RESEARCH) {
					nextStep = "researcher";
					updated.put("research_team_next_node", nextStep);
					return updated;
				}
				else if (step.getStepType() == Plan.StepType.PROCESSING) {
					nextStep = "coder";
					updated.put("research_team_next_node", nextStep);
					return updated;
				}
			}
		}
		updated.put("research_team_next_node", nextStep);
		return updated;
	}

	public boolean areAllExecutionResultsPresent(Plan plan) {
		if (plan.getSteps() == null || plan.getSteps().isEmpty()) {
			return false;
		}

		return plan.getSteps().stream().allMatch(step -> StringUtils.hasLength(step.getExecutionRes()));
	}

}
