/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.scheduling;

import java.util.Optional;
import java.util.Set;

import org.springframework.scheduling.TaskScheduler;

/**
 * Interface for managing scheduled agent tasks. This abstraction allows for different
 * implementations: - Local management (DefaultScheduledAgentManager) - Centralized
 * management platform integration - Distributed/clustered management
 *
 * @author yaohui
 * @since 1.0.0
 */
public interface ScheduledAgentManager {

	/**
	 * Register a scheduled agent task
	 * @param task the task to register
	 * @return the unique task ID assigned to this task
	 */
	String registerTask(ScheduledAgentTask task);

	/**
	 * Unregister a scheduled agent task
	 * @param taskId the task ID to unregister
	 * @return true if the task was found and removed, false otherwise
	 */
	boolean unregisterTask(String taskId);

	/**
	 * Get a scheduled agent task by ID
	 * @param taskId the task ID
	 * @return the ScheduledAgentTask if found, empty Optional otherwise
	 */
	Optional<ScheduledAgentTask> getTask(String taskId);

	/**
	 * Get all active task IDs
	 * @return a set of all active task IDs
	 */
	Set<String> getAllActiveTaskIds();

	/**
	 * Get the count of active tasks
	 * @return the number of active tasks
	 */
	int getActiveTaskCount();

	/**
	 * Get the task scheduler for this manager
	 * @return the TaskScheduler instance
	 */
	TaskScheduler getTaskScheduler();

	/**
	 * Check if the manager is shut down
	 * @return true if shut down, false otherwise
	 */
	boolean isShutdown();

	/**
	 * Shutdown the manager and all its tasks
	 */
	void shutdown();

}
