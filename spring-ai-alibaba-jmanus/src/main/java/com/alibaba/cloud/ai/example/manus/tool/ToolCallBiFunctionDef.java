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

import java.util.function.BiFunction;

import org.springframework.ai.chat.model.ToolContext;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

/**
 * Tool 定义的接口，提供统一的工具定义方法
 *
 * @param <I> 工具输入类型
 */
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {

	/**
	 * 获取工具组的名称
	 * @return 返回工具的唯一标识名称
	 */
	String getServiceGroup();

	/**
	 * 获取工具的名称
	 * @return 返回工具的唯一标识名称
	 */
	String getName();

	/**
	 * 获取工具的描述信息
	 * @return 返回工具的功能描述
	 */
	String getDescription();

	/**
	 * 获取工具的参数定义 schema
	 * @return 返回JSON格式的参数定义架构
	 */
	String getParameters();

	/**
	 * 获取工具的输入类型
	 * @return 返回工具接受的输入参数类型Class
	 */
	Class<I> getInputType();

	/**
	 * 判断工具是否直接返回结果
	 * @return 如果工具直接返回结果返回true，否则返回false
	 */
	boolean isReturnDirect();

	/**
	 * 设置关联的Agent实例
	 * @param agent 要关联的BaseAgent实例
	 */
	public void setPlanId(String planId);

	/**
	 * 获取工具当前的状态字符串
	 * @return 返回描述工具当前状态的字符串
	 */
	String getCurrentToolStateString();

	/**
	 * 清理指定 planId 的所有相关资源
	 * @param planId 计划ID
	 */
	void cleanup(String planId);

}
