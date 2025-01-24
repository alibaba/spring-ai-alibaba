package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateGraphTest {
    @Test
    public void graphTest() throws Exception {
        // ....node1 -> node2 -> node3 -> END
        OverAllState overAllState = new OverAllState()
                .initData(Map.of("key1", "input text"))
                .addKeyStrategy("key1", (o, o2) -> o2)
                .addKeyStrategy("key2", (o, o2) -> Lists.newArrayList(o,o2))
                .addKeyStrategy("key3", (o, o2) -> o.toString() + o2.toString());

        StateGraph workflow = new StateGraph(new JSONStateSerializer())
                .addEdge(START, "agent_1")
                .addNode("agent_1", AsyncNodeActionWithConfig.node_async((state, config) -> {
                    System.out.print("agent_1");
                    System.out.println(state);
                    return Map.of("key1", "test");
                }))
                .addNode("agent_2", node_async(state -> {
                    System.out.print("agent_2");
                    System.out.println(state);
                    return Map.of("key2", "test2");
                }))
                .addNode("agent_3", node_async(state -> {
                    System.out.print("agent_3");
                    System.out.println(state);
                    return Map.of("key4", "test2");
                }))
                .addEdge("agent_1", "agent_2")
                .addEdge("agent_2", "agent_3")
                .addEdge("agent_3", END);
        CompiledGraph app = workflow.compile();

        Optional result = app.invoke(overAllState);
        System.out.println("result = " + result);
        assertTrue(result.isPresent());

        Map<String, String> expected = Map.of("input", "test1", "prop1", "test");

    }

    @Test
    public void resumeGraphTest() throws Exception {
        StateGraph stateGraph = new StateGraph(new JSONStateSerializer());
        CompileConfig config = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(SaverConstant.MEMORY, new MemorySaver())
                        .build())
                .interruptBefore("node2")
                .build();
        stateGraph.addNode("node1", AsyncNodeAction.node_async(t -> Map.of("input", "hello world")));
        stateGraph.addNode("node2", AsyncNodeActionWithConfig.node_async((state, config12) -> {
            System.out.println("t1 = " + state);
            return Map.of("input", "interrupt");
        }));
        stateGraph.addNode("node3", AsyncNodeActionWithConfig.node_async((t, config1) -> {
            System.out.println("t2 = " + t);
            return Map.of("input", "continue");
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
                .build();
//        Optional invoke1 = compile.invoke();
//        System.out.println("invoke1 = " + invoke1);

    }
}
