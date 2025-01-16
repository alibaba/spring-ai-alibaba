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

/**
 * Represents a state graph with nodes and edges.</p>
 * Except 'START' node and 'END' node, every node has four edges: affirmative edge,negative edge,refusal edge,default edge.
 *
 * @author 黄展鹏
 */
public class StateGraphWithEdge extends StateGraph {
    /**
     * Constructs a new StateGraph with the specified serializer.
     *
     * @param stateSerializer the serializer to serialize the state
     */
    public StateGraphWithEdge(@NonNull StateSerializer stateSerializer) {
        super(stateSerializer);
    }

    public StateGraphWithEdge addNodeWithEdge(String id, String nextNode, AsyncNodeAction action) throws GraphStateException {
        this.addNode(id, action);
        this.addEdge(id, nextNode);
        return this;
    }

    public StateGraphWithEdge addNodeWithEdge(String id, String affirmativeNode, String negativeNode, String refusalNode, String defaultNode, AsyncNodeAction action) throws GraphStateException {
        this.addNode(id, action);
        this.addEdge(id, affirmativeNode);
        this.addEdge(id, negativeNode);
        this.addEdge(id, refusalNode);
        this.addEdge(id, defaultNode);
        return this;
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
        try {
            super.addEdge(sourceId, targetId);
        } catch (GraphStateException e) {
            // 忽略重复边
        } catch (Throwable e) {
            throw new GraphStateException(e.getMessage());
        }
        return this;
    }

    @Override
    public StateGraphWithEdge addConditionalEdges(String sourceId, AsyncEdgeAction condition, Map<String, String> mappings) throws GraphStateException {
        try {
            super.addConditionalEdges(sourceId, condition, mappings);
        } catch (GraphStateException e) {
            // 忽略重复边
        } catch (Throwable e) {
            throw new GraphStateException(e.getMessage());
        }
        return this;
    }

}