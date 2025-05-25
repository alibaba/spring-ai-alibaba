/**
 * The {@code com.alibaba.cloud.ai.graph} package provides classes and interfaces for
 * building stateful, multi-agent applications with LLMs. It includes core components such
 * as {@link com.alibaba.cloud.ai.graph.StateGraph},
 * {@link com.alibaba.cloud.ai.graph.CompiledGraph},
 * {@link com.alibaba.cloud.ai.graph.internal.node.Node}, and
 * {@link com.alibaba.cloud.ai.graph.internal.edge.Edge}, which facilitate the creation
 * and management of state graphs.
 *
 * <p>
 * Key classes and interfaces:
 * </p>
 * <ul>
 * <li>{@link com.alibaba.cloud.ai.graph.StateGraph} - Represents a state graph with nodes
 * and edges.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.CompiledGraph} - Represents a compiled state
 * graph ready for execution.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.internal.node.Node} - Represents a node in the
 * graph with a unique identifier and an associated action.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.internal.edge.Edge} - Represents an edge in the
 * graph with a source ID and a target value.</li>
 * </ul>
 *
 * <p>
 * Utility classes:
 * </p>
 * <ul>
 * <li>{@link com.alibaba.cloud.ai.graph.utils.CollectionsUtils} - Provides utility
 * methods for creating collections.</li>
 * </ul>
 *
 * <p>
 * Exception classes:
 * </p>
 * <ul>
 * <li>{@link com.alibaba.cloud.ai.graph.GraphStateException} - Exception thrown when
 * there is an error related to the state of a graph.</li>
 * </ul>
 */
package com.alibaba.cloud.ai.graph;