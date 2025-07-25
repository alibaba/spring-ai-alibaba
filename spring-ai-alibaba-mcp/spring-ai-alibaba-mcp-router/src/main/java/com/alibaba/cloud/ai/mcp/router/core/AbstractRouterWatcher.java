/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRouterWatcher {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractRouterWatcher.class);

	protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public AbstractRouterWatcher() {
		startScheduledPolling();
	}

	protected void startScheduledPolling() {
		long interval = Math.max(1, getPollingInterval()); // 保证最小为1秒
		scheduler.scheduleAtFixedRate(this::watch, interval, interval, TimeUnit.SECONDS);
		logger.info("Started router watcher polling, interval: {}s", interval);
	}

	public void stop() {
		scheduler.shutdown();
		logger.info("Stopped router watcher polling");
	}

	protected void watch() {
		try {
			handleChange();
		}
		catch (Exception e) {
			logger.error("Error in router watcher", e);
		}
	}

	protected abstract void handleChange();

	protected long getPollingInterval() {
		return 30L; // 默认30秒
	}

}
