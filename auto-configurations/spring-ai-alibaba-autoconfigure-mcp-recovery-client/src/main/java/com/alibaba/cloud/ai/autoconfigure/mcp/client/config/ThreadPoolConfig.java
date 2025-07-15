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

package com.alibaba.cloud.ai.autoconfigure.mcp.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author yingzi
 * @since 2025/7/15
 */
@Configuration
public class ThreadPoolConfig {

	private static final Logger logger = LoggerFactory.getLogger(ThreadPoolConfig.class);

	/**
	 * 定时任务线程池，用于周期性执行 checkMcpClients 方法
	 */
	@Bean
	public ThreadPoolTaskScheduler pingScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("mcp-ping-scheduler-");
		scheduler.setDaemon(true);
		scheduler.setErrorHandler(ex -> logError("Ping task exception", ex));
		scheduler.setRejectedExecutionHandler((r, executor) -> logError("Ping task rejected",
				new RuntimeException("Task was rejected by the executor")));
		return scheduler;
	}

	/**
	 * 异步任务线程池，持续运行 processReconnectQueue 方法
	 */
	@Bean
	public ExecutorService reconnectExecutor() {
		ThreadFactory threadFactory = new ThreadFactory() {
			private int count = 0;

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "mcp-reconnect-thread-" + count++);
				t.setDaemon(true);
				t.setUncaughtExceptionHandler((thread, ex) -> logError("Reconnect task exception", thread, ex));
				return t;
			}
		};

		return Executors.newSingleThreadExecutor(threadFactory);
	}

	/**
	 * 统一异常日志输出方法
	 */
	private void logError(String context, Throwable ex) {
		logger.error("{}: {}%n", context, ex.getMessage());
	}

	private void logError(String context, Thread thread, Throwable ex) {
		logger.error("{} in thread {}: {}%n", context, thread.getName(), ex.getMessage());
	}

}
