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
package com.alibaba.cloud.ai.graph.serializer;

import java.util.List;
import java.util.Optional;

/**
 * Represents a value that can be appended to and provides various utility methods.
 *
 * @param <T> the type of the value
 */
@Deprecated(forRemoval = true)
public interface AppendableValue<T> {

	/**
	 * Returns the list of values.
	 * @return a list of values
	 */
	List<T> values();

	/**
	 * Checks if the value list is empty.
	 * @return true if the value list is empty, false otherwise
	 */
	boolean isEmpty();

	/**
	 * Returns the size of the value list.
	 * @return the size of the value list
	 */
	int size();

	/**
	 * Returns the last value in the list, if present.
	 * @return an Optional containing the last value if present, otherwise an empty
	 * Optional
	 */
	Optional<T> last();

	/**
	 * Returns the value at the specified position from the end of the list, if present.
	 * @param n the position from the end of the list
	 * @return an Optional containing the value at the specified position if present,
	 * otherwise an empty Optional
	 */
	Optional<T> lastMinus(int n);

}
