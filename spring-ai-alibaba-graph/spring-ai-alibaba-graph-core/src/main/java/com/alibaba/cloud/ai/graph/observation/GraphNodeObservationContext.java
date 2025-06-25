package com.alibaba.cloud.ai.graph.observation;

import io.micrometer.observation.Observation;
import java.util.Map;

public class GraphNodeObservationContext extends Observation.Context {
    private final String nodeId;
    private final String event;
    private final Map<String, Object> state;
    private final Throwable error;

    public GraphNodeObservationContext(String nodeId, String event, Map<String, Object> state, Throwable error) {
        this.nodeId = nodeId;
        this.event = event;
        this.state = state;
        this.error = error;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getEvent() {
        return event;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public Throwable getError() {
        return error;
    }
} 