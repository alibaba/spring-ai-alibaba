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


public interface StatefulTask {

	/**
	 * Executes the task using the provided execution context.
	 *
	 * <p>
	 * The implementation should:
	 * <ul>
	 * <li>Read any required input from the context</li>
	 * <li>Perform the task-specific logic</li>
	 * <li>Store any results back to the context for subsequent tasks</li>
	 * <li>Handle errors gracefully and provide meaningful error messages</li>
	 * </ul>
	 * @param context The execution context containing shared state
	 * @throws TaskExecutionException if the task fails to execute
	 * @throws IllegalArgumentException if the context is null or invalid
	 */
	void execute(JManusExecutionContext context) throws TaskExecutionException;

	/**
	 * Returns a human-readable name for this task. This name is used for logging,
	 * debugging, and state visualization.
	 * @return The task name, should not be null or empty
	 */
	String getName();

	/**
	 * Returns a description of what this task does. This is optional but recommended for
	 * debugging and documentation purposes.
	 * @return A description of the task, or empty string if not provided
	 */
	default String getDescription() {
		return "";
	}

	/**
	 * Validates that the execution context contains all required inputs for this task.
	 * This method is called before execute() to ensure the task can run successfully.
	 * @param context The execution context to validate
	 * @return true if the context is valid for this task, false otherwise
	 */
	default boolean validateContext(JManusExecutionContext context) {
		return context != null;
	}

	/**
	 * Returns the expected execution time for this task in milliseconds. This is used for
	 * monitoring and timeout management.
	 * @return Expected execution time in milliseconds, or -1 if unknown
	 */
	default long getExpectedExecutionTimeMs() {
		return -1;
	}

	/**
	 * Indicates whether this task can be safely retried if it fails.
	 * @return true if the task is safe to retry, false otherwise
	 */
	default boolean isRetryable() {
		return true;
	}

	/**
	 * Returns the maximum number of retry attempts for this task.
	 * @return Maximum retry attempts, or 0 if retries are not allowed
	 */
	default int getMaxRetryAttempts() {
		return isRetryable() ? 3 : 0;
	}

}
