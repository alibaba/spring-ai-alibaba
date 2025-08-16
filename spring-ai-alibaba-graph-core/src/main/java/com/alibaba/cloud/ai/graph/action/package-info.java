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

/**
 * Defines the core interfaces and classes for actions that constitute the building blocks
 * of a LangGraph4j graph. Actions in this context represent the executable logic
 * associated with graph nodes and the conditional logic that determines transitions along
 * graph edges. They operate on an {@link com.alibaba.cloud.ai.graph.OverAllState} and can
 * optionally interact with a {@link com.alibaba.cloud.ai.graph.RunnableConfig}.
 *
 * <p>
 * This package provides a clear separation between synchronous and asynchronous
 * operations, allowing for flexible graph construction that can accommodate various
 * execution models.
 * </p>
 *
 * <h2>Key Components:</h2>
 * <ul>
 * <li>{@link com.alibaba.cloud.ai.graph.action.NodeAction}: Represents a synchronous
 * operation performed at a node. It takes the current agent state and returns a
 * {@link java.util.Map} representing updates to the state.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.action.NodeActionWithConfig}: Similar to
 * {@code NodeAction}, but also accepts a
 * {@link com.alibaba.cloud.ai.graph.RunnableConfig} parameter, allowing for more
 * configurable node behavior at runtime.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.action.EdgeAction}: Defines synchronous
 * conditional logic for an edge. It evaluates the current agent state and returns a
 * {@link java.lang.String} indicating the name of the next node to transition to, or a
 * special value to end the graph.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.action.CommandAction}: Represents a synchronous
 * action that evaluates the agent state and a
 * {@link com.alibaba.cloud.ai.graph.RunnableConfig} to produce a
 * {@link com.alibaba.cloud.ai.graph.action.Command}. This command encapsulates both
 * potential state updates and the next node to transition to, offering a structured way
 * to define node outcomes.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.action.AsyncNodeAction}: An asynchronous
 * counterpart to {@code NodeAction}. It allows for non-blocking operations at nodes,
 * returning a {@link java.util.concurrent.CompletableFuture} that will eventually provide
 * the {@link java.util.Map} of state updates. This is suitable for I/O-bound tasks or
 * long-running computations.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.action.AsyncEdgeAction}: The asynchronous version
 * of {@code EdgeAction}. It provides a non-blocking way to determine the next path in the
 * graph, returning a {@link java.util.concurrent.CompletableFuture} with the name of the
 * next node or an indication to end.</li>
 * </ul>
 *
 * <p>
 * Implementations of these interfaces are fundamental to defining the behavior and flow
 * of control within a stateful agent graph constructed using LangGraph4j.
 * </p>
 *
 */
package com.alibaba.cloud.ai.graph.action;
