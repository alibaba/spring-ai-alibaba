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
package com.alibaba.cloud.ai.graph.agent.tool;

import com.alibaba.cloud.ai.graph.agent.event.McpToolsChangedEvent;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for ToolCallbackProvider with caching and event-driven invalidation.
 */
public abstract class CachingToolCallbackProvider
		implements ToolCallbackProvider, ApplicationListener<McpToolsChangedEvent> {

	private final Lock lock = new ReentrantLock();

	private volatile boolean invalidateCache = true;

	private volatile ToolCallback[] cachedToolCallbacks;

	@Override
	public final ToolCallback[] getToolCallbacks() {
		if (invalidateCache) {
			lock.lock();
			try {
				if (invalidateCache) {
					cachedToolCallbacks = loadToolCallbacks();
					invalidateCache = false;
				}
			}
			finally {
				lock.unlock();
			}
		}
		return cachedToolCallbacks;
	}

	protected abstract ToolCallback[] loadToolCallbacks();

	public void invalidateCache() {
		this.invalidateCache = true;
	}

	@Override
	public void onApplicationEvent(McpToolsChangedEvent event) {
		invalidateCache();
	}

}
