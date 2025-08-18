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
package com.alibaba.cloud.ai.graph.observation;

/**
 * Enumeration defining the types of AI operations in Spring AI Alibaba. Provides
 * standardized identifiers for different graph-related observation kinds. Used for
 * categorizing and filtering observation metrics and traces.
 *
 * @author XiaoYunTao
 * @since 2025/6/28
 */
public enum SpringAiAlibabaKind {

	/**
	 * Represents a complete graph operation.
	 */
	GRAPH("graph"),

	/**
	 * Represents a graph node operation.
	 */
	GRAPH_NODE("graph_node"),

	/**
	 * Represents a graph edge operation.
	 */
	GRAPH_EDGE("graph_edge");

	private final String value;

	/**
	 * Constructs a new SpringAiAlibabaKind with the specified value.
	 * @param value the string representation of this kind
	 */
	SpringAiAlibabaKind(String value) {
		this.value = value;
	}

	/**
	 * Gets the string value of this kind.
	 * @return the string representation of this kind
	 */
	public String getValue() {
		return this.value;
	}

}
