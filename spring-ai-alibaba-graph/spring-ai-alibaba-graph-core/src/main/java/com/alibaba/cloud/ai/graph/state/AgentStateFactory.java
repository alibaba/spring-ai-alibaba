package com.alibaba.cloud.ai.graph.state;

import java.util.Map;
import java.util.function.Function;

/**
 * A factory interface for creating instances of {@link NodeState}.
 *
 */
public interface AgentStateFactory extends Function<Map<String, Object>, NodeState> {

}
