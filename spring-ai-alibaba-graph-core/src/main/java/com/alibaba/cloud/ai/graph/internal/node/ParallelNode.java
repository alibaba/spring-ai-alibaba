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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.NodeAggregationStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import com.alibaba.cloud.ai.graph.utils.LifeListenerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


public class ParallelNode extends Node {
	
	private static final Logger logger = LoggerFactory.getLogger(ParallelNode.class);

	public static final String PARALLEL_PREFIX = "__PARALLEL__";
	public static final String PARALLEL_TARGET_PREFIX = "__PARALLEL_TARGET__";

	public static final String MAX_CONCURRENCY_KEY = "__MAX_CONCURRENCY__";

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
			thread.setDaemon(false); // Non-daemon thread to ensure task completion
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
		int finalCorePoolSize = Math.max(corePoolSize, 4);
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
		int maxPoolSize = Math.min(cpuCores * 4, 200);
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

	/**
	 * Represents an asynchronous parallel node action that executes multiple branches concurrently.
	 * This record encapsulates all the information needed to execute parallel branches and aggregate their results.
	 *
	 * @param nodeId the formatted identifier of the parallel node (with {@link #PARALLEL_PREFIX} prefix).
	 *               This is used for logging and executor lookup in RunnableConfig.
	 * @param targetNodeId the ID of the target node that follows the parallel branches (the merge node).
	 *                     This is used to look up the aggregation strategy configuration (ANY_OF or ALL_OF)
	 *                     from RunnableConfig using {@link #formatTargetNodeId(String)}. The strategy determines
	 *                     whether to wait for all branches to complete (ALL_OF) or proceed with the first
	 *                     completed branch (ANY_OF).
	 * @param actions the list of actions to be executed in parallel. Each action represents a branch
	 *                in the parallel execution flow. These actions are executed concurrently using
	 *                the executor configured in RunnableConfig.
	 * @param actionNodeIds the list of node IDs corresponding to each action. Must have the same size
	 *                      as the actions list. Each ID identifies the actual node being executed in
	 *                      the corresponding parallel branch. Used for lifecycle listener callbacks and logging.
	 * @param channels the key strategy map that defines how state values are merged when multiple
	 *                 parallel branches produce results with the same keys. Keys in this map correspond
	 *                 to state keys, and values define the merge strategy (e.g., AppendStrategy, ReplaceStrategy).
	 *                 This is used by {@link OverAllState#updateState(Map, Map, Map)} when processing results.
	 * @param compileConfig the compilation configuration containing lifecycle listeners and other
	 *                      compile-time settings. Lifecycle listeners are invoked before and after each
	 *                      parallel branch execution.
	 */
	public record AsyncParallelNodeAction(String nodeId, String targetNodeId, List<AsyncNodeActionWithConfig> actions,
			List<String> actionNodeIds, Map<String, KeyStrategy> channels, CompileConfig compileConfig)
			implements AsyncNodeActionWithConfig {

		private CompletableFuture<Map<String, Object>> evalNodeActionSync(AsyncNodeActionWithConfig action,
																		  String actualNodeId, OverAllState state, RunnableConfig config) {
			LifeListenerUtil.processListenersLIFO(actualNodeId,
					new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config, NODE_BEFORE,
					null);
			return action.apply(state, config)
					.whenComplete((stringObjectMap, throwable) -> LifeListenerUtil.processListenersLIFO(actualNodeId,
							new LinkedBlockingDeque<>(compileConfig.lifecycleListeners()), state.data(), config,
							NODE_AFTER,
							throwable));
		}

		private CompletableFuture<Map<String, Object>> evalNodeActionAsync(AsyncNodeActionWithConfig action,
				String actualNodeId, OverAllState state, RunnableConfig config, Executor executor) {
			
			// Log thread pool metrics if it's a ThreadPoolExecutor
			if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
				logger.debug("Thread pool metrics - Active threads: {}, Pool size: {}, Queue size: {}, Completed tasks: {}", 
						threadPoolExecutor.getActiveCount(), 
						threadPoolExecutor.getPoolSize(), 
						threadPoolExecutor.getQueue().size(), 
						threadPoolExecutor.getCompletedTaskCount());
			}
			
			logger.debug("Submitting task for node {} to executor", actualNodeId);
			return CompletableFuture.supplyAsync(() -> {
				try {
					logger.debug("Executing task for node {} in thread {}", actualNodeId, Thread.currentThread().getName());
					return evalNodeActionSync(action, actualNodeId, state, config).join();
				} catch (Exception e) {
					logger.error("Error executing task for node {}", actualNodeId, e);
					throw new RuntimeException(e);
				}
			}, executor);
		}

		/**
		 * Wait for the first successful completion among multiple futures.
		 * This method returns a CompletableFuture that completes with the result of the first
		 * future that completes successfully (without exception). If all futures fail, the returned
		 * future fails with a CompletionException containing details of all failures.
		 *
		 * Thread-safety: CompletableFuture.complete() is thread-safe and atomic. Only the first
		 * successful completion will trigger the result future. Multiple concurrent calls to
		 * complete() are safe - only the first one succeeds and triggers downstream processing.
		 *
		 * @param futures the list of futures to wait for
		 * @return a CompletableFuture that completes with the first successful result
		 */
		private CompletableFuture<Map<String, Object>> waitForFirstSuccessful(
				List<CompletableFuture<Map<String, Object>>> futures) {

			CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
			AtomicInteger completedCount = new AtomicInteger(0);
			List<Throwable> failures = new CopyOnWriteArrayList<>();
			int totalFutures = futures.size();

			for (int i = 0; i < totalFutures; i++) {
				final int index = i;
				futures.get(i).whenComplete((value, throwable) -> {
					if (throwable == null) {
						if (result.complete(value)) {
							logger.debug("ANY_OF strategy: Future {} completed successfully first", index);
						}
						completedCount.incrementAndGet();
					} else {
						// This future failed - record the failure
						logger.debug("ANY_OF strategy: Future {} failed with exception", index, throwable);
						failures.add(throwable);

						// Check if all futures have completed (both successful and failed)
						if (completedCount.incrementAndGet() == totalFutures) {
							// All futures have completed, check if result is still not set
							// This means ALL futures failed (no successful completion)
							if (!result.isDone()) {
								// Create a composite exception with all failures
								RuntimeException compositeException = new RuntimeException(
									String.format("ALL %d parallel branches failed in ANY_OF strategy", totalFutures)
								);
								for (Throwable failure : failures) {
									compositeException.addSuppressed(failure);
								}
								result.completeExceptionally(compositeException);
								logger.error("ANY_OF strategy: All {} futures failed", totalFutures, compositeException);
							}
						}
					}
				});
			}

			return result;
		}

		/**
		 * Executes a node action asynchronously with semaphore-based concurrency control.
		 * This method ensures that the number of concurrently executing tasks does not exceed
		 * the specified limit by using a semaphore to acquire permits before execution.
		 *
		 * @param action the node action to execute
		 * @param actualNodeId the ID of the node being executed
		 * @param state the state snapshot for this execution
		 * @param config the runnable configuration
		 * @param executor the executor to use for async execution
		 * @param semaphore the semaphore used to control concurrency
		 * @return a CompletableFuture containing the execution results
		 */
		private CompletableFuture<Map<String, Object>> evalNodeActionWithSemaphore(
				AsyncNodeActionWithConfig action,
				String actualNodeId,
				OverAllState state,
				RunnableConfig config,
				Executor executor,
				Semaphore semaphore) {

			return CompletableFuture.supplyAsync(() -> {
				try {
					// Acquire semaphore permit (blocks if max concurrency reached)
					logger.debug("Node {} waiting for semaphore permit. Available permits: {}",
							actualNodeId, semaphore.availablePermits());
					semaphore.acquire();
					logger.debug("Node {} acquired semaphore permit. Remaining permits: {}",
							actualNodeId, semaphore.availablePermits());

					try {
						logger.debug("Executing task for node {} in thread {} with concurrency control",
								actualNodeId, Thread.currentThread().getName());
						return evalNodeActionSync(action, actualNodeId, state, config).join();
					} finally {
						// Always release the semaphore permit
						semaphore.release();
						logger.debug("Node {} released semaphore permit. Available permits: {}",
								actualNodeId, semaphore.availablePermits());
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.error("Node {} was interrupted while waiting for semaphore", actualNodeId, e);
					throw new RuntimeException("Interrupted while waiting for execution slot", e);
				} catch (Exception e) {
					logger.error("Error executing task for node {}", actualNodeId, e);
					throw new RuntimeException(e);
				}
			}, executor);
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			// Get maxConcurrency from config metadata
			Integer maxConcurrency = config.metadata(formatMaxConcurrencyKey(nodeId))
					.filter(value -> value instanceof Integer)
					.map(Integer.class::cast)
					.orElse(null);

			// Create semaphore for concurrency control if maxConcurrency is set
			Semaphore semaphore = maxConcurrency != null ? new Semaphore(maxConcurrency) : null;

			if (semaphore != null) {
				logger.info("Parallel node {} will execute with max concurrency: {}", nodeId, maxConcurrency);
			} else {
				logger.debug("Parallel node {} will execute without concurrency limit", nodeId);
			}

			List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
			for (int i = 0; i < actions.size(); i++) {
				AsyncNodeActionWithConfig action = actions.get(i);
				String actualNodeId = actionNodeIds.get(i);

				// Create a defensive copy of the state for each parallel action
				// This prevents race conditions if actions modify the state in-place
				OverAllState stateSnapshot = state.snapShot().orElse(new OverAllState());

				// First try to get node-specific executor, then default executor, finally use DEFAULT_EXECUTOR
				Executor executor = getExecutor(config, nodeId);

				// Use semaphore-controlled execution if maxConcurrency is set
				CompletableFuture<Map<String, Object>> future;
				if (semaphore != null) {
					future = evalNodeActionWithSemaphore(action, actualNodeId, stateSnapshot, config, executor, semaphore);
				} else {
					future = evalNodeActionAsync(action, actualNodeId, stateSnapshot, config, executor);
				}

				futures.add(future);
			}

		// Get aggregation strategy from config
		NodeAggregationStrategy strategy = getAggregationStrategy(config, targetNodeId);

		if (strategy == NodeAggregationStrategy.ANY_OF) {
			// Wait for the first successful task to complete
			// This implementation returns the first successful result, skipping failures
			// Only fails if ALL tasks fail
			return waitForFirstSuccessful(futures).thenApply(firstSuccessfulResult -> {
				// Cancel remaining futures to prevent unnecessary execution and resource waste
				int cancelledCount = 0;
				for (CompletableFuture<Map<String, Object>> future : futures) {
					if (!future.isDone()) {
						// mayInterruptIfRunning=true: Stop running tasks to save resources
						// Tasks should handle InterruptedException gracefully, this might cause unexpected behavior if not handled properly
						boolean cancelled = future.cancel(true);
						if (cancelled) {
							cancelledCount++;
							logger.debug("Cancelled pending future in ANY_OF strategy");
						}
					}
				}
				if (cancelledCount > 0) {
					logger.info("ANY_OF strategy: Cancelled {} remaining futures after first successful completion", cancelledCount);
				}

				List<Map<String, Object>> results = new ArrayList<>();
				results.add(firstSuccessfulResult);

				return processParallelResults(results, state, actions);
			});
		} else {
			// Wait for all tasks to complete (default behavior)
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
				// Collect all results
				List<Map<String, Object>> results = futures.stream()
						.map(CompletableFuture::join)
						.collect(Collectors.toList());

				return processParallelResults(results, state, actions);
			});
		}
		}

		/**
		 * Process parallel execution results, handling GraphFlux, traditional Flux, and
		 * regular objects.
		 * Priority: GraphFlux > traditional Flux > regular objects
		 */
		private Map<String, Object> processParallelResults(List<Map<String, Object>> results,
				OverAllState state, List<AsyncNodeActionWithConfig> actionList) {

			// Check if any result contains GraphFlux or traditional Flux
			List<GraphFlux<?>> graphFluxList = new ArrayList<>();
			List<String> graphFluxNodeIds = new ArrayList<>();

			// Collect non-streaming state
			Map<String, Object> mergedState = new HashMap<>();
			// First pass: collect GraphFlux and traditional Flux instances
			for (int i = 0; i < results.size(); i++) {
				Map<String, Object> result = results.get(i);
				AsyncNodeActionWithConfig action = actionList.get(i);
				String effectiveNodeId = generateEffectiveNodeId(action, i);

				for (Map.Entry<String, Object> entry : result.entrySet()) {
					Object value = entry.getValue();

					if (value instanceof GraphFlux) {
						GraphFlux<?> graphFlux = (GraphFlux<?>) value;
						// Use GraphFlux's own nodeId, or generate one if not set properly
						String graphFluxNodeId = graphFlux.getNodeId() != null ? graphFlux.getNodeId()
								: effectiveNodeId;

						// Create new GraphFlux with correct nodeId if needed
						if (!graphFluxNodeId.equals(graphFlux.getNodeId())) {
							@SuppressWarnings("unchecked")
							GraphFlux<Object> castedFlux = (GraphFlux<Object>) graphFlux;
							@SuppressWarnings("unchecked")
							GraphFlux<Object> newGraphFlux = GraphFlux.of(graphFluxNodeId, entry.getKey(),
									castedFlux.getFlux(), castedFlux.getMapResult(), castedFlux.getChunkResult());
							graphFlux = newGraphFlux;
						}

						graphFluxList.add(graphFlux);
						graphFluxNodeIds.add(graphFluxNodeId);
					} else if (value instanceof Flux flux) {
						// Traditional Flux - wrap it in GraphFlux for unified processing
						GraphFlux<Object> graphFlux = GraphFlux.of(effectiveNodeId, entry.getKey(), flux, null, null);
						graphFluxList.add(graphFlux);
					} else {
						// Regular object - add to merged state
						Map<String, Object> singleEntryMap = Map.of(entry.getKey(), value);
						mergedState = OverAllState.updateState(mergedState, singleEntryMap, channels);
					}
				}
			}

			// Handle the results based on what we found
			if (!graphFluxList.isEmpty()) {
				// We have GraphFlux instances - create ParallelGraphFlux with node identity
				// preservation
				ParallelGraphFlux parallelGraphFlux = ParallelGraphFlux.of(graphFluxList);

				mergedState.put("__parallel_graph_flux__", parallelGraphFlux);
				return mergedState;
			} else {
				Map<String, Object> initialState = new HashMap<>();
				// No streaming output, directly merge all results
				return results.stream()
						.reduce(initialState,
								(result, actionResult) -> OverAllState.updateState(result, actionResult, channels));
			}
		}

		/**
		 * Generate effective node ID for parallel execution.
		 * This ensures each parallel branch has a unique and traceable identifier.
		 */
		private String generateEffectiveNodeId(AsyncNodeActionWithConfig action, int index) {
			// Try to extract meaningful identifier from action
			String actionClass = action.getClass().getSimpleName();
			return String.format("%s_parallel_%d_%s", nodeId, index, actionClass);
		}
	}

	/**
	 * Constructs a new ParallelNode instance.
	 *
	 * @param id the identifier of the parallel node (will be formatted with {@link #PARALLEL_PREFIX})
	 * @param targetNodeId the ID of the target node that follows the parallel branches (the merge node).
	 *                     This is used to look up the aggregation strategy configuration (ANY_OF or ALL_OF)
	 *                     from RunnableConfig. The strategy determines whether to wait for all branches to
	 *                     complete (ALL_OF) or proceed with the first completed branch (ANY_OF).
	 * @param actions the list of actions to be executed in parallel. Each action represents a branch
	 *                in the parallel execution flow.
	 * @param actionNodeIds the list of node IDs corresponding to each action. Must have the same size
	 *                      as the actions list. Each ID identifies the actual node being executed in
	 *                      the corresponding parallel branch.
	 * @param channels the key strategy map that defines how state values are merged when multiple
	 *                 parallel branches produce results with the same keys. Keys in this map correspond
	 *                 to state keys, and values define the merge strategy (e.g., AppendStrategy, ReplaceStrategy).
	 * @param compileConfig the compilation configuration containing lifecycle listeners and other
	 *                      compile-time settings that affect how the parallel node is executed.
	 */
	public ParallelNode(String id, String targetNodeId, List<AsyncNodeActionWithConfig> actions, List<String> actionNodeIds,
			Map<String, KeyStrategy> channels, CompileConfig compileConfig) {
		super(formatNodeId(id),
				(config) -> new AsyncParallelNodeAction(formatNodeId(id), targetNodeId, actions, actionNodeIds, channels,
						compileConfig));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}
