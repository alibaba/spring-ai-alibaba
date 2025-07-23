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
package com.alibaba.cloud.ai.example.manus.planning;

import java.util.Map;

import org.springframework.web.client.RestClient;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.ToolCallbackProvider;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;

/**
 * 规划工厂接口，提供规划相关的对象创建功能
 */
public interface IPlanningFactory {

	/**
	 * 创建规划协调器
	 * @param planId 计划ID
	 * @return 规划协调器
	 */
	PlanningCoordinator createPlanningCoordinator(String planId);

	/**
	 * 创建工具回调映射
	 * @param planId 计划ID
	 * @param rootPlanId 根计划ID
	 * @param terminateColumns 终止列
	 * @return 工具回调映射
	 */
	Map<String, PlanningFactory.ToolCallBackContext> toolCallbackMap(String planId, String rootPlanId,
			java.util.List<String> terminateColumns);

	/**
	 * 创建RestClient
	 * @return RestClient构建器
	 */
	RestClient.Builder createRestClient();

	/**
	 * 创建空的工具回调提供者
	 * @return 工具回调提供者函数式接口
	 */
	ToolCallbackProvider emptyToolCallbackProvider();

}
