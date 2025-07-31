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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

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

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.config.McpProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.repository.McpConfigRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * MCP缓存管理器 - 支持无闪断缓存更新
 */
@Component
public class McpCacheManager {

	private static final Logger logger = LoggerFactory.getLogger(McpCacheManager.class);

	/**
	 * MCP连接结果封装类
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
	 * 双缓存包装器 - 实现无闪断更新
	 */
	private static class DoubleCacheWrapper {

		private volatile Map<String, McpServiceEntity> activeCache = new ConcurrentHashMap<>();

		private volatile Map<String, McpServiceEntity> backgroundCache = new ConcurrentHashMap<>();

		private final Object switchLock = new Object();

		/**
		 * 原子性切换缓存
		 */
		public void switchCache() {
			synchronized (switchLock) {
				Map<String, McpServiceEntity> temp = activeCache;
				activeCache = backgroundCache;
				backgroundCache = temp;
			}
		}

		/**
		 * 获取当前活跃缓存
		 */
		public Map<String, McpServiceEntity> getActiveCache() {
			return activeCache;
		}

		/**
		 * 获取后台缓存（用于构建新数据）
		 */
		public Map<String, McpServiceEntity> getBackgroundCache() {
			return backgroundCache;
		}

		/**
		 * 更新后台缓存
		 */
		public void updateBackgroundCache(Map<String, McpServiceEntity> newCache) {
			backgroundCache = new ConcurrentHashMap<>(newCache);
		}

	}

	private final McpConnectionFactory connectionFactory;

	private final McpConfigRepository mcpConfigRepository;

	private final McpProperties mcpProperties;

	private final ManusProperties manusProperties;

	// 双缓存包装器
	private final DoubleCacheWrapper doubleCache = new DoubleCacheWrapper();

	// 线程池管理
	private final AtomicReference<ExecutorService> connectionExecutorRef = new AtomicReference<>();

	// 定时任务执行器
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "McpCacheUpdateTask");
		t.setDaemon(true);
		return t;
	});

	private ScheduledFuture<?> updateTask;

	private volatile int lastConfigHash = 0;

	// 缓存更新间隔（10分钟）
	private static final long CACHE_UPDATE_INTERVAL_MINUTES = 10;

	public McpCacheManager(McpConnectionFactory connectionFactory, McpConfigRepository mcpConfigRepository,
			McpProperties mcpProperties, ManusProperties manusProperties) {
		this.connectionFactory = connectionFactory;
		this.mcpConfigRepository = mcpConfigRepository;
		this.mcpProperties = mcpProperties;
		this.manusProperties = manusProperties;

		// 初始化线程池
		updateConnectionExecutor();
	}

	/**
	 * 启动时自动加载缓存
	 */
	@PostConstruct
	public void initializeCache() {
		logger.info("Initializing MCP cache manager with double buffer mechanism");

		try {
			// 启动时加载初始缓存
			Map<String, McpServiceEntity> initialCache = loadMcpServices(
					mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE));

			// 同时设置活跃缓存和后台缓存
			doubleCache.updateBackgroundCache(initialCache);
			doubleCache.switchCache(); // 切换到初始缓存

			logger.info("Initial cache loaded successfully with {} services", initialCache.size());

			// 启动定时更新任务
			startScheduledUpdate();

		}
		catch (Exception e) {
			logger.error("Failed to initialize cache", e);
		}
	}

	/**
	 * 启动定时更新任务
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
	 * 定时更新缓存任务
	 */
	private void updateCacheTask() {
		try {
			logger.debug("Starting scheduled cache update task");

			// 查询所有enable的配置
			List<McpConfigEntity> configs = mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE);

			// 在后台缓存中构建新数据
			Map<String, McpServiceEntity> newCache = loadMcpServices(configs);

			// 更新后台缓存
			doubleCache.updateBackgroundCache(newCache);

			// 原子性切换缓存
			doubleCache.switchCache();

			logger.info("Cache updated successfully via scheduled task, services count: {}", newCache.size());

		}
		catch (Exception e) {
			logger.error("Failed to update cache via scheduled task", e);
		}
	}

	/**
	 * 更新连接线程池（支持配置动态调整）
	 */
	private void updateConnectionExecutor() {
		int currentConfigHash = calculateConfigHash();

		// 检查配置是否发生变化
		if (currentConfigHash != lastConfigHash) {
			logger.info("MCP service loader configuration changed, updating thread pool");

			// 关闭旧的线程池
			ExecutorService oldExecutor = connectionExecutorRef.get();
			if (oldExecutor != null && !oldExecutor.isShutdown()) {
				shutdownExecutor(oldExecutor);
			}

			// 创建新的线程池
			int maxConcurrentConnections = manusProperties.getMcpMaxConcurrentConnections();
			ExecutorService newExecutor = Executors.newFixedThreadPool(maxConcurrentConnections);
			connectionExecutorRef.set(newExecutor);

			lastConfigHash = currentConfigHash;
			logger.info("Updated MCP service loader thread pool with max {} concurrent connections",
					maxConcurrentConnections);
		}
	}

	/**
	 * 计算配置哈希值，用于检测配置变更
	 */
	private int calculateConfigHash() {
		int hash = 17;
		hash = 31 * hash + manusProperties.getMcpConnectionTimeoutSeconds();
		hash = 31 * hash + manusProperties.getMcpMaxRetryCount();
		hash = 31 * hash + manusProperties.getMcpMaxConcurrentConnections();
		return hash;
	}

	/**
	 * 安全关闭线程池
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
	 * 获取当前连接线程池
	 */
	private ExecutorService getConnectionExecutor() {
		// 检查配置是否需要更新
		updateConnectionExecutor();
		return connectionExecutorRef.get();
	}

	/**
	 * 加载MCP服务（并行处理版本）
	 * @param mcpConfigEntities MCP配置实体列表
	 * @return MCP服务实体映射
	 * @throws IOException 加载失败时抛出异常
	 */
	private Map<String, McpServiceEntity> loadMcpServices(List<McpConfigEntity> mcpConfigEntities) throws IOException {
		Map<String, McpServiceEntity> toolCallbackMap = new ConcurrentHashMap<>();

		if (mcpConfigEntities == null || mcpConfigEntities.isEmpty()) {
			logger.info("No MCP server configurations found");
			return toolCallbackMap;
		}

		// 记录主线程开始时间
		long mainStartTime = System.currentTimeMillis();
		logger.info("Loading {} MCP server configurations in parallel", mcpConfigEntities.size());

		// 获取当前配置的线程池
		ExecutorService executor = getConnectionExecutor();

		// 并行创建连接
		List<CompletableFuture<McpConnectionResult>> futures = mcpConfigEntities.stream()
			.map(config -> CompletableFuture.supplyAsync(() -> createConnectionWithRetry(config), executor))
			.collect(Collectors.toList());

		// 等待所有任务完成，设置超时
		CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		try {
			// 设置整体超时（使用当前配置）
			allFutures.get(manusProperties.getMcpConnectionTimeoutSeconds(), TimeUnit.SECONDS);

			// 收集结果
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
			// 尝试获取已完成的结果
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

		// 计算主线程总耗时
		long mainEndTime = System.currentTimeMillis();
		long mainTotalTime = mainEndTime - mainStartTime;

		// 收集所有结果用于详细日志输出
		List<McpConnectionResult> allResults = new ArrayList<>();
		for (int i = 0; i < mcpConfigEntities.size(); i++) {
			try {
				if (futures.get(i).isDone()) {
					allResults.add(futures.get(i).get());
				}
			}
			catch (Exception e) {
				// 如果获取结果失败，创建一个失败的结果记录
				String serverName = mcpConfigEntities.get(i).getMcpServerName();
				allResults.add(new McpConnectionResult(false, null, serverName, "Failed to get result", 0, 0, "N/A"));
			}
		}

		// 输出详细的执行日志
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
	 * 带重试的连接创建方法
	 * @param config MCP配置实体
	 * @return 连接结果
	 */
	private McpConnectionResult createConnectionWithRetry(McpConfigEntity config) {
		String serverName = config.getMcpServerName();
		String connectionType = config.getConnectionType().toString();
		long startTime = System.currentTimeMillis();
		int retryCount = 0;

		// 尝试连接，最多重试MAX_RETRY_COUNT次
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

		// 这行代码理论上不会执行到，但为了编译安全
		long connectionTime = System.currentTimeMillis() - startTime;
		return new McpConnectionResult(false, null, serverName, "Max retry attempts exceeded", connectionTime,
				retryCount, connectionType);
	}

	/**
	 * 获取MCP服务（统一使用default缓存）
	 * @param planId 计划ID（如果为null则使用default）
	 * @return MCP服务实体映射
	 */
	public Map<String, McpServiceEntity> getOrLoadServices(String planId) {
		try {
			// planId不使用。
			// 直接读取活跃缓存，无需加锁，保证无闪断
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
	 * 手动触发缓存重新加载
	 */
	public void triggerCacheReload() {
		try {
			logger.info("Manually triggering cache reload");

			// 查询所有enable的配置
			List<McpConfigEntity> configs = mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE);

			// 在后台缓存中构建新数据
			Map<String, McpServiceEntity> newCache = loadMcpServices(configs);

			// 更新后台缓存
			doubleCache.updateBackgroundCache(newCache);

			// 原子性切换缓存
			doubleCache.switchCache();

			logger.info("Manual cache reload completed, services count: {}", newCache.size());

		}
		catch (Exception e) {
			logger.error("Failed to manually reload cache", e);
		}
	}

	/**
	 * 清除缓存（兼容性方法，实际使用双缓存机制）
	 * @param planId 计划ID
	 */
	public void invalidateCache(String planId) {
		logger.info("Cache invalidation requested for plan: {}, but using double buffer mechanism - no action needed",
				planId);
		// 双缓存机制下，不需要手动清除缓存，会自动更新
	}

	/**
	 * 清除所有缓存（兼容性方法，实际使用双缓存机制）
	 */
	public void invalidateAllCache() {
		logger.info("All cache invalidation requested, but using double buffer mechanism - triggering reload instead");
		// 触发重新加载而不是清除
		triggerCacheReload();
	}

	/**
	 * 刷新缓存（兼容性方法，实际使用双缓存机制）
	 * @param planId 计划ID
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
	 * 手动更新连接配置（支持运行时动态调整）
	 */
	public void updateConnectionConfiguration() {
		logger.info("Manually updating MCP service loader configuration");
		updateConnectionExecutor();
	}

	/**
	 * 获取当前连接配置信息
	 * @return 配置信息字符串
	 */
	public String getConnectionConfigurationInfo() {
		return String.format("MCP Service Loader Config - Timeout: %ds, MaxRetry: %d, MaxConcurrent: %d",
				manusProperties.getMcpConnectionTimeoutSeconds(), manusProperties.getMcpMaxRetryCount(),
				manusProperties.getMcpMaxConcurrentConnections());
	}

	/**
	 * 获取缓存更新配置信息
	 * @return 缓存更新配置信息
	 */
	public String getCacheUpdateConfigurationInfo() {
		return String.format("Cache Update Config - Interval: %d minutes, Double Buffer: enabled",
				CACHE_UPDATE_INTERVAL_MINUTES);
	}

	/**
	 * 关闭资源（在应用关闭时调用）
	 */
	@PreDestroy
	public void shutdown() {
		logger.info("Shutting down MCP cache manager");

		// 停止定时任务
		if (updateTask != null && !updateTask.isCancelled()) {
			updateTask.cancel(false);
		}

		// 关闭定时执行器
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

		// 关闭连接线程池
		ExecutorService executor = connectionExecutorRef.get();
		if (executor != null) {
			shutdownExecutor(executor);
		}

		// 关闭所有MCP客户端连接
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
