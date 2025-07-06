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

import com.alibaba.cloud.ai.graph.observation.SpringAiAlibabaKind;
import com.alibaba.cloud.ai.graph.observation.graph.GraphObservationDocumentation.HighCardinalityKeyNames;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Default implementation of GraphObservationConvention. Provides standard observation
 * conventions for graph operations with configurable naming.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public class DefaultGraphObservationConvention implements GraphObservationConvention {

	/** Default operation name for graph observations */
	public static final String DEFAULT_OPERATION_NAME = "spring.ai.alibaba.graph";

	private String name;

	/**
	 * Constructs a default convention with the default operation name.
	 */
	public DefaultGraphObservationConvention() {
		this(DEFAULT_OPERATION_NAME);
	}

	/**
	 * Constructs a convention with a custom operation name.
	 * @param name the custom operation name
	 */
	public DefaultGraphObservationConvention(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Generates a contextual name for the observation. Combines the operation name with
	 * the graph name if available.
	 */
	@Override
	@Nullable
	public String getContextualName(GraphObservationContext context) {
		if (StringUtils.hasText(context.getName())) {
			return "%s.%s".formatted(DEFAULT_OPERATION_NAME, context.getName());
		}
		return DEFAULT_OPERATION_NAME;
	}

	/**
	 * Provides low cardinality key values for metrics. Includes graph kind and name for
	 * grouping and filtering.
	 */
	@Override
	public KeyValues getLowCardinalityKeyValues(GraphObservationContext context) {
		return KeyValues.of(
				KeyValue.of(GraphObservationDocumentation.LowCardinalityKeyNames.SPRING_AI_ALIBABA_KIND,
						SpringAiAlibabaKind.GRAPH.getValue()),
				KeyValue.of(GraphObservationDocumentation.LowCardinalityKeyNames.GRAPH_NAME, context.getGraphName()));
	}

	/**
	 * Provides high cardinality key values for detailed analysis. Includes graph state
	 * and output information.
	 */
	@Override
	public KeyValues getHighCardinalityKeyValues(GraphObservationContext context) {
		KeyValues keyValues = KeyValues
			.of(KeyValue.of(HighCardinalityKeyNames.GRAPH_NODE_STATE, context.getState().toString()));
		if (context.getOutput() != null) {
			keyValues.and(KeyValue.of(HighCardinalityKeyNames.GRAPH_NODE_OUTPUT, context.getOutput().toString()));
		}
		return keyValues;
	}

}
