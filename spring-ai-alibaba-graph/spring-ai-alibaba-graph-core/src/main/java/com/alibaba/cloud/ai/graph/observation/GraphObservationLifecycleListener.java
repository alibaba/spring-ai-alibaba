package com.alibaba.cloud.ai.graph.observation;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.Map;

public class GraphObservationLifecycleListener implements GraphLifecycleListener {
    private final ObservationRegistry observationRegistry;

    public GraphObservationLifecycleListener(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
        Observation.start(
                GraphObservationDocumentation.GRAPH_NODE_EXECUTION.getName(),
                () -> new GraphNodeObservationContext(nodeId, "onStart", state, null),
                observationRegistry
        ).stop();
    }

    @Override
    public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
        Observation.start(
                GraphObservationDocumentation.GRAPH_NODE_EXECUTION.getName(),
                () -> new GraphNodeObservationContext(nodeId, "before", state, null),
                observationRegistry
        ).stop();
    }

    @Override
    public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
        Observation.start(
                GraphObservationDocumentation.GRAPH_NODE_EXECUTION.getName(),
                () -> new GraphNodeObservationContext(nodeId, "after", state, null),
                observationRegistry
        ).stop();
    }

    @Override
    public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
        Observation.start(
                GraphObservationDocumentation.GRAPH_NODE_EXECUTION.getName(),
                () -> new GraphNodeObservationContext(nodeId, "onError", state, ex),
                observationRegistry
        ).stop();
    }

    @Override
    public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
        Observation.start(
                GraphObservationDocumentation.GRAPH_NODE_EXECUTION.getName(),
                () -> new GraphNodeObservationContext(nodeId, "onComplete", state, null),
                observationRegistry
        ).stop();
    }
} 