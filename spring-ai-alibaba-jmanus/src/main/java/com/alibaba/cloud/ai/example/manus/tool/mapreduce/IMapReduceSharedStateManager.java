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
package com.alibaba.cloud.ai.example.manus.tool.mapreduce;

import java.util.List;
import java.util.Map;

/**
 * MapReduce工具共享状态管理器接口，用于管理不同Agent实例之间的共享状态信息
 */
public interface IMapReduceSharedStateManager {

	/**
	 * 获取或创建计划状态
	 * @param planId 计划ID
	 * @return 计划状态
	 */
	MapReduceSharedStateManager.PlanState getOrCreatePlanState(String planId);

	/**
	 * 获取计划状态
	 * @param planId 计划ID
	 * @return 计划状态
	 */
	MapReduceSharedStateManager.PlanState getPlanState(String planId);

	/**
	 * 清理计划状态
	 * @param planId 计划ID
	 */
	void cleanupPlanState(String planId);

	/**
	 * 获取下一个任务ID
	 * @param planId 计划ID
	 * @return 下一个任务ID
	 */
	String getNextTaskId(String planId);

	/**
	 * 添加分割结果
	 * @param planId 计划ID
	 * @param taskDirectory 任务目录
	 */
	void addSplitResult(String planId, String taskDirectory);

	/**
	 * 获取分割结果
	 * @param planId 计划ID
	 * @return 分割结果列表
	 */
	List<String> getSplitResults(String planId);

	/**
	 * 设置分割结果
	 * @param planId 计划ID
	 * @param splitResults 分割结果列表
	 */
	void setSplitResults(String planId, List<String> splitResults);

	/**
	 * 记录Map任务状态
	 * @param planId 计划ID
	 * @param taskId 任务ID
	 * @param taskStatus 任务状态
	 */
	void recordMapTaskStatus(String planId, String taskId, MapReduceSharedStateManager.TaskStatus taskStatus);

	/**
	 * 获取Map任务状态
	 * @param planId 计划ID
	 * @param taskId 任务ID
	 * @return 任务状态
	 */
	MapReduceSharedStateManager.TaskStatus getMapTaskStatus(String planId, String taskId);

	/**
	 * 获取所有Map任务状态
	 * @param planId 计划ID
	 * @return 所有任务状态
	 */
	Map<String, MapReduceSharedStateManager.TaskStatus> getAllMapTaskStatuses(String planId);

	/**
	 * 设置最后操作结果
	 * @param planId 计划ID
	 * @param result 操作结果
	 */
	void setLastOperationResult(String planId, String result);

	/**
	 * 获取最后操作结果
	 * @param planId 计划ID
	 * @return 最后操作结果
	 */
	String getLastOperationResult(String planId);

	/**
	 * 设置最后处理的文件
	 * @param planId 计划ID
	 * @param filePath 文件路径
	 */
	void setLastProcessedFile(String planId, String filePath);

	/**
	 * 获取最后处理的文件
	 * @param planId 计划ID
	 * @return 最后处理的文件路径
	 */
	String getLastProcessedFile(String planId);

	/**
	 * 获取当前工具状态字符串
	 * @param planId 计划ID
	 * @return 当前工具状态字符串
	 */
	String getCurrentToolStateString(String planId);

	/**
	 * 获取所有计划概览
	 * @return 所有计划概览字符串
	 */
	String getAllPlansOverview();

	/**
	 * 清理所有计划状态
	 */
	void cleanupAllPlanStates();

}
