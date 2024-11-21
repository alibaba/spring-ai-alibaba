package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.model.Workflow;

public interface GraphConverter<State extends AgentState> {

    Workflow fromGraph(StateGraph<State> graph);

    StateGraph<State> toGraph(Workflow workflow);
}
