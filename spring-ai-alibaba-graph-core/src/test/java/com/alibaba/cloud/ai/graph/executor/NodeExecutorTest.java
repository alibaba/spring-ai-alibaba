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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NodeExecutor} state mapping.
 */
class NodeExecutorTest {

	@Test
	@SuppressWarnings("unchecked")
	void subgraphWrapperSnapshotExcludesInheritedWrapperValues() throws Exception {
		String wrapperKey = "subgraph_weather_workflow_compiled_graph";
		String ancestorWrapperKey = "subgraph_parent_router_compiled_graph";
		String siblingWrapperKey = "subgraph_other_workflow_compiled_graph";
		GraphFlux<NodeOutput> graphFlux = GraphFlux.of("weather_workflow", wrapperKey, Flux.empty());
		GraphResponse<NodeOutput> lastData = GraphResponse.done(Map.of(
				wrapperKey, Map.of("previous", "snapshot"),
				ancestorWrapperKey, Map.of("previous", "ancestor snapshot"),
				siblingWrapperKey, Map.of("previous", "sibling snapshot"),
				"messages", "current answer"));

		Map<String, Object> state = (Map<String, Object>) graphFluxResultState(graphFlux, lastData);

		assertTrue(state.containsKey(ancestorWrapperKey));
		assertTrue(state.containsKey(siblingWrapperKey));
		assertTrue(state.containsKey(wrapperKey));
		Map<String, Object> wrapperSnapshot = assertInstanceOf(Map.class, state.get(wrapperKey));
		assertFalse(wrapperSnapshot.containsKey(wrapperKey),
				"Subgraph wrapper snapshots must not nest the previous wrapper value");
		assertFalse(wrapperSnapshot.containsKey(ancestorWrapperKey),
				"Subgraph wrapper snapshots must not nest inherited ancestor wrappers");
		assertFalse(wrapperSnapshot.containsKey(siblingWrapperKey),
				"Subgraph wrapper snapshots must not nest inherited sibling wrappers");
		assertTrue(wrapperSnapshot.containsKey("messages"));
	}

	private Object graphFluxResultState(GraphFlux<?> graphFlux, Object lastData) throws Exception {
		NodeExecutor executor = new NodeExecutor(null);
		Method method = NodeExecutor.class.getDeclaredMethod("graphFluxResultState", GraphFlux.class, Object.class);
		method.setAccessible(true);
		return method.invoke(executor, graphFlux, lastData);
	}

}
