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

import java.util.List;

import static com.alibaba.cloud.ai.constant.Constant.PLAN_CURRENT_STEP;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE_OUTPUT;

/**
 * 基于计划执行的节点抽象基类
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
	 * 获取当前执行步骤
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
	 * 获取计划对象
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
	 * 获取当前步骤号
	 */
	protected Integer getCurrentStepNumber(OverAllState state) {
		return state.value(PLAN_CURRENT_STEP, 1);
	}

	/**
	 * 记录节点进入日志
	 */
	protected void logNodeEntry() {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());
	}

	/**
	 * 记录节点输出日志
	 */
	protected void logNodeOutput(String outputKey, Object output) {
		logger.info("{} 节点输出 {}：{}", this.getClass().getSimpleName(), outputKey, output);
	}

}
