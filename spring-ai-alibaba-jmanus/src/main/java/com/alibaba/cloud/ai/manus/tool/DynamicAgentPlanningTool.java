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
package com.alibaba.cloud.ai.manus.tool;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.DynamicAgentExecutionPlan;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class DynamicAgentPlanningTool extends AbstractBaseTool<DynamicAgentPlanningTool.DynamicAgentPlanningInput>
		implements PlanningToolInterface {

	private static final Logger log = LoggerFactory.getLogger(DynamicAgentPlanningTool.class);

	private DynamicAgentExecutionPlan currentPlan;

	public DynamicAgentPlanningTool() {
	}

	/**
	 * Step definition class for dynamic agent planning
	 */
	public static class StepDefinition {

		private String stepRequirement;

		private String agentName;

		private String modelName;

		private List<String> selectedToolKeys;

		public StepDefinition() {
		}

		public StepDefinition(String stepRequirement, String agentName, String modelName,
				List<String> selectedToolKeys) {
			this.stepRequirement = stepRequirement;
			this.agentName = agentName;
			this.modelName = modelName;
			this.selectedToolKeys = selectedToolKeys;
		}

		public String getStepRequirement() {
			return stepRequirement;
		}

		public void setStepRequirement(String stepRequirement) {
			this.stepRequirement = stepRequirement;
		}

		public String getAgentName() {
			return agentName;
		}

		public void setAgentName(String agentName) {
			this.agentName = agentName;
		}

		public String getModelName() {
			return modelName;
		}

		public void setModelName(String modelName) {
			this.modelName = modelName;
		}

		public List<String> getSelectedToolKeys() {
			return selectedToolKeys;
		}

		public void setSelectedToolKeys(List<String> selectedToolKeys) {
			this.selectedToolKeys = selectedToolKeys;
		}

	}

	/**
	 * Internal input class for defining dynamic agent planning tool input parameters
	 */
	public static class DynamicAgentPlanningInput {

		private String title;

		private StepDefinition step;

		private String terminateColumns;

		private boolean directResponse = false;

		public DynamicAgentPlanningInput() {
		}

		public DynamicAgentPlanningInput(String title, StepDefinition step, boolean directResponse) {
			this.title = title;
			this.step = step;
			this.terminateColumns = null;
			this.directResponse = directResponse;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public StepDefinition getStep() {
			return step;
		}

		public void setStep(StepDefinition step) {
			this.step = step;
		}

		public String getTerminateColumns() {
			return terminateColumns;
		}

		public void setTerminateColumns(String terminateColumns) {
			this.terminateColumns = terminateColumns;
		}

		public boolean isDirectResponse() {
			return directResponse;
		}

		public void setDirectResponse(boolean directResponse) {
			this.directResponse = directResponse;
		}

	}

	public String getCurrentPlanId() {
		return currentPlan != null ? currentPlan.getCurrentPlanId() : null;
	}

	public void setCurrentPlanId(String planId) {
		if (currentPlan != null) {
			currentPlan.setCurrentPlanId(planId);
		}
	}

	public DynamicAgentExecutionPlan getCurrentPlan() {
		return currentPlan;
	}

	private static final String PARAMETERS = """
			{
			 "type": "object",
			 "properties": {
			  "title": {
			   "description": "Title for the dynamic agent plan",
			   "type": "string"
			  },
			  "step": {
			   "description": "Single plan step for dynamic agent execution",
			   "type": "object",
			   "properties": {
				   "stepRequirement": {
					   "description": "Description of what this step should accomplish",
					   "type": "string"
				   },
				   "modelName": {
					   "description": "Model name to use for this step (optional)",
					   "type": "string"
				   },
					"selectedToolKeys": {
						"description": "List of selected tool keys for dynamic agent execution",
						"type": "array",
						"items": {
							"type": "string"
						}
					}
			   },
			   "required": ["stepRequirement"]
			  },
			  "terminateColumns": {
				   "description": "Terminate structure output columns for all steps (optional, will be applied to every step)",
				   "type": "string"
			  }
			 },
			 "required": [
			"title",
			"step"
			 ]
			}
			""";

	private static final String name = "dynamic_agent_planning";

	private static final String description = "Dynamic agent planning tool for managing dynamic agent execution plans";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	// Parameterized FunctionToolCallback with appropriate types.
	public static FunctionToolCallback<DynamicAgentPlanningInput, ToolExecuteResult> getFunctionToolCallback(
			DynamicAgentPlanningTool toolInstance) {
		return FunctionToolCallback.builder(name, toolInstance)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(DynamicAgentPlanningInput.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	/**
	 * Build FunctionToolCallback with common configuration
	 * @param toolInstance The tool instance to use
	 * @return Configured FunctionToolCallback
	 */
	private FunctionToolCallback<DynamicAgentPlanningInput, ToolExecuteResult> buildFunctionToolCallback(
			PlanningToolInterface toolInstance) {
		return FunctionToolCallback
			.<DynamicAgentPlanningInput, ToolExecuteResult>builder(name, (DynamicAgentPlanningInput input) -> {
				if (toolInstance instanceof DynamicAgentPlanningTool) {
					return ((DynamicAgentPlanningTool) toolInstance).run(input);
				}
				throw new UnsupportedOperationException("Tool instance type not supported");
			})
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(DynamicAgentPlanningInput.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	@Override
	public ToolExecuteResult run(DynamicAgentPlanningInput input) {
		String title = input.getTitle();
		StepDefinition step = input.getStep();
		boolean directResponse = input.isDirectResponse();
		// Support directResponse mode
		if (directResponse) {
			log.info("Direct response mode enabled for dynamic agent");
			DynamicAgentExecutionPlan plan = new DynamicAgentExecutionPlan();
			plan.setTitle(title);
			plan.setDirectResponse(true);
			plan.setUserRequest(title); // Here title is the user request content

			this.currentPlan = plan;
			return new ToolExecuteResult("Direct response mode: dynamic agent plan created successfully");
		}

		// Convert single step to list for internal processing
		List<StepDefinition> steps = step != null ? List.of(step) : new ArrayList<>();
		return createDynamicAgentPlan(title, steps, input.getTerminateColumns());
	}

	/**
	 * Create a single execution step
	 * @param step step definition
	 * @param index step index
	 * @return created ExecutionStep instance
	 */
	private ExecutionStep createExecutionStep(StepDefinition step, int index) {
		ExecutionStep executionStep = new ExecutionStep();
		executionStep.setStepRequirement(step.getStepRequirement());
		executionStep.setAgentName(step.getAgentName());
		executionStep.setModelName(step.getModelName());
		executionStep.setSelectedToolKeys(step.getSelectedToolKeys());
		executionStep.setStepIndex(index);
		return executionStep;
	}

	public ToolExecuteResult createDynamicAgentPlan(String title, List<StepDefinition> steps, String terminateColumns) {
		if (title == null || steps == null || steps.isEmpty()) {
			log.info("Missing required parameters when creating dynamic agent plan: title={}, steps={}", title, steps);
			return new ToolExecuteResult("Required parameters missing");
		}

		DynamicAgentExecutionPlan plan = new DynamicAgentExecutionPlan();
		plan.setTitle(title);

		int index = 0;
		for (StepDefinition step : steps) {
			ExecutionStep execStep = createExecutionStep(step, index);
			if (terminateColumns != null && !terminateColumns.isEmpty()) {
				execStep.setTerminateColumns(terminateColumns);
			}
			plan.addStep(execStep);
			index++;
		}

		this.currentPlan = plan;
		return new ToolExecuteResult(
				"Dynamic agent plan created successfully\n" + plan.getPlanExecutionStateStringFormat(false));
	}

	// ToolCallBiFunctionDef interface methods
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<DynamicAgentPlanningInput> getInputType() {
		return DynamicAgentPlanningInput.class;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public String getCurrentToolStateString() {
		if (currentPlan != null) {
			return "Current dynamic agent plan: " + currentPlan.getPlanExecutionStateStringFormat(false);
		}
		return "No active dynamic agent plan";
	}

	@Override
	public void cleanup(String planId) {
		// Implementation can be added if needed
	}

	@Override
	public String getServiceGroup() {
		return "dynamic-agent-service-group";
	}

	// PlanningToolInterface methods
	@Override
	public FunctionToolCallback<DynamicAgentPlanningInput, ToolExecuteResult> getFunctionToolCallback() {
		return buildFunctionToolCallback(this);
	}

	@Override
	public FunctionToolCallback<DynamicAgentPlanningInput, ToolExecuteResult> getFunctionToolCallback(
			PlanningToolInterface planningToolInterface) {
		return buildFunctionToolCallback(planningToolInterface);
	}

}
