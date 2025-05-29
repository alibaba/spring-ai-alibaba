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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.function.Function;

public class PlanningTool implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(PlanningTool.class);

	private ExecutionPlan currentPlan;

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
			        "plan_id": {
			            "description": "Unique identifier for the plan",
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
			        },
			        "step_index": {
			            "description": "Index of step to update",
			            "type": "integer"
			        },
			        "step_status": {
			            "description": "Status to set for step",
			            "enum": ["not_started", "in_progress", "completed", "blocked"],
			            "type": "string"
			        },
			        "step_notes": {
			            "description": "Additional notes for step",
			            "type": "string"
			        }
			    },
			    "required": ["command"]
			}
			""";

	private static final String name = "planning";

	private static final String description = "Planning tool for managing tasks ";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	// Parameterized FunctionToolCallback with appropriate types.

	public FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, this)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public ToolExecuteResult run(String toolInput) {
		try {
			Map<String, Object> input = objectMapper.readValue(toolInput, new TypeReference<Map<String, Object>>() {
			});
			String command = (String) input.get("command");
			String planId = (String) input.get("plan_id");
			String title = (String) input.get("title");
			List<String> steps = objectMapper.convertValue(input.get("steps"), new TypeReference<List<String>>() {
			});

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
		catch (JsonProcessingException e) {
			log.info("执行计划工具时发生错误", e);
			return new ToolExecuteResult("Error executing planning tool: " + e.getMessage());
		}
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
		if (planId == null || title == null || steps == null || steps.isEmpty()) {
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

	// public ToolExecuteResult updatePlan(String planId, String title, List<String>
	// steps) {
	// if (planId == null) {
	// log.info("更新计划时缺少planId");
	// return new ToolExecuteResult("plan_id required");
	// }

	// if (currentPlan == null || !currentPlan.getPlanId().equals(planId)) {
	// return new ToolExecuteResult("Plan not found: " + planId);
	// }

	// if (title != null) {
	// currentPlan.setTitle(title);
	// }

	// if (steps != null) {
	// List<ExecutionStep> oldSteps = new ArrayList<>(currentPlan.getSteps());
	// currentPlan.setSteps(new ArrayList<>());
	// for (String step : steps) {
	// currentPlan.addStep(createExecutionStep(step, currentPlan.getStepCount() + 1));
	// }
	// //TODO 以后这里要优化的，目前update不会替代原有的步骤的执行结果，这个本身有点问题，但目前阶段可以不管，因为目前只用到create ，其他都没有用。
	// // // 保持原有步骤的状态和备注
	// // for (int i = 0; i < Math.min(oldSteps.size(), steps.size()); i++) {
	// // if (oldSteps.get(i).getStepRequirement().equals(steps.get(i))) {
	// // currentPlan.getSteps().get(i).setStatus(oldSteps.get(i).getStatus());
	// // currentPlan.getSteps().get(i).setResult();(oldSteps.get(i).getResult());
	// // } else {
	// // currentPlan.getSteps().get(i).setStatus(PlanStepStatus.NOT_STARTED);
	// // currentPlan.getSteps().get(i).setNotes(null);
	// // }
	// // }
	// }

	// return new ToolExecuteResult("Plan updated: " + planId + "\n" +
	// formatPlan(currentPlan));
	// }

	// public ToolExecuteResult getPlan(String planId) {
	// if (currentPlan == null) {
	// log.info("没有活动的计划");
	// return new ToolExecuteResult("No active plan");
	// }

	// if (planId != null && !currentPlan.getPlanId().equals(planId)) {
	// return new ToolExecuteResult("Plan not found: " + planId);
	// }

	// return new ToolExecuteResult(formatPlan(currentPlan));
	// }

	// public ToolExecuteResult markStep(String planId, Integer stepIndex, String
	// stepStatus, String stepNotes) {
	// if (currentPlan == null) {
	// return new ToolExecuteResult("No active plan");
	// }

	// if (planId != null && !currentPlan.getPlanId().equals(planId)) {
	// return new ToolExecuteResult("Plan not found: " + planId);
	// }

	// if (stepIndex == null || stepIndex < 0 || stepIndex >= currentPlan.getStepCount())
	// {
	// log.info("无效的步骤索引: {}, 总步骤数: {}", stepIndex, currentPlan.getStepCount());
	// return new ToolExecuteResult("Invalid step index");
	// }

	// currentPlan.updateStep(stepIndex, stepStatus, stepNotes);
	// return new ToolExecuteResult("Step " + stepIndex + " updated\n" +
	// formatPlan(currentPlan));
	// }

	// public ToolExecuteResult deletePlan(String planId) {
	// if (currentPlan == null) {
	// log.info("没有活动的计划");
	// return new ToolExecuteResult("No active plan");
	// }

	// if (planId != null && !currentPlan.getPlanId().equals(planId)) {
	// return new ToolExecuteResult("Plan not found: " + planId);
	// }

	// currentPlan = null;
	// return new ToolExecuteResult("Plan deleted: " + planId);
	// }

	// private String formatPlan(ExecutionPlan plan) {
	// StringBuilder out = new StringBuilder();
	// out.append("Plan: ").append(plan.getTitle()).append(" (ID:
	// ").append(plan.getPlanId()).append(")\n");
	// out.append("=".repeat(out.length())).append("\n\n");

	// long completed = plan.getStepCount();
	// int total = plan.getStepCount();
	// double progress = total > 0 ? (completed * 100.0 / total) : 0;

	// out.append(String.format("Progress: %d/%d steps (%.1f%%)\n\n", completed, total,
	// progress));

	// List<String> steps = plan.getSteps();
	// List<String> statuses = plan.getStepStatuses();
	// List<String> notes = plan.getStepNotes();

	// for (int i = 0; i < steps.size(); i++) {
	// String status = statuses.get(i);
	// String symbol = switch(status) {
	// case "completed" -> "[completed]";
	// case "in_progress" -> "[in_progress]";
	// case "blocked" -> "[blocked]";
	// default -> "[ ]";
	// };

	// out.append(i).append(". ").append(symbol).append("
	// ").append(steps.get(i)).append("\n");
	// String note = notes.get(i);
	// if (note != null && !note.isEmpty()) {
	// out.append(" Notes: ").append(note).append("\n");
	// }
	// }
	// return out.toString();
	// }

	@Override
	public ToolExecuteResult apply(String input) {
		return run(input);
	}

}
