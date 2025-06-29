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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckpointTest {

	// Test using the following code in Memory Saver, since the '_checkpointsByThread' is
	// private.
	@Test
	public void concurrentExceptionTest() throws Exception {
		var memorySaver = new MemorySaver();
		ExecutorService executorService = Executors.newCachedThreadPool();
		int count = 50;
		CountDownLatch latch = new CountDownLatch(count);
		var index = new AtomicInteger(0);
		var futures = new ArrayList<Future<?>>();

		for (int i = 0; i < count; i++) {

			var future = executorService.submit(() -> {
				try {

					var threadName = format("thread-%d", index.incrementAndGet());
					System.out.println(threadName);
					memorySaver.list(RunnableConfig.builder().threadId(threadName).build());

				}
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					latch.countDown();
				}
			});

			futures.add(future);
		}

		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();

		for (var future : futures) {

			// assertTrue(future.isDone());
			assertNull(future.get());
		}

		int size = memorySaver.get_checkpointsByThread().size();
		// size must be equals to count

		assertEquals(count, size, "Checkpoint Lost during concurrency");
	}

}
