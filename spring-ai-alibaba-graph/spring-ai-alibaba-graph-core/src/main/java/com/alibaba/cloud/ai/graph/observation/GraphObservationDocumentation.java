package com.alibaba.cloud.ai.graph.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

public enum GraphObservationDocumentation implements ObservationDocumentation {

    GRAPH_NODE_EXECUTION {
        @Override
        public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
            return GraphNodeObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeyNames.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityKeyNames.values();
        }

        @Override
        public Observation.Event[] getEvents() {
            return new Observation.Event[0];
        }
    };

    public static final String GRAPH_NODE_ID = "graph.node.id";
    public static final String GRAPH_EVENT = "graph.event";
    public static final String GRAPH_STATE = "graph.state";
    public static final String GRAPH_ERROR = "graph.error";

    public enum LowCardinalityKeyNames implements KeyName {
        GRAPH_NODE_ID {
            @Override
            public String asString() {
                return GRAPH_NODE_ID.asString();
            }
        },
        GRAPH_EVENT {
            @Override
            public String asString() {
                return GRAPH_EVENT.asString();
            }
        }
    }

    public enum HighCardinalityKeyNames implements KeyName {
        GRAPH_STATE {
            @Override
            public String asString() {
                return GRAPH_STATE.asString();
            }
        },
        GRAPH_ERROR {
            @Override
            public String asString() {
                return GRAPH_ERROR.asString();
            }
        }
    }
} 