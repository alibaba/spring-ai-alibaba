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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/18 16:54
 */

public class HumanFeedbackNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("HumanFeedback node is running.");
		String nextStep = "research_team";
		Map<String, Object> updated = new HashMap<>();

		boolean autoAcceptedPlan = state.value("auto_accepted_plan", false);
		if (!autoAcceptedPlan) {
			// todo 这里改为接口形式，中断，然后从走resume复原逻辑
			logger.info("Do you accept the plan? [y/n]：");
			String feedback = state.value("feed_back", "y");
			if (StringUtils.hasLength(feedback) && "n".equals(feedback)) {
				String feedbackContent = state.value("feed_back_content").orElse("").toString();

				nextStep = "planner";
				updated.put("human_next_node", nextStep);
				updated.put("feed_back_content", List.of(new UserMessage(feedbackContent)));
				logger.info("Human feedback content: {}", feedbackContent);
				return updated;
			}
			else if (StringUtils.hasLength(feedback) && feedback.startsWith("y")) {
				logger.info("Plan is accepted by user.");
			}
			else {
				throw new Exception(String.format("Interrupt value of %s is not supported", feedback));
			}
		}

		Integer planIterations = state.value("plan_iterations", 0);
		planIterations += 1;
		Plan currentPlan = state.value("current_plan", Plan.class).get();
		if (currentPlan.isHasEnoughContext()) {
			nextStep = "reporter";
		}

		updated.put("plan_iterations", planIterations);
		updated.put("human_next_node", nextStep);
		return updated;
	}

}
