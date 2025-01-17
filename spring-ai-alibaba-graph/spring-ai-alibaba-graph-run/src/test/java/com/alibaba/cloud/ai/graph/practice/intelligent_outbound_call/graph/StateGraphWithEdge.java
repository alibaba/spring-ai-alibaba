package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import lombok.NonNull;

import java.util.Map;

public class StateGraphWithEdge extends StateGraph {
    public StateGraphWithEdge(@NonNull StateSerializer stateSerializer) {
        super(stateSerializer);
    }

    public void addNodeWithEdge(String id, String nextNode, AsyncNodeAction action) throws GraphStateException {
        this.addNode(id, action);
        this.addEdge(id, nextNode);
    }

    public void addNodeWithConditionalEdge(String id, String affirmativeNode, String negativeNode, String refusalNode, String defaultNode, AsyncNodeAction action, AsyncEdgeAction condition) throws GraphStateException {
        Map<String, String> mappings = Map.of("affirmative",affirmativeNode,"negative",negativeNode,"refusal",refusalNode,"default",defaultNode);
        this.addNode(id, action);
        this.addConditionalEdges(id, condition, mappings);
    }

    @Override
    public StateGraphWithEdge addSubgraph(String id, CompiledGraph subGraph) throws GraphStateException {
        super.addSubgraph(id, subGraph);
        return this;
    }

    @Override
    public StateGraphWithEdge addNode(String id, AsyncNodeAction action) throws GraphStateException {
        super.addNode(id, action);
        return this;
    }

    @Override
    public StateGraphWithEdge addNode(String id, AsyncNodeActionWithConfig actionWithConfig) throws GraphStateException {
        super.addNode(id, actionWithConfig);
        return this;
    }

    @Override
    public StateGraphWithEdge addEdge(String sourceId, String targetId) throws GraphStateException {
        super.addEdge(sourceId, targetId);
        return this;
    }

    @Override
    public StateGraphWithEdge addConditionalEdges(String sourceId, AsyncEdgeAction condition, Map<String, String> mappings) throws GraphStateException {
        super.addConditionalEdges(sourceId, condition, mappings);
        return this;
    }

}