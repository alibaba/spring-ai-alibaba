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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 执行计划抽象基类 包含所有执行计划类型的共同属性和基本实现
 */
public abstract class AbstractExecutionPlan implements PlanInterface {

	protected String currentPlanId;

	protected String rootPlanId;

	/**
	 * 计划标题
	 */
	protected String title;

	/**
	 * 规划思考过程
	 */
	@JsonIgnore
	protected String planningThinking;

	/**
	 * 执行参数
	 */
	@JsonIgnore
	protected String executionParams;

	private String userRequest;

	/**
	 * 默认构造函数
	 */
	public AbstractExecutionPlan() {
		this.executionParams = "";
	}

	/**
	 * 带参数的构造函数
	 * @param planId 计划ID
	 * @param title 计划标题
	 */
	public AbstractExecutionPlan(String currentPlanId, String rootPlanId, String title) {
		this();
		this.currentPlanId = currentPlanId;
		this.rootPlanId = rootPlanId;
		this.title = title;
	}

	// PlanInterface 基本属性的实现

	@Override
	public String getCurrentPlanId() {
		return currentPlanId;
	}

	@Override
	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
	}

	@Override
	public String getRootPlanId() {
		return rootPlanId;
	}

	@Override
	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	public void setPlanId(String planId) {
		this.rootPlanId = planId;
	}

	public String getPlanId() {
		return rootPlanId;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getPlanningThinking() {
		return planningThinking;
	}

	@Override
	public void setPlanningThinking(String planningThinking) {
		this.planningThinking = planningThinking;
	}

	@Override
	public String getExecutionParams() {
		return executionParams;
	}

	@Override
	public void setExecutionParams(String executionParams) {
		this.executionParams = executionParams != null ? executionParams : "";
	}

	// 抽象方法 - 子类必须实现

	/**
	 * 获取所有执行步骤的扁平列表
	 * @return 所有执行步骤
	 */
	@Override
	public abstract List<ExecutionStep> getAllSteps();

	/**
	 * 获取总步骤数量
	 * @return 总步骤数
	 */
	@Override
	public abstract int getTotalStepCount();

	/**
	 * 添加执行步骤
	 * @param step 执行步骤
	 */
	@Override
	public abstract void addStep(ExecutionStep step);

	/**
	 * 移除执行步骤
	 * @param step 执行步骤
	 */
	@Override
	public abstract void removeStep(ExecutionStep step);

	/**
	 * 检查计划是否为空
	 * @return 如果计划为空则返回true
	 */
	@Override
	public abstract boolean isEmpty();

	/**
	 * 获取计划执行状态的字符串格式
	 * @param onlyCompletedAndFirstInProgress 当为true时，只输出所有已完成的步骤和第一个进行中的步骤
	 * @return 计划状态字符串
	 */
	@Override
	public abstract String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress);

	// 通用实现方法

	@Override
	public void clear() {
		clearSteps();
		planningThinking = null;
		executionParams = "";
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

	/**
	 * 清空步骤的抽象方法 子类需要实现具体的步骤清空逻辑
	 */
	protected abstract void clearSteps();

	@Override
	public String toString() {
		return "AbstractExecutionPlan{" + "rootPlanId='" + rootPlanId + '\'' + ", currentPlanId='" + currentPlanId
				+ '\'' + ", title='" + title + '\'' + ", planningThinking='" + planningThinking + '\''
				+ ", executionParams='" + executionParams + '\'' + ", userRequest='" + userRequest + '\'' + '}';
	}

}
