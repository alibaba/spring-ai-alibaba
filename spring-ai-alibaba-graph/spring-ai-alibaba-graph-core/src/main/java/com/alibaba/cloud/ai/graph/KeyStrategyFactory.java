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
package com.alibaba.cloud.ai.graph;

import java.util.Map;

/**
 * Factory interface for providing key strategy implementations. This interface is
 * designed to allow flexible creation of different KeyStrategy instances, which are used
 * for generating cache keys based on input parameters and system state. The apply method
 * should return a map of strategies keyed by String identifiers, enabling retrieval of
 * specific strategies by name when needed.
 *
 * @author disaster
 * @since 1.0.0.1
 */
@FunctionalInterface
public interface KeyStrategyFactory {

	/**
	 * Creates and returns a map of KeyStrategy instances. Implementations of this method
	 * should provide one or more named KeyStrategy implementations.
	 * @return a Map containing strategy names as keys and corresponding KeyStrategy
	 * instances as values
	 */
	Map<String, KeyStrategy> apply();

}
