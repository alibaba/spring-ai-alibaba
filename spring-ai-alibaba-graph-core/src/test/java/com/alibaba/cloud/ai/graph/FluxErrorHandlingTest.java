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

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FluxErrorHandlingTest {

	@Test
	public void testEmptyFluxShouldNotCauseInfiniteLoop() throws Exception {
		StateGraph workflow = new StateGraph();

		workflow.addNode("errorNode", state -> {
			Flux<ChatResponse> emptyFlux = Flux.just(new ChatResponse(null));
			Map<String, Object> result = new HashMap<>();
			result.put("output", emptyFlux);
			return CompletableFuture.completedFuture(result);
		});

		workflow.addEdge(START, "errorNode");
		workflow.addEdge("errorNode", END);

		var app = workflow.compile();

		long startTime = System.currentTimeMillis();
		AtomicBoolean completed = new AtomicBoolean(false);
		AtomicInteger responseCount = new AtomicInteger(0);

		app.stream(Map.of()).subscribe(
			response -> responseCount.incrementAndGet(),
			error -> completed.set(true),
			() -> completed.set(true)
		);

		Thread.sleep(1000);
		long duration = System.currentTimeMillis() - startTime;

		assertTrue(completed.get(), "Should complete");
		assertTrue(duration < 2000, "Should fail fast, actual duration: " + duration + "ms");

		assertTrue(responseCount.get() < 10, "Should not repeat execution, actual: " + responseCount.get());
	}

	@Test
	public void testValidFluxShouldWorkNormally() throws Exception {
		StateGraph workflow = new StateGraph();

		workflow.addNode("normalNode", state -> {
			Flux<ChatResponse> validFlux = Flux.just(
				new ChatResponse(List.of(
					new Generation(new AssistantMessage("Hello"), ChatGenerationMetadata.NULL)
				))
			);
			Map<String, Object> result = new HashMap<>();
			result.put("messages", validFlux);
			return CompletableFuture.completedFuture(result);
		});

		workflow.addEdge(START, "normalNode");
		workflow.addEdge("normalNode", END);

		var app = workflow.compile();

		AtomicInteger responseCount = new AtomicInteger(0);
		AtomicBoolean completed = new AtomicBoolean(false);

		app.stream(Map.of()).subscribe(
			response -> responseCount.incrementAndGet(),
			error -> {},
			() -> completed.set(true)
		);

		Thread.sleep(1000);

		assertTrue(completed.get(), "Should complete normally");
		assertTrue(responseCount.get() > 0, "Should receive responses");
	}
}
