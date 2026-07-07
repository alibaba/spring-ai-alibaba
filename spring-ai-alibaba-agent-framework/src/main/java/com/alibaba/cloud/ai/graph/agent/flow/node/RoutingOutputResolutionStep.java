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

/**
 * One item on the explicit output-resolution stack.
 * <p>
 * The resolver uses a stack instead of recursion so wrapper outputs can be delayed
 * until after nested agent outputs have been tried.
 */
interface RoutingOutputResolutionStep {
}

/**
 * Expands an agent through the matching {@link RoutingOutputStrategy}.
 * @param agent agent being expanded
 * @param allowDefaultMessagesOutput whether default messages output can be used
 * @param allowRoutingMergedOutput whether nested routing merged output can be used
 * @param preferWrapperOutput whether wrapper output should be emitted first
 */
record ExpandAgentOutputStep(Agent agent, boolean allowDefaultMessagesOutput,
		boolean allowRoutingMergedOutput, boolean preferWrapperOutput) implements RoutingOutputResolutionStep {

	/**
	 * Creates a nested expansion step while preserving routing-merge permissions.
	 * @param nestedAgent nested agent to expand next
	 * @param allowDefaultOutput whether the nested agent may use default messages output
	 * @return nested agent expansion step
	 */
	ExpandAgentOutputStep forNestedAgent(Agent nestedAgent, boolean allowDefaultOutput) {
		return new ExpandAgentOutputStep(nestedAgent, allowDefaultOutput, allowRoutingMergedOutput, false);
	}

}

/**
 * Emits one concrete state key candidate in the current stack order.
 * @param candidate output candidate to append
 */
record EmitOutputCandidateStep(RoutingOutputCandidate candidate) implements RoutingOutputResolutionStep {
}
