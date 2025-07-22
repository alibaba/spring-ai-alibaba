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

package com.alibaba.cloud.ai.mcp.gateway.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MCP Gateway 工具监听器抽象基类 提供了通用的工具监听和更新功能
 */
public abstract class AbstractMcpGatewayToolsWatcher {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractMcpGatewayToolsWatcher.class);

	protected static final long DEFAULT_POLLING_INTERVAL = 30L;

	protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	protected final McpGatewayToolManager toolManager;

	public AbstractMcpGatewayToolsWatcher(McpGatewayToolManager toolManager) {
		// 验证参数
		if (toolManager == null) {
			throw new IllegalArgumentException("McpGatewayToolManager cannot be null");
		}
		this.toolManager = toolManager;
		// 启动定时任务
		this.startScheduledPolling();
	}

	/**
	 * 启动定时轮询
	 */
	protected void startScheduledPolling() {
		long pollingInterval = getPollingInterval();
		scheduler.scheduleAtFixedRate(this::watch, pollingInterval, pollingInterval, TimeUnit.SECONDS);
		logger.info("Started scheduled service polling with interval: {} seconds", pollingInterval);
	}

	/**
	 * 停止监听器
	 */
	public void stop() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
					if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
						logger.error("Scheduler did not terminate");
					}
				}
			}
			catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
				logger.error("Interrupted while waiting for scheduler to terminate", e);
			}
		}

		logger.info("Stopped scheduled service polling and cleared cache");
	}

	/**
	 * 监听服务变更
	 */
	protected void watch() {
		try {
			handleChange();
		}
		catch (Exception e) {
			logger.error("Error occurred during service watching", e);
		}
	}

	/**
	 * 处理服务变更
	 */
	protected abstract void handleChange();

	/**
	 * 获取轮询间隔
	 * @return 轮询间隔（秒）
	 */
	protected long getPollingInterval() {
		return DEFAULT_POLLING_INTERVAL;
	}

}
