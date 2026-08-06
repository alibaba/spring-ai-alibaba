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
package com.alibaba.cloud.ai.graph.internal.node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #4875 Reproduction Test: ParallelNode's static initializer fails on hosts with
 * more than 100 CPU cores.
 *
 * <p>The default executor was built with an unbounded core pool size
 * ({@code max(cores * 2, 4)}) but a capped maximum pool size
 * ({@code min(cores * 4, 200)}). For any host reporting more than 100 available
 * processors the core pool size exceeds the maximum pool size, so
 * {@link java.util.concurrent.ThreadPoolExecutor}'s constructor throws
 * {@code IllegalArgumentException}. Because the executor is a static field, that failure
 * surfaced as {@code ExceptionInInitializerError} and left the class permanently
 * unusable for the lifetime of the JVM.
 */
class Issue4875ReproductionTest {

	/**
	 * The upper bound applied to the maximum pool size, see
	 * {@code ParallelNode#calculateMaximumPoolSize(int)}.
	 */
	private static final int MAX_POOL_SIZE_CAP = 200;

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 4, 8, 16, 50, 100, 101, 128, 256, 512 })
	void corePoolSizeNeverExceedsMaximumPoolSize(int cpuCores) {
		int corePoolSize = ParallelNode.calculateCorePoolSize(cpuCores);
		int maximumPoolSize = ParallelNode.calculateMaximumPoolSize(cpuCores);

		assertTrue(corePoolSize <= maximumPoolSize,
				"corePoolSize (" + corePoolSize + ") must not exceed maximumPoolSize (" + maximumPoolSize
						+ ") for " + cpuCores + " CPU cores");
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 4, 8, 16, 50, 100, 101, 128, 256, 512 })
	void threadPoolExecutorAcceptsTheCalculatedSizing(int cpuCores) {
		int corePoolSize = ParallelNode.calculateCorePoolSize(cpuCores);
		int maximumPoolSize = ParallelNode.calculateMaximumPoolSize(cpuCores);

		assertDoesNotThrow(() -> new java.util.concurrent.ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60L,
				java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(1000))
			.shutdownNow(), "ThreadPoolExecutor rejected the sizing calculated for " + cpuCores + " CPU cores");
	}

	/**
	 * Hosts with 100 cores or fewer were never affected; their sizing must stay exactly
	 * as before so the fix does not silently change existing behaviour.
	 */
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 4, 8, 16, 50, 100 })
	void sizingIsUnchangedForHostsUpTo100Cores(int cpuCores) {
		assertEquals(Math.max(cpuCores * 2, 4), ParallelNode.calculateCorePoolSize(cpuCores));
		assertEquals(Math.min(cpuCores * 4, MAX_POOL_SIZE_CAP), ParallelNode.calculateMaximumPoolSize(cpuCores));
	}

	/**
	 * Above the cap the core pool size saturates at the maximum pool size instead of
	 * growing past it.
	 */
	@ParameterizedTest
	@ValueSource(ints = { 101, 128, 256, 512 })
	void corePoolSizeSaturatesAtTheCapForLargeHosts(int cpuCores) {
		assertEquals(MAX_POOL_SIZE_CAP, ParallelNode.calculateCorePoolSize(cpuCores));
		assertEquals(MAX_POOL_SIZE_CAP, ParallelNode.calculateMaximumPoolSize(cpuCores));
	}

	/**
	 * Guards the actual regression: loading the class must not fail, whatever the core
	 * count of the machine running the build happens to be.
	 */
	@Test
	void classInitializationSucceeds() {
		assertDoesNotThrow(() -> Class.forName(ParallelNode.class.getName()));
	}

}
