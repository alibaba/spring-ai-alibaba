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

import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.StreamingModelInterceptor;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Deterministic unit tests for {@link InterceptorChain#applyStreamingInterceptors}.
 *
 * <p>Pin down the lifecycle guarantees that the streaming interceptor extension point
 * relies on:
 * <ul>
 *   <li>Per-subscription isolation of aggregator state and {@link ModelRequest} copies
 *       (so retries / multi-subscribe don't cross-contaminate)</li>
 *   <li>{@code onStreamChunk} returning {@code null} drops the chunk from downstream
 *       and from the aggregated message</li>
 *   <li>Exceptions thrown from {@code beforeStreamCall} fan out to every interceptor's
 *       {@code onStreamError} and surface as an upstream error to the subscriber</li>
 *   <li>In-place mutation of the request inside {@code beforeStreamCall} does not
 *       affect later subscriptions of the same wrapped Flux</li>
 * </ul>
 *
 * <p>These tests use mock chunks (not a real ChatModel) so they run in CI without
 * any DashScope credentials.
 */
class InterceptorChainStreamingTest {

	// ---------- helpers ----------

	private static ChatResponse chunk(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static ModelRequest newRequest() {
		return ModelRequest.builder()
				.messages(new ArrayList<>(List.of(new UserMessage("hi"))))
				.options(ToolCallingChatOptions.builder().build())
				.build();
	}

	// ---------- tests ----------

	@Test
	void multiSubscribe_aggregatorIsPerSubscription() {
		AtomicInteger completeCount = new AtomicInteger();
		AtomicReference<String> lastAggregated = new AtomicReference<>();

		StreamingModelInterceptor i = new StreamingModelInterceptor() {
			@Override
			public void afterStreamComplete(AssistantMessage m, ModelRequest r) {
				completeCount.incrementAndGet();
				lastAggregated.set(m.getText());
			}
		};

		Flux<ChatResponse> wrapped = InterceptorChain.applyStreamingInterceptors(
				List.of(i), Flux.just(chunk("a"), chunk("b")), newRequest());

		wrapped.blockLast();
		wrapped.blockLast();

		assertEquals(2, completeCount.get(), "afterStreamComplete should fire once per subscription");
		assertEquals("ab", lastAggregated.get(),
				"second subscription must see 'ab' (its own buffer), not 'abab' (shared buffer)");
	}

	@Test
	void onStreamChunkReturningNull_dropsChunkAndExcludesFromAggregate() {
		StreamingModelInterceptor dropAllB = new StreamingModelInterceptor() {
			@Override
			public ChatResponse onStreamChunk(ChatResponse c, ModelRequest r) {
				if ("b".equals(c.getResult().getOutput().getText())) {
					return null;
				}
				return c;
			}
		};

		AtomicReference<String> aggregated = new AtomicReference<>();
		StreamingModelInterceptor capture = new StreamingModelInterceptor() {
			@Override
			public void afterStreamComplete(AssistantMessage m, ModelRequest r) {
				aggregated.set(m.getText());
			}
		};

		Flux<ChatResponse> wrapped = InterceptorChain.applyStreamingInterceptors(
				List.of(dropAllB, capture),
				Flux.just(chunk("a"), chunk("b"), chunk("c")),
				newRequest());

		List<String> emitted = wrapped
				.map(c -> c.getResult().getOutput().getText())
				.collectList()
				.block();

		assertEquals(List.of("a", "c"), emitted, "dropped chunk must not reach downstream");
		assertEquals("ac", aggregated.get(),
				"aggregator must not include text from dropped chunks");
	}

	@Test
	void beforeStreamCallThrows_fansOutOnStreamErrorToAllInterceptors_andSignalsErrorDownstream() {
		AtomicInteger errorCalls = new AtomicInteger();
		AtomicReference<Throwable> errorSeen = new AtomicReference<>();

		RuntimeException boom = new RuntimeException("boom");
		StreamingModelInterceptor exploding = new StreamingModelInterceptor() {
			@Override
			public ModelRequest beforeStreamCall(ModelRequest r) {
				throw boom;
			}

			@Override
			public void onStreamError(Throwable t, ModelRequest r) {
				errorCalls.incrementAndGet();
				errorSeen.set(t);
			}
		};
		StreamingModelInterceptor neverInvoked = new StreamingModelInterceptor() {
			@Override
			public void onStreamError(Throwable t, ModelRequest r) {
				errorCalls.incrementAndGet();
			}
		};

		Flux<ChatResponse> wrapped = InterceptorChain.applyStreamingInterceptors(
				List.of(exploding, neverInvoked), Flux.just(chunk("x")), newRequest());

		RuntimeException thrown = assertThrows(RuntimeException.class, wrapped::blockLast,
				"the original exception must propagate to the subscriber");
		assertSame(boom, thrown, "downstream subscriber should observe the original throwable");

		assertEquals(2, errorCalls.get(),
				"every interceptor's onStreamError should fire when beforeStreamCall fails");
		assertSame(boom, errorSeen.get(), "onStreamError should receive the originating exception");
	}

	@Test
	void inPlaceContextMutation_doesNotLeakAcrossSubscriptions() {
		StreamingModelInterceptor mutator = new StreamingModelInterceptor() {
			@Override
			public ModelRequest beforeStreamCall(ModelRequest r) {
				r.getContext().put("counter",
						(int) r.getContext().getOrDefault("counter", 0) + 1);
				return r;
			}
		};

		AtomicReference<Object> seenCounter = new AtomicReference<>();
		StreamingModelInterceptor reader = new StreamingModelInterceptor() {
			@Override
			public ModelRequest beforeStreamCall(ModelRequest r) {
				seenCounter.set(r.getContext().get("counter"));
				return r;
			}
		};

		ModelRequest shared = newRequest();
		Flux<ChatResponse> wrapped = InterceptorChain.applyStreamingInterceptors(
				List.of(mutator, reader), Flux.just(chunk("a")), shared);

		wrapped.blockLast();
		Object firstSeen = seenCounter.get();
		wrapped.blockLast();
		Object secondSeen = seenCounter.get();

		assertEquals(1, firstSeen, "first subscription's mutator should bump counter to 1");
		assertEquals(1, secondSeen,
				"second subscription must start fresh — counter must be 1 again, not 2");
		assertNotNull(shared.getContext(),
				"original request's context should remain (and never grow) across subscriptions");
		assertEquals(0, shared.getContext().size(),
				"original request must not be mutated by interceptor in-place changes");
	}
}
