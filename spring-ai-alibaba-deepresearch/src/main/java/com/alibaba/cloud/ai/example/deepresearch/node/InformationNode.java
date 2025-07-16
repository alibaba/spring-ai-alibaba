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
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * @author yingzi
 * @since 2025/5/18 15:52
 */

public class InformationNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(InformationNode.class);

	private final BeanOutputConverter<Plan> converter;

	public InformationNode() {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) {
		String result = state.value("planner_content", "");
		logger.info("planner_content: {}", result);
		assert Strings.isBlank(result);

		String nextStep = "reporter";
		Map<String, Object> updated = new HashMap<>();
		Plan curPlan = null;
		try {
			curPlan = converter.convert(result);
			logger.info("反序列成功，convert: {}", curPlan);
			// 2.1 反序列化成功，上下文充足，跳转reporter节点
			if (curPlan.isHasEnoughContext()) {
				logger.info("Information has enough context.");
				updated.put("current_plan", curPlan);
				updated.put("information_next_node", nextStep);
				logger.info("information node -> {} node", nextStep);
				return updated;
			}
		}
		catch (Exception e) {
			// 2.2 反序列化失败，尝试重新生成计划
			logger.error("反序列化失败");
			if (StateUtil.getPlanIterations(state) < StateUtil.getPlanMaxIterations(state)) {
				// 尝试重新生成计划
				updated.put("plan_iterations", StateUtil.getPlanIterations(state) + 1);
				nextStep = "planner";
				updated.put("information_next_node", nextStep);
				logger.info("information node -> {} node", nextStep);
				return updated;
			}
			else {
				nextStep = END;
				updated.put("information_next_node", nextStep);
				logger.warn("information node -> {} node", nextStep);
				return updated;
			}
		}
		// 2.3 上下文不足，跳转到human_feedback节点
		if (!StateUtil.getAutoAcceptedPlan(state)) {
			nextStep = "human_feedback";
		}
		else {
			nextStep = "research_team";
			updated.put("plan_iterations", StateUtil.getPlanIterations(state) + 1);
		}
		updated.put("current_plan", curPlan);
		updated.put("information_next_node", nextStep);
		logger.info("information node -> {} node", nextStep);
		return updated;
	}

}
