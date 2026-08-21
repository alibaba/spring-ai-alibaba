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

import com.alibaba.cloud.ai.graph.agent.Agent;

import java.util.List;

/**
 * A state key that may contain the visible result for one routed agent.
 * <p>
 * Wrapper candidates are namespaced subgraph outputs such as
 * {@code subgraph_<agent>_compiled_graph}; non-wrapper candidates are raw output
 * keys written by nested agents.
 * @param outputKey state key to probe
 * @param outputOwner agent whose raw output or wrapper fallback produced this candidate
 * @param wrapperOutput whether the key is a subgraph wrapper key
 * @param routingWrapperOutput whether the wrapper belongs to a routing graph whose
 * merged result is valid inside the wrapper snapshot
 * @param preferredInnerOutputKeys ordered output keys to collect inside a wrapper
 * snapshot; more than one key represents the visible outputs of a workflow such as a
 * {@code ParallelAgent} without a merge output key
 */
record RoutingOutputCandidate(String outputKey, Agent outputOwner, boolean wrapperOutput,
			boolean routingWrapperOutput, List<String> preferredInnerOutputKeys) {

	RoutingOutputCandidate {
		preferredInnerOutputKeys = preferredInnerOutputKeys != null
				? List.copyOf(preferredInnerOutputKeys) : List.of();
	}

	RoutingOutputCandidate(String outputKey, Agent outputOwner, boolean wrapperOutput) {
		this(outputKey, outputOwner, wrapperOutput, false, List.of());
	}

	RoutingOutputCandidate(String outputKey, Agent outputOwner, boolean wrapperOutput,
			boolean routingWrapperOutput) {
		this(outputKey, outputOwner, wrapperOutput, routingWrapperOutput, List.of());
	}

}
