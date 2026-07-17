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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link ToolRetryInterceptor} that exercise the retry loop directly
 * with a mock {@link com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler}, so no
 * model / API key is required. Guards against the off-by-one regression where the loop
 * performed one attempt too few, skipped the tool entirely for a single-attempt config
 * (leading to an NPE), and slept an unused backoff after the final failure. Also verifies
 * the {@code maxAttempts} API and the deprecated {@code maxRetries} alias stay in sync.
 */
class ToolRetryInterceptorUnitTest {

	private ToolCallRequest request() {
		return new ToolCallRequest("t", "{}", "id-1", new HashMap<>());
	}

	private ToolRetryInterceptor.Builder builder(int maxAttempts) {
		return ToolRetryInterceptor.builder().maxAttempts(maxAttempts).initialDelay(1).jitter(false);
	}

	@Test
	void runsAllConfiguredAttempts() {
		AtomicInteger calls = new AtomicInteger();
		ToolCallResponse response = builder(3).build().interceptToolCall(request(), r -> {
			calls.incrementAndGet();
			throw new RuntimeException("boom");
		});
		assertEquals(3, calls.get());
		assertTrue(response.getResult().contains("3 attempts"));
	}

	@Test
	void twoAttemptsMeansOneRetry() {
		AtomicInteger calls = new AtomicInteger();
		builder(2).build().interceptToolCall(request(), r -> {
			calls.incrementAndGet();
			throw new RuntimeException("boom");
		});
		assertEquals(2, calls.get());
	}

	@Test
	void singleAttemptRunsToolExactlyOnceWithoutNpe() {
		AtomicInteger calls = new AtomicInteger();
		ToolCallResponse response = builder(1).build().interceptToolCall(request(), r -> {
			calls.incrementAndGet();
			throw new RuntimeException("boom");
		});
		// The tool must still execute once, and no NullPointerException must be thrown.
		assertEquals(1, calls.get());
		assertTrue(response.getResult().contains("1 attempts"));
	}

	@Test
	void stopsRetryingOnceSuccessful() {
		AtomicInteger calls = new AtomicInteger();
		ToolCallResponse response = builder(6).build().interceptToolCall(request(), r -> {
			if (calls.incrementAndGet() < 3) {
				throw new RuntimeException("transient");
			}
			return ToolCallResponse.success(r.getToolCallId(), r.getToolName(), "ok");
		});
		assertEquals(3, calls.get());
		assertEquals(ToolCallResponse.SUCCESS_STATUS, response.getStatus());
		assertEquals("ok", response.getResult());
	}

	@Test
	void raiseBehaviorThrowsAfterAllAttempts() {
		AtomicInteger calls = new AtomicInteger();
		ToolRetryInterceptor interceptor = builder(3)
			.onFailure(ToolRetryInterceptor.OnFailureBehavior.RAISE)
			.build();
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> interceptor.interceptToolCall(request(), r -> {
					calls.incrementAndGet();
					throw new RuntimeException("boom");
				}));
		assertEquals(3, calls.get());
		assertTrue(ex.getMessage().contains("3 attempts"));
	}

	@Test
	void nonRetryableExceptionIsRethrownImmediately() {
		AtomicInteger calls = new AtomicInteger();
		ToolRetryInterceptor interceptor = builder(4)
			.retryOn(IllegalStateException.class) // only retry ISE
			.build();
		assertThrows(IllegalArgumentException.class,
				() -> interceptor.interceptToolCall(request(), r -> {
					calls.incrementAndGet();
					throw new IllegalArgumentException("not retryable");
				}));
		assertEquals(1, calls.get());
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedMaxRetriesAliasMapsToAttempts() {
		// maxRetries(n) == maxAttempts(n + 1)
		AtomicInteger calls = new AtomicInteger();
		ToolRetryInterceptor.builder().maxRetries(2).initialDelay(1).jitter(false).build()
			.interceptToolCall(request(), r -> {
				calls.incrementAndGet();
				throw new RuntimeException("boom");
			});
		assertEquals(3, calls.get());

		// The previously buggy edge case: maxRetries(0) must still run the tool once, not zero times.
		AtomicInteger zeroCalls = new AtomicInteger();
		ToolRetryInterceptor.builder().maxRetries(0).initialDelay(1).jitter(false).build()
			.interceptToolCall(request(), r -> {
				zeroCalls.incrementAndGet();
				throw new RuntimeException("boom");
			});
		assertEquals(1, zeroCalls.get());
	}
}
