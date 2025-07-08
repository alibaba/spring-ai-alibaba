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
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * @author zhangshenghang
 */
public class PlanExecutorNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlanExecutorNode.class);

	private final BeanOutputConverter<Plan> converter;

	public PlanExecutorNode() {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());
		String plannerNodeOutput = (String) state.value(PLANNER_NODE_OUTPUT).orElseThrow();
		logger.info("plannerNodeOutput: {}", plannerNodeOutput);

		Map<String, Object> updated = new HashMap<>();
		Plan plan = converter.convert(plannerNodeOutput);
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		Integer planCurrentStep = state.value(PLAN_CURRENT_STEP, 1);
		if (planCurrentStep > executionPlan.size()) {
			logger.info("计划已完成，当前步骤: {}, 总步骤: {}", planCurrentStep, executionPlan.size());
			updated.put(PLAN_CURRENT_STEP, 1);
			updated.put(PLAN_NEXT_NODE, REPORT_GENERATOR_NODE);
			return updated;
		}
		ExecutionStep executionStep = executionPlan.get(planCurrentStep - 1);
		String toolToUse = executionStep.getToolToUse();
		switch (toolToUse) {
			case SQL_EXECUTE_NODE:
				updated.put(PLAN_NEXT_NODE, SQL_EXECUTE_NODE);
				return updated;
			case PYTHON_EXECUTE_NODE:
				updated.put(PLAN_NEXT_NODE, PYTHON_EXECUTE_NODE);
				return updated;
			case REPORT_GENERATOR_NODE:
				updated.put(PLAN_NEXT_NODE, REPORT_GENERATOR_NODE);
				break;
			default:
				throw new RuntimeException("未知的节点: " + toolToUse);
		}
		return updated;
	}

}
