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
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;

import java.util.Deque;
import java.util.List;

/**
 * Appends output-key candidates for one supported agent type.
 * <p>
 * Implementations do not read state. They only describe where an agent would
 * publish its result, leaving attribution and stale-state checks to
 * {@link RoutingOutputResolver}.
 */
interface RoutingOutputStrategy {

	/**
	 * Checks whether this strategy can resolve output candidates for the agent.
	 * @param agent agent to inspect
	 * @return true when this strategy supports the agent type
	 */
	boolean supports(Agent agent);

	/**
	 * Appends output candidates or nested resolution steps for the agent.
	 * @param step current agent expansion step
	 * @param steps stack used to continue nested output resolution
	 * @param candidates ordered output candidates collected so far
	 */
	void appendCandidates(ExpandAgentOutputStep step, Deque<RoutingOutputResolutionStep> steps,
			List<RoutingOutputCandidate> candidates);

	/**
	 * Pushes nested agents in reverse order so stack traversal preserves list order.
	 * @param agents nested agents to push
	 * @param source current expansion step used as a template
	 * @param steps stack receiving nested expansion steps
	 * @param allowDefaultMessagesOutput whether nested base agents may use messages
	 * fallback
	 */
	static void pushAllAgentSteps(List<Agent> agents, ExpandAgentOutputStep source,
			Deque<RoutingOutputResolutionStep> steps, boolean allowDefaultMessagesOutput) {
		if (agents == null) {
			return;
		}
		for (int i = agents.size() - 1; i >= 0; i--) {
			steps.push(source.forNestedAgent(agents.get(i), allowDefaultMessagesOutput));
		}
	}

}

/**
 * Base agents publish either their explicit output key or the shared messages key.
 */
final class BaseAgentOutputStrategy implements RoutingOutputStrategy {

	@Override
	public boolean supports(Agent agent) {
		return agent instanceof BaseAgent;
	}

	@Override
	public void appendCandidates(ExpandAgentOutputStep step, Deque<RoutingOutputResolutionStep> steps,
			List<RoutingOutputCandidate> candidates) {
		BaseAgent baseAgent = (BaseAgent) step.agent();
		String outputKey = baseAgent.getOutputKey();
		if (outputKey != null) {
			candidates.add(new RoutingOutputCandidate(outputKey, false));
		}
		else if (step.allowDefaultMessagesOutput()) {
			candidates.add(new RoutingOutputCandidate(RoutingMergeNode.DEFAULT_MESSAGES_OUTPUT_KEY, false));
		}
	}

}

/**
 * Parallel agents use their configured merge output or collect each child output.
 */
final class ParallelAgentOutputStrategy implements RoutingOutputStrategy {

	@Override
	public boolean supports(Agent agent) {
		return agent instanceof ParallelAgent;
	}

	@Override
	public void appendCandidates(ExpandAgentOutputStep step, Deque<RoutingOutputResolutionStep> steps,
			List<RoutingOutputCandidate> candidates) {
		ParallelAgent parallelAgent = (ParallelAgent) step.agent();
		if (parallelAgent.mergeOutputKey() != null) {
			candidates.add(new RoutingOutputCandidate(parallelAgent.mergeOutputKey(), false));
		}
		else {
			RoutingOutputStrategy.pushAllAgentSteps(parallelAgent.subAgents(), step, steps, false);
		}
	}

}

/**
 * Nested routers publish their synthesized answer through the routing merge key.
 */
final class LlmRoutingAgentOutputStrategy implements RoutingOutputStrategy {

	@Override
	public boolean supports(Agent agent) {
		return agent instanceof LlmRoutingAgent;
	}

	@Override
	public void appendCandidates(ExpandAgentOutputStep step, Deque<RoutingOutputResolutionStep> steps,
			List<RoutingOutputCandidate> candidates) {
		if (step.allowRoutingMergedOutput()) {
			candidates.add(new RoutingOutputCandidate(RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY, false));
		}
	}

}

/**
 * Sequential agents expose the final child output as the workflow result.
 */
final class SequentialAgentOutputStrategy implements RoutingOutputStrategy {

	@Override
	public boolean supports(Agent agent) {
		return agent instanceof SequentialAgent;
	}

	@Override
	public void appendCandidates(ExpandAgentOutputStep step, Deque<RoutingOutputResolutionStep> steps,
			List<RoutingOutputCandidate> candidates) {
		SequentialAgent sequentialAgent = (SequentialAgent) step.agent();
		List<Agent> nestedAgents = sequentialAgent.subAgents();
		if (nestedAgents != null && !nestedAgents.isEmpty()) {
			steps.push(step.forNestedAgent(nestedAgents.get(nestedAgents.size() - 1),
					step.allowDefaultMessagesOutput()));
		}
	}

}

/**
 * Generic flow agents may expose outputs from any nested child.
 */
final class FlowAgentOutputStrategy implements RoutingOutputStrategy {

	@Override
	public boolean supports(Agent agent) {
		return agent instanceof FlowAgent;
	}

	@Override
	public void appendCandidates(ExpandAgentOutputStep step, Deque<RoutingOutputResolutionStep> steps,
			List<RoutingOutputCandidate> candidates) {
		FlowAgent flowAgent = (FlowAgent) step.agent();
		RoutingOutputStrategy.pushAllAgentSteps(flowAgent.subAgents(), step, steps,
				step.allowDefaultMessagesOutput());
	}

}
