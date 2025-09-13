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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.util.StateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Human feedback node for plan review and modification.
 *
 * @author zhangshenghang
 */
public class HumanFeedbackNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(HumanFeedbackNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Human feedback node is running.");

		Map<String, Object> updated = new HashMap<>();

		// 检查最大修复次数
		int repairCount = StateUtils.getObjectValue(state, PLAN_REPAIR_COUNT, Integer.class, 0);
		int maxRepairAttempts = 3; // 最大修复次数

		if (repairCount >= maxRepairAttempts) {
			logger.info("Maximum repair attempts exceeded, repairCount: {}, maxRepairAttempts: {}", repairCount,
					maxRepairAttempts);
			updated.put("human_next_node", "END");
			return updated;
		}

		// 获取计划内容并输出给前端
		String planContent = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT, "");
		if (StringUtils.hasLength(planContent)) {
			logger.info("Human feedback node: plan content available for review");
			// 这里可以添加计划内容的输出，让前端能够获取到
		}

		// 获取人类反馈数据
		OverAllState.HumanFeedback humanFeedback = state.humanFeedback();
		if (humanFeedback == null) {
			logger.info("Human feedback not available yet, waiting for user input");
			// 如果还没有人类反馈数据，返回等待状态
			updated.put("human_next_node", "WAIT_FOR_FEEDBACK");
			return updated;
		}

		Map<String, Object> feedbackData = humanFeedback.data();
		boolean feedback = (boolean) feedbackData.getOrDefault("feed_back", true);

		if (!feedback) {
			// 用户拒绝了计划，需要重新生成
			logger.info("Human feedback: plan rejected, routing back to planner");
			updated.put("human_next_node", PLANNER_NODE);

			// 增加修复次数
			updated.put(PLAN_REPAIR_COUNT, repairCount + 1);

			// 获取用户建议
			String feedbackContent = feedbackData.getOrDefault("feed_back_content", "").toString();
			if (StringUtils.hasLength(feedbackContent)) {
				updated.put(PLAN_VALIDATION_ERROR, feedbackContent);
				logger.info("Human feedback content: {}", feedbackContent);
			}
			else {
				updated.put(PLAN_VALIDATION_ERROR, "User rejected the plan. Please revise according to suggestions.");
			}

			// 清除恢复标志，让PlannerNode重新生成计划
			state.withoutResume();
		}
		else {
			// 用户通过了计划，继续执行
			logger.info("Human feedback: plan approved, continuing execution");
			updated.put("human_next_node", "PLAN_EXECUTOR_NODE");
			// 清除人工复核标志，避免再次进入人工复核循环
			updated.put(HUMAN_REVIEW_ENABLED, false);
		}

		logger.info("Human feedback node -> {} node", updated.get("human_next_node"));
		return updated;
	}

}
