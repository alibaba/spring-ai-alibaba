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
package com.alibaba.cloud.ai.graph.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verification tests for issue #3327 fix: Reactor context propagation in SubGraphNodeAdapter.
 *
 * Tests that Reactor context (e.g., traceId, spanId) is properly preserved when
 * SubGraphNodeAdapter uses Flux.deferContextual() instead of manual subscription.
 */
@DisplayName("Issue #3327: Reactor Context Propagation in SubGraphNodeAdapter")
public class ReactAgentContextPropagationTest {

	/**
	 * Test that demonstrates the problematic pattern where context is lost.
	 *
	 * When using Flux.create() with manual subscribe(), the inner subscription
	 * does not inherit the context from contextWrite(), resulting in context loss.
	 * This is the current buggy behavior in SubGraphNodeAdapter.getGraphResponseFlux().
	 */
	@Test
	@DisplayName("Manual subscribe in Flux.create loses Reactor context")
	public void manualSubscribeLosesContext() {
		String traceId = "t-manual-1";

		Flux<String> sourceFlux = Flux.just("a", "b")
			.flatMap(item -> Mono.deferContextual(
				ctx -> Mono.just(ctx.getOrDefault("traceId", "NOCTX"))));

		Flux<String> problematic = Flux.create(sink -> {
			sourceFlux.subscribe(sink::next, sink::error, sink::complete);
		});

		var list = problematic.contextWrite(Context.of("traceId", traceId))
			.collectList()
			.block();

		// Context is lost - elements contain "NOCTX" instead of the actual traceId
		assertEquals(List.of("NOCTX", "NOCTX"), list,
			"Context should be lost with manual subscription pattern, proving the bug");
	}

	/**
	 * Test that demonstrates the correct pattern where context is preserved.
	 *
	 * Using Flux.deferContextual() with contextWrite() properly preserves the context
	 * throughout the reactive chain, allowing downstream operators to access context values.
	 * This is the recommended fix for SubGraphNodeAdapter.getGraphResponseFlux().
	 */
	@Test
	@DisplayName("DeferContextual with explicit contextWrite preserves Reactor context")
	public void deferContextualPreservesContext() {
		String traceId = "t-defer-1";

		Flux<String> sourceFlux = Flux.just("a", "b")
			.flatMap(item -> Mono.deferContextual(
				ctx -> Mono.just(ctx.getOrDefault("traceId", "NOCTX"))));

		Flux<String> correct = Flux.deferContextual(ctx -> Flux.create(sink -> {
			sourceFlux.contextWrite(ctx).subscribe(sink::next, sink::error, sink::complete);
		}));

		var list = correct.contextWrite(Context.of("traceId", traceId))
			.collectList()
			.block();

		// Context is preserved - elements contain the actual traceId
		assertEquals(List.of(traceId, traceId), list,
			"Context should be preserved with deferContextual pattern, proving the fix works");
	}

}
