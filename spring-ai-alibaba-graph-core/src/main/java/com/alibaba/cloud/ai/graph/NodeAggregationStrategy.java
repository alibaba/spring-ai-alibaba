/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph;

/**
 * Strategy for aggregating results from parallel node execution.
 * <p>
 * Determines how parallel branches should be aggregated:
 * <ul>
 *   <li>{@code ALL_OF}: Wait for all parallel branches to complete before proceeding</li>
 *   <li>{@code ANY_OF}: Proceed after the first successful branch completion</li>
 * </ul>
 */
public enum NodeAggregationStrategy {
	/**
	 * Wait for all parallel branches to complete before proceeding.
	 * This is the default behavior and ensures all results are collected.
	 */
	ALL_OF,

	/**
	 * Proceed after the first successful branch, including completion of its streams.
	 * Only the winning branch's result and stream events are published.
	 */
	ANY_OF
}
