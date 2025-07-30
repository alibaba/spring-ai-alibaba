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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

/**
 * MCP缓存管理器
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

	private final McpConnectionFactory connectionFactory;

	private final McpConfigRepository mcpConfigRepository;

	private final McpProperties mcpProperties;

	private final ManusProperties manusProperties;

	private final LoadingCache<String, Map<String, McpServiceEntity>> toolCallbackMapCache;

	// 线程池管理
	private final AtomicReference<ExecutorService> connectionExecutorRef = new AtomicReference<>();

	private volatile int lastConfigHash = 0;

	public McpCacheManager(McpConnectionFactory connectionFactory, McpConfigRepository mcpConfigRepository,
			McpProperties mcpProperties, ManusProperties manusProperties) {
		this.connectionFactory = connectionFactory;
		this.mcpConfigRepository = mcpConfigRepository;
		this.mcpProperties = mcpProperties;
		this.manusProperties = manusProperties;

		this.toolCallbackMapCache = buildCache();
		// 初始化线程池
		updateConnectionExecutor();
	}

	/**
	 * 构建缓存
	 * @return 加载缓存
	 */
	private LoadingCache<String, Map<String, McpServiceEntity>> buildCache() {
		return CacheBuilder.newBuilder()
			.expireAfterAccess(mcpProperties.getCacheExpireAfterAccess().toMinutes(), TimeUnit.MINUTES)
			.removalListener((RemovalListener<String, Map<String, McpServiceEntity>>) notification -> {
				Map<String, McpServiceEntity> mcpServiceEntityMap = notification.getValue();
				if (mcpServiceEntityMap == null) {
					return;
				}
				for (McpServiceEntity mcpServiceEntity : mcpServiceEntityMap.values()) {
					try {
						mcpServiceEntity.getMcpAsyncClient().close();
					}
					catch (Throwable t) {
						logger.error("Failed to close MCP client", t);
					}
				}
			})
			.build(new CacheLoader<>() {
				@Override
				public Map<String, McpServiceEntity> load(String key) throws Exception {
					return loadMcpServices(mcpConfigRepository.findByStatus(McpConfigStatus.ENABLE));
				}
			});
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
	 * 获取或加载MCP服务
	 * @param planId 计划ID
	 * @return MCP服务实体映射
	 */
	public Map<String, McpServiceEntity> getOrLoadServices(String planId) {
		try {
			return toolCallbackMapCache.get(planId != null ? planId : "DEFAULT");
		}
		catch (Exception e) {
			logger.error("Failed to get or load MCP services for plan: {}", planId, e);
			return new ConcurrentHashMap<>();
		}
	}

	/**
	 * 获取MCP服务实体列表
	 * @param planId 计划ID
	 * @return MCP服务实体列表
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
	 * 清除缓存
	 * @param planId 计划ID
	 */
	public void invalidateCache(String planId) {
		toolCallbackMapCache.invalidate(planId != null ? planId : "DEFAULT");
		logger.info("Invalidated cache for plan: {}", planId);
	}

	/**
	 * 清除所有缓存
	 */
	public void invalidateAllCache() {
		toolCallbackMapCache.invalidateAll();
		logger.info("Invalidated all MCP service caches");
	}

	/**
	 * 刷新缓存
	 * @param planId 计划ID
	 */
	public void refreshCache(String planId) {
		toolCallbackMapCache.refresh(planId != null ? planId : "DEFAULT");
		logger.info("Refreshed cache for plan: {}", planId);
	}

	/**
	 * 获取缓存统计信息
	 * @return 缓存统计信息
	 */
	public String getCacheStats() {
		return toolCallbackMapCache.stats().toString();
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
	 * 关闭资源（在应用关闭时调用）
	 */
	public void shutdown() {
		logger.info("Shutting down MCP cache manager");
		ExecutorService executor = connectionExecutorRef.get();
		if (executor != null) {
			shutdownExecutor(executor);
		}
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
