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

import com.alibaba.cloud.ai.graph.observation.SpringAiAlibabaKind;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Default implementation of GraphNodeObservationConvention. Provides standard observation
 * conventions for graph node operations including contextual naming and key-value
 * generation for both low and high cardinality metrics.
 *
 * @author XiaoYunTao
 * @since 2025/6/28
 */
public class DefaultGraphNodeObservationConvention implements GraphNodeObservationConvention {

	/**
	 * Default operation name for graph node observations.
	 */
	public static final String DEFAULT_OPERATION_NAME = "spring.ai.alibaba.graph.node";

	private String name;

	/**
	 * Constructs a new DefaultGraphNodeObservationConvention with the default operation
	 * name.
	 */
	public DefaultGraphNodeObservationConvention() {
		this(DEFAULT_OPERATION_NAME);
	}

	/**
	 * Constructs a new DefaultGraphNodeObservationConvention with the specified name.
	 * @param name the custom operation name for this convention
	 */
	public DefaultGraphNodeObservationConvention(String name) {
		this.name = name;
	}

	/**
	 * Gets the operation name for this convention.
	 * @return the operation name
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Generates a contextual name based on the node observation context. If the context
	 * has a name, it appends it to the default operation name.
	 * @param context the graph node observation context
	 * @return the contextual name, or the default operation name if no context name is
	 * available
	 */
	@Override
	@Nullable
	public String getContextualName(GraphNodeObservationContext context) {
		if (StringUtils.hasText(context.getName())) {
			return "%s.%s".formatted(DEFAULT_OPERATION_NAME, context.getName());
		}
		return DEFAULT_OPERATION_NAME;
	}

	/**
	 * Generates low cardinality key-values for graph node observations. These keys have
	 * limited unique values and are suitable for grouping and filtering.
	 * @param context the graph node observation context
	 * @return KeyValues containing low cardinality metrics
	 */
	@Override
	public KeyValues getLowCardinalityKeyValues(GraphNodeObservationContext context) {
		return KeyValues.of(
				KeyValue.of(GraphNodeObservationDocumentation.LowCardinalityKeyNames.SPRING_AI_ALIBABA_KIND,
						SpringAiAlibabaKind.GRAPH_NODE.getValue()),
				KeyValue.of(GraphNodeObservationDocumentation.LowCardinalityKeyNames.GRAPH_NODE_NAME,
						context.getNodeName()),
				KeyValue.of(GraphNodeObservationDocumentation.LowCardinalityKeyNames.GRAPH_EVENT, context.getEvent()));
	}

	/**
	 * Generates high cardinality key-values for graph node observations. These keys have
	 * many unique values and provide detailed observation data.
	 * @param context the graph node observation context
	 * @return KeyValues containing high cardinality metrics
	 */
	@Override
	public KeyValues getHighCardinalityKeyValues(GraphNodeObservationContext context) {
		return KeyValues.empty();
	}

}
