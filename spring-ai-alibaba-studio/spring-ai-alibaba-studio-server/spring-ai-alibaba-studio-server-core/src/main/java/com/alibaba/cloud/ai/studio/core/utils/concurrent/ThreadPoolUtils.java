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

package com.alibaba.cloud.ai.studio.core.utils.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for managing thread pools in the application.
 *
 * @since 1.0.0.3
 */
public class ThreadPoolUtils {

	/**
	 * Default name for the task executor thread pool
	 */
	private static final String DEFAULT_TASK_EXECUTOR_NAME = "default-task-executor";

	/**
	 * Default task executor with queue size 1024, thread count 100-200, and fallback
	 * execution policy
	 */
	public static final ExecutorService DEFAULT_TASK_EXECUTOR = new RequestContextThreadPoolWrapper(
			new ThreadPoolExecutor(100, 200, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024),
					new ThreadFactoryBuilder().setNameFormat(DEFAULT_TASK_EXECUTOR_NAME + "-%d")
						.setDaemon(true)
						.build()));

	/**
	 * Thread pool names for workflow execution
	 */
	private final static String TASK_EXECUTOR_NAME = "WorkflowTaskExecutor";

	private final static String NODE_EXECUTOR_NAME = "WorkflowNodeExecutor";

	/**
	 * Thread pool for workflow task execution with queue size 100 and caller-runs policy
	 */
	public static final ExecutorService taskExecutorService = new RequestContextThreadPoolWrapper(
			new ThreadPoolExecutor(100, 200, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
					new ThreadFactoryBuilder().setNameFormat(TASK_EXECUTOR_NAME + "-%d").setDaemon(true).build(),
					new ThreadPoolExecutor.CallerRunsPolicy()));

	/**
	 * Thread pool for workflow node execution with queue size 100 and caller-runs policy
	 */
	public static final ExecutorService nodeExecutorService = new RequestContextThreadPoolWrapper(
			new ThreadPoolExecutor(100, 200, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
					new ThreadFactoryBuilder().setNameFormat(NODE_EXECUTOR_NAME + "-%d").setDaemon(true).build(),
					new ThreadPoolExecutor.CallerRunsPolicy()));

	/**
	 * Thread pool for plugin execution with queue size 50 and thread count 40-50
	 */
	public static final String TOOL_TASK_EXECUTOR_NAME = "tool-task-executor";

	public static final ExecutorService TOOL_TASK_EXECUTOR = new ThreadPoolExecutor(40, 50, 120, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(50),
			new ThreadFactoryBuilder().setNameFormat(TOOL_TASK_EXECUTOR_NAME + "-%d").setDaemon(true).build());

}
