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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;

/**
 * 执行计划通用接口 定义了所有执行计划类型共同的基本操作
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "planType",
		defaultImpl = ExecutionPlan.class // 默认实现为 ExecutionPlan
)
@JsonSubTypes({ @JsonSubTypes.Type(value = ExecutionPlan.class, name = "simple"),
		@JsonSubTypes.Type(value = MapReduceExecutionPlan.class, name = "advanced") })
public interface PlanInterface {

	/**
	 * 获取计划ID
	 * @return 计划ID
	 */
	String getRootPlanId();

	/**
	 * 设置计划ID
	 * @param rootPlanId 计划ID
	 */
	void setRootPlanId(String rootPlanId);


	/**
	 * 获取当前计划ID
	 * @return 当前计划ID
	 */
	String getCurrentPlanId();

	/**
	 * 设置当前计划ID
	 * @param currentPlanId 当前计划ID
	 */
	void setCurrentPlanId(String currentPlanId);

	/**
	 * 获取计划类型
	 * @return 计划类型
	 */
	String getPlanType();

	/**
	 * 设置计划类型
	 * @param planType 计划类型
	 */
	void setPlanType(String planType);

	/**
	 * 获取计划标题
	 * @return 计划标题
	 */
	String getTitle();

	/**
	 * 设置计划标题
	 * @param title 计划标题
	 */
	void setTitle(String title);

	/**
	 * 获取规划思考过程
	 * @return 规划思考过程
	 */
	String getPlanningThinking();

	/**
	 * 设置规划思考过程
	 * @param planningThinking 规划思考过程
	 */
	void setPlanningThinking(String planningThinking);

	/**
	 * 获取执行参数
	 * @return 执行参数
	 */
	String getExecutionParams();

	/**
	 * 设置执行参数
	 * @param executionParams 执行参数
	 */
	void setExecutionParams(String executionParams);

	/**
	 * 获取所有执行步骤的扁平列表
	 * @return 所有执行步骤
	 */
	List<ExecutionStep> getAllSteps();

	/**
	 * 获取总步骤数量
	 * @return 总步骤数
	 */
	int getTotalStepCount();

	public String getUserRequest();

	/**
	 * 设置用户请求
	 * @param userRequest 用户请求
	 */
	void setUserRequest(String userRequest);

	/**
	 * 添加执行步骤
	 * @param step 执行步骤
	 */
	void addStep(ExecutionStep step);

	/**
	 * 移除执行步骤
	 * @param step 执行步骤
	 */
	void removeStep(ExecutionStep step);

	/**
	 * 检查计划是否为空
	 * @return 如果计划为空则返回true
	 */
	boolean isEmpty();

	/**
	 * 清空计划内容
	 */
	void clear();

	/**
	 * 获取计划执行状态的字符串格式
	 * @param onlyCompletedAndFirstInProgress 当为true时，只输出所有已完成的步骤和第一个进行中的步骤
	 * @return 计划状态字符串
	 */
	String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress);

	/**
	 * 更新所有步骤的索引，从0开始递增
	 */
	default void updateStepIndices() {
		List<ExecutionStep> allSteps = getAllSteps();
		for (int i = 0; i < allSteps.size(); i++) {
			allSteps.get(i).setStepIndex(i);
		}
	}

}
