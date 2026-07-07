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
package com.alibaba.cloud.ai.graph.agent.flow.node;

/**
 * A state key that may contain the visible result for one routed agent.
 * <p>
 * Wrapper candidates are namespaced subgraph outputs such as
 * {@code subgraph_<agent>_compiled_graph}; non-wrapper candidates are raw output
 * keys written by nested agents.
 * @param outputKey state key to probe
 * @param wrapperOutput whether the key is a subgraph wrapper key
 */
record RoutingOutputCandidate(String outputKey, boolean wrapperOutput) {
}
