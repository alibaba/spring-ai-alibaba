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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.NodeAggregationStrategy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


public class ParallelNode extends Node {
	
	private static final Logger logger = LoggerFactory.getLogger(ParallelNode.class);

	public static final String PARALLEL_PREFIX = "__PARALLEL__";
	public static final String PARALLEL_TARGET_PREFIX = "__PARALLEL_TARGET__";

	public static final String EXECUTION_KEY = "__parallel_execution__";

	public static final String RESTORED_DISPATCH_KEY = "__parallel_restored_dispatch__";

	public static final String MAX_CONCURRENCY_KEY = "__MAX_CONCURRENCY__";

	private static final int MIN_DEFAULT_POOL_SIZE = 4;

	private static final int MAX_DEFAULT_POOL_SIZE = 200;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdownDefaultExecutor();
		}, "default-executor-shutdown-hook"));
	}

	private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
		private final AtomicInteger threadNum = new AtomicInteger(1);
		private final String prefix = "parallel-node-action-thread-";

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName(prefix + threadNum.getAndIncrement());
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler((t, e) ->
					logger.error("Thread {} encountered an exception during task execution: {}", t.getName(), e.getMessage(), e)
			);
			return thread;
		}
	};

	public static Executor getExecutor(RunnableConfig config, String nodeId) {
		return config.metadata(nodeId)
				.filter(value -> value instanceof Executor)
				.map(Executor.class::cast)
				.orElseGet(() -> config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY)
						.filter(value -> value instanceof Executor)
						.map(Executor.class::cast)
						.orElse(DEFAULT_EXECUTOR));
	}

	/**
	 * Gets the aggregation strategy for a parallel node from the configuration.
	 * First checks for node-specific strategy using formatted targetNodeId (the node right after parallel branches),
	 * then falls back to default strategy. If no strategy is configured, defaults to ALL_OF.
	 *
	 * @param config the RunnableConfig containing the strategy configuration
	 * @param targetNodeId the ID of the target node that follows the parallel node
	 * @return the NodeAggregationStrategy to use
	 */
	public static NodeAggregationStrategy getAggregationStrategy(RunnableConfig config, String targetNodeId) {
		String formattedTargetNodeId = formatTargetNodeId(targetNodeId);
		return config.metadata(formattedTargetNodeId)
				.filter(value -> value instanceof NodeAggregationStrategy)
				.map(NodeAggregationStrategy.class::cast)
				.orElseGet(() -> config.metadata(RunnableConfig.DEFAULT_PARALLEL_AGGREGATION_STRATEGY_KEY)
						.filter(value -> value instanceof NodeAggregationStrategy)
						.map(NodeAggregationStrategy.class::cast)
						.orElse(NodeAggregationStrategy.ALL_OF));
	}

	/**
	 * Optimized default thread pool executor based on industry best practices.
	 * Features:
	 * 1. Dynamic core pool size based on system resources
	 * 2. Higher maximum pool size for handling burst workloads
	 * 3. Larger work queue to buffer tasks during peak loads
	 * 4. Caller runs policy to prevent task rejection under extreme load
	 * 5. Configurable keep-alive time for better resource management
	 */
	private static final ExecutorService DEFAULT_EXECUTOR = new ThreadPoolExecutor(
			// Core thread count: CPU cores * 2 (optimized for mixed IO/CPU workloads)
			calculateCorePoolSize(),
			// Maximum thread count: CPU cores * 4 (handles burst workloads)
			calculateMaximumPoolSize(),
			// Idle thread recycling time: 60 seconds (standard value for better resource management)
			60L,
			TimeUnit.SECONDS,
			// Work queue: SynchronousQueue -> LinkedBlockingQueue with dynamic capacity
			// Using LinkedBlockingQueue with larger capacity to prevent task rejections during bursts
			new LinkedBlockingQueue<>(calculateQueueCapacity()),
			// Thread factory: custom naming for better monitoring
			DEFAULT_THREAD_FACTORY,
			// Rejection policy: CallerRunsPolicy to prevent data loss under extreme load
			// This policy executes tasks in the calling thread when the queue is full
			new ThreadPoolExecutor.CallerRunsPolicy() {
				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					logger.warn("Task rejected by thread pool, executing in calling thread. Pool state - Active: {}, Queue size: {}", 
							executor.getActiveCount(), executor.getQueue().size());
					super.rejectedExecution(r, executor);
				}
			}
	) {
		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			logger.debug("Starting execution of task in thread: {}", t.getName());
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			if (t != null) {
				logger.error("Task execution failed", t);
			} else {
				logger.debug("Task execution completed successfully");
			}
		}

		@Override
		protected void terminated() {
			super.terminated();
			logger.info("ParallelNode default thread pool terminated");
		}
	};

	/**
	 * Calculate optimal core pool size based on system resources and workload characteristics.
	 * For mixed IO/CPU workloads, 2x CPU cores is typically optimal.
	 * Minimum of 4 threads to ensure reasonable parallelism on small systems.
	 * 
	 * @return optimal core pool size
	 */
	private static int calculateCorePoolSize() {
		int cpuCores = Runtime.getRuntime().availableProcessors();
		// For mixed workloads, 2x CPU cores is typically optimal
		int corePoolSize = cpuCores * 2;
		// Ensure minimum of 4 threads for reasonable parallelism on small systems
		int finalCorePoolSize = Math.min(Math.max(corePoolSize, MIN_DEFAULT_POOL_SIZE), MAX_DEFAULT_POOL_SIZE);
		logger.info("Calculated core pool size: {} (CPU cores: {})", finalCorePoolSize, cpuCores);
		return finalCorePoolSize;
	}

	/**
	 * Calculate maximum pool size based on system resources.
	 * Allows for handling burst workloads while preventing resource exhaustion.
	 * 
	 * @return maximum pool size
	 */
	private static int calculateMaximumPoolSize() {
		int cpuCores = Runtime.getRuntime().availableProcessors();
		// Allow for handling burst workloads with 4x CPU cores
		// Cap at reasonable maximum to prevent resource exhaustion
		int maxPoolSize = Math.min(cpuCores * 4, MAX_DEFAULT_POOL_SIZE);
		logger.info("Calculated maximum pool size: {} (CPU cores: {})", maxPoolSize, cpuCores);
		return maxPoolSize;
	}

	/**
	 * Calculate optimal queue capacity based on expected workload.
	 * Larger queue prevents task rejections during bursts while avoiding excessive memory usage.
	 * 
	 * @return optimal queue capacity
	 */
	private static int calculateQueueCapacity() {
		// Larger queue capacity to handle burst workloads
		// Balance between preventing task rejections and controlling memory usage
		int queueCapacity = 1000;
		logger.info("Calculated queue capacity: {}", queueCapacity);
		return queueCapacity;
	}

	public static void shutdownDefaultExecutor() {
		if (!DEFAULT_EXECUTOR.isShutdown()) {
			logger.info("Shutting down ParallelNode default executor");
			DEFAULT_EXECUTOR.shutdown();
			try {
				// Wait for 60 seconds, force shutdown if tasks remain unfinished
				if (!DEFAULT_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
					logger.warn("ParallelNode default executor did not terminate gracefully, forcing shutdown");
					DEFAULT_EXECUTOR.shutdownNow();
				} else {
					logger.info("ParallelNode default executor shut down successfully");
				}
			} catch (InterruptedException e) {
				logger.warn("Interrupted while waiting for ParallelNode default executor shutdown", e);
				DEFAULT_EXECUTOR.shutdownNow();
				Thread.currentThread().interrupt(); // Preserve interrupt status
			}
		}
	}

	public static String formatNodeId(String nodeId) {
		return format("%s(%s)", PARALLEL_PREFIX, requireNonNull(nodeId, "nodeId cannot be null!"));
	}

	public static String formatMaxConcurrencyKey(String nodeId) {
		return format("%s_%s", MAX_CONCURRENCY_KEY, requireNonNull(nodeId, "nodeId cannot be null!"));
	}

	/**
	 * Formats the target node ID for aggregation strategy configuration.
	 * This adds a prefix to distinguish target node IDs used for aggregation strategy lookup.
	 *
	 * @param targetNodeId the ID of the target node that follows the parallel node
	 * @return formatted target node ID with prefix
	 */
	public static String formatTargetNodeId(String targetNodeId) {
		return format("%s(%s)", PARALLEL_TARGET_PREFIX, requireNonNull(targetNodeId, "targetNodeId cannot be null!"));
	}

	public record Branch(String nodeId, AsyncNodeActionWithConfig action, Map<String, Object> input) {

		public Branch(String nodeId, AsyncNodeActionWithConfig action) {
			this(nodeId, action, Map.of());
		}

	}

	/**
	 * Deferred work handed to the graph executor so concurrency and cancellation
	 * cover both the node action and its streaming results.
	 */
	public record Execution(List<Branch> branches, Map<String, Object> routingUpdate,
			NodeAggregationStrategy strategy, int maxConcurrency) {
	}

	static Map<String, Object> schedule(String nodeId, String targetNodeId, List<Branch> branches,
			Map<String, Object> routingUpdate, RunnableConfig config) {
		int limit = config.metadata(formatMaxConcurrencyKey(nodeId))
				.filter(Integer.class::isInstance)
				.map(Integer.class::cast)
				.orElse(Integer.MAX_VALUE);
		if (limit <= 0) {
			throw new IllegalArgumentException("Parallel node '" + nodeId + "' requires positive max concurrency");
		}
		return Map.of(EXECUTION_KEY,
				new Execution(List.copyOf(branches), routingUpdate, getAggregationStrategy(config, targetNodeId), limit));
	}

	public ParallelNode(String id, String targetNodeId, List<AsyncNodeActionWithConfig> actions,
			List<String> actionNodeIds) {
		super(formatNodeId(id), (compileConfig) -> {
			List<Branch> branches = IntStream.range(0, actions.size())
					.mapToObj(index -> new Branch(actionNodeIds.get(index), actions.get(index)))
					.toList();
			return (state, config) -> CompletableFuture
					.completedFuture(schedule(formatNodeId(id), targetNodeId, branches, Map.of(), config));
		});
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}
