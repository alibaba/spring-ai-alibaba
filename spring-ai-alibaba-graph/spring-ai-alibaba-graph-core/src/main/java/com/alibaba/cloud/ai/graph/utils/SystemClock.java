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

package com.alibaba.cloud.ai.graph.utils;

import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * Optimization for performance issues with System.currentTimeMillis() in high concurrency
 * scenarios. This approach is also used to implement timestamps in Redis's LRU list (very
 * efficient and useful)
 * </p>
 * <p>
 * Invoking System.currentTimeMillis() is much more time-consuming compared to creating a
 * regular object<br>
 * The reason System.currentTimeMillis() is slow is because it interacts with the
 * operating system<br>
 * The clock is updated periodically in the background, and the thread is automatically
 * reclaimed when the JVM exits<br>
 * Performance comparison data:<br>
 * 1 Billion calls: 43410 vs 206, a difference of 210.73%<br>
 * 100 Million calls: 4699 vs 29, a difference of 162.03%<br>
 * 10 Million calls: 480 vs 12, a difference of 40.0%<br>
 * 1 Million calls: 50 vs 10, a difference of 5.0%<br>
 * </p>
 *
 * @author disaster
 * @version 1.0.0.1
 */
public class SystemClock {

	private final long period;

	private final AtomicLong now;

	private SystemClock(long period) {
		this.period = period;
		this.now = new AtomicLong(System.currentTimeMillis());
		scheduleClockUpdating();
	}

	private static SystemClock instance() {
		return InstanceHolder.INSTANCE;
	}

	public static long now() {
		return instance().currentTimeMillis();
	}

	public static String nowDate() {
		return new Timestamp(instance().currentTimeMillis()).toString();
	}

	private void scheduleClockUpdating() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "System Clock");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleAtFixedRate(() -> now.set(System.currentTimeMillis()), period, period, TimeUnit.MILLISECONDS);
	}

	private long currentTimeMillis() {
		return now.get();
	}

	private static class InstanceHolder {

		public static final SystemClock INSTANCE = new SystemClock(1);

	}

}
