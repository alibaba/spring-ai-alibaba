package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.AgentState;
import org.bsc.async.AsyncGenerator;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;

class SubgraphNodeAction<State extends AgentState> implements AsyncNodeActionWithConfig<State> {

    final CompiledGraph<State> subGraph;

    SubgraphNodeAction(CompiledGraph<State> subGraph ) {
        this.subGraph = subGraph;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config)  {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        try {

            AsyncGenerator<NodeOutput<State>> generator = subGraph.stream( state.data(), config );

            future.complete( mapOf( "_subgraph", generator ) );

        } catch (Exception e) {

            future.completeExceptionally(e);
        }

        return future;
    }
}
