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
/**
 * Provides classes and interfaces for managing the state of agents in the Spring AI
 * Alibaba framework.
 * <p>
 * This package includes:
 * <ul>
 * <li>{@link com.alibaba.cloud.ai.graph.state.AgentState} - Represents the state of an
 * agent with a map of data.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.state.AgentStateFactory} - A factory interface
 * for creating instances of {@link com.alibaba.cloud.ai.graph.state.AgentState}.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.state.AppendableValue} - Represents a value that
 * can be appended to and provides various utility methods.</li>
 * <li>{@link com.alibaba.cloud.ai.graph.state.AppendableValueRW} - A class that
 * implements the {@link com.alibaba.cloud.ai.graph.state.AppendableValue} interface and
 * provides functionality to append values to a list and retrieve various properties of
 * the list.</li>
 * </ul>
 */
package com.alibaba.cloud.ai.graph.state;