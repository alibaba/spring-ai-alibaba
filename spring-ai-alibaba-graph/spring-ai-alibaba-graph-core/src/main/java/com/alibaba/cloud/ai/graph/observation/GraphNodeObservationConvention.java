package com.alibaba.cloud.ai.graph.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import org.springframework.util.StringUtils;

public class GraphNodeObservationConvention implements ObservationConvention<GraphNodeObservationContext> {

    public static final String DEFAULT_OPERATION_NAME = "graph_node_execution";

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof GraphNodeObservationContext;
    }

    @Override
    public String getName() {
        return DEFAULT_OPERATION_NAME;
    }

    @Override
    public String getContextualName(GraphNodeObservationContext context) {
        if (StringUtils.hasText(context.getNodeId())) {
            return "%s %s".formatted(DEFAULT_OPERATION_NAME, context.getNodeId());
        }
        return DEFAULT_OPERATION_NAME;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(GraphNodeObservationContext context) {
        return KeyValues.of(
                KeyValue.of(GraphObservationDocumentation.GRAPH_NODE_ID, context.getNodeId()),
                KeyValue.of(GraphObservationDocumentation.GRAPH_EVENT, context.getEvent())
        );
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(GraphNodeObservationContext context) {
        KeyValues keyValues = KeyValues.empty();
        if (context.getState() != null) {
            keyValues = keyValues.and(GraphObservationDocumentation.GRAPH_STATE, context.getState().toString());
        }
        if (context.getError() != null) {
            keyValues = keyValues.and(GraphObservationDocumentation.GRAPH_ERROR, context.getError().toString());
        }
        return keyValues;
    }
} 