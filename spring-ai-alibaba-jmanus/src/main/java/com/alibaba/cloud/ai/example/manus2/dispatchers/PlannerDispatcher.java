package com.alibaba.cloud.ai.example.manus2.dispatchers;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.FinalizerConstants;
import com.alibaba.cloud.ai.example.manus2.plan.ExecutionPlan;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.*;

public class PlannerDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState t) throws Exception {
        return HUMAN_ID;
    }

}
