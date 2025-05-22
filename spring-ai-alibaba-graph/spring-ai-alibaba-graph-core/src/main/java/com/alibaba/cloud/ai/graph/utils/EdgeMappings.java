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

import java.util.*;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class EdgeMappings {

	public static class Builder {

		private final Map<String, String> mappings = new LinkedHashMap<>();

		public Builder toEND() {
			mappings.put(END, END);
			return this;
		}

		public Builder toEND(String label) {
			mappings.put(label, END);
			return this;
		}

		public Builder to(String destination) {
			mappings.put(destination, destination);
			return this;
		}

		public Builder to(String destination, String label) {
			mappings.put(label, destination);
			return this;
		}

		public Builder to(List<String> destinations) {
			destinations.forEach(this::to);
			return this;
		}

		public Builder to(String[] destinations) {
			return to(Arrays.asList(destinations));
		}

		public Map<String, String> build() {
			return Collections.unmodifiableMap(mappings);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

}
