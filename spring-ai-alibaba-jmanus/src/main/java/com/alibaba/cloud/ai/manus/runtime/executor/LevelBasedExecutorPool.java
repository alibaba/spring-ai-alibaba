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

package com.alibaba.cloud.ai.manus.runtime.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level-based executor common pool that manages thread pools by depth level. Each depth
 * level has its own thread pool to execute tasks at that hierarchy level. Maximum depth
 * level is 10.
 */
@Component
public class LevelBasedExecutorPool {

	private static final Logger log = LoggerFactory.getLogger(LevelBasedExecutorPool.class);

	private static final int MAX_DEPTH_LEVEL = 10;

	private static final int DEFAULT_CORE_POOL_SIZE = 5;

	private static final int DEFAULT_MAX_POOL_SIZE = 20;

	private static final int DEFAULT_QUEUE_CAPACITY = 100;

	private static final long DEFAULT_KEEP_ALIVE_TIME = 60L;

	private final Map<Integer, ExecutorService> levelPools = new ConcurrentHashMap<>();

	private final AtomicInteger poolCounter = new AtomicInteger(0);

	/**
	 * Get or create an executor for the specified depth level
	 * @param depthLevel The depth level (0-9, where 0 is root level)
	 * @return ExecutorService for the specified level
	 */
	public ExecutorService getExecutorForLevel(int depthLevel) {
		if (depthLevel < 0 || depthLevel >= MAX_DEPTH_LEVEL) {
			log.warn("Invalid depth level: {}. Using root level (0) instead.", depthLevel);
			depthLevel = 0;
		}

		return levelPools.computeIfAbsent(depthLevel, this::createLevelPool);
	}

	/**
	 * Get or create an executor for the specified depth level with custom pool
	 * configuration
	 * @param depthLevel The depth level (0-9)
	 * @param corePoolSize Core pool size for this level
	 * @param maxPoolSize Maximum pool size for this level
	 * @param queueCapacity Queue capacity for this level
	 * @return ExecutorService for the specified level
	 */
	public ExecutorService getExecutorForLevel(int depthLevel, int corePoolSize, int maxPoolSize, int queueCapacity) {
		if (depthLevel < 0 || depthLevel >= MAX_DEPTH_LEVEL) {
			log.warn("Invalid depth level: {}. Using root level (0) instead.", depthLevel);
			depthLevel = 0;
		}

		return levelPools.computeIfAbsent(depthLevel,
				level -> createLevelPool(level, corePoolSize, maxPoolSize, queueCapacity));
	}

	/**
	 * Submit a task to the executor for the specified depth level
	 * @param depthLevel The depth level (0-9)
	 * @param task The task to execute
	 * @param <T> The type of the task result
	 * @return CompletableFuture representing the task execution
	 */
	public <T> CompletableFuture<T> submitTask(int depthLevel, Callable<T> task) {
		ExecutorService executor = getExecutorForLevel(depthLevel);
		return CompletableFuture.supplyAsync(() -> {
			try {
				return task.call();
			}
			catch (Exception e) {
				log.error("Task execution failed at depth level {}: {}", depthLevel, e.getMessage(), e);
				throw new CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Submit a runnable task to the executor for the specified depth level
	 * @param depthLevel The depth level (0-9)
	 * @param task The runnable task to execute
	 * @return CompletableFuture representing the task execution
	 */
	public CompletableFuture<Void> submitTask(int depthLevel, Runnable task) {
		ExecutorService executor = getExecutorForLevel(depthLevel);
		return CompletableFuture.runAsync(() -> {
			try {
				task.run();
			}
			catch (Exception e) {
				log.error("Task execution failed at depth level {}: {}", depthLevel, e.getMessage(), e);
				throw new CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Get pool statistics for all levels
	 * @return Map containing pool information for each level
	 */
	public Map<String, Object> getPoolStatistics() {
		Map<String, Object> stats = new ConcurrentHashMap<>();

		for (Map.Entry<Integer, ExecutorService> entry : levelPools.entrySet()) {
			int level = entry.getKey();
			ExecutorService executor = entry.getValue();

			if (executor instanceof ThreadPoolExecutor) {
				ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
				Map<String, Object> levelStats = new ConcurrentHashMap<>();
				levelStats.put("corePoolSize", tpe.getCorePoolSize());
				levelStats.put("maximumPoolSize", tpe.getMaximumPoolSize());
				levelStats.put("currentPoolSize", tpe.getPoolSize());
				levelStats.put("activeThreads", tpe.getActiveCount());
				levelStats.put("queueSize", tpe.getQueue().size());
				levelStats.put("completedTasks", tpe.getCompletedTaskCount());
				levelStats.put("totalTasks", tpe.getTaskCount());

				stats.put("level_" + level, levelStats);
			}
		}

		return stats;
	}

	/**
	 * Shutdown a specific level pool
	 * @param depthLevel The depth level to shutdown
	 */
	public void shutdownLevel(int depthLevel) {
		ExecutorService executor = levelPools.remove(depthLevel);
		if (executor != null) {
			log.info("Shutting down executor pool for depth level: {}", depthLevel);
			shutdownExecutor(executor);
		}
	}

	/**
	 * Shutdown all level pools
	 */
	@PreDestroy
	public void shutdownAll() {
		log.info("Shutting down all level-based executor pools");
		for (Map.Entry<Integer, ExecutorService> entry : levelPools.entrySet()) {
			int level = entry.getKey();
			ExecutorService executor = entry.getValue();
			log.info("Shutting down executor pool for depth level: {}", level);
			shutdownExecutor(executor);
		}
		levelPools.clear();
	}

	/**
	 * Create a new thread pool for the specified depth level with default configuration
	 */
	private ExecutorService createLevelPool(int depthLevel) {
		return createLevelPool(depthLevel, DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, DEFAULT_QUEUE_CAPACITY);
	}

	/**
	 * Create a new thread pool for the specified depth level with custom configuration
	 */
	private ExecutorService createLevelPool(int depthLevel, int corePoolSize, int maxPoolSize, int queueCapacity) {
		String poolName = "level-" + depthLevel + "-executor-" + poolCounter.getAndIncrement();

		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, DEFAULT_KEEP_ALIVE_TIME,
				TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueCapacity), new ThreadFactory() {
					private final AtomicInteger threadCounter = new AtomicInteger(1);

					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r, poolName + "-thread-" + threadCounter.getAndIncrement());
						thread.setDaemon(false);
						return thread;
					}
				}, new ThreadPoolExecutor.CallerRunsPolicy());

		log.info("Created executor pool for depth level {}: {} (core: {}, max: {}, queue: {})", depthLevel, poolName,
				corePoolSize, maxPoolSize, queueCapacity);

		return executor;
	}

	/**
	 * Shutdown an executor gracefully
	 */
	private void shutdownExecutor(ExecutorService executor) {
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
					executor.shutdownNow();
					if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
						log.error("Executor pool did not terminate");
					}
				}
			}
			catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Get the maximum depth level supported
	 * @return Maximum depth level (10)
	 */
	public int getMaxDepthLevel() {
		return MAX_DEPTH_LEVEL;
	}

	/**
	 * Check if a depth level is valid
	 * @param depthLevel The depth level to check
	 * @return true if valid, false otherwise
	 */
	public boolean isValidDepthLevel(int depthLevel) {
		return depthLevel >= 0 && depthLevel < MAX_DEPTH_LEVEL;
	}

	/**
	 * Get the number of active level pools
	 * @return Number of active pools
	 */
	public int getActivePoolCount() {
		return levelPools.size();
	}

}
