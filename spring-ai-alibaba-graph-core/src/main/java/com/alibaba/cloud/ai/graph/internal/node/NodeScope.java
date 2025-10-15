package com.alibaba.cloud.ai.graph.internal.node;

/**
 * Defines the lifecycle of node action instances during a single graph execution.
 */
public enum NodeScope {

    /**
     * Reuse a single action instance per graph execution via runtime cache.
     */
    SINGLETON_PER_REQUEST,

    /**
     * Always create a fresh action instance for each invocation.
     */
    PROTOTYPE

}
