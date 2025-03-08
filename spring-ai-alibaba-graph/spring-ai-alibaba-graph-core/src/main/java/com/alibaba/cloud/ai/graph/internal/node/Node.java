/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.internal.node;

import java.util.Objects;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

/**
 * Represents a node in a graph, characterized by a unique identifier and a factory for
 * creating actions to be executed by the node. This is a generic record where the state
 * type is specified by the type parameter {@code State}.
 *
 * {@link OverAllState}.
 *
 */
public class Node {

	public interface ActionFactory {

		AsyncNodeActionWithConfig apply(CompileConfig config) throws GraphStateException;

	}

	private final String id;

	private final ActionFactory actionFactory;

	public Node(String id, ActionFactory actionFactory) {
		this.id = id;
		this.actionFactory = actionFactory;
	}

	/**
	 * Constructor that accepts only the `id` and sets `actionFactory` to null.
	 * @param id the unique identifier for the node
	 */
	public Node(String id) {
		this(id, null);
	}

	/**
	 * id
	 * @return the unique identifier for the node.
	 */
	public String id() {
		return id;
	}

	/**
	 * actionFactory
	 * @return a factory function that takes a {@link CompileConfig} and returns an
	 * {@link AsyncNodeActionWithConfig} instance for the specified {@code State}.
	 */
	public ActionFactory actionFactory() {
		return actionFactory;
	}

	public boolean isParallel() {
		// return id.startsWith(PARALLEL_PREFIX);
		return false;
	}

	public Node withIdUpdated(Function<String, String> newId) {
		return new Node(newId.apply(id), actionFactory);
	}

	/**
	 * Checks if this node is equal to another object.
	 * @param o the object to compare with
	 * @return true if this node is equal to the specified object, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (o instanceof Node node) {
			return Objects.equals(id, node.id);
		}
		return false;

	}

	/**
	 * Returns the hash code value for this node.
	 * @return the hash code value for this node
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return format("Node(%s,%s)", id, actionFactory != null ? "action" : "null");
	}

}
