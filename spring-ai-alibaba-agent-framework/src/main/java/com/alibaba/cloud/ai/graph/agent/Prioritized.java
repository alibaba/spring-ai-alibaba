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
package com.alibaba.cloud.ai.graph.agent;

/**
 * Interface for objects that can be prioritized and sorted by their order value.
 * <p>
 * Classes implementing this interface can be sorted based on their order value.
 * Objects with smaller order values will be placed before objects with larger order values
 * (ascending order). This is commonly used for hooks and other components that need
 * to execute in a specific sequence.
 * </p>
 *
 * @since 1.0.0
 */
public interface Prioritized {

	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

	/**
	 * Returns the order value for this object.
	 * <p>
	 * The order value determines the priority of this object when sorted with other
	 * {@link Prioritized} objects. Objects with smaller order values will be sorted
	 * before objects with larger order values (ascending order).
	 * </p>
	 * <p>
	 * For example:
	 * <ul>
	 *   <li>An object with order 1 will be placed before an object with order 5</li>
	 *   <li>An object with order 10 will be placed before an object with order 20</li>
	 * </ul>
	 * </p>
	 *
	 * @return the order value (smaller values indicate higher priority)
	 */
	int getOrder();
}
