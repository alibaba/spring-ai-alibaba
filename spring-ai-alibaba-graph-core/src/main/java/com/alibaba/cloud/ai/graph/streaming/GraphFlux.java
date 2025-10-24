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

import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * GraphFlux is a generic wrapper class for managing streaming output and associating it with a node identifier.
 * <p>
 * Core features:
 * - Uniformly wraps Flux streaming data and attaches a node identifier
 * - Supports custom result mapping functions
 * - Maintains backward compatibility without modifying existing interfaces
 *
 * @param <T> the type of streaming data
 * @author disaster
 * @since 1.0.4
 */
public class GraphFlux<T> {

    /**
     * Node identifier
     */
    private final String nodeId;

    /**
     * Streaming data
     */
    private final Flux<T> flux;

    /**
     * Storage key
     */
    private final String key;

    /**
     * Result mapping function, used to convert the final result of streaming data into Map format
     */
    private final Function<T,?> mapResult;

    /**
     * Chunk result function, used to process individual chunks of data
     */
    private final Function<Object,String> chunkResult;


    /**
     * Private constructor, instances are created through static factory methods
     */
    private GraphFlux(String nodeId, Flux<T> flux, String key, Function<T,?> mapResult, Function<Object, String> chunkResult) {
        this.nodeId = nodeId;
        this.flux = flux;
        this.key = key;
        this.mapResult = mapResult;
        this.chunkResult = chunkResult;
    }

    /**
     * Static factory method to create a GraphFlux instance
     *
     * @param nodeId node identifier
     * @param flux   streaming data
     * @param <T>    type of streaming data
     * @return GraphFlux instance
     */
    public static <T> GraphFlux<T> of(String nodeId, Flux<T> flux) {
        return new GraphFlux<>(nodeId, flux,null, null,null);
    }

    /**
     * Static factory method to create a GraphFlux instance with a result mapping function
     *
     * @param nodeId    node identifier
     * @param key       storage key
     * @param flux      streaming data
     * @param mapResult result mapping function
     * @param chunkResult chunk processing function
     * @param <T>       type of streaming data
     * @return GraphFlux instance
     */
    public static <T> GraphFlux<T> of(String nodeId, String key, Flux<T> flux, Function<T, ?> mapResult, Function<T, String> chunkResult) {
        return new GraphFlux<>(nodeId, flux, key, mapResult, o -> chunkResult.apply((T) o));
    }

    public Function<Object,String> getChunkResult() {
        return chunkResult;
    }

    /**
     * Get the node identifier
     *
     * @return node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    public String getKey() {
        return key;
    }

    /**
     * Get the streaming data
     *
     * @return Flux streaming data
     */
    public Flux<T> getFlux() {
        return flux;
    }

    /**
     * Get the result mapping function
     *
     * @return result mapping function, may be null
     */
    public Function getMapResult() {
        return mapResult;
    }


    /**
     * Check if there is a result mapping function
     *
     * @return true if there is a result mapping function, false otherwise
     */
    public boolean hasMapResult() {
        return mapResult != null;
    }


    public boolean hasChunkResult() {
        return chunkResult != null;
    }




    @Override
    public String toString() {
        return String.format("GraphFlux{nodeId='%s', hasMapResult=%s}",
                nodeId, hasMapResult());
    }
}
