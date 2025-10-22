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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.alibaba.cloud.ai.graph.StateGraph.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GraphFlux fixes addressing the three key issues:
 * 1. Parallel node streaming data merge failures
 * 2. Streaming merge logic separated from graph core
 * 3. Missing real nodeId in parallel node streaming output
 *
 * @author disaster
 * @since 1.0.4
 */
public class GraphFluxIntegrationTest {

    /**
     * Test 1: Single GraphFlux with node ID preservation
     */
    @Test
    public void testSingleGraphFluxWithNodeIdPreservation() throws Exception {
        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            keyStrategyMap.put("stream_result", new ReplaceStrategy());
            return keyStrategyMap;
        });

        // Node that returns GraphFlux with proper node ID
        AsyncNodeAction streamingNode = state -> {
            Flux<String> dataStream = Flux.just("chunk1", "chunk2", "chunk3")
                    .delayElements(Duration.ofMillis(10));

            Function<String, Map<String, Object>> mapResult = lastChunk ->
                    Map.of("final_result", "All chunks processed: " + lastChunk);
            Function<String, String> chunkResult = chunk -> chunk;

            GraphFlux<String> graphFlux = GraphFlux.of("streaming_node", "stream_output", dataStream, mapResult,chunkResult);

            return CompletableFuture.completedFuture(Map.of("stream_output", graphFlux));
        };

        stateGraph.addNode("streaming_node", streamingNode)
                .addEdge(START, "streaming_node")
                .addEdge("streaming_node", END);

        CompiledGraph app = stateGraph.compile();

        // Collect streaming outputs
        AtomicInteger streamCount = new AtomicInteger(0);
        String[] lastNodeId = new String[1];

        app.stream(Map.of("input", "test"))
                .filter(output -> output instanceof StreamingOutput)
                .map(output -> (StreamingOutput) output)
                .doOnNext(streamingOutput -> {
                    streamCount.incrementAndGet();
                    lastNodeId[0] = streamingOutput.node();
                    System.out.println("Streaming from node: " + streamingOutput.node() +
                            ", chunk: " + streamingOutput.chunk());
                })
                .blockLast();

        // Verify node ID preservation
        assertEquals("streaming_node", lastNodeId[0], "Node ID should be preserved in streaming output");
        assertTrue(streamCount.get() > 0, "Should have streaming outputs");
    }

    /**
     * Test 2: Parallel nodes with GraphFlux - each node should maintain its own identity
     */
    @Test
    public void testParallelGraphFluxWithNodeIdPreservation() throws Exception {
        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            keyStrategyMap.put("parallel_results", new AppendStrategy());
            return keyStrategyMap;
        });

        // Parallel nodes that return GraphFlux with distinct node IDs
        AsyncNodeAction node1 = state -> {
            Flux<String> stream1 = Flux.just("node1_chunk1", "node1_chunk2")
                    .delayElements(Duration.ofMillis(10));
            StringBuilder sb = new StringBuilder("Node1 completed: ");
            Function<String, String> mapResult1 = lastChunk ->
                    sb.append(lastChunk+"\t").toString();

            Function<String, String> chunkResult1 = chunk -> chunk;

            GraphFlux<String> graphFlux1 = GraphFlux.of("parallel_node_1","stream1", stream1, mapResult1,chunkResult1);
            return CompletableFuture.completedFuture(Map.of("stream1", graphFlux1));
        };

        AsyncNodeAction node2 = state -> {
            Flux<String> stream2 = Flux.just("node2_chunk1", "node2_chunk2")
                    .delayElements(Duration.ofMillis(15));
            StringBuilder sb = new StringBuilder("Node2 completed: ");
            Function<String, String> mapResult2 = lastChunk ->
                    sb.append(lastChunk+"\t").toString();

            Function<String, String> chunkResult2 = chunk -> chunk;

            GraphFlux<String> graphFlux2 = GraphFlux.of("parallel_node_2","stream2", stream2, mapResult2,chunkResult2);
            return CompletableFuture.completedFuture(Map.of("stream2", graphFlux2));
        };

        AsyncNodeAction mergeNode = state -> {
            System.out.println("Merge node received state: " + state.data());
            return CompletableFuture.completedFuture(Map.of("messages", "Merge completed"));
        };

        stateGraph.addNode("node1", node1)
                .addNode("node2", node2)
                .addNode("merge", mergeNode)
                .addEdge(START, "node1")
                .addEdge(START, "node2")
                .addEdge("node1", "merge")
                .addEdge("node2", "merge")
                .addEdge("merge", END);

        CompiledGraph app = stateGraph.compile();

        // Track which nodes produced streaming output
        Map<String, Integer> nodeStreamCounts = new HashMap<>();

        app.stream(Map.of("input", "test"))
                .filter(output -> output instanceof StreamingOutput)
                .map(output -> (StreamingOutput) output)
                .doOnNext(streamingOutput -> {
                    String nodeId = streamingOutput.node();
                    nodeStreamCounts.merge(nodeId, 1, Integer::sum);
                    System.out.println("Parallel streaming from node: " + nodeId +
                            ", chunk: " + streamingOutput.chunk());
                })
                .blockLast();

        // Verify both parallel nodes maintained their identities
        assertTrue(nodeStreamCounts.containsKey("parallel_node_1"),
                "Should have streaming output from parallel_node_1");
        assertTrue(nodeStreamCounts.containsKey("parallel_node_2"),
                "Should have streaming output from parallel_node_2");

        assertTrue(nodeStreamCounts.get("parallel_node_1") > 0,
                "parallel_node_1 should produce streaming output");
        assertTrue(nodeStreamCounts.get("parallel_node_2") > 0,
                "parallel_node_2 should produce streaming output");

        System.out.println("Node stream counts: " + nodeStreamCounts);
    }


    /**
     * Test 4: Mixed scenario - GraphFlux and traditional objects in the same graph
     */
    @Test
    public void testMixedGraphFluxAndTraditionalObjects() throws Exception {
        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            keyStrategyMap.put("static_data", new ReplaceStrategy());
            return keyStrategyMap;
        });

        // Node that returns both GraphFlux and regular objects
        AsyncNodeAction mixedNode = state -> {
            Flux<String> stream = Flux.just("mixed1", "mixed2")
                    .delayElements(Duration.ofMillis(10));

            GraphFlux<String> graphFlux = GraphFlux.of("mixed_node", "stream_output", stream,
                    data -> Map.of("stream_final", "Stream completed with: " + data), s -> s);

            return CompletableFuture.completedFuture(Map.of(
                    "stream_output", graphFlux,
                    "static_data", "This is static data",
                    "timestamp", System.currentTimeMillis()
            ));
        };

        stateGraph.addNode("mixed_node", mixedNode)
                .addEdge(START, "mixed_node")
                .addEdge("mixed_node", END);

        CompiledGraph app = stateGraph.compile();

        boolean hasStreamingOutput = false;
        boolean hasStaticData = false;

        for (NodeOutput output : app.stream(Map.of("input", "test")).toIterable()) {
            if (output instanceof StreamingOutput) {
                hasStreamingOutput = true;
                assertEquals("mixed_node", output.node(), "Should preserve mixed_node ID");
            } else {
                Map<String, Object> state = output.state().data();
                if (state.containsKey("static_data")) {
                    hasStaticData = true;
                }
            }
        }

        assertTrue(hasStreamingOutput, "Should have streaming output");
        assertTrue(hasStaticData, "Should have static data");
    }
}
