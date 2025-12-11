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
package com.alibaba.cloud.ai.graph.agent.streaming;

import com.alibaba.cloud.ai.graph.streaming.GraphFlux;

import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for GraphFlux functionality.
 * These tests verify the basic structure and behavior of GraphFlux wrapping.
 */
class GraphFluxBasicTest {

    @Test
    void testGraphFluxCreation() {
        // Given: A simple Flux
        Flux<String> flux = Flux.just("test1", "test2");

        // When: Creating a GraphFlux
        GraphFlux<String> graphFlux = GraphFlux.of("testNode", flux);

        // Then: GraphFlux should preserve node ID and flux
        assertNotNull(graphFlux);
        assertEquals("testNode", graphFlux.getNodeId());
        assertNotNull(graphFlux.getFlux());
    }

    @Test
    void testGraphFluxWithKey() {
        // Given: A Flux and a key
        Flux<String> flux = Flux.just("data");

        // When: Creating GraphFlux with key
        // The last two null parameters are:
        // - mapResult: optional function to transform final result (not needed here)
        // - chunkResult: optional function to process individual chunks (not needed
        // here)
        GraphFlux<String> graphFlux = GraphFlux.of("node1", "outputKey", flux, null, null);

        // Then: Should preserve both node ID and key
        assertEquals("node1", graphFlux.getNodeId());
        assertEquals("outputKey", graphFlux.getKey());
        assertNotNull(graphFlux.getFlux());
    }

    @Test
    void testGraphFluxNodeIdNotNull() {
        // Given: A Flux
        Flux<Integer> flux = Flux.just(1, 2, 3);

        // When: Creating GraphFlux
        GraphFlux<Integer> graphFlux = GraphFlux.of("numberNode", flux);

        // Then: Node ID should not be null or empty
        assertNotNull(graphFlux.getNodeId());
        assertFalse(graphFlux.getNodeId().isEmpty());
    }

    @Test
    void testGraphFluxPreservesFluxType() {
        // Given: A typed Flux
        Flux<ChatResponse> chatFlux = Flux.empty();

        // When: Wrapping with GraphFlux
        GraphFlux<ChatResponse> graphFlux = GraphFlux.of("chatNode", chatFlux);

        // Then: Type should be preserved
        assertNotNull(graphFlux);
        assertEquals("chatNode", graphFlux.getNodeId());
        assertSame(chatFlux, graphFlux.getFlux());
    }

    @Test
    void testMultipleGraphFluxInstances() {
        // Given: Multiple Flux instances
        Flux<String> flux1 = Flux.just("a");
        Flux<String> flux2 = Flux.just("b");

        // When: Creating multiple GraphFlux instances
        GraphFlux<String> gf1 = GraphFlux.of("node1", flux1);
        GraphFlux<String> gf2 = GraphFlux.of("node2", flux2);

        // Then: Each should maintain its own identity
        assertEquals("node1", gf1.getNodeId());
        assertEquals("node2", gf2.getNodeId());
        assertNotSame(gf1, gf2);
    }

    @Test
    void testGraphFluxWithNullKey() {
        // Given: A Flux
        Flux<String> flux = Flux.just("test");

        // When: Creating GraphFlux with null key (using simple factory method)
        GraphFlux<String> graphFlux = GraphFlux.of("node", flux);

        // Then: Should work fine (key is optional)
        assertNotNull(graphFlux);
        assertEquals("node", graphFlux.getNodeId());
        // Key may be null when using simple factory method
    }

    @Test
    void testGraphFluxNodeIdFormat() {
        // Test various node ID formats that are used in the codebase

        // LLM Node format
        GraphFlux<String> llmFlux = GraphFlux.of("model", Flux.just("llm"));
        assertEquals("model", llmFlux.getNodeId());

        // A2A Node format
        GraphFlux<String> a2aFlux = GraphFlux.of("A2aNode", Flux.just("a2a"));
        assertEquals("A2aNode", a2aFlux.getNodeId());

        // Subgraph format (example)
        GraphFlux<String> subgraphFlux = GraphFlux.of("subgraph_agent1", Flux.just("sub"));
        assertEquals("subgraph_agent1", subgraphFlux.getNodeId());
    }

    @Test
    void testGraphFluxImmutability() {
        // Given: A GraphFlux
        Flux<String> flux = Flux.just("data");
        // Creating with explicit null values for optional mapResult and chunkResult
        // functions
        GraphFlux<String> graphFlux = GraphFlux.of("node", "key", flux, null, null);

        // When: Getting properties
        String nodeId1 = graphFlux.getNodeId();
        String nodeId2 = graphFlux.getNodeId();
        String key1 = graphFlux.getKey();
        String key2 = graphFlux.getKey();

        // Then: Properties should be consistent (immutable)
        assertEquals(nodeId1, nodeId2);
        assertEquals(key1, key2);
    }
}
