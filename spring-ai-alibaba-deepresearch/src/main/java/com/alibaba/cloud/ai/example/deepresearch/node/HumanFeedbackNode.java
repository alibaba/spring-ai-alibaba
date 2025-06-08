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
import com.alibaba.cloud.ai.graph.exception.GraphInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author yingzi
 * @since 2025/5/18 16:54
 */

public class HumanFeedbackNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(HumanFeedbackNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("HumanFeedback node is running.");
		String nextStep = "research_team";
		Map<String, Object> updated = new HashMap<>();

		// auto_accepted、yes、no 迭代次数都+1
		Integer planIterations = state.value("plan_iterations", 0);
		planIterations += 1;
		updated.put("plan_iterations", planIterations);

		boolean autoAcceptedPlan = state.value("auto_accepted_plan", false);

		if (!autoAcceptedPlan) {
			// todo 这里改为接口形式
			logger.info("Do you accept the plan? [y/n]：");
			interrupt(state);

			Map<String, Object> feedBackData = state.humanFeedback().data();
			String feedback = feedBackData.getOrDefault("feed_back", "n").toString();

			if (StringUtils.hasLength(feedback) && "n".equals(feedback)) {
				nextStep = "llm_stream";
				updated.put("human_next_node", nextStep);

				String feedbackContent = feedBackData.getOrDefault("feed_back_content", "").toString();
				updated.put("feed_back_content", feedbackContent);

				logger.info("Human feedback content: {}", feedbackContent);
				state.withoutResume();
				return updated;
			}
			else if (StringUtils.hasLength(feedback) && "y".equals(feedback)) {
				logger.info("Plan is accepted by user.");
			}
			else {
				throw new Exception(String.format("Interrupt value of %s is not supported", feedback));
			}
		}

		Plan currentPlan = state.value("current_plan", Plan.class).get();
		if (currentPlan.isHasEnoughContext()) {
			nextStep = "reporter";
		}

		updated.put("human_next_node", nextStep);
		return updated;
	}

	private void interrupt(OverAllState state) throws GraphInterruptException {
		if (state.humanFeedback() == null || !state.isResume()) {
			throw new GraphInterruptException("interrupt");
		}
	}

}
