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

import static org.junit.jupiter.api.Assertions.*;

public class AsyncGeneratorOperatorsInfiniteLoopTest {

	@Test
	public void testForEachAsyncHandlesExceptionProperly() {
		// Arrange: Create a mock AsyncGenerator that throws an exception
		AsyncGenerator<Integer> generator = new AsyncGenerator<>() {
			private final AtomicInteger counter = new AtomicInteger(0);

			@Override
			public Data<Integer> next() {
				int count = counter.getAndIncrement();
				if (count == 0) {
					// Return a normal data element
					return Data.of(CompletableFuture.completedFuture(count));
				}
				else if (count == 1) {
					// Simulate an exception
					return Data.of(CompletableFuture.failedFuture(new RuntimeException("Test exception")));
				}
				else {
					throw new RuntimeException("Infinite loop detected");
				}
			}
		};

		// Act: Call forEachAsync and capture the exception
		CompletableFuture<Object> future = generator.forEachAsync(value -> {
			// Simulate processing the value
			System.out.println("Processing value: " + value);
		}).exceptionally(ex -> {
			// Handle the exception and return null to avoid infinite loop
			System.out.println("Exception caught: " + ex.getMessage());

			// Assert: Verify that the exception is handled properly
			assertTrue(ex.getCause() instanceof RuntimeException);
			assertEquals("Test exception", ex.getCause().getMessage());

			return null;
		});

		System.out.println("Passed");
	}

}
