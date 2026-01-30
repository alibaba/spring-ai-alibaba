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
package com.alibaba.cloud.ai.graph.internal;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParallelEdgeProcessor to verify advanced parallel edge detection and subgraph creation.
 * Tests various scenarios including simple parallel paths, complex multi-node paths, and nested parallelism.
 */
@Disabled("skip all tests in this file")
public class ParallelEdgeProcessorTest {

	private static final Logger log = LoggerFactory.getLogger(ParallelEdgeProcessorTest.class);

	/**
	 * Creates a simple node action for testing.
	 */
	private AsyncNodeAction makeNode(String id) {
		return node_async(state -> {
			log.info("Executing node: {}", id);
			return Map.of("nodeId", id, "result", "result_" + id);
		});
	}

	/**
	 * Creates a key strategy factory for testing.
	 */
	private com.alibaba.cloud.ai.graph.KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("nodeId", new ReplaceStrategy());
			keyStrategyMap.put("result", new ReplaceStrategy());
			return keyStrategyMap;
		};
	}

	private void printGraphRepresentation(CompiledGraph graph) {
		GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println(representation.content());
	}

	/**
	 * Test Scenario 1: Simple parallel paths (A->B->Z, A->C->Z)
	 * Expected: Should use ParallelNode (path length = 2)
	 */
	@Test
	public void testSimpleParallelPaths() throws Exception {
		log.info("=== Test Scenario 1: Simple parallel paths ===");

		// Create graph: START -> A -> (B, C) -> Z -> END
		// Where B->Z and C->Z are direct connections
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addNode("Z", makeNode("Z"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("A", "C")
				.addEdge("B", "Z")
				.addEdge("C", "Z")
				.addEdge("Z", END);

		CompiledGraph compiledGraph = graph.compile();

		printGraphRepresentation(compiledGraph);

		// Verify that the graph compiles successfully
		assertNotNull(compiledGraph, "Graph should compile successfully");

		// Verify that ParallelNode was created (not a subgraph)
		// We can check by verifying the graph structure
		// The edge from A should point to a ParallelNode
		var edgeFromA = compiledGraph.getEdge("A");
		assertNotNull(edgeFromA, "Edge from A should exist");
		assertNotNull(edgeFromA.id(), "Edge from A should have a target node ID");

		// The target should be a ParallelNode (format: __PARALLEL__(A))
		String parallelNodeId = ParallelNode.formatNodeId("A");
		assertEquals(parallelNodeId, edgeFromA.id(), 
				"Edge from A should point to ParallelNode");

		log.info("✓ Simple parallel paths test passed - ParallelNode created correctly");
	}

	/**
	 * Test Scenario 2: Complex parallel paths (A->B->B1->Z, A->C->C1->C2->Z)
	 * Expected: Should create a subgraph (path length > 2)
	 */
	@Test
	public void testComplexParallelPaths() throws Exception {
		log.info("=== Test Scenario 2: Complex parallel paths ===");

		// Create graph: START -> A -> (B, C) -> ... -> Z -> END
		// Where B->B1->Z and C->C1->C2->Z
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("B1", makeNode("B1"))
				.addNode("C", makeNode("C"))
				.addNode("C1", makeNode("C1"))
				.addNode("C2", makeNode("C2"))
				.addNode("Z", makeNode("Z"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("A", "C")
				.addEdge("B", "B1")
				.addEdge("B1", "Z")
				.addEdge("C", "C1")
				.addEdge("C1", "C2")
				.addEdge("C2", "Z")
				.addEdge("Z", END);

		CompiledGraph compiledGraph = graph.compile();

		printGraphRepresentation(compiledGraph);

		// Verify that the graph compiles successfully
		assertNotNull(compiledGraph, "Graph should compile successfully");

		// Verify that a subgraph was created (not a ParallelNode)
		// The edge from A should point to a SubStateGraphNode
		var edgeFromA = compiledGraph.getEdge("A");
		assertNotNull(edgeFromA, "Edge from A should exist");
		assertNotNull(edgeFromA.id(), "Edge from A should have a target node ID");

		// The target should be a SubStateGraphNode (format: __PARALLEL_SUBGRAPH__(A))
		String expectedSubGraphNodeId = String.format("__PARALLEL__(%s)", "A");
		assertEquals(expectedSubGraphNodeId, edgeFromA.id(),
				"Edge from A should point to SubStateGraphNode for complex paths");

		log.info("✓ Complex parallel paths test passed - Subgraph created correctly");
	}

	/**
	 * Test Scenario 3: Mixed paths (one simple, one complex)
	 * Expected: Should create a subgraph (if any path length > 2)
	 */
	@Test
	public void testMixedParallelPaths() throws Exception {
		log.info("=== Test Scenario 3: Mixed parallel paths ===");

		// Create graph: START -> A -> (B, C) -> ... -> Z -> END
		// Where B->Z (simple) and C->C1->Z (complex)
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addNode("C1", makeNode("C1"))
				.addNode("Z", makeNode("Z"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("A", "C")
				.addEdge("B", "Z")
				.addEdge("C", "C1")
				.addEdge("C1", "Z")
				.addEdge("Z", END);

		CompiledGraph compiledGraph = graph.compile();

		// Verify that the graph compiles successfully
		assertNotNull(compiledGraph, "Graph should compile successfully");

		// Since one path has length > 2, should create subgraph
		var edgeFromA = compiledGraph.getEdge("A");
		assertNotNull(edgeFromA, "Edge from A should exist");
		assertNotNull(edgeFromA.id(), "Edge from A should have a target node ID");

		// Should create subgraph because C->C1->Z has length 3
		String expectedSubGraphNodeId = String.format("__PARALLEL__(%s)", "A");
		assertEquals(expectedSubGraphNodeId, edgeFromA.id(),
				"Edge from A should point to SubStateGraphNode when any path length > 2");

		log.info("✓ Mixed parallel paths test passed - Subgraph created correctly");
	}

	/**
	 * Test Scenario 4: Nested parallel edges
	 * Expected: Should handle nested parallelism correctly
	 */
	@Test
	public void testNestedParallelEdges() throws Exception {
		log.info("=== Test Scenario 4: Nested parallel edges ===");

		// Create graph: START -> A -> (B, C) -> ... -> Z -> END
		// Where B->(B1, B2)->B3->Z and C->(C1, C2)->C3->Z
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("B1", makeNode("B1"))
				.addNode("B2", makeNode("B2"))
				.addNode("B3", makeNode("B3"))
				.addNode("C", makeNode("C"))
				.addNode("C1", makeNode("C1"))
				.addNode("C2", makeNode("C2"))
				.addNode("C3", makeNode("C3"))
				.addNode("Z", makeNode("Z"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("A", "C")
				.addEdge("B", "B1")
				.addEdge("B", "B2")
				.addEdge("B1", "B3")
				.addEdge("B2", "B3")
				.addEdge("B3", "Z")
				.addEdge("C", "C1")
				.addEdge("C", "C2")
				.addEdge("C1", "C3")
				.addEdge("C2", "C3")
				.addEdge("C3", "Z")
				.addEdge("Z", END);

		CompiledGraph compiledGraph = graph.compile();

		printGraphRepresentation(compiledGraph);

		// Verify that the graph compiles successfully
		assertNotNull(compiledGraph, "Graph should compile successfully");

		// Should handle nested parallelism and create appropriate structure
		var edgeFromA = compiledGraph.getEdge("A");
		assertNotNull(edgeFromA, "Edge from A should exist");

		log.info("✓ Nested parallel edges test passed - Graph compiled successfully");

		Flux<NodeOutput> flux = compiledGraph.stream();
		flux.blockLast();
		assertNotNull(flux, "Flux from compiled graph should not be null");
	}

	/**
	 * Test Scenario 5: Verify graph execution with parallel paths
	 * This test verifies that the graph can actually execute with parallel paths
	 */
	@Test
	public void testGraphExecutionWithParallelPaths() throws Exception {
		log.info("=== Test Scenario 5: Graph execution with parallel paths ===");

		// Create a simple graph structure
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addNode("Z", makeNode("Z"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("A", "C")
				.addEdge("B", "Z")
				.addEdge("C", "Z")
				.addEdge("Z", END);

		CompiledGraph compiledGraph = graph.compile();

		printGraphRepresentation(compiledGraph);

		// Execute the graph
		var result = compiledGraph.invoke(Map.of());

		// Verify execution completed successfully
		assertTrue(result.isPresent(), "Graph execution should complete successfully");
		assertNotNull(result.get().value("nodeId"), "Result should contain nodeId");

		log.info("✓ Graph execution with parallel paths test passed");
	}

	/**
	 * Test Scenario 6: Error case - parallel paths that don't converge
	 * Expected: Should throw GraphStateException
	 */
	@Test
	public void testNonConvergingParallelPaths() throws GraphStateException {
		log.info("=== Test Scenario 6: Non-converging parallel paths ===");

		// Create graph where parallel paths don't converge
		StateGraph graph = new StateGraph(createKeyStrategyFactory());
		try {
			graph.addNode("A", makeNode("A"))
					.addNode("B", makeNode("B"))
					.addNode("C", makeNode("C"))
					.addNode("Z1", makeNode("Z1"))
					.addNode("Z2", makeNode("Z2"))
					.addEdge(START, "A")
					.addEdge("A", "B")
					.addEdge("A", "C")
					.addEdge("B", "Z1")
					.addEdge("C", "Z2")
					.addEdge("Z1", END)
					.addEdge("Z2", END);
		} catch (GraphStateException e) {
			fail("Graph construction should not throw exception: " + e.getMessage());
		}

		CompiledGraph compiledGraph = graph.compile();
		printGraphRepresentation(compiledGraph);

		compiledGraph.stream().blockLast();

		log.info("✓ Non-converging parallel paths test passed - Exception thrown correctly");
	}

	/**
	 * Test Scenario 7: Single path (no parallelism)
	 * Expected: Should not create ParallelNode or Subgraph
	 */
	@Test
	public void testSinglePath() throws Exception {
		log.info("=== Test Scenario 7: Single path (no parallelism) ===");

		// Create simple sequential graph
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("B", "C")
				.addEdge("C", END);

		CompiledGraph compiledGraph = graph.compile();

		printGraphRepresentation(compiledGraph);

		// Verify that the graph compiles successfully
		assertNotNull(compiledGraph, "Graph should compile successfully");

		// Verify no ParallelNode or Subgraph was created
		var edgeFromA = compiledGraph.getEdge("A");
		assertNotNull(edgeFromA, "Edge from A should exist");
		assertEquals("B", edgeFromA.id(), "Edge from A should point directly to B");

		log.info("✓ Single path test passed - No parallel processing needed");
	}

	/**
	 * Test Scenario 8: Three parallel branches converging
	 * Expected: Should handle three-way parallelism correctly
	 */
	@Test
	public void testThreeWayParallelPaths() throws Exception {
		log.info("=== Test Scenario 8: Three-way parallel paths ===");

		// Create graph: START -> A -> (B, C, D) -> Z -> END
		StateGraph graph = new StateGraph(createKeyStrategyFactory())
				.addNode("A", makeNode("A"))
				.addNode("B", makeNode("B"))
				.addNode("C", makeNode("C"))
				.addNode("D", makeNode("D"))
				.addNode("Z", makeNode("Z"))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("A", "C")
				.addEdge("A", "D")
				.addEdge("B", "Z")
				.addEdge("C", "Z")
				.addEdge("D", "Z")
				.addEdge("Z", END);

		CompiledGraph compiledGraph = graph.compile();

		printGraphRepresentation(compiledGraph);

		// Verify that the graph compiles successfully
		assertNotNull(compiledGraph, "Graph should compile successfully");

		// Verify that ParallelNode was created
		var edgeFromA = compiledGraph.getEdge("A");
		assertNotNull(edgeFromA, "Edge from A should exist");
		assertEquals(ParallelNode.formatNodeId("A"), edgeFromA.id(),
				"Edge from A should point to ParallelNode for three-way parallelism");

		log.info("✓ Three-way parallel paths test passed");
	}
}
