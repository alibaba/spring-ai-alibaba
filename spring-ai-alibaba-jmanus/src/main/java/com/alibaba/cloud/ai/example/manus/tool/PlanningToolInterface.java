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

import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * 规划工具的通用接口，定义了所有规划工具的基本行为
 */
public interface PlanningToolInterface {

	/**
	 * 获取当前计划的ID
	 * @return 当前计划的ID，如果没有计划则返回null
	 */
	String getCurrentPlanId();

	/**
	 * 获取当前的执行计划
	 * @return 当前的执行计划，如果没有计划则返回null
	 */
	PlanInterface getCurrentPlan();

	/**
	 * 获取函数工具回调，用于与LLM集成
	 * @return FunctionToolCallback实例
	 */
	FunctionToolCallback<?, ToolExecuteResult> getFunctionToolCallback();

	/**
	 * 执行工具输入并返回结果
	 * @param input 工具输入字符串
	 * @return 工具执行结果
	 */
	ToolExecuteResult apply(String input);

}
