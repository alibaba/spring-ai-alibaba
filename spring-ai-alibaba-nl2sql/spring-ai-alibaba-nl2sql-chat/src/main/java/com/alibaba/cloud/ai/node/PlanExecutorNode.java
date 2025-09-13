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
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.model.execution.Plan;
import com.alibaba.cloud.ai.util.StateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alibaba.cloud.ai.constant.Constant.IS_ONLY_NL2SQL;
import static com.alibaba.cloud.ai.constant.Constant.HUMAN_REVIEW_ENABLED;
import static com.alibaba.cloud.ai.constant.Constant.ONLY_NL2SQL_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_CURRENT_STEP;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_NEXT_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_REPAIR_COUNT;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_VALIDATION_ERROR;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_VALIDATION_STATUS;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_GENERATE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.REPORT_GENERATOR_NODE;
import static com.alibaba.cloud.ai.constant.Constant.SQL_EXECUTE_NODE;

/**
 * Plan execution and validation node, decides next execution node based on plan, and
 * validates before execution.
 *
 * @author zhangshenghang
 */
public class PlanExecutorNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(PlanExecutorNode.class);

	// Supported node types
	private static final Set<String> SUPPORTED_NODES = Set.of(SQL_EXECUTE_NODE, PYTHON_GENERATE_NODE,
			REPORT_GENERATOR_NODE);

	private final BeanOutputConverter<Plan> converter;

	public PlanExecutorNode() {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		// 1. Validate the Plan
		String plannerOutput = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT);
		try {
			Plan plan = converter.convert(plannerOutput);

			if (plan == null || plan.getExecutionPlan() == null || plan.getExecutionPlan().isEmpty()) {
				return buildValidationResult(state, false,
						"Validation failed: The generated plan is empty or has no execution steps.");
			}

			for (ExecutionStep step : plan.getExecutionPlan()) {
				if (step.getToolToUse() == null || !SUPPORTED_NODES.contains(step.getToolToUse())) {
					return buildValidationResult(state, false,
							"Validation failed: Plan contains an invalid tool name: '" + step.getToolToUse()
									+ "' in step " + step.getStep());
				}
				if (step.getToolParameters() == null) {
					return buildValidationResult(state, false,
							"Validation failed: Tool parameters are missing for step " + step.getStep());
				}
			}

			// NL2SQL模式只能有一个计划且为SQL_EXECUTE_NODE（用于判断生成的SQL是否正确）
			Boolean onlyNl2sql = state.value(IS_ONLY_NL2SQL, false);
			if (onlyNl2sql && (plan.getExecutionPlan().size() != 1
					|| !SQL_EXECUTE_NODE.equals(plan.getExecutionPlan().get(0).getToolToUse()))) {
				return buildValidationResult(state, false,
						"Validation failed: The generated plan is not fit with prompt.");
			}

			logger.info("Plan validation successful.");

		}
		catch (Exception e) {
			logger.error("Plan validation failed due to a parsing error.", e);
			return buildValidationResult(state, false,
					"Validation failed: The plan is not a valid JSON structure. Error: " + e.getMessage());
		}

		// 2. If开启人工复核，则在执行前暂停，跳转到human_feedback节点
		Boolean humanReviewEnabled = state.value(HUMAN_REVIEW_ENABLED, false);
		if (Boolean.TRUE.equals(humanReviewEnabled)) {
			logger.info("Human review enabled: routing to human_feedback node");
			return Map.of(PLAN_VALIDATION_STATUS, true, PLAN_NEXT_NODE, "human_feedback");
		}

		Plan plan = getPlan(state);
		Integer currentStep = getCurrentStepNumber(state);
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();

		// Check if the plan is completed
		if (currentStep > executionPlan.size()) {
			logger.info("Plan completed, current step: {}, total steps: {}", currentStep, executionPlan.size());
			// 如果为nl2sql模式，则将结果保存，直接走向END
			Boolean onlyNl2sql = state.value(IS_ONLY_NL2SQL, false);
			if (onlyNl2sql) {
				String resultSql = executionPlan.get(0).getToolParameters().getSqlQuery();
				logger.info("Nl2sql Result: {}", resultSql);
				return Map.of(PLAN_CURRENT_STEP, 1, PLAN_NEXT_NODE, StateGraph.END, PLAN_VALIDATION_STATUS, true,
						ONLY_NL2SQL_OUTPUT, resultSql);
			}
			return Map.of(PLAN_CURRENT_STEP, 1, PLAN_NEXT_NODE, REPORT_GENERATOR_NODE, PLAN_VALIDATION_STATUS, true);
		}

		// Get current step and determine next node
		ExecutionStep executionStep = executionPlan.get(currentStep - 1);
		String toolToUse = executionStep.getToolToUse();

		return determineNextNode(toolToUse);
	}

	/**
	 * Determine the next node to execute
	 */
	private Map<String, Object> determineNextNode(String toolToUse) {
		if (SUPPORTED_NODES.contains(toolToUse)) {
			logger.info("Determined next execution node: {}", toolToUse);
			return Map.of(PLAN_NEXT_NODE, toolToUse, PLAN_VALIDATION_STATUS, true);
		}
		else if ("human_feedback".equals(toolToUse)) {
			logger.info("Determined next execution node: {}", toolToUse);
			return Map.of(PLAN_NEXT_NODE, toolToUse, PLAN_VALIDATION_STATUS, true);
		}
		else {
			// This case should ideally not be reached if validation is done correctly
			// before.
			return Map.of(PLAN_VALIDATION_STATUS, false, PLAN_VALIDATION_ERROR, "Unsupported node type: " + toolToUse);
		}
	}

	private Map<String, Object> buildValidationResult(OverAllState state, boolean isValid, String errorMessage) {
		if (isValid) {
			return Map.of(PLAN_VALIDATION_STATUS, true);
		}
		else {
			// When validation fails, increment the repair count here.
			int repairCount = StateUtils.getObjectValue(state, PLAN_REPAIR_COUNT, Integer.class, 0);
			return Map.of(PLAN_VALIDATION_STATUS, false, PLAN_VALIDATION_ERROR, errorMessage, PLAN_REPAIR_COUNT,
					repairCount + 1);
		}
	}

}
