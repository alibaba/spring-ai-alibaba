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

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import com.alibaba.cloud.ai.graph.agent.interceptor.modelretry.ModelRetryInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ModelRetryInterceptorTest {

	@Test
	void testSuccessOnFirstAttempt() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(100)
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			attemptCount.incrementAndGet();
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		ModelRequest request = ModelRequest.builder().build();
		ModelResponse response = interceptor.interceptModel(request, handler);

		assertEquals(1, attemptCount.get(), "应该只调用一次");
		assertEquals("Success", ((AssistantMessage) response.getMessage()).getText());
	}

	@Test
	void testSuccessOnSecondAttempt() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(100)
			.backoffMultiplier(1.5)
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count == 1) {
				throw new RuntimeException("I/O error on POST request");
			}
			return ModelResponse.of(new AssistantMessage("Success on retry"));
		};

		ModelRequest request = ModelRequest.builder().build();
		ModelResponse response = interceptor.interceptModel(request, handler);

		assertEquals(2, attemptCount.get(), "应该调用两次（首次失败，第二次成功）");
		assertEquals("Success on retry", ((AssistantMessage) response.getMessage()).getText());
	}

	@Test
	void testMaxAttemptsReached() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(50)
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			attemptCount.incrementAndGet();
			throw new RuntimeException("I/O error: connection timeout");
		};

		ModelRequest request = ModelRequest.builder().build();
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			interceptor.interceptModel(request, handler);
		});

		assertEquals(3, attemptCount.get(), "应该尝试3次");
		assertTrue(exception.getMessage().contains("maximum number of retries reached"));
	}

	@Test
	void testNonRetryableException() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(50)
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			attemptCount.incrementAndGet();
			throw new RuntimeException("Authentication failed");
		};

		ModelRequest request = ModelRequest.builder().build();
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			interceptor.interceptModel(request, handler);
		});

		assertEquals(1, attemptCount.get(), "不可重试的异常应该只尝试一次");
		assertTrue(exception.getMessage().contains("non-retryable exception"));
	}

	@Test
	void testExceptionMessageRetry() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(50)
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 3) {
				// Simulate the message returned by AgentLlmNode after capturing an exception.
				return ModelResponse.of(new AssistantMessage("Exception: I/O error on POST request for \"https://api.deepseek.com/chat/completions\": Remote host terminated the handshake"));
			}
			return ModelResponse.of(new AssistantMessage("Success after retry"));
		};

		ModelRequest request = ModelRequest.builder().build();
		ModelResponse response = interceptor.interceptModel(request, handler);

		assertEquals(3, attemptCount.get(), "应该重试直到成功");
		assertEquals("Success after retry", ((AssistantMessage) response.getMessage()).getText());
	}

	@Test
	void testExponentialBackoff() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(100)
			.maxDelay(500)
			.backoffMultiplier(2.0)
			.build();

		long startTime = System.currentTimeMillis();
		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 3) {
				throw new RuntimeException("Connection timeout");
			}
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		ModelRequest request = ModelRequest.builder().build();
		ModelResponse response = interceptor.interceptModel(request, handler);
		long duration = System.currentTimeMillis() - startTime;

		assertEquals(3, attemptCount.get());
		// First retry: 100ms, Second retry: 200ms, Total at least 300ms
		assertTrue(duration >= 300, "应该有指数退避延迟");
		assertTrue(duration < 1000, "延迟不应该太长");
	}

	@Test
	void testCustomRetryablePredicate() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(50)
			.retryableExceptionPredicate(e -> e.getMessage().contains("custom-retry"))
			.build();

		AtomicInteger attemptCount = new AtomicInteger(0);
		
		// Test custom retryable exceptions
		ModelCallHandler handler1 = req -> {
			attemptCount.incrementAndGet();
			throw new RuntimeException("custom-retry error");
		};

		ModelRequest request = ModelRequest.builder().build();
		
		assertThrows(RuntimeException.class, () -> {
			interceptor.interceptModel(request, handler1);
		});
		assertEquals(3, attemptCount.get(), "自定义可重试异常应该重试");

		// Test cannot be retried exception
		attemptCount.set(0);
		ModelCallHandler handler2 = req -> {
			attemptCount.incrementAndGet();
			throw new RuntimeException("other error");
		};

		assertThrows(RuntimeException.class, () -> {
			interceptor.interceptModel(request, handler2);
		});
		assertEquals(1, attemptCount.get(), "非自定义可重试异常应该不重试");
	}

	@Test
	void testMaxDelayLimit() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(4)
			.initialDelay(100)
			.maxDelay(150)
			.backoffMultiplier(3.0)
			.build();

		long startTime = System.currentTimeMillis();
		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 4) {
				throw new RuntimeException("timeout");
			}
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		ModelRequest request = ModelRequest.builder().build();
		interceptor.interceptModel(request, handler);
		long duration = System.currentTimeMillis() - startTime;

		// First retry: 100ms, Second retry: 150ms (limit), Third retry: 150ms (limit)
		// Total at least 400ms, but should not exceed 600ms
		assertTrue(duration >= 400, "应该有延迟");
		assertTrue(duration < 600, "maxDelay 应该生效");
	}

	@Test
	void testZeroDelay() {
		ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
			.maxAttempts(3)
			.initialDelay(0)
			.build();

		long startTime = System.currentTimeMillis();
		AtomicInteger attemptCount = new AtomicInteger(0);
		
		ModelCallHandler handler = request -> {
			int count = attemptCount.incrementAndGet();
			if (count < 3) {
				throw new RuntimeException("I/O error");
			}
			return ModelResponse.of(new AssistantMessage("Success"));
		};

		ModelRequest request = ModelRequest.builder().build();
		interceptor.interceptModel(request, handler);
		long duration = System.currentTimeMillis() - startTime;

		assertEquals(3, attemptCount.get());
		assertTrue(duration < 100, "零延迟应该快速重试");
	}

	@Test
	void testBuilderValidation() {
		assertThrows(IllegalArgumentException.class, () -> {
			ModelRetryInterceptor.builder().maxAttempts(0).build();
		}, "maxAttempts 必须 >= 1");

		assertThrows(IllegalArgumentException.class, () -> {
			ModelRetryInterceptor.builder().initialDelay(-1).build();
		}, "initialDelay 必须 >= 0");

		assertThrows(IllegalArgumentException.class, () -> {
			ModelRetryInterceptor.builder().maxDelay(-1).build();
		}, "maxDelay 必须 >= 0");

		assertThrows(IllegalArgumentException.class, () -> {
			ModelRetryInterceptor.builder().backoffMultiplier(0.5).build();
		}, "backoffMultiplier 必须 >= 1.0");
	}
}

