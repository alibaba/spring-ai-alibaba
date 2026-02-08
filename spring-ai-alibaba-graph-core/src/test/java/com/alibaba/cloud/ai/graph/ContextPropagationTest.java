/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;


import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContextPropagationTest {

    @Test
    public void testContextPropagationToSubgraph() throws Exception {
        // Child graph node that reads from context
        AsyncNodeActionWithConfig childNode = AsyncNodeActionWithConfig.node_async((state, config) -> {
            String contextValue = (String) config.context().get("testKey");
            return Map.of("result", contextValue != null ? contextValue : "MISSING");
        });

        StateGraph childGraph = new StateGraph()
                .addNode("childNode", childNode)
                .addEdge(START, "childNode")
                .addEdge("childNode", END);

        // Parent graph
        StateGraph parentGraph = new StateGraph()
                .addNode("subgraph", childGraph.compile())
                .addEdge(START, "subgraph")
                .addEdge("subgraph", END);

        CompiledGraph compiledGraph = parentGraph.compile();

        // Create config with context
        RunnableConfig config = RunnableConfig.builder()
                .build();
        config.context().put("testKey", "testValue");

        // Run graph
        Map<String, Object> result = compiledGraph.invoke(Map.of(), config).orElseThrow().data();

        // Verify result
        assertEquals("testValue", result.get("result"), "Context value should be propagated to subgraph");
    }
}
