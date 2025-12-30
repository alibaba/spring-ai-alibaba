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

import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NodeOutput.getNextNode() functionality
 */
class NodeOutputNextNodeTest {

    private StateGraph stateGraph;
    private CompiledGraph compiledGraph;
    private OverAllState overAllState;

    @BeforeEach
    void setUp() throws GraphStateException {
        stateGraph = new StateGraph();
        overAllState = new OverAllState();

        // Create a simple linear graph: START -> node1 -> node2 -> END
        stateGraph.addNode("node1", (state, config) -> {
            return CompletableFuture.completedFuture(Map.of("result", "node1 executed"));
        });

        stateGraph.addNode("node2", (state, config) -> {
            return CompletableFuture.completedFuture(Map.of("result", "node2 executed"));
        });

        stateGraph.addEdge(StateGraph.START, "node1");
        stateGraph.addEdge("node1", "node2");
        stateGraph.addEdge("node2", StateGraph.END);

        compiledGraph = stateGraph.compile();
    }

    @Test
    void testGetNextNode_FromNode1() {
        NodeOutput nodeOutput = NodeOutput.of("node1", "agent1", overAllState, null, compiledGraph);

        String nextNode = nodeOutput.getNextNode();
        assertEquals("node2", nextNode, "Expected next node from node1 to be node2");
    }

    @Test
    void testGetNextNode_FromNode2() {
        NodeOutput nodeOutput = NodeOutput.of("node2", "agent2", overAllState, null, compiledGraph);

        String nextNode = nodeOutput.getNextNode();
        assertEquals(StateGraph.END, nextNode, "Expected next node from node2 to be END");
    }

    @Test
    void testGetNextNode_fromNonExistentNode() {
        NodeOutput nodeOutput = NodeOutput.of("nonexistent", "agent", overAllState, null, compiledGraph);

        String nextNode = nodeOutput.getNextNode();
        assertNull(nextNode, "Expected null for non-existent node");
    }

    @Test
    void testGetNextNode_WithoutCompiledGraph() {
        NodeOutput nodeOutput = NodeOutput.of("node1", "agent1", overAllState, null);

        String nextNode = nodeOutput.getNextNode();
        assertNull(nextNode, "Expected null when compiledGraph is not provided");
    }

    @Test
    void testGetAllNextNodes_FromNode1() {
        NodeOutput nodeOutput = NodeOutput.of("node1", "agent1", overAllState, null, compiledGraph);

        List<String> nextNodes = nodeOutput.getAllNextNodes();
        assertEquals(1, nextNodes.size(), "Expected exactly one next node from node1");
        assertEquals("node2", nextNodes.get(0), "Expected node2 as the only next node from node1");
    }

    @Test
    void testGetAllNextNodes_FromNode2() {
        NodeOutput nodeOutput = NodeOutput.of("node2", "agent2", overAllState, null, compiledGraph);

        List<String> nextNodes = nodeOutput.getAllNextNodes();
        assertEquals(1, nextNodes.size(), "Expected exactly one next node from node2");
        assertEquals(StateGraph.END, nextNodes.get(0), "Expected END as the only next node from node2");
    }

    @Test
    void testGetAllNextNodes_WithoutCompiledGraph() {
        NodeOutput nodeOutput = NodeOutput.of("node1", "agent1", overAllState, null);

        List<String> nextNodes = nodeOutput.getAllNextNodes();
        assertTrue(nextNodes.isEmpty(), "Expected empty list when compiledGraph is not provided");
    }

    @Test
    void testGetAllNextNodes_fromNonExistentNode() {
        NodeOutput nodeOutput = NodeOutput.of("nonexistent", "agent", overAllState, null, compiledGraph);

        List<String> nextNodes = nodeOutput.getAllNextNodes();
        assertTrue(nextNodes.isEmpty(), "Expected empty list for non-existent node");
    }

    @Test
    void testWithConditionalEdges() throws GraphStateException {
        // Create a graph with conditional edges: START -> node1 -> (condition) -> node2/node3
        StateGraph conditionalGraph = new StateGraph();

        conditionalGraph.addNode("node1", (state, config) ->
            CompletableFuture.completedFuture(Map.of("result", "node1 executed")));
        conditionalGraph.addNode("node2", (state, config) ->
            CompletableFuture.completedFuture(Map.of("result", "node2 executed")));
        conditionalGraph.addNode("node3", (state, config) ->
            CompletableFuture.completedFuture(Map.of("result", "node3 executed")));

        conditionalGraph.addEdge(StateGraph.START, "node1");

        // Add conditional edge from node1 to either node2 or node3
        conditionalGraph.addConditionalEdges("node1",
            (AsyncCommandAction) (state, config) -> CompletableFuture.completedFuture(new Command("condition_value")),
            Map.of("condition_value", "node2", "default", "node3"));

        conditionalGraph.addEdge("node2", StateGraph.END);
        conditionalGraph.addEdge("node3", StateGraph.END);

        CompiledGraph conditionalCompiledGraph = conditionalGraph.compile();
        NodeOutput nodeOutput = NodeOutput.of("node1", "agent1", overAllState, null, conditionalCompiledGraph);

        // Test getNextNode for conditional edge (should return first available target)
        String nextNode = nodeOutput.getNextNode();
        assertNotNull(nextNode, "Expected a next node for conditional edge");
        assertTrue(List.of("node2", "node3").contains(nextNode), "Expected next node to be either node2 or node3");

        // Test getAllNextNodes for conditional edge
        List<String> allNextNodes = nodeOutput.getAllNextNodes();
        assertEquals(2, allNextNodes.size(), "Expected two possible next nodes for conditional edge");
        assertTrue(allNextNodes.contains("node2"), "Expected node2 to be in the list of next nodes");
        assertTrue(allNextNodes.contains("node3"), "Expected node3 to be in the list of next nodes");
    }

    @Test
    void testExistingFunctionalityUnchanged() {
        // Ensure existing NodeOutput functionality still works
        NodeOutput nodeOutput = NodeOutput.of("node1", "agent1", overAllState, null, compiledGraph);

        assertEquals("node1", nodeOutput.node());
        assertEquals("agent1", nodeOutput.agent());
        assertEquals(overAllState, nodeOutput.state());
        assertFalse(nodeOutput.isSubGraph());
        assertFalse(nodeOutput.isSTART());
        assertFalse(nodeOutput.isEND());
    }
}
