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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documentation for graph observation conventions. Defines key names for low and high
 * cardinality metrics in graph observations.
 */
public enum GraphObservationDocumentation implements ObservationDocumentation {

	/** Graph observation documentation entry */
	GRAPH {

		@Override
		public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
			return GraphObservationConvention.class;
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
	 * Low cardinality key names for graph observations. These keys have limited unique
	 * values and are suitable for grouping and filtering.
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/** Spring AI Alibaba kind identifier */
		SPRING_AI_ALIBABA_KIND {
			@Override
			public String asString() {
				return "spring.ai.alibaba.kind";
			}
		},
		/** Graph name identifier */
		GRAPH_NAME {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.name";
			}
		}

	}

	/**
	 * High cardinality key names for graph observations. These keys may have many unique
	 * values and are suitable for detailed analysis.
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		/** Graph node state information */
		GRAPH_NODE_STATE {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.state";
			}
		},
		/** Graph node output information */
		GRAPH_NODE_OUTPUT {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.output";
			}
		}

	}

}
