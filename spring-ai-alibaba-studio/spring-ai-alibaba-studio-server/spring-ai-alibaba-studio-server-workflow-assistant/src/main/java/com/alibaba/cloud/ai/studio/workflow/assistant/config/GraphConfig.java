package com.alibaba.cloud.ai.studio.workflow.assistant.config;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {

    /**
     * A simple StateGraph bean for testing purposes.
     * @return a new StateGraph instance.
     */
    @Bean
    public StateGraph testGraph() {
        // For now, it's an empty graph. You can add nodes and edges here later.
        return new StateGraph();
    }

    /**
     * Creates a SAAGraphFlow bean for testing.
     * It depends on the testGraph bean which is automatically injected by Spring.
     *
     * @param testGraph The StateGraph bean to be included in the flow.
     * @return A configured SAAGraphFlow bean.
     */
    @Bean
    public SAAGraphFlow testGraphFlow(StateGraph testGraph) {
        return SAAGraphFlow.builder()
                .id("test")
                .title("A test GraphFLow")
                .stateGraph(testGraph) // Correctly inject the StateGraph bean
                .addTag("TEST")
                .ownerID("saa")
                .build();
    }
}
