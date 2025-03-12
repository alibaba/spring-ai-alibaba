package com.alibaba.cloud.ai.graph;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.SubGraphTest.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class CommandGraphTest {


    @Test
    public void testCommandSubgraph01() throws Exception {

        var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
                .addNode("B2", _makeNode("B2"))
                .addCommandNode("command", (t, config) -> {
                    Command command = new Command();
                    //If nodeid is not set, the nodeid of the current subgraph is obtained by default
                    // command.setNodeId("B");
                    command.setGraph(Command.GraphType.CHILD);
                    command.setEdge("B1");
                    //Additional parameters can be added to the command object
                    command.put("messages","command");
                    return command;
                })
                .addEdge(START, "command")
                .addEdge("B1",END);

        var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
                .addSubgraph("B", workflowChild)
                .addNode("C", _makeNode("C"))
                .addEdge(START, "C")
                .addEdge("C", "B")
                .addEdge("B","A")
                .addEdge("A", END);


        var app = workflowParent.compile();
        _execute(app, Map.of());
	}

    @Test
    public void testCommandSubgraph02() throws Exception {

        var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
                .addNode("B2", _makeNode("B2"))
                .addCommandNode("command", (t, config) -> {
                    Command command = new Command();
                    // If graph is of the parent type, nodeId is not required
                    command.setGraph(Command.GraphType.PARENT);
                    command.setEdge("A");
                    // Additional parameters can be added to the command object
                    command.put("messages","command");
                    return command;
                })
                .addEdge(START, "command")
                .addEdge("B1",END);

        var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
                .addSubgraph("B", workflowChild)
                .addNode("C", _makeNode("C"))
                .addEdge(START, "C")
                .addEdge("C", "B")
                .addEdge("B","A")
                .addEdge("A", END);


        var app = workflowParent.compile();
        _execute(app, Map.of());
    }

    @Test
    public void testCommandSubgraph03() throws Exception {

        var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
                .addNode("B2", _makeNode("B2"))
                .addCommandNode("command", (t, config) -> {
                    Command command = new Command();
                    // If graph is of the parent type, nodeId is not required
                    command.setGraph(Command.GraphType.PARENT);
                    command.setEdge("A");
                    // Additional parameters can be added to the command object
                    command.put("messages","command");
                    return command;
                })
                .addEdge(START, "command")
                .addEdge("B1",END);

        var workflowChild2 = new StateGraph().addNode("B1", _makeNode("B1"))
                .addNode("B2", _makeNode("B2"))
                .addCommandNode("command", (t, config) -> {
                    Command command = new Command();
                    // If graph is of the parent type, nodeId is not required
                    command.setGraph(Command.GraphType.CHILD);
                    command.setNodeId("B");
                    command.setEdge("B1");
                    // Additional parameters can be added to the command object
                    command.put("messages","command");
                    return command;
                })
                .addEdge(START, "command")
                .addEdge("B1",END);

        var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
                .addSubgraph("B", workflowChild)
                .addNode("C", _makeNode("C"))
                .addSubgraph("D", workflowChild2)
                .addEdge(START, "C")
                .addEdge("C", "B")
                .addEdge("B","A")
                .addEdge("A","D")
                .addEdge("D", END);


        var app = workflowParent.compile();
        _execute(app, Map.of());
    }
}
