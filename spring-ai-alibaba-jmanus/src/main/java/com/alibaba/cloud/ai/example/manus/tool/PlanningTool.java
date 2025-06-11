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

	@Override
	public ToolExecuteResult apply(String input) {
		return run(input);
	}

}
