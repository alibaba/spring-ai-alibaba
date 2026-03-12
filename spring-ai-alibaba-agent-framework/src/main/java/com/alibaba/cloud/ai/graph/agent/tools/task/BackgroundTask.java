/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Manages the execution of a background task using CompletableFuture.
 * <p>
 * Provides thread-safe access to task status, results, and error information.
 * Tasks are automatically started upon construction. Callers can check completion
 * status, wait for completion, cancel tasks, and retrieve results or errors.
 *
 * @author Spring AI Alibaba
 */
public class BackgroundTask {

	private final String taskId;

	private final CompletableFuture<String> future;

	/**
	 * Create a BackgroundTask with an existing future.
	 * @param taskId the task identifier
	 * @param future the completable future to wrap
	 */
	public BackgroundTask(String taskId, CompletableFuture<String> future) {
		this.taskId = taskId;
		this.future = future;
	}

	/**
	 * Check if the task has completed execution.
	 */
	public boolean isCompleted() {
		return this.future.isDone();
	}

	/**
	 * Get the result of the task execution (non-blocking).
	 * @return the task result, or null if not yet completed or if an error occurred
	 */
	public String getResult() {
		try {
			return this.future.getNow(null);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the error that occurred during task execution, if any.
	 */
	public Exception getError() {
		if (this.future.isCompletedExceptionally()) {
			try {
				this.future.getNow(null);
			}
			catch (Exception e) {
				if (e.getCause() instanceof Exception cause) {
					return cause;
				}
				return e;
			}
		}
		return null;
	}

	/**
	 * Get a human-readable status description of the task.
	 */
	public String getStatus() {
		if (this.future.isCompletedExceptionally()) {
			Exception error = getError();
			return "Failed: " + (error != null ? error.getMessage() : "Unknown error");
		}
		return this.future.isDone() ? "Completed" : "Running";
	}

	/**
	 * Wait for the task to complete within the specified timeout.
	 * @param timeoutMs the maximum time to wait in milliseconds
	 * @return true if the task completed within the timeout, false if it timed out
	 */
	public boolean waitForCompletion(long timeoutMs) throws InterruptedException {
		if (this.future.isDone()) {
			return true;
		}
		try {
			this.future.get(timeoutMs, TimeUnit.MILLISECONDS);
			return true;
		}
		catch (InterruptedException e) {
			throw e;
		}
		catch (TimeoutException e) {
			return false;
		}
		catch (Exception e) {
			return true;
		}
	}

	/**
	 * Cancel the task if it hasn't completed yet.
	 */
	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.future.cancel(mayInterruptIfRunning);
	}

	/**
	 * Get the task ID.
	 */
	public String getTaskId() {
		return this.taskId;
	}

}
