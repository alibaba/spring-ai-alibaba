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

import java.util.HashMap;
import java.util.Map;

/**
 * 执行上下文类，用于在计划的创建、执行和总结过程中传递和维护状态信息。 该类作为计划执行流程中的核心数据载体，在
 * {@link com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator}
 * 的各个阶段之间传递。
 *
 * 主要职责： - 存储计划ID和计划实体信息 - 保存用户原始请求 - 维护计划执行状态 - 存储执行结果摘要 - 控制是否需要生成执行总结
 *
 * @see com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan
 * @see com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator
 */
public class ExecutionContext {

	private Map<String, String> toolsContext = new HashMap<>();

	/**
	 * 工具上下文，存储工具执行的上下文信息
	 */
	/** 计划的唯一标识符 */
	private String planId;

	/** 执行计划实体，包含计划的详细信息和执行步骤 */
	private ExecutionPlan plan;

	/** 用户的原始请求内容 */
	private String userRequest;

	/** 计划执行完成后的结果摘要 */
	private String resultSummary;

	/** 是否需要为执行结果调用大模型生成摘要，true是调用大模型，false是不调用直接输出结果 */
	private boolean needSummary;

	/** 计划执行是否成功的标志 */
	private boolean success = false;

	/** 是否使用记忆， 场景是 如果只构建计划，那么不应该用记忆，否则记忆无法删除 */
	private boolean useMemory = false;

	/**
	 * 获取计划ID
	 * @return 计划的唯一标识符
	 */
	public String getPlanId() {
		return planId;
	}

	/**
	 * 设置计划ID
	 * @param planId 计划的唯一标识符
	 */
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	/**
	 * 获取执行计划实体
	 * @return 执行计划实体对象
	 */
	public ExecutionPlan getPlan() {
		return plan;
	}

	/**
	 * 设置执行计划实体
	 * @param plan 执行计划实体对象
	 */
	public void setPlan(ExecutionPlan plan) {
		this.plan = plan;
	}

	/**
	 * 检查是否需要生成执行结果摘要
	 * @return 如果需要生成摘要返回true，否则返回false
	 */
	public boolean isNeedSummary() {
		return needSummary;
	}

	/**
	 * 设置是否需要生成执行结果摘要
	 * @param needSummary 是否需要生成摘要的标志
	 */
	public void setNeedSummary(boolean needSummary) {
		this.needSummary = needSummary;
	}

	/**
	 * 检查计划执行是否成功
	 * @return 如果执行成功返回true，否则返回false
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * 设置计划执行的成功状态
	 * @param success 执行成功状态的标志
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Map<String, String> getToolsContext() {
		return toolsContext;
	}

	public void setToolsContext(Map<String, String> toolsContext) {
		this.toolsContext = toolsContext;
	}

	public void addToolContext(String toolsKey, String value) {
		this.toolsContext.put(toolsKey, value);
	}

	/**
	 * 获取用户的原始请求内容
	 * @return 用户请求的字符串
	 */
	public String getUserRequest() {
		return userRequest;
	}

	/**
	 * 设置用户的原始请求内容
	 * @param userRequest 用户请求的字符串
	 */
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}

	/**
	 * 获取执行结果摘要
	 * @return 执行结果的摘要说明
	 */
	public String getResultSummary() {
		return resultSummary;
	}

	/**
	 * 设置执行结果摘要
	 * @param resultSummary 执行结果的摘要说明
	 */
	public void setResultSummary(String resultSummary) {
		this.resultSummary = resultSummary;
	}

	/**
	 * 使用另一个ExecutionContext实例的内容更新当前实例
	 * <p>
	 * 此方法会复制传入context的计划实体、用户请求和结果摘要到当前实例
	 * @param context 源执行上下文实例
	 */
	public void updateContext(ExecutionContext context) {
		this.plan = context.getPlan();
		this.userRequest = context.getUserRequest();
		this.resultSummary = context.getResultSummary();
	}

	public boolean isUseMemory() {
		return useMemory;
	}

	public void setUseMemory(boolean useMemory) {
		this.useMemory = useMemory;
	}

}
