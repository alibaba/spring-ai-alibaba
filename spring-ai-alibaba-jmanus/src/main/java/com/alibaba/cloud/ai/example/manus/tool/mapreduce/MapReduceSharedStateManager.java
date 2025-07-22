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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MapReduce工具共享状态管理器 用于管理不同Agent实例之间的共享状态信息，确保MapReduce流程的一致性
 */
@Component
public class MapReduceSharedStateManager implements IMapReduceSharedStateManager {

	private static final Logger log = LoggerFactory.getLogger(MapReduceSharedStateManager.class);

	/**
	 * 计划状态信息 Key: planId, Value: PlanState
	 */
	private final Map<String, PlanState> planStates = new ConcurrentHashMap<>();

	/**
	 * 计划状态内部类 包含单个计划的所有共享状态信息
	 */
	public static class PlanState {

		// Map任务状态管理
		private final Map<String, TaskStatus> mapTaskStatuses = new ConcurrentHashMap<>();

		// 任务计数器，用于生成任务ID
		private final AtomicInteger taskCounter = new AtomicInteger(1);

		// 分割结果列表
		private final List<String> splitResults = Collections.synchronizedList(new ArrayList<>());

		// 最后操作结果
		private volatile String lastOperationResult = "";

		// 最后处理的文件
		private volatile String lastProcessedFile = "";

		// 创建时间戳
		private final long createTime = System.currentTimeMillis();

		public Map<String, TaskStatus> getMapTaskStatuses() {
			return mapTaskStatuses;
		}

		public AtomicInteger getTaskCounter() {
			return taskCounter;
		}

		public List<String> getSplitResults() {
			return splitResults;
		}

		public String getLastOperationResult() {
			return lastOperationResult;
		}

		public void setLastOperationResult(String lastOperationResult) {
			this.lastOperationResult = lastOperationResult;
		}

		public String getLastProcessedFile() {
			return lastProcessedFile;
		}

		public void setLastProcessedFile(String lastProcessedFile) {
			this.lastProcessedFile = lastProcessedFile;
		}

		public long getCreateTime() {
			return createTime;
		}

	}

	/**
	 * 任务状态类
	 */
	public static class TaskStatus {

		public String taskId;

		public String inputFile;

		public String outputFilePath;

		public String status;

		public String timestamp;

		public TaskStatus() {
		}

		public TaskStatus(String taskId, String status) {
			this.taskId = taskId;
			this.status = status;
		}

	}

	/**
	 * 获取或创建计划状态
	 * @param planId 计划ID
	 * @return 计划状态
	 */
	public PlanState getOrCreatePlanState(String planId) {
		if (planId == null || planId.trim().isEmpty()) {
			throw new IllegalArgumentException("planId不能为空");
		}

		return planStates.computeIfAbsent(planId, id -> {
			log.info("为计划 {} 创建新的共享状态", id);
			return new PlanState();
		});
	}

	/**
	 * 获取计划状态（如果不存在则返回null）
	 * @param planId 计划ID
	 * @return 计划状态，不存在则返回null
	 */
	public PlanState getPlanState(String planId) {
		return planStates.get(planId);
	}

	/**
	 * 清理计划状态
	 * @param planId 计划ID
	 */
	public void cleanupPlanState(String planId) {
		PlanState removed = planStates.remove(planId);
		if (removed != null) {
			log.info("已清理计划 {} 的共享状态", planId);
		}
	}

	/**
	 * 获取下一个任务ID
	 * @param planId 计划ID
	 * @return 任务ID
	 */
	public String getNextTaskId(String planId) {
		PlanState planState = getOrCreatePlanState(planId);
		int taskNumber = planState.getTaskCounter().getAndIncrement();
		return String.format("task_%03d", taskNumber);
	}

	/**
	 * 添加分割结果
	 * @param planId 计划ID
	 * @param taskDirectory 任务目录
	 */
	public void addSplitResult(String planId, String taskDirectory) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.getSplitResults().add(taskDirectory);
		log.debug("为计划 {} 添加分割结果: {}", planId, taskDirectory);
	}

	/**
	 * 获取分割结果列表
	 * @param planId 计划ID
	 * @return 分割结果列表的副本
	 */
	public List<String> getSplitResults(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(planState.getSplitResults());
	}

	/**
	 * 设置分割结果列表
	 * @param planId 计划ID
	 * @param splitResults 分割结果列表
	 */
	public void setSplitResults(String planId, List<String> splitResults) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.getSplitResults().clear();
		planState.getSplitResults().addAll(splitResults);
		log.info("为计划 {} 设置分割结果，共 {} 个任务", planId, splitResults.size());
	}

	/**
	 * 记录Map任务状态
	 * @param planId 计划ID
	 * @param taskId 任务ID
	 * @param taskStatus 任务状态
	 */
	public void recordMapTaskStatus(String planId, String taskId, TaskStatus taskStatus) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.getMapTaskStatuses().put(taskId, taskStatus);
		log.debug("为计划 {} 记录任务 {} 状态: {}", planId, taskId, taskStatus.status);
	}

	/**
	 * 获取Map任务状态
	 * @param planId 计划ID
	 * @param taskId 任务ID
	 * @return 任务状态
	 */
	public TaskStatus getMapTaskStatus(String planId, String taskId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return null;
		}
		return planState.getMapTaskStatuses().get(taskId);
	}

	/**
	 * 获取所有Map任务状态
	 * @param planId 计划ID
	 * @return 任务状态映射的副本
	 */
	public Map<String, TaskStatus> getAllMapTaskStatuses(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return new HashMap<>();
		}
		return new HashMap<>(planState.getMapTaskStatuses());
	}

	/**
	 * 设置最后操作结果
	 * @param planId 计划ID
	 * @param result 操作结果
	 */
	public void setLastOperationResult(String planId, String result) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.setLastOperationResult(result);
	}

	/**
	 * 获取最后操作结果
	 * @param planId 计划ID
	 * @return 最后操作结果
	 */
	public String getLastOperationResult(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return "";
		}
		return planState.getLastOperationResult();
	}

	/**
	 * 设置最后处理的文件
	 * @param planId 计划ID
	 * @param filePath 文件路径
	 */
	public void setLastProcessedFile(String planId, String filePath) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.setLastProcessedFile(filePath);
	}

	/**
	 * 获取最后处理的文件
	 * @param planId 计划ID
	 * @return 最后处理的文件路径
	 */
	public String getLastProcessedFile(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return "";
		}
		return planState.getLastProcessedFile();
	}

	/**
	 * 获取当前工具状态字符串
	 * @param planId 计划ID
	 * @return 状态字符串
	 */
	public String getCurrentToolStateString(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return "reduce_operation_tool 当前状态:\n- Plan ID: " + planId + " (状态不存在)\n";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("reduce_operation_tool 当前状态:\n");
		sb.append("- Plan ID: ").append(planId).append("\n");
		sb.append("- 最后处理文件: ")
			.append(planState.getLastProcessedFile().isEmpty() ? "无" : planState.getLastProcessedFile())
			.append("\n");
		sb.append("- 最后操作结果: ")
			.append(planState.getLastOperationResult().isEmpty() ? "无" : "已完成: " + planState.getLastOperationResult())
			.append("\n");
		return sb.toString();
	}

	/**
	 * 获取所有计划的状态概览
	 * @return 状态概览字符串
	 */
	public String getAllPlansOverview() {
		StringBuilder sb = new StringBuilder();
		sb.append("MapReduce共享状态管理器概览:\n");
		sb.append("- 活跃计划数: ").append(planStates.size()).append("\n");

		for (Map.Entry<String, PlanState> entry : planStates.entrySet()) {
			String planId = entry.getKey();
			PlanState planState = entry.getValue();
			sb.append("  - 计划 ").append(planId).append(": ");
			sb.append("任务数=").append(planState.getSplitResults().size());
			sb.append(", 状态数=").append(planState.getMapTaskStatuses().size());
			sb.append(", 计数器=").append(planState.getTaskCounter().get());
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 清理所有计划状态
	 */
	public void cleanupAllPlanStates() {
		int count = planStates.size();
		planStates.clear();
		log.info("已清理所有计划状态，共 {} 个计划", count);
	}

}
