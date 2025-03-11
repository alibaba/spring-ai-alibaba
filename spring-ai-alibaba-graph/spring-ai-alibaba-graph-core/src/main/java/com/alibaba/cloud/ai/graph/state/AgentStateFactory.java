package com.alibaba.cloud.ai.graph.state;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;
import java.util.function.Function;

/**
 * A factory interface for creating instances of {@link AgentState}.
 *
 */
public interface AgentStateFactory<T> extends Function<Map<String, Object>, T> {

}
