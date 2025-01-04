package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.node.variable_aggregator.VariableAggregatorNodeAction;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

public class HumanNodeActionTest {
    @Test
    public void humanNodeTest() throws Exception {
        StateGraph stateGraph = new StateGraph(new JSONStateSerializer());
        CompileConfig config = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(SaverConstant.MEMORY, new MemorySaver())
                        .build())
                .interruptBefore("node2")
                .build();
        stateGraph.addNode("node1", AsyncNodeAction.node_async(t -> Map.of("input", "hello world")));
        stateGraph.addNode("node2", AsyncNodeActionWithConfig.node_async(new NodeActionWithConfig() {
            @Override
            public Map<String, Object> apply(NodeState t, RunnableConfig config) throws Exception {
                System.out.println("t1 = " + t);
                Optional<String> resumed = t.resumeInput();
                System.out.println("resumed = " + resumed.get());
                return Map.of("input", "interrupt");
            }
        }));
        stateGraph.addNode("node3", AsyncNodeActionWithConfig.node_async(new NodeActionWithConfig() {
            @Override
            public Map<String, Object> apply(NodeState t, RunnableConfig config) throws Exception {
                System.out.println("t2 = " + t);
                return Map.of("input", "continue");
            }
        }));
        stateGraph.addEdge(StateGraph.START, "node1");
        stateGraph.addEdge("node1", "node2");
        stateGraph.addEdge("node2", "node3");
        stateGraph.addEdge("node3", StateGraph.END);
        CompiledGraph compile = stateGraph.compile(config);
        HashMap<String, Object> input = new HashMap<>();
        input.put("input", "start");
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId("thread1")
                .build();
        Optional invoke = compile.invoke(input, runnableConfig);
        System.out.println("invoke = " + invoke);

        RunnableConfig resumeRunnableConfig = RunnableConfig.builder()
                .threadId("thread1")
                .resume()
                .build();
        Optional invoke1 = compile.invoke(Map.of("resume_input","human input"), resumeRunnableConfig);
        System.out.println("invoke1 = " + invoke1);
    }
}
