package com.alibaba.cloud.ai.graph;

import lombok.Value;
import lombok.experimental.Accessors;
import com.alibaba.cloud.ai.graph.state.AgentState;

@Value
@Accessors(fluent = true)
public class EdgeValue<State extends AgentState> {

	/**
	 * The unique identifier for the edge value.
	 */
	String id;

	/**
	 * The condition associated with the edge value.
	 */
	EdgeCondition<State> value;

}
