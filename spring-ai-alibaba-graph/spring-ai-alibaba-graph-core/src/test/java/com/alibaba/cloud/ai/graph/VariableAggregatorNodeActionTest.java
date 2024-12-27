package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.node.variable_aggregator.VariableAggregatorDescriptor;
import com.alibaba.cloud.ai.graph.node.variable_aggregator.VariableAggregatorNodeAction;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class VariableAggregatorNodeActionTest {


    @Test
    public void nodeTest() throws Exception {
        StateGraph stateGraph = new StateGraph(new JSONStateSerializer());
        CompileConfig config = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(SaverConstant.MEMORY, new MemorySaver())
                        .build())
                .build();
        stateGraph.addNode("node1", AsyncNodeAction.node_async(t -> Map.of("input", "hello world")));
        VariableAggregatorDescriptor variableAggregatorDescriptor = new VariableAggregatorDescriptor();
        variableAggregatorDescriptor.setVariables(List.of(List.of("node1", "input")));
        variableAggregatorDescriptor.setOutputType("string");
        //advanceSettings Test
        VariableAggregatorDescriptor.AdvancedSettings advancedSettings = new VariableAggregatorDescriptor.AdvancedSettings();
        advancedSettings.setGroupEnabled(true);
        VariableAggregatorDescriptor.Groups groups = new VariableAggregatorDescriptor.Groups();
        groups.setGroupName("Group1");
        groups.setOutputType("String");
        groups.setVariables(List.of(List.of("node1", "input")));
        advancedSettings.setGroups(List.of(groups));
        variableAggregatorDescriptor.setAdvancedSettings(advancedSettings);
        stateGraph.addNode("node2", AsyncNodeActionWithConfig.node_async(new VariableAggregatorNodeAction(variableAggregatorDescriptor, config)));
        stateGraph.addEdge(StateGraph.START, "node1");
        stateGraph.addEdge("node1", "node2");
        stateGraph.addEdge("node2", StateGraph.END);
        CompiledGraph compile = stateGraph.compile(config);
        HashMap<String, Object> input = new HashMap<>();
        input.put("input", "start");
        Optional invoke = compile.invoke(input);
        System.out.println(invoke.get());
    }

}
