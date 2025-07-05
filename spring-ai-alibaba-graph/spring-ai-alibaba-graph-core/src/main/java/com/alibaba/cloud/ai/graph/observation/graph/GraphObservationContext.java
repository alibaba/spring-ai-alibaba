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
package com.alibaba.cloud.ai.graph.observation.graph;

import io.micrometer.observation.Observation;

import java.util.Map;

/**
 * Context class for graph observation operations. This class extends Observation.Context
 * to provide graph-specific observation data including graph name, state, and output
 * information for monitoring and metrics collection.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public class GraphObservationContext extends Observation.Context {

	private final String graphName;

	private final Map<String, Object> state;

	private final Object output;

	public GraphObservationContext(String graphName, Map<String, Object> state, Object output) {
		this.graphName = graphName;
		this.state = state;
		this.output = output;
	}

	public String getGraphName() {
		return this.graphName;
	}

	@Override
	public String getName() {
		return this.graphName;
	}

	public Map<String, Object> getState() {
		return this.state;
	}

	public Object getOutput() {
		return this.output;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private String graphName;

		private Map<String, Object> state;

		private Object output;

		public Builder graphName(String graphName) {
			this.graphName = graphName;
			return this;
		}

		public Builder state(Map<String, Object> state) {
			this.state = state;
			return this;
		}

		public Builder output(Object output) {
			this.output = output;
			return this;
		}

		public GraphObservationContext build() {
			return new GraphObservationContext(this.graphName, this.state, this.output);
		}

	}

}
