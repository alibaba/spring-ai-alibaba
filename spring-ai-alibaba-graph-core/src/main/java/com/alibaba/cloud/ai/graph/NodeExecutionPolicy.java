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
package com.alibaba.cloud.ai.graph;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Execution options for a graph node. The timeout covers producing the result and
 * consuming any publisher returned by the action. Retries apply to failures of
 * the action before it returns a result. Actions with external side effects can
 * use the stable {@link RunnableConfig#taskId()} as an idempotency key.
 *
 * @param timeout deadline for one attempt, or null for no deadline
 * @param maxAttempts maximum attempts per graph invocation, including the first attempt
 * @param retryDelay delay before each retry
 * @param retryOn which action failures can be retried
 * @param blocking whether to run action invocation and stream subscription on a bounded worker pool
 */
public record NodeExecutionPolicy(Duration timeout, int maxAttempts, Duration retryDelay,
		Predicate<Throwable> retryOn, boolean blocking) {

	public NodeExecutionPolicy {
		if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
			throw new IllegalArgumentException("Node timeout must be positive");
		}
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("Max attempts must be at least one");
		}
		if (Objects.requireNonNull(retryDelay, "Retry delay must not be null").isNegative()) {
			throw new IllegalArgumentException("Retry delay must not be negative");
		}
		Objects.requireNonNull(retryOn, "Retry predicate must not be null");
	}

}
