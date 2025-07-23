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

package com.alibaba.cloud.ai.mcp.client.component;

import com.alibaba.cloud.ai.mcp.client.config.McpRecoveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author yingzi
 * @since 2025/7/20
 */

public class CommonUtil {

	private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

	private final McpRecoveryProperties mcpRecoveryProperties;

	private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();

	private final ExecutorService reconnectExecutor = Executors.newSingleThreadExecutor();

	public CommonUtil(McpRecoveryProperties mcpRecoveryProperties1) {
		this.mcpRecoveryProperties = mcpRecoveryProperties1;
	}

	public ScheduledExecutorService getPingScheduler() {
		return pingScheduler;
	}

	public ExecutorService getReconnectExecutor() {
		return reconnectExecutor;
	}

	public static String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

	public void stop() {
		pingScheduler.shutdown();
		logger.info("pingScheduler stop...");

		// 关闭异步任务线程池
		try {
			reconnectExecutor.shutdown();
			if (!reconnectExecutor.awaitTermination(mcpRecoveryProperties.getStop().getSeconds(), TimeUnit.SECONDS)) {
				reconnectExecutor.shutdownNow();
			}
			logger.info("reconnectExecutor stop successfully");
		}
		catch (InterruptedException e) {
			logger.error("reconnectExecutor stop error", e);
			reconnectExecutor.shutdownNow();
			Thread.currentThread().interrupt(); // 恢复中断状态
		}
	}

}
