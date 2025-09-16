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
package com.alibaba.cloud.ai.example.manus.task;

import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TaskOrchestrator {

	private static final Logger logger = LoggerFactory.getLogger(TaskOrchestrator.class);

	/**
	 * Default timeout for task execution in milliseconds.
	 */
	@SuppressWarnings("unused")
	private static final long DEFAULT_TASK_TIMEOUT_MS = 30000; // 30 seconds

	/**
	 * Executes a list of stateful tasks in sequence, managing the execution context and
	 * integrating with existing plan execution state visualization.
	 * @param planId The unique identifier of the execution plan
	 * @param tasks The list of tasks to execute in order
	 * @param executionContext The existing execution context for integration
	 * @return The execution result containing success status and context state
	 * @throws IllegalArgumentException if planId is null/empty or tasks is null
	 */
	public ExecutionResult executeTasks(String planId, List<StatefulTask> tasks, ExecutionContext executionContext) {
		validateInputs(planId, tasks);

		logger.info("Starting execution of {} tasks for plan: {}", tasks.size(), planId);

		// Create enhanced JManusExecutionContext with JManus integration
		JManusExecutionContext context = new JManusExecutionContext(planId, executionContext);
		ExecutionResult.Builder resultBuilder = new ExecutionResult.Builder(planId);

		LocalDateTime startTime = LocalDateTime.now();
		AtomicInteger completedTasks = new AtomicInteger(0);

		try {
			// Execute tasks sequentially
			for (int i = 0; i < tasks.size(); i++) {
				StatefulTask task = tasks.get(i);

				logger.debug("Executing task {}/{}: {} for plan: {}", i + 1, tasks.size(), task.getName(), planId);

				TaskExecutionResult taskResult = executeTask(task, context, i + 1, tasks.size());
				resultBuilder.addTaskResult(taskResult);

				if (!taskResult.isSuccess()) {
					logger.error("Task execution failed: {} for plan: {}", task.getName(), planId);
					logCurrentState(context, executionContext);
					return resultBuilder.success(false).error(taskResult.getErrorMessage()).build();
				}

				completedTasks.incrementAndGet();

				// Log state after each task for observability
				logCurrentState(context, executionContext);
			}

			Duration totalDuration = Duration.between(startTime, LocalDateTime.now());
			logger.info("Successfully completed all {} tasks for plan: {} in {} ms", tasks.size(), planId,
					totalDuration.toMillis());

			return resultBuilder.success(true).executionContext(context).build();

		}
		catch (Exception e) {
			logger.error("Unexpected error during task orchestration for plan: {}", planId, e);
			logCurrentState(context, executionContext);
			return resultBuilder.success(false).error("Unexpected orchestration error: " + e.getMessage()).build();
		}
	}

	/**
	 * Executes a single task with retry logic and comprehensive error handling.
	 * @param task The task to execute
	 * @param context The execution context
	 * @param taskNumber The current task number (for logging)
	 * @param totalTasks The total number of tasks (for logging)
	 * @return The task execution result
	 */
	private TaskExecutionResult executeTask(StatefulTask task, JManusExecutionContext context, int taskNumber,
			int totalTasks) {
		String taskName = task.getName();
		LocalDateTime startTime = LocalDateTime.now();

		try {
			// Validate context before execution
			if (!task.validateContext(context)) {
				String error = String.format("Context validation failed for task: %s", taskName);
				logger.warn(error);
				return TaskExecutionResult.failure(taskName, error, null);
			}

			// Execute with retry logic
			TaskExecutionException lastException = null;
			int maxRetries = task.getMaxRetryAttempts();

			for (int attempt = 0; attempt <= maxRetries; attempt++) {
				try {
					if (attempt > 0) {
						logger.info("Retrying task: {} (attempt {}/{})", taskName, attempt, maxRetries);
						Thread.sleep(1000 * attempt); // Exponential backoff
					}

					task.execute(context);

					Duration duration = Duration.between(startTime, LocalDateTime.now());
					logger.debug("Task completed successfully: {} in {} ms", taskName, duration.toMillis());

					return TaskExecutionResult.success(taskName, duration);

				}
				catch (TaskExecutionException e) {
					lastException = e;

					if (!e.isRetryable() || attempt >= maxRetries) {
						break;
					}

					logger.warn("Task failed (attempt {}): {} - {}", attempt + 1, taskName, e.getMessage());
				}
			}

			// All retries exhausted
			String error = String.format("Task failed after %d attempts: %s", maxRetries + 1,
					lastException.getMessage());

			logger.error(error, lastException);
			return TaskExecutionResult.failure(taskName, error, lastException);

		}
		catch (Exception e) {
			String error = String.format("Unexpected error in task: %s", e.getMessage());

			logger.error("Unexpected error executing task: {}", taskName, e);
			return TaskExecutionResult.failure(taskName, error, e);
		}
	}

	/**
	 * Logs the current state of the execution context, integrating with existing plan
	 * execution state visualization functionality.
	 * @param jmanusContext The JManus execution context
	 * @param executionContext The existing execution context for integration
	 */
	private void logCurrentState(JManusExecutionContext jmanusContext, ExecutionContext executionContext) {
		try {
			// Generate state string from JManus context
			String stateString = jmanusContext.getStateString(false);

			// Integrate with existing plan execution state if available
			if (executionContext != null && executionContext.getPlan() != null) {
				PlanInterface plan = executionContext.getPlan();
				String planState = plan.getPlanExecutionStateStringFormat(false);

				logger.debug("=== Combined Execution State ===\n{}\n\n{}", stateString, planState);
			}
			else {
				logger.debug("=== JManus Context State ===\n{}", stateString);
			}

		}
		catch (Exception e) {
			logger.warn("Failed to log current state for plan: {} - {}", jmanusContext.getPlanId(), e.getMessage());
		}
	}

	/**
	 * Validates the input parameters for task execution.
	 * @param planId The plan ID to validate
	 * @param tasks The task list to validate
	 * @throws IllegalArgumentException if inputs are invalid
	 */
	private void validateInputs(String planId, List<StatefulTask> tasks) {
		if (planId == null || planId.trim().isEmpty()) {
			throw new IllegalArgumentException("Plan ID cannot be null or empty");
		}

		if (tasks == null) {
			throw new IllegalArgumentException("Tasks list cannot be null");
		}

		// Check for null tasks
		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i) == null) {
				throw new IllegalArgumentException("Task at index " + i + " is null");
			}
		}
	}

	/**
	 * Result of executing a list of stateful tasks.
	 */
	public static class ExecutionResult {

		private final String planId;

		private final boolean success;

		private final List<TaskExecutionResult> taskResults;

		private final JManusExecutionContext executionContext;

		private final String errorMessage;

		private final LocalDateTime executionTime;

		private ExecutionResult(Builder builder) {
			this.planId = builder.planId;
			this.success = builder.success;
			this.taskResults = Collections.unmodifiableList(new ArrayList<>(builder.taskResults));
			this.executionContext = builder.executionContext;
			this.errorMessage = builder.errorMessage;
			this.executionTime = LocalDateTime.now();
		}

		public String getPlanId() {
			return planId;
		}

		public boolean isSuccess() {
			return success;
		}

		public List<TaskExecutionResult> getTaskResults() {
			return taskResults;
		}

		public JManusExecutionContext getExecutionContext() {
			return executionContext;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public LocalDateTime getExecutionTime() {
			return executionTime;
		}

		public static class Builder {

			private final String planId;

			private boolean success = false;

			private final List<TaskExecutionResult> taskResults = new ArrayList<>();

			private JManusExecutionContext executionContext;

			private String errorMessage;

			public Builder(String planId) {
				this.planId = Objects.requireNonNull(planId);
			}

			public Builder success(boolean success) {
				this.success = success;
				return this;
			}

			public Builder addTaskResult(TaskExecutionResult result) {
				this.taskResults.add(Objects.requireNonNull(result));
				return this;
			}

			public Builder executionContext(JManusExecutionContext context) {
				this.executionContext = context;
				return this;
			}

			public Builder error(String message) {
				this.errorMessage = message;
				return this;
			}

			public ExecutionResult build() {
				return new ExecutionResult(this);
			}

		}

	}

	/**
	 * Result of executing a single stateful task.
	 */
	public static class TaskExecutionResult {

		private final String taskName;

		private final boolean success;

		private final Duration executionDuration;

		private final String errorMessage;

		private final Throwable cause;

		private TaskExecutionResult(String taskName, boolean success, Duration executionDuration, String errorMessage,
				Throwable cause) {
			this.taskName = taskName;
			this.success = success;
			this.executionDuration = executionDuration;
			this.errorMessage = errorMessage;
			this.cause = cause;
		}

		public static TaskExecutionResult success(String taskName, Duration duration) {
			return new TaskExecutionResult(taskName, true, duration, null, null);
		}

		public static TaskExecutionResult failure(String taskName, String error, Throwable cause) {
			return new TaskExecutionResult(taskName, false, null, error, cause);
		}

		public String getTaskName() {
			return taskName;
		}

		public boolean isSuccess() {
			return success;
		}

		public Duration getExecutionDuration() {
			return executionDuration;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public Throwable getCause() {
			return cause;
		}

	}

}
