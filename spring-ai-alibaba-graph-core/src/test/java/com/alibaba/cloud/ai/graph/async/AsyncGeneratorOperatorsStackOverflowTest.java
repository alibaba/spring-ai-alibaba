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

package com.alibaba.cloud.ai.graph.async;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncGeneratorOperatorsStackOverflowTest {

	@Test
	void forEachAsyncShouldProcessAllElementsWithoutStackOverflow() {
		// Given a generator with a large number of elements
		int largeNumberOfElements = 20000;
		var counter = new AtomicInteger(0);
		var stream = IntStream.range(0, largeNumberOfElements).iterator();

		AsyncGenerator<Integer> largeGenerator = () -> {
			if (!stream.hasNext()) {
				return AsyncGenerator.Data.done(null);
			}
			return AsyncGenerator.Data.of(CompletableFuture.completedFuture(stream.next()));
		};

		// When forEachAsync is called, it should not throw an exception
		assertDoesNotThrow(() -> {
			largeGenerator.forEachAsync(i -> counter.incrementAndGet()).join();
		});

		// Then all elements should be processed
		assertEquals(largeNumberOfElements, counter.get(), "All elements should have been processed");
	}

}
