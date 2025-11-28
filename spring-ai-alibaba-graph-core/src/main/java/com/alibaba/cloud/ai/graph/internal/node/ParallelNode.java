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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


public class ParallelNode extends Node {
	
	private static final Logger logger = LoggerFactory.getLogger(ParallelNode.class);

	public static final String PARALLEL_PREFIX = "__PARALLEL__";

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

	public record AsyncParallelNodeAction(String nodeId, List<AsyncNodeActionWithConfig> actions,
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

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
			for (int i = 0; i < actions.size(); i++) {
				AsyncNodeActionWithConfig action = actions.get(i);
				String actualNodeId = actionNodeIds.get(i);

				// Create a defensive copy of the state for each parallel action
				// This prevents race conditions if actions modify the state in-place
				OverAllState stateSnapshot = state.snapShot().orElse(new OverAllState());

				// First try to get node-specific executor, then default executor, finally use DEFAULT_EXECUTOR
				Executor executor = config.metadata(nodeId)
						.filter(value -> value instanceof Executor)
						.map(Executor.class::cast)
						.orElseGet(() -> config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY)
								.filter(value -> value instanceof Executor)
								.map(Executor.class::cast)
								.orElse(DEFAULT_EXECUTOR));
				
				CompletableFuture<Map<String, Object>> future = evalNodeActionAsync(action, actualNodeId, stateSnapshot, config, executor);

				futures.add(future);
			}

			// Wait for all tasks to complete
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
				// Collect all results
				List<Map<String, Object>> results = futures.stream()
						.map(CompletableFuture::join)
						.collect(Collectors.toList());

				return processParallelResults(results, state, actions);
			});
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

	public ParallelNode(String id, List<AsyncNodeActionWithConfig> actions, List<String> actionNodeIds,
			Map<String, KeyStrategy> channels, CompileConfig compileConfig) {
		super(formatNodeId(id),
				(config) -> new AsyncParallelNodeAction(formatNodeId(id), actions, actionNodeIds, channels,
						compileConfig));
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}
