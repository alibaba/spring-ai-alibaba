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
package com.alibaba.cloud.ai.graph.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * Utility class for managing edge mappings in a graph structure. Provides a fluent API
 * for defining mappings between labels and destination nodes.
 *
 * @author disaster
 * @since 1.0.0.1
 */
public class EdgeMappings {

	/**
	 * Builder class for constructing EdgeMappings instances.
	 */
	public static class Builder {

		private final Map<String, String> mappings = new LinkedHashMap<>();

		/**
		 * Adds a mapping from the END constant to itself.
		 * @return this Builder instance for method chaining
		 */
		public Builder toEND() {
			mappings.put(END, END);
			return this;
		}

		/**
		 * Adds a mapping from a specified label to the END constant.
		 * @param label the label to map
		 * @return this Builder instance for method chaining
		 */
		public Builder toEND(String label) {
			mappings.put(label, END);
			return this;
		}

		/**
		 * Adds a self-mapping for the given destination.
		 * @param destination the destination to map to itself
		 * @return this Builder instance for method chaining
		 */
		public Builder to(String destination) {
			mappings.put(destination, destination);
			return this;
		}

		/**
		 * Adds a mapping from a label to a specific destination.
		 * @param destination the destination node
		 * @param label the label pointing to the destination
		 * @return this Builder instance for method chaining
		 */
		public Builder to(String destination, String label) {
			mappings.put(label, destination);
			return this;
		}

		/**
		 * Adds mappings for a list of destinations, each mapped to themselves.
		 * @param destinations list of destination nodes
		 * @return this Builder instance for method chaining
		 */
		public Builder to(List<String> destinations) {
			destinations.forEach(this::to);
			return this;
		}

		/**
		 * Adds mappings for an array of destinations by converting it to a list first.
		 * @param destinations array of destination nodes
		 * @return this Builder instance for method chaining
		 */
		public Builder to(String[] destinations) {
			return to(Arrays.asList(destinations));
		}

		/**
		 * Builds and returns an unmodifiable map of the current mappings.
		 * @return an unmodifiable map representing the edge mappings
		 */
		public Map<String, String> build() {
			return Collections.unmodifiableMap(mappings);
		}

	}

	/**
	 * Returns a new instance of the Builder class.
	 * @return a new Builder for creating EdgeMappings
	 */
	public static Builder builder() {
		return new Builder();
	}

}
