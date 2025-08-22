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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Default manager for all scheduled agent executions. Provides a shared task scheduler
 * pool and manages all ScheduledAgentTask instances.
 *
 * @author yaohui &#064;create 2025/8/21
 */
public class DefaultScheduledAgentManager implements ScheduledAgentManager {

	private static final Logger log = LoggerFactory.getLogger(DefaultScheduledAgentManager.class);

	private static final DefaultScheduledAgentManager INSTANCE = new DefaultScheduledAgentManager();

	private final TaskScheduler taskScheduler;

	private final Map<String, ScheduledAgentTask> activeTasks = new ConcurrentHashMap<>();

	private final AtomicInteger taskIdGenerator = new AtomicInteger(1);

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private volatile boolean shutdown = false;

	private DefaultScheduledAgentManager() {
		this.taskScheduler = createTaskScheduler();
		log.info("Default Scheduled Agent Manager initialized with shared task scheduler");
	}

	/**
	 * Get the singleton instance of the default manager
	 */
	public static DefaultScheduledAgentManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Get the task scheduler
	 */
	@Override
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Register a new scheduled agent execution
	 * @param task the ScheduledAgentTask to register
	 * @return the unique Task ID assigned to this Task
	 */
	@Override
	public String registerTask(ScheduledAgentTask task) {
		if (shutdown) {
			throw new IllegalStateException("Default Scheduled Agent Manager is shut down");
		}
		String taskId = String.format("agent-task-%s-%d", task.getName(), taskIdGenerator.getAndIncrement());
		lock.writeLock().lock();
		try {
			activeTasks.put(taskId, task);
			log.debug("Registered scheduled agent task: {}", taskId);
			return taskId;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Unregister a scheduled agent execution
	 * @param taskId the task ID to unregister
	 * @return true if the task was found and removed, false otherwise
	 */
	@Override
	public boolean unregisterTask(String taskId) {
		lock.writeLock().lock();
		try {
			ScheduledAgentTask removed = activeTasks.remove(taskId);
			if (removed != null) {
				log.debug("Unregistered scheduled agent task: {}", taskId);
				return true;
			}
			return false;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Get a scheduled agent task by ID
	 * @param taskId the task ID
	 * @return the ScheduledAgentTask if found, empty Optional otherwise
	 */
	@Override
	public Optional<ScheduledAgentTask> getTask(String taskId) {
		lock.readLock().lock();
		try {
			return Optional.ofNullable(activeTasks.get(taskId));
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Get all active execution IDs
	 * @return a set of all active execution IDs
	 */
	@Override
	public Set<String> getAllActiveTaskIds() {
		lock.readLock().lock();
		try {
			return Set.copyOf(activeTasks.keySet());
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Get the count of active executions
	 * @return the number of active executions
	 */
	@Override
	public int getActiveTaskCount() {
		lock.readLock().lock();
		try {
			return activeTasks.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Stop all active executions and shutdown the manager
	 */
	@Override
	public void shutdown() {
		lock.writeLock().lock();
		try {
			if (shutdown) {
				return;
			}

			log.info("Shutting down Default Scheduled Agent Manager with {} active executions", activeTasks.size());

			// Stop all active executions
			activeTasks.values().forEach(task -> {
				try {
					task.stop();
				}
				catch (Exception e) {
					log.warn("Error stopping scheduled execution during shutdown", e);
				}
			});

			activeTasks.clear();

			// Shutdown the task scheduler
			if (taskScheduler instanceof ThreadPoolTaskScheduler) {
				((ThreadPoolTaskScheduler) taskScheduler).shutdown();
			}

			shutdown = true;
			log.info("Default Scheduled Agent Manager shut down successfully");
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Check if the manager is shut down
	 */
	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	private TaskScheduler createTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(3);
		scheduler.setThreadNamePrefix("agent-scheduler-");
		scheduler.setThreadGroupName("DefaultAgentScheduler");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(30);
		scheduler.initialize();
		log.info("Task scheduler initialized with pool size: {}", scheduler.getPoolSize());
		return scheduler;
	}

}
