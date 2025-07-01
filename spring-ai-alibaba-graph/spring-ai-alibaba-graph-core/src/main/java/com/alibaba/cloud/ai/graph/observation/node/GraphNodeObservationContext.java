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
package com.alibaba.cloud.ai.graph.observation.node;

import io.micrometer.observation.Observation;

import java.util.Map;

/**
 * Context class for graph node observation operations. Provides node-specific observation
 * data including node name, event type, state, and output information. Extends
 * Observation.Context to integrate with Micrometer's observation framework.
 */
public class GraphNodeObservationContext extends Observation.Context {

	private final String nodeName;

	private final String event;

	private final Map<String, Object> state;

	private final Map<String, Object> output;

	/**
	 * Constructs a new GraphNodeObservationContext with the specified parameters.
	 * @param nodeName the name of the graph node being observed
	 * @param event the type of event occurring on the node
	 * @param state the current state of the node execution
	 * @param output the output data from the node execution
	 */
	public GraphNodeObservationContext(String nodeName, String event, Map<String, Object> state,
			Map<String, Object> output) {
		this.nodeName = nodeName;
		this.event = event;
		this.state = state;
		this.output = output;
	}

	/**
	 * Gets the output data from the node execution.
	 * @return the node output as a map of key-value pairs
	 */
	public Map<String, Object> getOutput() {
		return this.output;
	}

	/**
	 * Gets the name of the graph node being observed.
	 * @return the node name
	 */
	public String getNodeName() {
		return this.nodeName;
	}

	/**
	 * Gets the type of event occurring on the node.
	 * @return the event type
	 */
	public String getEvent() {
		return this.event;
	}

	/**
	 * Gets the current state of the node execution.
	 * @return the node state as a map of key-value pairs
	 */
	public Map<String, Object> getState() {
		return this.state;
	}

	/**
	 * Creates a new Builder instance for constructing GraphNodeObservationContext
	 * objects.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for constructing GraphNodeObservationContext instances. Provides a
	 * fluent API for setting node observation context properties.
	 */
	public static final class Builder {

		private String nodeName;

		private String event;

		private Map<String, Object> state;

		private Map<String, Object> output;

		/**
		 * Sets the node name for the observation context.
		 * @param nodeId the name of the graph node
		 * @return this builder instance for method chaining
		 */
		public Builder nodeName(String nodeId) {
			this.nodeName = nodeId;
			return this;
		}

		/**
		 * Sets the event type for the observation context.
		 * @param event the type of event occurring on the node
		 * @return this builder instance for method chaining
		 */
		public Builder event(String event) {
			this.event = event;
			return this;
		}

		/**
		 * Sets the state for the observation context.
		 * @param state the current state of the node execution
		 * @return this builder instance for method chaining
		 */
		public Builder state(Map<String, Object> state) {
			this.state = state;
			return this;
		}

		/**
		 * Sets the output for the observation context.
		 * @param output the output data from the node execution
		 * @return this builder instance for method chaining
		 */
		public Builder output(Map<String, Object> output) {
			this.output = output;
			return this;
		}

		/**
		 * Builds and returns a new GraphNodeObservationContext instance with the
		 * configured properties.
		 * @return a new GraphNodeObservationContext instance
		 */
		public GraphNodeObservationContext build() {
			return new GraphNodeObservationContext(nodeName, event, state, output);
		}

	}

}
