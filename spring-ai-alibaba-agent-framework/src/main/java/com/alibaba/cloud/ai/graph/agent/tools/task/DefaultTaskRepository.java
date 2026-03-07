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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Default implementation of TaskRepository using a thread pool for background execution.
 *
 * @author Spring AI Alibaba
 */
public class DefaultTaskRepository implements TaskRepository {

	private final Map<String, BackgroundTask> backgroundTasks = new ConcurrentHashMap<>();

	private final ExecutorService executor;

	private final boolean ownsExecutor;

	/**
	 * Create a repository with a default cached thread pool executor.
	 */
	public DefaultTaskRepository() {
		this(Executors.newCachedThreadPool(r -> {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("background-task-" + thread.getId());
			return thread;
		}), true);
	}

	/**
	 * Create a repository with a custom executor service.
	 */
	public DefaultTaskRepository(ExecutorService executor) {
		this(executor, false);
	}

	/**
	 * Internal constructor for specifying executor ownership.
	 */
	public DefaultTaskRepository(ExecutorService executor, boolean ownsExecutor) {
		this.executor = executor;
		this.ownsExecutor = ownsExecutor;
	}

	@Override
	public BackgroundTask getTask(String taskId) {
		return this.backgroundTasks.get(taskId);
	}

	@Override
	public BackgroundTask putTask(String taskId, Supplier<String> taskExecution) {
		CompletableFuture<String> future = CompletableFuture.supplyAsync(taskExecution, this.executor);
		BackgroundTask backgroundTask = new BackgroundTask(taskId, future);
		this.backgroundTasks.put(taskId, backgroundTask);
		return backgroundTask;
	}

	@Override
	public void removeTask(String taskId) {
		this.backgroundTasks.remove(taskId);
	}

	@Override
	public void clear() {
		this.backgroundTasks.clear();
	}

	/**
	 * Remove completed tasks from the repository.
	 */
	public void clearCompletedTasks() {
		this.backgroundTasks.entrySet().removeIf(entry -> entry.getValue().isCompleted());
	}

	/**
	 * Shutdown the executor service if this repository owns it.
	 */
	public void shutdown() {
		if (this.ownsExecutor && this.executor != null) {
			this.executor.shutdown();
			try {
				if (!this.executor.awaitTermination(60, TimeUnit.SECONDS)) {
					this.executor.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				this.executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

}
