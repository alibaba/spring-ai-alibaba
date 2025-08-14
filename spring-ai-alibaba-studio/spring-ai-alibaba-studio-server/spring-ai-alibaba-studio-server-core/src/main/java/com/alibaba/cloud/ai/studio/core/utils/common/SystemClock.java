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

package com.alibaba.cloud.ai.studio.core.utils.common;

import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * System clock implementation that provides efficient time access. Uses a background
 * thread to update the current time at regular intervals.
 *
 * @since 1.0.0.3
 */
public class SystemClock {

	/**
	 * Clock update interval in milliseconds
	 */
	private final long period;

	/**
	 * Current time in milliseconds
	 */
	private volatile long now;

	/**
	 * Constructor
	 */
	private SystemClock(long period) {
		this.period = period;
		this.now = System.currentTimeMillis();
		scheduleClockUpdating();
	}

	/**
	 * Starts the clock update scheduler
	 */
	private void scheduleClockUpdating() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "System Clock");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				now = System.currentTimeMillis();
			}
		}, period, period, TimeUnit.MILLISECONDS);
	}

	/**
	 * @return Current time in milliseconds
	 */
	private long currentTimeMillis() {
		return now;
	}

	// ------------------------------------------------------------------------ static

	/**
	 * Singleton holder
	 */
	private static class InstanceHolder {

		public static final SystemClock INSTANCE = new SystemClock(1);

	}

	/**
	 * @return Singleton instance
	 */
	private static SystemClock instance() {
		return InstanceHolder.INSTANCE;
	}

	/**
	 * @return Current time in milliseconds
	 */
	public static long now() {
		return instance().currentTimeMillis();
	}

	/**
	 * @return Current time as a string
	 */
	public static String nowDate() {
		return new Timestamp(instance().currentTimeMillis()).toString();
	}

}
