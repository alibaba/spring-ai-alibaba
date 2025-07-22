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
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.model.execution.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

import static com.alibaba.cloud.ai.constant.Constant.PLAN_CURRENT_STEP;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE_OUTPUT;

/**
 * Abstract base class for plan-based execution nodes Provides common functionality for
 * nodes that execute based on predefined plans
 *
 * @author zhangshenghang
 */
public abstract class AbstractPlanBasedNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPlanBasedNode.class);

	private final BeanOutputConverter<Plan> converter;

	protected AbstractPlanBasedNode() {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
		});
	}

	/**
	 * Get the current execution step from the plan
	 * @param state the overall state containing plan information
	 * @return the current execution step
	 * @throws IllegalStateException if plan output is empty, plan parsing fails, or step
	 * index is out of range
	 */
	protected ExecutionStep getCurrentExecutionStep(OverAllState state) {
		String plannerNodeOutput = (String) state.value(PLANNER_NODE_OUTPUT)
			.orElseThrow(() -> new IllegalStateException("计划节点输出为空"));

		Plan plan = converter.convert(plannerNodeOutput);
		if (plan == null) {
			throw new IllegalStateException("计划解析失败");
		}

		Integer currentStep = state.value(PLAN_CURRENT_STEP, 1);

		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		if (executionPlan == null || executionPlan.isEmpty()) {
			throw new IllegalStateException("执行计划为空");
		}

		int stepIndex = currentStep - 1;
		if (stepIndex < 0 || stepIndex >= executionPlan.size()) {
			throw new IllegalStateException("当前步骤索引超出范围: " + stepIndex);
		}

		return executionPlan.get(stepIndex);
	}

	/**
	 * Get the plan object from state
	 * @param state the overall state containing plan information
	 * @return the parsed plan object
	 * @throws IllegalStateException if plan output is empty or plan parsing fails
	 */
	protected Plan getPlan(OverAllState state) {
		String plannerNodeOutput = (String) state.value(PLANNER_NODE_OUTPUT)
			.orElseThrow(() -> new IllegalStateException("计划节点输出为空"));
		Plan plan = converter.convert(plannerNodeOutput);
		if (plan == null) {
			throw new IllegalStateException("计划解析失败");
		}
		return plan;
	}

	/**
	 * Get the current step number from state
	 * @param state the overall state
	 * @return the current step number (defaults to 1 if not set)
	 */
	protected Integer getCurrentStepNumber(OverAllState state) {
		return state.value(PLAN_CURRENT_STEP, 1);
	}

	/**
	 * Log node entry
	 */
	protected void logNodeEntry() {
		logger.info("Entering {} node", this.getClass().getSimpleName());
	}

	/**
	 * Log node output
	 */
	protected void logNodeOutput(String outputKey, Object output) {
		logger.info("{} node output {}: {}", this.getClass().getSimpleName(), outputKey, output);
	}

}
