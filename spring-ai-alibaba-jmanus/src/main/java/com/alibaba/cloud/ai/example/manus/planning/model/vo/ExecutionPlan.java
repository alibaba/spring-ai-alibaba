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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;

/**
 * 计划实体类，用于管理执行计划的相关信息
 */
public class ExecutionPlan {

	private String planId;

	private String title;

	private String userRequest;

	private String planningThinking;

	// 使用简单字符串存储执行参数
	private String executionParams;

	private List<ExecutionStep> steps;

	public ExecutionPlan(String planId, String title) {
		this.planId = planId;
		this.title = title;
		this.steps = new ArrayList<>();
		this.executionParams = "";
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

	public List<ExecutionStep> getSteps() {
		return steps;
	}

	public void setSteps(List<ExecutionStep> steps) {
		this.steps = steps;
	}

	public void addStep(ExecutionStep step) {
		this.steps.add(step);
	}

	public void removeStep(ExecutionStep step) {
		this.steps.remove(step);
	}

	public int getStepCount() {
		return steps.size();
	}

	public String getPlanningThinking() {
		return planningThinking;
	}

	public void setPlanningThinking(String planningThinking) {
		this.planningThinking = planningThinking;
	}

	public String getExecutionParams() {
		return executionParams;
	}

	public void setExecutionParams(String executionParams) {
		this.executionParams = executionParams;
	}

	/**
	 * 获取用户请求
	 * @return 用户请求字符串
	 */
	public String getUserRequest() {
		return userRequest;
	}

	/**
	 * 设置用户请求
	 * @param userRequest 用户请求字符串
	 */
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}

	@Override
	public String toString() {
		return "ExecutionPlan{" + "planId='" + planId + '\'' + ", title='" + title + '\'' + ", stepsCount="
				+ (steps != null ? steps.size() : 0) + '}';
	}

	// state.append("全局目标 (全局目标只是一个方向性指导，你在当前请求内不需要完成全局目标，只需要关注当前正在执行的步骤即可): ")
	// .append("\n")
	// .append(title)
	// .append("\n");
	public String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();

		state.append("- 用户原始需求 (这个需求是用户最初的输入，信息可以参考，但当前交互轮次中只需要完成当前步骤要求即可!) :\n");
		state.append(title).append("\n");
		if (userRequest != null && !userRequest.isEmpty()) {
			state.append("").append(userRequest).append("\n\n");
		}
		state.append("\n- 执行参数: ").append("\n");
		if (executionParams != null && !executionParams.isEmpty()) {
			state.append(executionParams).append("\n\n");
		}
		else {
			state.append("未提供执行参数。\n\n");
		}

		state.append("- 历史执行过的步骤记录:\n");
		state.append(getStepsExecutionStateStringFormat(onlyCompletedAndFirstInProgress));

		return state.toString();
	}

	/**
	 * 获取步骤执行状态的字符串格式
	 * @param onlyCompletedAndFirstInProgress 当为true时，只输出所有已完成的步骤和第一个进行中的步骤
	 * @return 格式化的步骤执行状态字符串
	 */
	public String getStepsExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();
		boolean foundInProgress = false;

		for (int i = 0; i < steps.size(); i++) {
			ExecutionStep step = steps.get(i);

			// 如果onlyCompletedAndFirstInProgress为true，则只显示COMPLETED状态的步骤和第一个IN_PROGRESS状态的步骤
			if (onlyCompletedAndFirstInProgress) {
				// 如果是COMPLETED状态，始终显示
				if (step.getStatus() == AgentState.COMPLETED) {
					// 什么都不做，继续显示
				}
				// 如果是IN_PROGRESS状态，且还没找到其他IN_PROGRESS的步骤
				else if (step.getStatus() == AgentState.IN_PROGRESS && !foundInProgress) {
					foundInProgress = true; // 标记已找到IN_PROGRESS步骤
				}
				// 其他所有情况（不是COMPLETED且不是第一个IN_PROGRESS）
				else {
					continue; // 跳过不符合条件的步骤
				}
			}

			String symbol = switch (step.getStatus()) {
				case COMPLETED -> "[completed]";
				case IN_PROGRESS -> "[in_progress]";
				case BLOCKED -> "[blocked]";
				case NOT_STARTED -> "[not_started]";
				default -> "[ ]";
			};

			state.append(i + 1)
				.append(".  **步骤 ")
				.append(i)
				.append(":**\n")
				.append("    *   **状态:** ")
				.append(symbol)
				.append("\n")
				.append("    *   **操作:** ")
				.append(step.getStepRequirement())
				.append("\n");

			String result = step.getResult();
			if (result != null && !result.isEmpty()) {
				state.append("    *   **结果:** ").append(result).append("\n\n");
			}

		}
		return state.toString();
	}

	/**
	 * 获取所有步骤执行状态的字符串格式（兼容旧版本）
	 * @return 格式化的步骤执行状态字符串
	 */
	public String getStepsExecutionStateStringFormat() {
		return getStepsExecutionStateStringFormat(false);
	}

	/**
	 * 将计划转换为JSON字符串
	 * @return 计划的JSON字符串表示
	 */
	public String toJson() {
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"planId\": \"").append(planId).append("\",\n");
		json.append("  \"title\": \"").append(title).append("\",\n");

		// 添加步骤数组
		json.append("  \"steps\": [\n");
		for (int i = 0; i < steps.size(); i++) {
			json.append(steps.get(i).toJson());
			if (i < steps.size() - 1) {
				json.append(",");
			}
			json.append("\n");
		}
		json.append("  ]\n");

		json.append("}");
		return json.toString();
	}

	/**
	 * 从JSON字符串解析并创建ExecutionPlan对象
	 * @param planJson JSON字符串
	 * @param newPlanId 新的计划ID（可选，如果提供将覆盖JSON中的planId）
	 * @return 解析后的ExecutionPlan对象
	 * @throws Exception 如果解析失败则抛出异常
	 */
	public static ExecutionPlan fromJson(String planJson, String newPlanId) throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(planJson);

		// 获取计划标题
		String title = rootNode.has("title") ? rootNode.get("title").asText() : "来自模板的计划";

		// 使用新的计划ID或从JSON中获取
		String planId = (newPlanId != null && !newPlanId.isEmpty()) ? newPlanId
				: (rootNode.has("planId") ? rootNode.get("planId").asText() : "unknown-plan");

		// 创建新的ExecutionPlan对象
		ExecutionPlan plan = new ExecutionPlan(planId, title);

		// 如果有计划步骤，添加到计划中
		if (rootNode.has("steps") && rootNode.get("steps").isArray()) {
			com.fasterxml.jackson.databind.JsonNode stepsNode = rootNode.get("steps");
			int stepIndex = 0;
			for (com.fasterxml.jackson.databind.JsonNode stepNode : stepsNode) {
				if (stepNode.has("stepRequirement")) {
					// 调用ExecutionStep的fromJson方法创建步骤
					ExecutionStep step = ExecutionStep.fromJson(stepNode);
					Integer stepIndexValFromJson = step.getStepIndex();
					if (stepIndexValFromJson != null) {
						stepIndex = stepIndexValFromJson;
					}
					else {
						step.setStepIndex(stepIndex);
					}
					plan.addStep(step);
					stepIndex++;
				}
			}
		}

		return plan;
	}

}
