package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelNodeStateLossReproductionTest {

	private AsyncNodeAction streamingNodeWithoutKey(String nodeId, String stateKey) {
		return node_async(state -> {
			Flux<String> stream = Flux.just(nodeId + "-chunk-1", nodeId + "-chunk-2")
				.delayElements(Duration.ofMillis(5));
			GraphFlux<String> graphFlux = GraphFlux.of(nodeId, stream);
			return Map.of(stateKey, graphFlux);
		});
	}

	private AsyncNodeAction mergeObserverNode() {
		return node_async(state -> {
			Optional<Object> translateValue = state.value("translate_content");
			return Map.of("mergeObservedTranslateContent", translateValue.orElse("missing"));
		});
	}

	@Test
	void graphFluxWithoutExplicitKeyShouldSurviveParallelExecution() throws Exception {
		var sequentialGraph = new StateGraph()
			.addNode("translateNode", streamingNodeWithoutKey("translateNode", "translate_content"))
			.addNode("mergeNode", mergeObserverNode())
			.addEdge(START, "translateNode")
			.addEdge("translateNode", "mergeNode")
			.addEdge("mergeNode", END)
			.compile();

		OverAllState sequentialState = sequentialGraph.invoke(Map.of()).orElseThrow();
		assertTrue(sequentialState.value("translate_content").isPresent(),
				"Sequential execution should register translate_content via GraphFlux");
		assertTrue(sequentialState.value("mergeObservedTranslateContent").isPresent(),
				"Merge node should observe translate_content in sequential execution");

		var parallelGraph = new StateGraph()
			.addNode("translateNode", streamingNodeWithoutKey("translateNode", "translate_content"))
			.addNode("expandNode", streamingNodeWithoutKey("expandNode", "expand_content"))
			.addNode("mergeNode", mergeObserverNode())
			.addEdge(START, "translateNode")
			.addEdge(START, "expandNode")
			.addEdge("translateNode", "mergeNode")
			.addEdge("expandNode", "mergeNode")
			.addEdge("mergeNode", END)
			.compile();

		final OverAllState[] parallelFinalStateHolder = new OverAllState[1];
		parallelGraph.stream(Map.of(),
				RunnableConfig.builder().addParallelNodeExecutor(START, Executors.newFixedThreadPool(3)).build())
			.doOnNext(step -> parallelFinalStateHolder[0] = step.state())
			.blockLast();

		OverAllState parallelState = parallelFinalStateHolder[0];
		assertNotNull(parallelState, "Parallel execution should produce a final state");

		assertTrue(parallelState.value("translate_content").isPresent(),
				"translate_content should survive parallel execution");
		assertTrue(parallelState.value("expand_content").isPresent(),
				"expand_content should survive parallel execution");
		assertTrue(parallelState.value("mergeObservedTranslateContent").isPresent(),
				"Merge node should observe translate_content during parallel execution");

		assertEquals(sequentialState.value("translate_content").get(), parallelState.value("translate_content").get(),
				"Parallel execution should preserve the same translate_content result as sequential execution");
		assertEquals(parallelState.value("translate_content").get(), parallelState.value("mergeObservedTranslateContent").get(),
				"Merge node should observe the translate_content result");
	}

}

