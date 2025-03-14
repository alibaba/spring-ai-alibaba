package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncSendEdgeAction;
import com.alibaba.cloud.ai.graph.action.SendEdgeAction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.SubGraphTest.*;

public class SendEdgeGraphTest {

    @Test
    public void sendEdgeGraphTest() throws Exception {
        var workflowChild = new StateGraph()
                .addNode("B1", _makeNode("B1"))
                .addNode("B2", _makeNode("B2"))
                .addConditionalEdges(START, AsyncSendEdgeAction.edge_async(t -> {
                    Send send = new Send();
                    send.setGraph(Command.GraphType.PARENT);
                    send.setEdge("C");
                    return send;
                }))
                .addEdge("B1", "B2")
                .addEdge("B2", END);

        var workflowParent = new StateGraph(getOverAllState())
                .addNode("A", _makeNode("A"))
                .addSubgraph("B", workflowChild)
                .addNode("C", _makeNode("C"))
                .addEdge(START, "B")
                .addEdge("B", "A")
                .addEdge("A", "C")
                .addEdge("C", END);

        var app = workflowParent.compile();
        _execute(app, Map.of());
    }
}
