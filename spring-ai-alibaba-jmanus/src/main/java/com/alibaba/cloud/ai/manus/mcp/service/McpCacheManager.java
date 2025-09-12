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
package com.alibaba.cloud.ai.manus.mcp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.mcp.config.McpProperties;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.manus.mcp.repository.McpConfigRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * MCP Cache Manager - supports seamless cache updates
 */
@Component
public class McpCacheManager {

	private static final Logger logger = LoggerFactory.getLogger(McpCacheManager.class);

	/**
	 * MCP connection result wrapper class
	 */
	private static class McpConnectionResult {

		private final boolean success;

		private final McpServiceEntity serviceEntity;

		private final String serverName;

		private final String errorMessage;

		private final long connectionTime;

		private final int retryCount;

		private final String connectionType;

		public McpConnectionResult(boolean success, McpServiceEntity serviceEntity, String serverName,
				String errorMessage, long connectionTime, int retryCount, String connectionType) {
			this.success = success;
			this.serviceEntity = serviceEntity;
			this.serverName = serverName;
			this.errorMessage = errorMessage;
			this.connectionTime = connectionTime;
			this.retryCount = retryCount;
			this.connectionType = connectionType;
		}

		public boolean isSuccess() {
			return success;
		}

		public McpServiceEntity getServiceEntity() {
			return serviceEntity;
		}

		public String getServerName() {
			return serverName;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public long getConnectionTime() {
			return connectionTime;
		}

		public int getRetryCount() {
			return retryCount;
		}

		public String getConnectionType() {
			return connectionType;
		}

	}

	/**
	 * Double cache wrapper - implements seamless updates
	 */
	private static class DoubleCacheWrapper {

		private volatile Map<String, McpServiceEntity> activeCache = new ConcurrentHashMap<>();

		private volatile Map<String, McpServiceEntity> backgroundCache = new ConcurrentHashMap<>();

		private final Object switchLock = new Object();

		/**
		 * Atomically switch cache
		 */
		public void switchCache() {
			synchronized (switchLock) {
				Map<String, McpServiceEntity> temp = activeCache;
				activeCache = backgroundCache;
				backgroundCache = temp;
			}
		}

		/**
		 * Get current active cache
		 */
		public Map<String, McpServiceEntity> getActiveCache() {
			return activeCache;
		}

		/**
		 * Get background cache (for building new data)
		 */
		public Map<String, McpServiceEntity> getBackgroundCache() {
			return backgroundCache;
		}

		/**
		 * Update background cache
		 */
		public void updateBackgroundCache(Map<String, McpServiceEntity> newCache) {
			backgroundCache = new ConcurrentHashMap<>(newCache);
		}

	}

	private final McpConnectionFactory connectionFactory;

	private final McpConfigRepository mcpConfigRepository;

	private final McpProperties mcpProperties;

	private final ManusProperties manusProperties;

	// Double cache wrapper
	private final DoubleCacheWrapper doubleCache = new DoubleCacheWrapper();

	// Thread pool management
	private final AtomicReference<ExecutorService> connectionExecutorRef = new AtomicReference<>();

	// Scheduled task executor
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "McpCacheUpdateTask");
		t.setDaemon(true);
		return t;
	});

	private ScheduledFuture<?> updateTask;

	private volatile int lastConfigHash = 0;

	// Cache update interval (10 minutes)
	private static final long CACHE_UPDATE_INTERVAL_MINUTES = 10;

	public McpCacheManager(McpConnectionFactory connectionFactory, McpConfigRepository mcpConfigRepository,
			McpProperties mcpProperties, ManusProperties manusProperties) {
		this.connectionFactory = connectionFactory;
		this.mcpConfigRepository = mcpConfigRepository;
		this.mcpProperties = mcpProperties;
		this.manusProperties = manusProperties;

		// Initialize thread pool
		updateConnectionExecutor();
	}

	/**
	 * Automatically load cache on startup
	 */
	@PostConstruct
	public void initializeCache() {
		logger.info("Initializing MCP cache manager with double buffer mechanism");

		try {
			// Load initial cache on startup
			Map<String, McpServiceEntity> initialCache = loadMcpServices(
					mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE));

			// Set both active cache and background cache
			doubleCache.updateBackgroundCache(initialCache);
			doubleCache.switchCache(); // Switch to initial cache

			logger.info("Initial cache loaded successfully with {} services", initialCache.size());

			// Start scheduled update task
			startScheduledUpdate();

		}
		catch (Exception e) {
			logger.error("Failed to initialize cache", e);
		}
	}

	/**
	 * Start scheduled update task
	 */
	private void startScheduledUpdate() {
		if (updateTask != null && !updateTask.isCancelled()) {
			updateTask.cancel(false);
		}

		updateTask = scheduledExecutor.scheduleAtFixedRate(this::updateCacheTask, CACHE_UPDATE_INTERVAL_MINUTES,
				CACHE_UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES);

		logger.info("Scheduled cache update task started, interval: {} minutes", CACHE_UPDATE_INTERVAL_MINUTES);
	}

	/**
	 * Scheduled cache update task
	 */
	private void updateCacheTask() {
		try {
			logger.debug("Starting scheduled cache update task");

			// Query all enabled configurations
			List<McpConfigEntity> configs = mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE);

			// Build new data in background cache
			Map<String, McpServiceEntity> newCache = loadMcpServices(configs);

			// Update background cache
			doubleCache.updateBackgroundCache(newCache);

			// Atomically switch cache
			doubleCache.switchCache();

			logger.info("Cache updated successfully via scheduled task, services count: {}", newCache.size());

		}
		catch (Exception e) {
			logger.error("Failed to update cache via scheduled task", e);
		}
	}

	/**
	 * Update connection thread pool (supports dynamic configuration adjustment)
	 */
	private void updateConnectionExecutor() {
		int currentConfigHash = calculateConfigHash();

		// Check if configuration has changed
		if (currentConfigHash != lastConfigHash) {
			logger.info("MCP service loader configuration changed, updating thread pool");

			// Close old thread pool
			ExecutorService oldExecutor = connectionExecutorRef.get();
			if (oldExecutor != null && !oldExecutor.isShutdown()) {
				shutdownExecutor(oldExecutor);
			}

			// Create new thread pool
			int maxConcurrentConnections = manusProperties.getMcpMaxConcurrentConnections();
			ExecutorService newExecutor = Executors.newFixedThreadPool(maxConcurrentConnections);
			connectionExecutorRef.set(newExecutor);

			lastConfigHash = currentConfigHash;
			logger.info("Updated MCP service loader thread pool with max {} concurrent connections",
					maxConcurrentConnections);
		}
	}

	/**
	 * Calculate configuration hash value for detecting configuration changes
	 */
	private int calculateConfigHash() {
		int hash = 17;
		hash = 31 * hash + manusProperties.getMcpConnectionTimeoutSeconds();
		hash = 31 * hash + manusProperties.getMcpMaxRetryCount();
		hash = 31 * hash + manusProperties.getMcpMaxConcurrentConnections();
		return hash;
	}

	/**
	 * Safely shutdown thread pool
	 */
	private void shutdownExecutor(ExecutorService executor) {
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
					if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
						logger.warn("Thread pool did not terminate");
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
	 * Get current connection thread pool
	 */
	private ExecutorService getConnectionExecutor() {
		// Check if configuration needs to be updated
		updateConnectionExecutor();
		return connectionExecutorRef.get();
	}

	/**
	 * Load MCP services (parallel processing version)
	 * @param mcpConfigEntities MCP configuration entity list
	 * @return MCP service entity mapping
	 * @throws IOException Thrown when loading fails
	 */
	private Map<String, McpServiceEntity> loadMcpServices(List<McpConfigEntity> mcpConfigEntities) throws IOException {
		Map<String, McpServiceEntity> toolCallbackMap = new ConcurrentHashMap<>();

		if (mcpConfigEntities == null || mcpConfigEntities.isEmpty()) {
			logger.info("No MCP server configurations found");
			return toolCallbackMap;
		}

		// Record main thread start time
		long mainStartTime = System.currentTimeMillis();
		logger.info("Loading {} MCP server configurations in parallel", mcpConfigEntities.size());

		// Get current configured thread pool
		ExecutorService executor = getConnectionExecutor();

		// Create connections in parallel
		List<CompletableFuture<McpConnectionResult>> futures = mcpConfigEntities.stream()
			.map(config -> CompletableFuture.supplyAsync(() -> createConnectionWithRetry(config), executor))
			.collect(Collectors.toList());

		// Wait for all tasks to complete, set timeout
		CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		try {
			// Set overall timeout (using current configuration)
			allFutures.get(manusProperties.getMcpConnectionTimeoutSeconds(), TimeUnit.SECONDS);

			// Collect results
			for (int i = 0; i < mcpConfigEntities.size(); i++) {
				try {
					McpConnectionResult result = futures.get(i).get();
					if (result.isSuccess()) {
						toolCallbackMap.put(result.getServerName(), result.getServiceEntity());
					}
				}
				catch (Exception e) {
					String serverName = mcpConfigEntities.get(i).getMcpServerName();
					logger.error("Failed to get result for MCP server: {}", serverName, e);
				}
			}
		}
		catch (Exception e) {
			logger.error("Timeout or error occurred during parallel MCP connection creation", e);
			// Try to get completed results
			for (int i = 0; i < futures.size(); i++) {
				if (futures.get(i).isDone()) {
					try {
						McpConnectionResult result = futures.get(i).get();
						if (result.isSuccess()) {
							toolCallbackMap.put(result.getServerName(), result.getServiceEntity());
						}
					}
					catch (Exception ex) {
						logger.debug("Failed to get completed result for index: {}", i, ex);
					}
				}
			}
		}

		// Calculate main thread total time
		long mainEndTime = System.currentTimeMillis();
		long mainTotalTime = mainEndTime - mainStartTime;

		// Collect all results for detailed log output
		List<McpConnectionResult> allResults = new ArrayList<>();
		for (int i = 0; i < mcpConfigEntities.size(); i++) {
			try {
				if (futures.get(i).isDone()) {
					allResults.add(futures.get(i).get());
				}
			}
			catch (Exception e) {
				// If getting result fails, create a failed result record
				String serverName = mcpConfigEntities.get(i).getMcpServerName();
				allResults.add(new McpConnectionResult(false, null, serverName, "Failed to get result", 0, 0, "N/A"));
			}
		}

		// Output detailed execution log
		logger.info("\n"
				+ "╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗\n"
				+ "║                                    MCP Service Loader Execution Report                                ║\n"
				+ "╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣\n"
				+ "║  Main Thread: Started at {}, Completed at {}, Total Time: {}ms                                      ║\n"
				+ "║  Configuration: Timeout={}s, MaxRetry={}, MaxConcurrent={}                                           ║\n"
				+ "║  Summary: {}/{} servers loaded successfully                                                         ║\n"
				+ "╠══════════════════════════════════════════════════════════════════════════════════════════════════════╣\n"
				+ "║  Individual Server Results:                                                                          ║\n"
				+ "{}"
				+ "╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝",
				formatTime(mainStartTime), formatTime(mainEndTime), mainTotalTime,
				manusProperties.getMcpConnectionTimeoutSeconds(), manusProperties.getMcpMaxRetryCount(),
				manusProperties.getMcpMaxConcurrentConnections(), toolCallbackMap.size(), mcpConfigEntities.size(),
				formatIndividualResults(allResults));

		return toolCallbackMap;
	}

	/**
	 * Connection creation method with retry
	 * @param config MCP configuration entity
	 * @return Connection result
	 */
	private McpConnectionResult createConnectionWithRetry(McpConfigEntity config) {
		String serverName = config.getMcpServerName();
		String connectionType = config.getConnectionType().toString();
		long startTime = System.currentTimeMillis();
		int retryCount = 0;

		// Try to connect, retry at most MAX_RETRY_COUNT times
		for (int attempt = 0; attempt <= manusProperties.getMcpMaxRetryCount(); attempt++) {
			try {
				McpServiceEntity serviceEntity = connectionFactory.createConnection(config);

				if (serviceEntity != null) {
					long connectionTime = System.currentTimeMillis() - startTime;
					return new McpConnectionResult(true, serviceEntity, serverName, null, connectionTime, retryCount,
							connectionType);
				}
				else {
					if (attempt == manusProperties.getMcpMaxRetryCount()) {
						long connectionTime = System.currentTimeMillis() - startTime;
						return new McpConnectionResult(false, null, serverName, "Service entity is null",
								connectionTime, retryCount, connectionType);
					}
					logger.debug("Attempt {} failed for server: {}, retrying...", attempt + 1, serverName);
					retryCount++;
				}
			}
			catch (Exception e) {
				if (attempt == manusProperties.getMcpMaxRetryCount()) {
					long connectionTime = System.currentTimeMillis() - startTime;
					return new McpConnectionResult(false, null, serverName, e.getMessage(), connectionTime, retryCount,
							connectionType);
				}
				logger.debug("Attempt {} failed for server: {}, error: {}, retrying...", attempt + 1, serverName,
						e.getMessage());
				retryCount++;
			}
		}

		// This line should theoretically never be reached, but for compilation safety
		long connectionTime = System.currentTimeMillis() - startTime;
		return new McpConnectionResult(false, null, serverName, "Max retry attempts exceeded", connectionTime,
				retryCount, connectionType);
	}

	/**
	 * Get MCP services (uniformly use default cache)
	 * @param planId Plan ID (use default if null)
	 * @return MCP service entity mapping
	 */
	public Map<String, McpServiceEntity> getOrLoadServices(String planId) {
		try {
			// planId is not used.
			// Directly read active cache, no locking needed, ensures seamless operation
			Map<String, McpServiceEntity> activeCache = doubleCache.getActiveCache();

			return new ConcurrentHashMap<>(activeCache);
		}
		catch (Exception e) {
			logger.error("Failed to get MCP services for plan: {}", planId, e);
			return new ConcurrentHashMap<>();
		}
	}

	/**
	 * Get MCP service entity list
	 * @param planId Plan ID
	 * @return MCP service entity list
	 */
	public List<McpServiceEntity> getServiceEntities(String planId) {
		try {
			return new ArrayList<>(getOrLoadServices(planId).values());
		}
		catch (Exception e) {
			logger.error("Failed to get MCP service entities for plan: {}", planId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * Manually trigger cache reload
	 */
	public void triggerCacheReload() {
		try {
			logger.info("Manually triggering cache reload");

			// Query all enabled configurations
			List<McpConfigEntity> configs = mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE);

			// Build new data in background cache
			Map<String, McpServiceEntity> newCache = loadMcpServices(configs);

			// Update background cache
			doubleCache.updateBackgroundCache(newCache);

			// Atomically switch cache
			doubleCache.switchCache();

			logger.info("Manual cache reload completed, services count: {}", newCache.size());

		}
		catch (Exception e) {
			logger.error("Failed to manually reload cache", e);
		}
	}

	/**
	 * Clear cache (compatibility method, actually uses double cache mechanism)
	 * @param planId Plan ID
	 */
	public void invalidateCache(String planId) {
		logger.info("Cache invalidation requested for plan: {}, but using double buffer mechanism - no action needed",
				planId);
		// Under double cache mechanism, no need to manually clear cache, will auto-update
	}

	/**
	 * Clear all cache (compatibility method, actually uses double cache mechanism)
	 */
	public void invalidateAllCache() {
		logger.info("All cache invalidation requested, but using double buffer mechanism - triggering reload instead");
		// Trigger reload instead of clearing
		triggerCacheReload();
	}

	/**
	 * Refresh cache (compatibility method, actually uses double cache mechanism)
	 * @param planId Plan ID
	 */
	public void refreshCache(String planId) {
		logger.info("Cache refresh requested for plan: {}, triggering reload", planId);
		triggerCacheReload();
	}

	/**
	 * Get cache statistics
	 * @return Cache statistics
	 */
	public String getCacheStats() {
		Map<String, McpServiceEntity> activeCache = doubleCache.getActiveCache();
		return String.format("Double Buffer Cache Stats - Active Services: %d, Last Update: %s", activeCache.size(),
				formatTime(System.currentTimeMillis()));
	}

	/**
	 * Manually update connection configuration (supports runtime dynamic adjustment)
	 */
	public void updateConnectionConfiguration() {
		logger.info("Manually updating MCP service loader configuration");
		updateConnectionExecutor();
	}

	/**
	 * Get current connection configuration information
	 * @return Configuration information string
	 */
	public String getConnectionConfigurationInfo() {
		return String.format("MCP Service Loader Config - Timeout: %ds, MaxRetry: %d, MaxConcurrent: %d",
				manusProperties.getMcpConnectionTimeoutSeconds(), manusProperties.getMcpMaxRetryCount(),
				manusProperties.getMcpMaxConcurrentConnections());
	}

	/**
	 * Get cache update configuration information
	 * @return Cache update configuration information
	 */
	public String getCacheUpdateConfigurationInfo() {
		return String.format("Cache Update Config - Interval: %d minutes, Double Buffer: enabled",
				CACHE_UPDATE_INTERVAL_MINUTES);
	}

	/**
	 * Close resources (called when application shuts down)
	 */
	@PreDestroy
	public void shutdown() {
		logger.info("Shutting down MCP cache manager");

		// Stop scheduled task
		if (updateTask != null && !updateTask.isCancelled()) {
			updateTask.cancel(false);
		}

		// Close scheduled executor
		if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
			scheduledExecutor.shutdown();
			try {
				if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduledExecutor.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				scheduledExecutor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}

		// Close connection thread pool
		ExecutorService executor = connectionExecutorRef.get();
		if (executor != null) {
			shutdownExecutor(executor);
		}

		// Close all MCP client connections
		Map<String, McpServiceEntity> activeCache = doubleCache.getActiveCache();
		for (McpServiceEntity serviceEntity : activeCache.values()) {
			try {
				serviceEntity.getMcpAsyncClient().close();
			}
			catch (Throwable t) {
				logger.error("Failed to close MCP client", t);
			}
		}

		logger.info("MCP cache manager shutdown completed");
	}

	private String formatTime(long time) {
		return String.format("%tF %tT", time, time);
	}

	private String formatIndividualResults(List<McpConnectionResult> results) {
		StringBuilder sb = new StringBuilder();
		for (McpConnectionResult result : results) {
			String status = result.isSuccess() ? "✅ Success" : "❌ Failed";
			String errorInfo = result.getErrorMessage() != null ? (result.getErrorMessage().length() > 15
					? result.getErrorMessage().substring(0, 12) + "..." : result.getErrorMessage()) : "N/A";

			sb.append(String.format("║  %-20s | %-12s | Type: %-8s | Time: %-6dms | Retry: %-2d | Error: %-15s ║\n",
					result.getServerName(), status, result.getConnectionType(), result.getConnectionTime(),
					result.getRetryCount(), errorInfo));
		}
		return sb.toString();
	}

}
