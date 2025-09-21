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
package com.alibaba.cloud.ai.manus.tool.mapreduce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MapReduce tool shared state manager for managing shared state information between
 * different Agent instances, ensuring MapReduce process consistency
 */
@Component
public class MapReduceSharedStateManager implements IMapReduceSharedStateManager {

	private static final Logger log = LoggerFactory.getLogger(MapReduceSharedStateManager.class);

	/**
	 * Plan state information Key: planId, Value: PlanState
	 */
	private final Map<String, PlanState> planStates = new ConcurrentHashMap<>();

	/**
	 * Plan state inner class containing all shared state information for a single plan
	 */
	public static class PlanState {

		// Map task status management
		private final Map<String, TaskStatus> mapTaskStatuses = new ConcurrentHashMap<>();

		// Task counter for generating task IDs
		private final AtomicInteger taskCounter = new AtomicInteger(1);

		// Split results list
		private final List<String> splitResults = Collections.synchronizedList(new ArrayList<>());

		// Last operation result
		private volatile String lastOperationResult = "";

		// Last processed file
		private volatile String lastProcessedFile = "";

		// Creation timestamp
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
	 * Task status class
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
	 * Get or create plan state
	 * @param planId Plan ID
	 * @return Plan state
	 */
	public PlanState getOrCreatePlanState(String planId) {
		if (planId == null || planId.trim().isEmpty()) {
			throw new IllegalArgumentException("planId cannot be empty");
		}

		return planStates.computeIfAbsent(planId, id -> {
			log.info("Creating new shared state for plan {}", id);
			return new PlanState();
		});
	}

	/**
	 * Get plan state (return null if not exists)
	 * @param planId Plan ID
	 * @return Plan state, return null if not exists
	 */
	public PlanState getPlanState(String planId) {
		return planStates.get(planId);
	}

	/**
	 * Clean up plan state
	 * @param planId Plan ID
	 */
	public void cleanupPlanState(String planId) {
		PlanState removed = planStates.remove(planId);
		if (removed != null) {
			log.info("Cleaned up shared state for plan {}", planId);
		}
	}

	/**
	 * Get next task ID
	 * @param planId Plan ID
	 * @return Task ID
	 */
	public String getNextTaskId(String planId) {
		PlanState planState = getOrCreatePlanState(planId);
		int taskNumber = planState.getTaskCounter().getAndIncrement();
		return String.format("task_%03d", taskNumber);
	}

	/**
	 * Add split result
	 * @param planId Plan ID
	 * @param taskDirectory Task directory
	 */
	public void addSplitResult(String planId, String taskDirectory) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.getSplitResults().add(taskDirectory);
		log.debug("Added split result for plan {}: {}", planId, taskDirectory);
	}

	/**
	 * Get split result list
	 * @param planId Plan ID
	 * @return Copy of split result list
	 */
	public List<String> getSplitResults(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(planState.getSplitResults());
	}

	/**
	 * Set split result list
	 * @param planId Plan ID
	 * @param splitResults Split result list
	 */
	public void setSplitResults(String planId, List<String> splitResults) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.getSplitResults().clear();
		planState.getSplitResults().addAll(splitResults);
		log.info("Set split results for plan {}, total {} tasks", planId, splitResults.size());
	}

	/**
	 * Record Map task status
	 * @param planId Plan ID
	 * @param taskId Task ID
	 * @param taskStatus Task status
	 */
	public void recordMapTaskStatus(String planId, String taskId, TaskStatus taskStatus) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.getMapTaskStatuses().put(taskId, taskStatus);
		log.debug("Recorded task {} status for plan {}: {}", taskId, planId, taskStatus.status);
	}

	/**
	 * Get Map task status
	 * @param planId Plan ID
	 * @param taskId Task ID
	 * @return Task status
	 */
	public TaskStatus getMapTaskStatus(String planId, String taskId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return null;
		}
		return planState.getMapTaskStatuses().get(taskId);
	}

	/**
	 * Get all Map task statuses
	 * @param planId Plan ID
	 * @return Copy of task status mapping
	 */
	public Map<String, TaskStatus> getAllMapTaskStatuses(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return new HashMap<>();
		}
		return new HashMap<>(planState.getMapTaskStatuses());
	}

	/**
	 * Set last operation result
	 * @param planId Plan ID
	 * @param result Operation result
	 */
	public void setLastOperationResult(String planId, String result) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.setLastOperationResult(result);
	}

	/**
	 * Get last operation result
	 * @param planId Plan ID
	 * @return Last operation result
	 */
	public String getLastOperationResult(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return "";
		}
		return planState.getLastOperationResult();
	}

	/**
	 * Set last processed file
	 * @param planId Plan ID
	 * @param filePath File path
	 */
	public void setLastProcessedFile(String planId, String filePath) {
		PlanState planState = getOrCreatePlanState(planId);
		planState.setLastProcessedFile(filePath);
	}

	/**
	 * Get last processed file
	 * @param planId Plan ID
	 * @return Last processed file path
	 */
	public String getLastProcessedFile(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return "";
		}
		return planState.getLastProcessedFile();
	}

	/**
	 * Get current tool status string
	 * @param planId Plan ID
	 * @return Status string
	 */
	public String getCurrentToolStateString(String planId) {
		PlanState planState = getPlanState(planId);
		if (planState == null) {
			return "reduce_operation_tool current status:\n- Plan ID: " + planId + " (status does not exist)\n";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("reduce_operation_tool current status:\n");
		sb.append("- Plan ID: ").append(planId).append("\n");
		sb.append("- Last processed file: ")
			.append(planState.getLastProcessedFile().isEmpty() ? "None" : planState.getLastProcessedFile())
			.append("\n");
		sb.append("- Last operation result: ")
			.append(planState.getLastOperationResult().isEmpty() ? "None"
					: "Completed: " + planState.getLastOperationResult())
			.append("\n");
		return sb.toString();
	}

	/**
	 * Get status overview of all plans
	 * @return Status overview string
	 */
	public String getAllPlansOverview() {
		StringBuilder sb = new StringBuilder();
		sb.append("MapReduce Shared State Manager Overview:\n");
		sb.append("- Active plan count: ").append(planStates.size()).append("\n");

		for (Map.Entry<String, PlanState> entry : planStates.entrySet()) {
			String planId = entry.getKey();
			PlanState planState = entry.getValue();
			sb.append("  - Plan ").append(planId).append(": ");
			sb.append("Task count=").append(planState.getSplitResults().size());
			sb.append(", Status count=").append(planState.getMapTaskStatuses().size());
			sb.append(", Counter=").append(planState.getTaskCounter().get());
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Clean up all plan states
	 */
	public void cleanupAllPlanStates() {
		int count = planStates.size();
		planStates.clear();
		log.info("Cleaned up all plan states, total {} plans", count);
	}

}
