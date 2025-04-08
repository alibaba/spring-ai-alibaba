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

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.support.PlanBasedLifecycleService;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;

/**
 * Tool 定义的接口，提供统一的工具定义方法
 */
public interface ToolCallBiFunctionDef extends BiFunction<String, ToolContext, ToolExecuteResult>, PlanBasedLifecycleService{

	/**
	 * 获取工具的名称
	 */
	String getName();

	/**
	 * 获取工具的描述信息
	 */
	String getDescription();

	/**
	 * 获取工具的参数定义 schema
	 */
	String getParameters();

	/**
	 * 获取工具的输入类型
	 */
	Class<?> getInputType();

	boolean isReturnDirect();

	public void setAgent(BaseAgent agent);

	String getCurrentToolStateString();

}
