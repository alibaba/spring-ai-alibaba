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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.chat.model.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class PlanningTool implements ToolCallBiFunctionDef<PlanningTool.PlanningInput> {

	private static final Logger log = LoggerFactory.getLogger(PlanningTool.class);

	private ExecutionPlan currentPlan;

	/**
	 * 内部输入类，用于定义规划工具的输入参数
	 */
	public static class PlanningInput {

		private String command;

		private String planId;

		private String title;

		private List<String> steps;

		public PlanningInput() {
		}

		public PlanningInput(String command, String planId, String title, List<String> steps) {
			this.command = command;
			this.planId = planId;
			this.title = title;
			this.steps = steps;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public String getPlanId() {
			return planId;
		}

		public void setPlanId(String planId) {
			this.planId = planId;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<String> getSteps() {
			return steps;
		}

		public void setSteps(List<String> steps) {
			this.steps = steps;
		}

	}

	public String getCurrentPlanId() {
		return currentPlan != null ? currentPlan.getPlanId() : null;
	}

	public ExecutionPlan getCurrentPlan() {
		return currentPlan;
	}

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "command": {
			            "description": "create a execution plan , Available commands: create",
			            "enum": [
			                "create"
			            ],
			            "type": "string"
			        },
			        "title": {
			            "description": "Title for the plan",
			            "type": "string"
			        },
			        "steps": {
			            "description": "List of plan steps",
			            "type": "array",
			            "items": {
			                "type": "string"
			            }
			        }
			    },
			    "required": [
			    	"command",
			    	"title",
			    	"steps"
			    ]
			}
			""";

	private static final String name = "planning";

	private static final String description = "Planning tool for managing tasks ";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	// Parameterized FunctionToolCallback with appropriate types.
	public static FunctionToolCallback<?, ToolExecuteResult> getFunctionToolCallback(PlanningTool toolInstance) {
		return FunctionToolCallback.builder(name, toolInstance)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(PlanningInput.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	public ToolExecuteResult run(PlanningInput input) {
		String command = input.getCommand();
		String planId = input.getPlanId();
		String title = input.getTitle();
		List<String> steps = input.getSteps();

		return switch (command) {
			case "create" -> createPlan(planId, title, steps);
			// case "update" -> updatePlan(planId, title, steps);
			// case "get" -> getPlan(planId);
			// case "mark_step" -> markStep(planId, stepIndex, stepStatus, stepNotes);
			// case "delete" -> deletePlan(planId);
			default -> {
				log.info("收到无效的命令: {}", command);
				throw new IllegalArgumentException("Invalid command: " + command);
			}
		};
	}

	/**
	 * 创建单个执行步骤
	 * @param step 步骤描述
	 * @param index 步骤索引
	 * @return 创建的ExecutionStep实例
	 */
	private ExecutionStep createExecutionStep(String step, int index) {
		ExecutionStep executionStep = new ExecutionStep();
		executionStep.setStepIndex(index);
		executionStep.setStepRequirement(step);
		return executionStep;
	}

	public ToolExecuteResult createPlan(String planId, String title, List<String> steps) {
		if (title == null || steps == null || steps.isEmpty()) {
			log.info("创建计划时缺少必要参数: planId={}, title={}, steps={}", planId, title, steps);
			return new ToolExecuteResult("Required parameters missing");
		}

		ExecutionPlan plan = new ExecutionPlan(planId, title);
		// 使用新的createExecutionStep方法创建并添加步骤
		int index = 0;
		for (String step : steps) {
			plan.addStep(createExecutionStep(step, index++));
		}

		this.currentPlan = plan;
		return new ToolExecuteResult("Plan created: " + planId + "\n" + plan.getPlanExecutionStateStringFormat(false));
	}

	// ToolCallBiFunctionDef interface methods
	@Override
	public ToolExecuteResult apply(PlanningInput input, ToolContext toolContext) {
		return run(input);
	}

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
	public Class<PlanningInput> getInputType() {
		return PlanningInput.class;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public void setPlanId(String planId) {
		// Implementation can be added if needed
	}

	@Override
	public String getCurrentToolStateString() {
		if (currentPlan != null) {
			return "Current plan: " + currentPlan.getPlanExecutionStateStringFormat(false);
		}
		return "No active plan";
	}

	@Override
	public void cleanup(String planId) {
		// Implementation can be added if needed
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

}
