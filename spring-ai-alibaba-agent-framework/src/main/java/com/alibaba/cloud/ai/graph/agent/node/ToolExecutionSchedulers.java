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
package com.alibaba.cloud.ai.graph.agent.node;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

final class ToolExecutionSchedulers {

	private ToolExecutionSchedulers() {
	}

	static Scheduler blockingTools() {
		return BlockingTools.INSTANCE;
	}

	static Scheduler interceptors() {
		return Interceptors.INSTANCE;
	}

	private static int defaultThreads() {
		return Math.min(32, Math.max(8, Runtime.getRuntime().availableProcessors() * 4));
	}

	private static int positiveSetting(String key, int defaultValue) {
		int value = Integer.parseInt(System.getProperty(key, Integer.toString(defaultValue)));
		if (value < 1) {
			throw new IllegalArgumentException(key + " must be positive");
		}
		return value;
	}

	private static Scheduler scheduler(String name, int threads, BlockingQueue<Runnable> queue) {
		var number = new AtomicInteger();
		var executor = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, queue, task -> {
			var thread = new Thread(task, name + "-" + number.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}, new ThreadPoolExecutor.AbortPolicy());
		executor.allowCoreThreadTimeOut(true);
		return Schedulers.fromExecutorService(executor, name);
	}

	private static final class BlockingTools {

		private static final Scheduler INSTANCE = scheduler("saa-blocking-tool",
				positiveSetting("spring.ai.alibaba.tools.threads", defaultThreads()),
				new ArrayBlockingQueue<>(positiveSetting("spring.ai.alibaba.tools.queue-capacity", 128)));

	}

	private static final class Interceptors {

		// The public interceptor contract requires a synchronous handler. A separate
		// direct handoff keeps compatibility waits off tool and child subscription workers.
		// With no queue, recursive interceptor chains fail fast at saturation.
		private static final Scheduler INSTANCE = scheduler("saa-tool-interceptor", defaultThreads(),
				new SynchronousQueue<>());

	}

}
