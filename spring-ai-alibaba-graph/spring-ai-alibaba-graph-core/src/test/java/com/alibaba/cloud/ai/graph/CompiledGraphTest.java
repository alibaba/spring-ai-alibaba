package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

class CompiledGraphTest {

    @Test
    void testCompiledGraphInvoke() throws GraphStateException, GraphRunnerException {
        // 状态工厂注册字段与策略
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                "input", new ReplaceStrategy(),
                "output", new ReplaceStrategy()
        );

        StateGraph graph = new StateGraph("hello graph", keyStrategyFactory)
                .addNode("hello", node_async(t -> Map.of("output", "hello " + t.value("input", ""))))
                .addEdge(START, "hello")
                .addEdge("hello", END);

        CompiledGraph compiledGraph = graph.compile();

        OverAllState result = compiledGraph.invoke(Map.of("input", "a"), RunnableConfig.builder().build()).orElseThrow();
        assertEquals("hello a", result.value("output").orElseThrow());

        result = compiledGraph.invoke(Map.of("input", "lfy"), RunnableConfig.builder().build()).orElseThrow();
        assertEquals("hello lfy", result.value("output").orElseThrow());
    }

}