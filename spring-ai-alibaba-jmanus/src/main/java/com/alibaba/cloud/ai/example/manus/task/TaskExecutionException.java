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

public class TaskExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * The name of the task that failed to execute.
	 */
	private final String taskName;

	/**
	 * The plan ID where the task execution failure occurred.
	 */
	private final String planId;

	/**
	 * Whether the task failure is retryable.
	 */
	private final boolean retryable;

	/**
	 * Constructs a new TaskExecutionException with the specified detail message.
	 * @param message The detail message
	 */
	public TaskExecutionException(String message) {
		super(message);
		this.taskName = null;
		this.planId = null;
		this.retryable = false;
	}

	/**
	 * Constructs a new TaskExecutionException with the specified detail message and
	 * cause.
	 * @param message The detail message
	 * @param cause The cause of the exception
	 */
	public TaskExecutionException(String message, Throwable cause) {
		super(message, cause);
		this.taskName = null;
		this.planId = null;
		this.retryable = false;
	}

	/**
	 * Constructs a new TaskExecutionException with detailed task information.
	 * @param taskName The name of the failed task
	 * @param planId The plan ID where the failure occurred
	 * @param message The detail message
	 */
	public TaskExecutionException(String taskName, String planId, String message) {
		super(formatMessage(taskName, planId, message));
		this.taskName = taskName;
		this.planId = planId;
		this.retryable = false;
	}

	/**
	 * Constructs a new TaskExecutionException with detailed task information and cause.
	 * @param taskName The name of the failed task
	 * @param planId The plan ID where the failure occurred
	 * @param message The detail message
	 * @param cause The cause of the exception
	 */
	public TaskExecutionException(String taskName, String planId, String message, Throwable cause) {
		super(formatMessage(taskName, planId, message), cause);
		this.taskName = taskName;
		this.planId = planId;
		this.retryable = false;
	}

	/**
	 * Constructs a new TaskExecutionException with full details including retry
	 * information.
	 * @param taskName The name of the failed task
	 * @param planId The plan ID where the failure occurred
	 * @param message The detail message
	 * @param cause The cause of the exception
	 * @param retryable Whether the task can be retried
	 */
	public TaskExecutionException(String taskName, String planId, String message, Throwable cause, boolean retryable) {
		super(formatMessage(taskName, planId, message), cause);
		this.taskName = taskName;
		this.planId = planId;
		this.retryable = retryable;
	}

	/**
	 * Formats the exception message with task and plan information.
	 * @param taskName The task name
	 * @param planId The plan ID
	 * @param message The original message
	 * @return A formatted message string
	 */
	private static String formatMessage(String taskName, String planId, String message) {
		StringBuilder sb = new StringBuilder();

		if (taskName != null) {
			sb.append("Task '").append(taskName).append("' failed");
		}
		else {
			sb.append("Task execution failed");
		}

		if (planId != null) {
			sb.append(" in plan '").append(planId).append("'");
		}

		if (message != null && !message.trim().isEmpty()) {
			sb.append(": ").append(message);
		}

		return sb.toString();
	}

	/**
	 * Gets the name of the task that failed.
	 * @return The task name, or null if not specified
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * Gets the plan ID where the failure occurred.
	 * @return The plan ID, or null if not specified
	 */
	public String getPlanId() {
		return planId;
	}

	/**
	 * Checks if the task failure is retryable.
	 * @return true if the task can be retried, false otherwise
	 */
	public boolean isRetryable() {
		return retryable;
	}

	/**
	 * Creates a new TaskExecutionException indicating that the task is retryable.
	 * @param taskName The name of the failed task
	 * @param planId The plan ID where the failure occurred
	 * @param message The detail message
	 * @param cause The cause of the exception
	 * @return A new retryable TaskExecutionException
	 */
	public static TaskExecutionException retryable(String taskName, String planId, String message, Throwable cause) {
		return new TaskExecutionException(taskName, planId, message, cause, true);
	}

	/**
	 * Creates a new TaskExecutionException indicating that the task is not retryable.
	 * @param taskName The name of the failed task
	 * @param planId The plan ID where the failure occurred
	 * @param message The detail message
	 * @param cause The cause of the exception
	 * @return A new non-retryable TaskExecutionException
	 */
	public static TaskExecutionException nonRetryable(String taskName, String planId, String message, Throwable cause) {
		return new TaskExecutionException(taskName, planId, message, cause, false);
	}

}
