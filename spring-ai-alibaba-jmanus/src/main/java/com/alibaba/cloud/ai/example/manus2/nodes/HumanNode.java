package com.alibaba.cloud.ai.example.manus2.nodes;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphInterruptException;

import java.util.Map;

public class HumanNode extends com.alibaba.cloud.ai.graph.node.HumanNode {


    @Override
    public Map<String, Object> apply(OverAllState state) throws GraphInterruptException {
        Map<String, Object> apply = super.apply(state);

        OverAllState.HumanFeedback humanFeedback = state.humanFeedback();

        return Map.of();
    }
}
