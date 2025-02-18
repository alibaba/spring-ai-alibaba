package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.bsc.async.AsyncGenerator;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;

class SubgraphNodeAction implements AsyncNodeActionWithConfig {

	final CompiledGraph subGraph;

	SubgraphNodeAction(CompiledGraph subGraph) {
		this.subGraph = subGraph;
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

		try {
			AsyncGenerator<NodeOutput> generator = subGraph.stream(state.data(), config);
			future.complete(mapOf(NodeState.SUB_GRAPH, generator));
		}
		catch (Exception e) {

			future.completeExceptionally(e);
		}

		return future;
	}

}
