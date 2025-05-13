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
package com.alibaba.cloud.ai.tool.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import org.springframework.ai.observation.conventions.AiObservationAttributes;

public enum ArmsToolCallingObservationDocumentation implements ObservationDocumentation {

	EXECUTE_TOOL_OPERATION {
		@Override
		public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
			return ArmsToolCallingObservationConvention.class;
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

	// OpenTelemetry semantic convention

	public static final String GEN_AI_TOOL_CALL_ID = "gen_ai.tool.call.id";

	public static final String GEN_AI_TOOL_NAME = "gen_ai.tool.name";

	// ARMS semantic convention

	public static final String GEN_AI_SPAN_KIND = "gen_ai.span.kind";

	public static final String GEN_AI_FRAMEWORK = "gen_ai.framework";

	public static final String TOOL_NAME = "tool.name";

	public static final String TOOL_DESCRIPTION = "tool.description";

	public static final String TOOL_PARAMETERS = "tool.parameters";

	public static final String TOOL_RETURN_DIRECT = "tool.return.direct";

	public static final String INPUT_VALUE = "input.value";

	public static final String OUTPUT_VALUE = "output.value";

	/**
	 * Low-cardinality observation key names for execute tool operations.
	 */
	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * The name of the operation being performed.
		 */
		AI_OPERATION_TYPE {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_OPERATION_TYPE.value();
			}
		},

		/**
		 * The span kind of the operation.
		 */
		GEN_AI_SPAN_KIND {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.GEN_AI_SPAN_KIND;
			}
		},

		/**
		 * The framework of the operation.
		 */
		GEN_AI_FRAMEWORK {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.GEN_AI_FRAMEWORK;
			}
		}

	}

	/**
	 * High-cardinality observation key names for execute tool operations.
	 */
	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * The identifier of the executed tool.
		 */
		GEN_AI_TOOL_CALL_ID {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.GEN_AI_TOOL_CALL_ID;
			}
		},

		/**
		 * The name of the executed tool.
		 */
		GEN_AI_TOOL_NAME {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.GEN_AI_TOOL_NAME;
			}
		},

		/**
		 * The name of the executed tool in ARMS semantic convention.
		 */
		TOOL_NAME {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.TOOL_NAME;
			}
		},

		/**
		 * The description of the executed tool.
		 */
		TOOL_DESCRIPTION {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.TOOL_DESCRIPTION;
			}
		},

		/**
		 * Whether to return the result of the tool call directly.
		 */
		TOOL_RETURN_DIRECT {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.TOOL_RETURN_DIRECT;
			}
		},

		// Content

		/**
		 * The parameters of the executed tool.
		 */
		TOOL_PARAMETERS {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.TOOL_PARAMETERS;
			}
		},

		/**
		 * The extended input of the tool execution.
		 */
		INPUT_VALUE {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.INPUT_VALUE;
			}
		},

		/**
		 * The full output of the tool execution.
		 */
		OUTPUT_VALUE {
			@Override
			public String asString() {
				return ArmsToolCallingObservationDocumentation.OUTPUT_VALUE;
			}
		}

	}

}
