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
package com.alibaba.cloud.ai.graph.observation.edge;

import io.micrometer.observation.Observation;

import java.util.Map;

/**
 * Context class for graph edge observation operations. Provides edge-specific observation
 * data including edge name, state, and next node information.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public class GraphEdgeObservationContext extends Observation.Context {

	private final String graphEdgeName;

	private final Map<String, Object> state;

	private final String nextNode;

	/**
	 * Constructs a new GraphEdgeObservationContext with the specified parameters.
	 * @param graphEdgeName the name of the graph edge being observed
	 * @param state the current state of the edge execution
	 * @param nextNode the next node in the graph flow
	 */
	public GraphEdgeObservationContext(String graphEdgeName, Map<String, Object> state, String nextNode) {
		this.graphEdgeName = graphEdgeName;
		this.state = state;
		this.nextNode = nextNode;
	}

	/**
	 * Gets the name of the graph edge being observed.
	 * @return the graph edge name
	 */
	public String getGraphEdgeName() {
		return this.graphEdgeName;
	}

	/**
	 * Gets the name for observation context.
	 * @return the graph edge name
	 */
	@Override
	public String getName() {
		return this.graphEdgeName;
	}

	/**
	 * Gets the current state of the edge execution.
	 * @return the edge state as a map of key-value pairs
	 */
	public Map<String, Object> getState() {
		return this.state;
	}

	/**
	 * Gets the next node in the graph flow.
	 * @return the next node identifier
	 */
	public String getNextNode() {
		return this.nextNode;
	}

	/**
	 * Creates a new Builder instance for constructing GraphEdgeObservationContext
	 * objects.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for constructing GraphEdgeObservationContext instances. Provides a
	 * fluent API for setting edge observation context properties.
	 */
	public static final class Builder {

		private String graphEdgeName;

		private Map<String, Object> state;

		private String nextNode;

		/**
		 * Sets the graph edge name for the observation context.
		 * @param graphEdgeName the name of the graph edge
		 * @return this builder instance for method chaining
		 */
		public Builder graphEdgeName(String graphEdgeName) {
			this.graphEdgeName = graphEdgeName;
			return this;
		}

		/**
		 * Sets the state for the observation context.
		 * @param state the current state of the edge execution
		 * @return this builder instance for method chaining
		 */
		public Builder state(Map<String, Object> state) {
			this.state = state;
			return this;
		}

		/**
		 * Sets the next node for the observation context.
		 * @param nextNode the next node in the graph flow
		 * @return this builder instance for method chaining
		 */
		public Builder nextNode(String nextNode) {
			this.nextNode = nextNode;
			return this;
		}

		/**
		 * Builds and returns a new GraphEdgeObservationContext instance with the
		 * configured properties.
		 * @return a new GraphEdgeObservationContext instance
		 */
		public GraphEdgeObservationContext build() {
			return new GraphEdgeObservationContext(graphEdgeName, state, nextNode);
		}

	}

}
