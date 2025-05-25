package com.alibaba.cloud.ai.example.manus2.nodes;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.CoordinatorConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

public class CoordinatorNode implements NodeAction {

    public static final String NODE_NAME = "Coordinator";

    @Override
    public Map<String, Object> apply(OverAllState t) throws Exception {
        return Map.of();
    }
}
