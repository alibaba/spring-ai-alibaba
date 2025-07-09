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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documentation enum for graph node observation operations. Defines observation
 * conventions and key names for node-specific metrics and tracing. Provides both low and
 * high cardinality key names for different observation granularities.
 *
 * @author XiaoYunTao
 * @since 2025/6/28
 */
public enum GraphNodeObservationDocumentation implements ObservationDocumentation {

	/**
	 * Represents a graph node observation operation. Defines the default convention and
	 * key names for node observations.
	 */
	GRAPH_NODE {

		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return GraphNodeObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

		@Override
		public Observation.Event[] getEvents() {
			return new Observation.Event[0];
		}
	};

	/**
	 * Low cardinality key names for graph node observations. These keys have limited
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
		 * Represents the name of the graph node being observed.
		 */
		GRAPH_NODE_NAME {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.node.name";
			}
		},

		/**
		 * Represents the type of event occurring on the graph node.
		 */
		GRAPH_EVENT {
			@Override
			public String asString() {
				return "spring.ai.alibaba.graph.event";
			}
		}

	}

	/**
	 * High cardinality key names for graph node observations. These keys have many unique
	 * values and provide detailed observation data.
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * Represents the input state when entering the graph node (OpenTelemetry semantic
		 * convention). Note: This key is dynamically added by
		 * GraphObservationLifecycleListener, not by Convention.
		 */
		GEN_AI_PROMPT {
			@Override
			public String asString() {
				return "gen_ai.prompt";
			}
		},

		/**
		 * Represents the output state after executing the graph node (OpenTelemetry
		 * semantic convention). Note: This key is dynamically added by
		 * GraphObservationLifecycleListener, not by Convention.
		 */
		GEN_AI_COMPLETION {
			@Override
			public String asString() {
				return "gen_ai.completion";
			}
		}

	}

}
