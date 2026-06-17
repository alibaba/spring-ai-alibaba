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

import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompiledGraphStreamOrderTest {

	@Test
	void shouldKeepGraphResponseSourceOrderWhenOutputFuturesCompleteOutOfOrder() {
		OverAllState state = new OverAllState();
		Flux<GraphResponse<NodeOutput>> responses = Flux.just(
				GraphResponse.of(delayedOutput("chunk-1", 120, state)),
				GraphResponse.of(delayedOutput("chunk-2", 40, state)),
				GraphResponse.of(delayedOutput("chunk-3", 10, state)));

		List<String> chunks = CompiledGraph.flattenGraphResponsesPreservingOrder(responses)
			.cast(StreamingOutput.class)
			.map(StreamingOutput::getOriginData)
			.cast(String.class)
			.collectList()
			.block(Duration.ofSeconds(3));

		assertEquals(List.of("chunk-1", "chunk-2", "chunk-3"), chunks);
	}

	private static CompletableFuture<NodeOutput> delayedOutput(String chunk, long delayMillis, OverAllState state) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(delayMillis);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ex);
			}
			return new StreamingOutput<>(chunk, "llmNode", state);
		});
	}

}
