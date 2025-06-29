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
package com.alibaba.cloud.ai.graph.observation.metric;

/**
 * Enumeration defining metric names for Spring AI Alibaba graph observations. Provides
 * standardized metric names for graph, node, and edge operations. Used for identifying
 * and categorizing different types of observation metrics.
 *
 * @author XiaoYunTao
 * @since 2025/6/28
 */
public enum SpringAiAlibabaObservationMetricNames {

	/**
	 * Metric name for graph operations.
	 */
	GRAPH("spring.ai.alibaba.graph"),

	/**
	 * Metric name for graph node operations.
	 */
	GRAPH_NODE("spring.ai.alibaba.graph.node"),

	/**
	 * Metric name for graph edge operations.
	 */
	GRAPH_EDGE("spring.ai.alibaba.graph.edge");

	private final String value;

	/**
	 * Constructs a new SpringAiAlibabaObservationMetricNames with the specified value.
	 * @param value the string representation of this metric name
	 */
	SpringAiAlibabaObservationMetricNames(String value) {
		this.value = value;
	}

	/**
	 * Gets the string value of this metric name.
	 * @return the string representation of this metric name
	 */
	public String value() {
		return value;
	}

}
