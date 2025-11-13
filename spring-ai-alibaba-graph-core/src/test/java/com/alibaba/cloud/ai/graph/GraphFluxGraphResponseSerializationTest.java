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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that GraphResponse objects returned through GraphFlux are safely converted to
 * serializable structures when merged into OverAllState.
 */
class GraphFluxGraphResponseSerializationTest {

	@Test
	void graphResponseErrorFromGraphFluxIsStoredAsSerializableMap() throws Exception {
		AsyncNodeAction streamNode = node_async(state -> Map.of("poem_result",
				GraphFlux.of("streamNode", "poem_result",
					Flux.just(GraphResponse.<NodeOutput>error(new IllegalStateException("Invalid API key"))), null, null)));

		AsyncNodeAction collectorNode = node_async(state -> Map.of("collector", "done"));

		StateGraph graph = new StateGraph()
			.addNode("streamNode", streamNode)
			.addNode("collector", collectorNode)
			.addEdge(START, "streamNode")
			.addEdge("streamNode", "collector")
			.addEdge("collector", END);

		Optional<OverAllState> result = graph.compile().invoke(Map.of());
		assertTrue(result.isPresent(), "Graph execution should produce a state");

		OverAllState finalState = result.get();
		Optional<Object> poemResult = finalState.value("poem_result");
		assertTrue(poemResult.isPresent(), "poem_result should be present in the final state");

		Object storedValue = poemResult.get();
		assertTrue(storedValue instanceof Map, "GraphResponse should be converted to a Map snapshot");

		@SuppressWarnings("unchecked")
		Map<String, Object> snapshot = (Map<String, Object>) storedValue;
		assertEquals("error", snapshot.get("status"), "Snapshot should capture error status");
		assertEquals(Boolean.TRUE, snapshot.get("error"), "Snapshot should indicate error state");
	}

}
