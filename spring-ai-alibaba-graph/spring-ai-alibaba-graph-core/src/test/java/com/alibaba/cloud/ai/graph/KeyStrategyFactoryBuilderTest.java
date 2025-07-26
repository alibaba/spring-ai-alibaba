package com.alibaba.cloud.ai.graph;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

class KeyStrategyFactoryBuilderTest {
    private static final Logger log = LoggerFactory.getLogger(KeyStrategyFactoryBuilderTest.class);

    @Test
    void buildTest() throws Exception {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .defaultStrategy(KeyStrategy.REPLACE)
                .addStrategy("prop1")
                .build();
        StateGraph workflow = new StateGraph(keyStrategyFactory)
                .addEdge(START, "agent_1")
                .addNode("agent_1", node_async(state -> {
                    log.info("agent_1\n{}", state);
                    return Map.of("prop1", "test");
                }))
                .addEdge("agent_1", END);

        CompiledGraph app = workflow.compile();

        Optional<OverAllState> result = app.invoke(Map.of(OverAllState.DEFAULT_INPUT_KEY, "test1"));
        System.out.println("result = " + result);
        assertTrue(result.isPresent());

        Map<String, String> expected = Map.of("input", "test1", "prop1", "test");
    }

}