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
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CachingToolCallbackProviderTest {

	@Test
	void toolsAreCachedAfterFirstCall() {
		var loadCounter = new AtomicInteger(0);
		var provider = new TestProvider(loadCounter);

		var tools1 = provider.getToolCallbacks();
		var tools2 = provider.getToolCallbacks();

		assertThat(tools1).isSameAs(tools2);
		assertThat(loadCounter.get()).isEqualTo(1);
	}

	@Test
	void cacheIsInvalidatedOnMcpToolsChangedEvent() {
		var loadCounter = new AtomicInteger(0);
		var provider = new TestProvider(loadCounter);

		provider.getToolCallbacks();
		provider.onApplicationEvent(new McpToolsChangedEvent(this, "test-server"));
		provider.getToolCallbacks();

		assertThat(loadCounter.get()).isEqualTo(2);
	}

	@Test
	void toolsAreReloadedAfterCacheInvalidation() {
		var loadCounter = new AtomicInteger(0);
		var provider = new TestProvider(loadCounter);

		var tools1 = provider.getToolCallbacks();
		provider.invalidateCache();
		var tools2 = provider.getToolCallbacks();

		assertThat(tools1).isNotSameAs(tools2);
		assertThat(loadCounter.get()).isEqualTo(2);
	}

	@Test
	void doubleCheckedLockingIsThreadSafe() throws InterruptedException {
		var loadCounter = new AtomicInteger(0);
		var provider = new TestProvider(loadCounter);
		var threadCount = 10;
		var latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			new Thread(() -> {
				provider.getToolCallbacks();
				latch.countDown();
			}).start();
		}

		latch.await();
		assertThat(loadCounter.get()).isEqualTo(1);
	}

	private static class TestProvider extends CachingToolCallbackProvider {

		private final AtomicInteger loadCounter;

		TestProvider(AtomicInteger loadCounter) {
			this.loadCounter = loadCounter;
		}

		@Override
		protected ToolCallback[] loadToolCallbacks() {
			loadCounter.incrementAndGet();
			return new ToolCallback[0];
		}

	}

}
