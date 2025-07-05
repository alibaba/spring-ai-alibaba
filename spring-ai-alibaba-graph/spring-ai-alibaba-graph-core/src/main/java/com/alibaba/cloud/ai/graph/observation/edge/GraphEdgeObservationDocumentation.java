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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documentation enum for graph edge observation operations. Defines observation
 * conventions and key names for edge-specific metrics and tracing. Provides both low and
 * high cardinality key names for different observation granularities.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public enum GraphEdgeObservationDocumentation implements ObservationDocumentation {

	/**
	 * Represents a graph edge observation operation. Defines the default convention and
	 * key names for edge observations.
	 */
	GRAPH_EDGE {

		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return GraphEdgeObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}
	};

	/**
	 * Low cardinality key names for graph edge observations. These keys have limited
	 * unique values and are suitable for grouping and filtering.
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * Represents the kind/type of the AI operation.
		 */
		SPRING_AI_ALIBABA_KIND {
			@Override
			public String asString() {
				return "spring.ai.alibaba.kind";
			}
		},

		/**
		 * Represents the name of the graph edge being observed.
		 */
		GRAPH_NAME {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.edge.name";
			}
		}

	}

	/**
	 * High cardinality key names for graph edge observations. These keys have many unique
	 * values and provide detailed observation data.
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * Represents the current state of the graph edge execution.
		 */
		GRAPH_NODE_STATE {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.edge.state";
			}
		},

		/**
		 * Represents the output data from the graph edge execution.
		 */
		GRAPH_NODE_OUTPUT {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.edge.output";
			}
		}

	}

}
