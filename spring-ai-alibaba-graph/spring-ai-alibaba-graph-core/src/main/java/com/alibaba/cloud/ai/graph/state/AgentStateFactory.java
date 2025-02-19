package com.alibaba.cloud.ai.graph.state;

import java.util.Map;
import java.util.function.Function;

/**
 * A factory interface for creating instances of {@link AgentState}.
 *
 * @param <State> the type of the agent state
 */
public interface AgentStateFactory<State extends AgentState> extends Function<Map<String, Object>, State> {

}
