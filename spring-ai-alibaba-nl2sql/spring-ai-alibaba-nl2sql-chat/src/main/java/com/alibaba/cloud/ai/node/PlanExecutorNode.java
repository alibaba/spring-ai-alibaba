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
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 计划执行节点，根据计划决定下一个执行的节点
 *
 * @author zhangshenghang
 */
public class PlanExecutorNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(PlanExecutorNode.class);

	// 支持的节点类型
	private static final Set<String> SUPPORTED_NODES = Set.of(SQL_EXECUTE_NODE, PYTHON_EXECUTE_NODE,
			REPORT_GENERATOR_NODE);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		Plan plan = getPlan(state);
		Integer currentStep = getCurrentStepNumber(state);
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();

		// 检查计划是否已完成
		if (currentStep > executionPlan.size()) {
			logger.info("计划已完成，当前步骤: {}, 总步骤: {}", currentStep, executionPlan.size());
			return Map.of(PLAN_CURRENT_STEP, 1, PLAN_NEXT_NODE, REPORT_GENERATOR_NODE);
		}

		// 获取当前步骤并确定下一个节点
		ExecutionStep executionStep = executionPlan.get(currentStep - 1);
		String toolToUse = executionStep.getToolToUse();

		return determineNextNode(toolToUse);
	}

	/**
	 * 确定下一个要执行的节点
	 */
	private Map<String, Object> determineNextNode(String toolToUse) {
		if (SUPPORTED_NODES.contains(toolToUse)) {
			logger.info("确定下一个执行节点: {}", toolToUse);
			return Map.of(PLAN_NEXT_NODE, toolToUse);
		}
		else {
			throw new IllegalArgumentException("不支持的节点类型: " + toolToUse + ", 支持的节点类型: " + SUPPORTED_NODES);
		}
	}

}
