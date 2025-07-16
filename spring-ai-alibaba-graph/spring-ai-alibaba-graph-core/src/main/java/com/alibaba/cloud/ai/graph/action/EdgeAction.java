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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;

/**
 * Represents an edge action that operates on an agent state and returns a result.
 *
 */
@FunctionalInterface
public interface EdgeAction {

	/**
	 * Applies this action to the given agent state.
	 * @param state the agent state
	 * @return a result of the action
	 * @throws Exception if an error occurs during the action
	 */
	String apply(OverAllState state) throws Exception;

}
