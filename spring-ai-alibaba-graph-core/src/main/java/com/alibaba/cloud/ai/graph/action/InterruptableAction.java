package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Optional;

/**
 * Defines a contract for actions that can interrupt the execution of a graph.
 *
 */
public interface InterruptableAction {

	/**
	 * Determines whether the graph execution should be interrupted at the current node.
	 * @param nodeId The identifier of the current node being processed.
	 * @param state The current state of the agent.
	 * @return An {@link Optional} containing {@link InterruptionMetadata} if the
	 * execution should be interrupted. Returns an empty {@link Optional} to continue
	 * execution.
	 */
	Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state);

}
