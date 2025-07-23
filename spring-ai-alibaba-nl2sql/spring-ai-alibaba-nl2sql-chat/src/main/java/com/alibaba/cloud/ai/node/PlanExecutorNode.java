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

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 计划执行与验证节点，根据计划决定下一个执行的节点，并在执行前进行验证。
 *
 * @author zhangshenghang
 */
public class PlanExecutorNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(PlanExecutorNode.class);

	// Supported node types
	private static final Set<String> SUPPORTED_NODES = Set.of(SQL_EXECUTE_NODE, PYTHON_EXECUTE_NODE,
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

			logger.info("Plan validation successful.");

		}
		catch (Exception e) {
			logger.error("Plan validation failed due to a parsing error.", e);
			return buildValidationResult(state, false,
					"Validation failed: The plan is not a valid JSON structure. Error: " + e.getMessage());
		}

		// 2. Execute the Plan if validation passes
		Plan plan = getPlan(state);
		Integer currentStep = getCurrentStepNumber(state);
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();

		// Check if the plan is completed
		if (currentStep > executionPlan.size()) {
			logger.info("Plan completed, current step: {}, total steps: {}", currentStep, executionPlan.size());
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
