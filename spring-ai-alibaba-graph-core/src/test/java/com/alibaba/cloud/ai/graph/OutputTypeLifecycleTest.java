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

import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;

class OutputTypeLifecycleTest {

	@Test
	void startAndEndShouldHaveLifecycleOutputType() throws GraphStateException {
		StateGraph graph = new StateGraph(() -> Map.of())
				.addNode("n1", state -> completedFuture(Map.of()))
				.addEdge(START, "n1")
				.addEdge("n1", END);

		CompiledGraph compiled = graph.compile();
		List<NodeOutput> outputs = compiled.stream(Map.of()).take(3).collectList().block();
		assertNotNull(outputs);
		assertEquals(3, outputs.size());

		NodeOutput start = outputs.get(0);
		assertTrue(start.isSTART());
		assertEquals(OutputType.GRAPH_START, start.getOutputType());

		NodeOutput n1 = outputs.get(1);
		assertEquals("n1", n1.node());
		assertInstanceOf(StreamingOutput.class, n1);
		assertEquals(OutputType.GRAPH_NODE_FINISHED, n1.getOutputType());

		NodeOutput end = outputs.get(2);
		assertTrue(end.isEND());
		assertEquals(OutputType.GRAPH_END, end.getOutputType());
	}
}
