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
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 计划实体类，用于管理执行计划的相关信息
 */
public class ExecutionPlan extends AbstractExecutionPlan {
	

	private List<ExecutionStep> steps;

	/**
	 * 计划类型，用于 Jackson 多态反序列化
	 */
	private String planType = "simple";

	/**
	 * 默认构造函数 - Jackson 反序列化需要
	 */
	public ExecutionPlan() {
		super();
		this.steps = new ArrayList<>();
	}

	public ExecutionPlan(String planId, String title) {
		super(planId, title);
		this.steps = new ArrayList<>();
	}

	@JsonIgnore
	public String getPlanType() {
		return planType;
	}

	public void setPlanType(String planType) {
		this.planType = planType;
	}

	// ExecutionPlan 特有的方法

	public List<ExecutionStep> getSteps() {
		return steps;
	}

	public void setSteps(List<ExecutionStep> steps) {
		this.steps = steps;
	}

	@JsonIgnore
	public int getStepCount() {
		return steps.size();
	}

	// AbstractExecutionPlan 抽象方法的实现

	@Override
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		return new ArrayList<>(steps);
	}

	@Override
	@JsonIgnore
	public int getTotalStepCount() {
		return getStepCount();
	}

	@Override
	public void addStep(ExecutionStep step) {
		this.steps.add(step);
	}

	@Override
	public void removeStep(ExecutionStep step) {
		this.steps.remove(step);
	}

	@Override
	@JsonIgnore
	public boolean isEmpty() {
		return steps.isEmpty();
	}

	@Override
	protected void clearSteps() {
		steps.clear();
	}

	// state.append("全局目标 (全局目标只是一个方向性指导，你在当前请求内不需要完成全局目标，只需要关注当前正在执行的步骤即可): ")
	// .append("\n")
	// .append(title)
	// .append("\n");
	@Override
	@JsonIgnore
	public String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder state = new StringBuilder();

		state.append("- 用户原始需求 (这个需求是用户最初的输入，信息可以参考，但当前交互轮次中只需要完成当前步骤要求即可!) :\n");
		state.append(title).append("\n");
		if (getUserRequest() != null && !getUserRequest().isEmpty()) {
			state.append("").append(getUserRequest()).append("\n\n");
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
	@JsonIgnore
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
	@JsonIgnore
	public String getStepsExecutionStateStringFormat() {
		return getStepsExecutionStateStringFormat(false);
	}

}
